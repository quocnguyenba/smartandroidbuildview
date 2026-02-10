package com.quocnguyen.smartbuildview

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * Tool window that shows all modules with their build configuration files.
 * Double-click to open any file.
 */
class ModuleBuildFilesToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ModuleBuildFilesPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}

class ModuleBuildFilesPanel(private val project: Project) : JPanel(BorderLayout()) {

    companion object {
        private val CONFIG_FILES = listOf(
            "build.gradle.kts" to "Build Script (Kotlin)",
            "build.gradle" to "Build Script (Groovy)",
            "proguard-rules.pro" to "ProGuard Rules",
            "consumer-rules.pro" to "Consumer Rules"
        )
    }

    private val rootNode = DefaultMutableTreeNode("Modules")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)

    init {
        setupTree()
        add(JBScrollPane(tree), BorderLayout.CENTER)
        refreshTree()
    }

    private fun setupTree() {
        tree.isRootVisible = false
        tree.showsRootHandles = true
        
        tree.cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(
                tree: javax.swing.JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                val node = value as? DefaultMutableTreeNode ?: return
                val userObject = node.userObject

                when (userObject) {
                    is ModuleItem -> {
                        icon = AllIcons.Nodes.Module
                        append(userObject.displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                    }
                    is FileItem -> {
                        icon = AllIcons.FileTypes.Any_type
                        append(userObject.fileName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                        append(" (${userObject.label})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                    else -> {
                        append(value.toString())
                    }
                }
            }
        }

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val path = tree.getPathForLocation(e.x, e.y) ?: return
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                    val userObject = node.userObject
                    
                    if (userObject is FileItem) {
                        openFile(userObject.file)
                    }
                }
            }
        })
    }

    private fun refreshTree() {
        rootNode.removeAllChildren()

        val modules = ModuleManager.getInstance(project).modules
            .sortedBy { it.name }

        for (module in modules) {
            val moduleDir = module.moduleFile?.parent
                ?: ModuleRootManager.getInstance(module).contentRoots.firstOrNull()
                ?: continue

            // Check if this is a Gradle module
            val hasBuildFile = moduleDir.findChild("build.gradle") != null ||
                    moduleDir.findChild("build.gradle.kts") != null
            
            if (!hasBuildFile) continue

            val displayName = module.name.let { name ->
                // Simplify nested module names (e.g., "project.core.data" -> "core:data")
                if (name.contains(".")) {
                    name.substringAfter(".").replace(".", ":")
                } else {
                    name
                }
            }

            val moduleNode = DefaultMutableTreeNode(ModuleItem(displayName, module.name))
            var hasFiles = false

            var addedBuildGradle = false
            for ((fileName, label) in CONFIG_FILES) {
                if (fileName.startsWith("build.gradle") && addedBuildGradle) continue
                
                val file = moduleDir.findChild(fileName)
                if (file != null && !file.isDirectory) {
                    moduleNode.add(DefaultMutableTreeNode(FileItem(fileName, label, file)))
                    hasFiles = true
                    if (fileName.startsWith("build.gradle")) addedBuildGradle = true
                }
            }

            if (hasFiles) {
                rootNode.add(moduleNode)
            }
        }

        treeModel.reload()
        
        // Expand all modules
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }

    private fun openFile(file: VirtualFile) {
        FileEditorManager.getInstance(project).openFile(file, true)
    }

    data class ModuleItem(val displayName: String, val fullName: String)
    data class FileItem(val fileName: String, val label: String, val file: VirtualFile)
}
