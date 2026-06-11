package com.quocnguyen.smartbuildview

import com.intellij.icons.AllIcons
import com.intellij.ide.SelectInContext
import com.intellij.ide.SelectInTarget
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import javax.swing.Icon

/**
 * Marker interface to identify when our custom tree structure is active.
 * Used by [AndroidBuildTreeStructureProvider] to conditionally modify the tree.
 */
interface AndroidBuildViewSettings

/**
 * Custom Project View Pane that mimics the Android view but includes build files under each module.
 * Appears as "Android + Build" in the Project view dropdown.
 */
class AndroidBuildViewPane(project: Project) : ProjectViewPane(project), DumbAware {

    companion object {
        const val ID = "AndroidBuildView"
    }

    init {
        project.messageBus.connect(this).subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: MutableList<out VFileEvent>) {
                    if (events.any { it.isRelevantResourceCreateEvent() }) {
                        ApplicationManager.getApplication().invokeLater(
                            { ProjectView.getInstance(project).refresh() },
                            ModalityState.nonModal(),
                            project.disposed
                        )
                    }
                }
            }
        )
    }

    override fun getTitle(): String = "Android + Build"

    override fun getId(): String = ID

    override fun getIcon(): Icon = AllIcons.Nodes.Module

    override fun getWeight(): Int = 105 // Just after Android view (which is 100)

    // Hide Scratches and Consoles from this view
    override fun supportsShowScratchesAndConsoles(): Boolean = false

    /**
     * Only expose the "Android + Build" pane in the project view selector for projects
     * that actually contain Android modules. For non-Android projects this view adds no
     * value (and would hide regular folders), so we keep it out of the dropdown.
     *
     * Computed once and cached, since the project's Android-ness rarely changes at runtime.
     */
    override fun isInitiallyVisible(): Boolean {
        return isAndroidProjectCached
            ?: AndroidProjectDetector.isAndroidProject(myProject).also { isAndroidProjectCached = it }
    }

    private var isAndroidProjectCached: Boolean? = null

    private fun VFileEvent.isRelevantResourceCreateEvent(): Boolean {
        if (this !is VFileCreateEvent && this !is VFileCopyEvent) return false
        val createdFile = file ?: return false
        if (createdFile.isDirectory) return false

        val fileIndex = ProjectFileIndex.getInstance(myProject)
        return fileIndex.isInContent(createdFile) && isUnderAndroidResourceOrAssetRoot(createdFile)
    }

    private fun isUnderAndroidResourceOrAssetRoot(file: VirtualFile): Boolean {
        var current = file.parent
        while (current != null) {
            val parent = current.parent
            if (parent?.name == "main" && current.name in setOf("res", "assets")) {
                return true
            }
            current = parent
        }
        return false
    }

    protected inner class AndroidBuildTreeStructure : ProjectViewPaneTreeStructure(), AndroidBuildViewSettings

    override fun createStructure(): ProjectAbstractTreeStructureBase {
        return AndroidBuildTreeStructure()
    }

    /**
     * Create a custom SelectInTarget to enable "Select Opened File" functionality.
     * This allows users to quickly locate the currently opened file in the tree view.
     */
    override fun createSelectInTarget(): SelectInTarget {
        return object : SelectInTarget {
            override fun canSelect(context: SelectInContext): Boolean {
                // Check if the file belongs to this project and is a valid file
                return context.project == myProject && context.virtualFile.isValid
            }

            override fun selectIn(context: SelectInContext, requestFocus: Boolean) {
                // Convert the virtual file to PSI and select it in the tree
                val virtualFile = context.virtualFile
                val psiFile = PsiManager.getInstance(myProject).findFile(virtualFile)
                
                // Select using the PSI element if available, otherwise use the virtual file
                val toSelect = psiFile ?: virtualFile
                select(toSelect, virtualFile, requestFocus)
            }

            override fun getToolWindowId(): String = "Project"

            override fun getMinorViewId(): String = ID

            override fun getWeight(): Float = this@AndroidBuildViewPane.weight.toFloat()

            override fun toString(): String = getTitle()
        }
    }
}

/**
 * Detects whether a project should be treated as an Android project.
 *
 * Kept as a top-level object (rather than in an extension implementation's companion)
 * so it can be shared by both the project view pane and the settings tool window.
 */
object AndroidProjectDetector {

    /**
     * Returns true if the project contains at least one Android module.
     *
     * Uses lightweight, index-free heuristics so it can run safely during project view
     * setup: a module is considered Android if it has an `AndroidManifest.xml` under
     * `src/main`, or its build script applies/declares the Android Gradle plugin (covers
     * the `com.android.*` ids and the `android { }` configuration block, including
     * version-catalog `alias(...)` usages).
     */
    fun isAndroidProject(project: Project): Boolean {
        return ModuleManager.getInstance(project).modules.any { module ->
            val dir = ModuleRootManager.getInstance(module).contentRoots.firstOrNull()
                ?: return@any false

            // Strong signal: a manifest in the standard Android source layout.
            if (dir.findFileByRelativePath("src/main/AndroidManifest.xml") != null) return@any true

            val buildFile = dir.findChild("build.gradle.kts")
                ?: dir.findChild("build.gradle")
                ?: return@any false

            try {
                val content = String(buildFile.contentsToByteArray())
                content.contains("com.android") ||
                    Regex("(^|\\s)android\\s*\\{").containsMatchIn(content)
            } catch (_: Exception) {
                false
            }
        }
    }
}
