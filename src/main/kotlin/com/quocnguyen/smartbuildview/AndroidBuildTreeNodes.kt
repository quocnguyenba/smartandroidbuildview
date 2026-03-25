package com.quocnguyen.smartbuildview

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ModuleGroup
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

/**
 * Module node that shows Android-style source groupings with config files AT THE BOTTOM.
 * Uses explicit sort keys to maintain order.
 */
class AndroidBuildModuleNode(
    project: Project,
    private val module: Module,
    private val settings: ViewSettings?
) : ProjectViewNode<Module>(project, module, settings) {

    companion object {
        val KNOWN_CONFIG_FILES = setOf(
            "build.gradle.kts", "build.gradle", 
            "proguard-rules.pro", "consumer-rules.pro",
            ".gitignore", "gradle.properties", "local.properties"
        )

        /**
         * Checks if a module is an Android application module by looking for
         * the com.android.application plugin in its build.gradle file.
         */
        fun isAndroidAppModule(moduleDir: VirtualFile?): Boolean {
            if (moduleDir == null) return false
            val buildFile = moduleDir.findChild("build.gradle.kts") 
                ?: moduleDir.findChild("build.gradle")
                ?: return false
            
            return try {
                val content = String(buildFile.contentsToByteArray())
                content.contains("com.android.application") || 
                content.contains("id(\"com.android.application\")") ||
                content.contains("id 'com.android.application'")
            } catch (_: Exception) {
                false
            }
        }

        /**
         * Checks if a module is an Android library module.
         */
        fun isAndroidLibraryModule(moduleDir: VirtualFile?): Boolean {
            if (moduleDir == null) return false
            val buildFile = moduleDir.findChild("build.gradle.kts") 
                ?: moduleDir.findChild("build.gradle")
                ?: return false
            
            return try {
                val content = String(buildFile.contentsToByteArray())
                content.contains("com.android.library") || 
                content.contains("id(\"com.android.library\")") ||
                content.contains("id 'com.android.library'")
            } catch (e: Exception) {
                false
            }
        }

        // Cached Android icons loaded via reflection (to work without Android plugin dependency)
        private val androidAppIcon: Icon? by lazy {
            loadStudioIcon("Shell", "Filetree", "ANDROID_MODULE")
        }

        private val androidLibraryIcon: Icon? by lazy {
            loadStudioIcon("Shell", "Filetree", "LIBRARY_MODULE")
        }

        /**
         * Loads a StudioIcon via reflection. Returns null if not available.
         * Path: icons.StudioIcons.$category.$subcategory.$iconName
         */
        private fun loadStudioIcon(category: String, subcategory: String, iconName: String): Icon? {
            return try {
                val studioIconsClass = Class.forName("icons.StudioIcons")
                val categoryClass = studioIconsClass.classes.find { it.simpleName == category }
                val subcategoryClass = categoryClass?.classes?.find { it.simpleName == subcategory }
                val field = subcategoryClass?.getField(iconName)
                field?.get(null) as? Icon
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Gets the appropriate icon for a module based on its type.
         * Uses Android-specific icons (green Android for app, library icon for libraries).
         * Falls back to standard module icon if Android icons are not available.
         */
        fun getModuleIcon(moduleDir: VirtualFile?): Icon {
            return when {
                isAndroidAppModule(moduleDir) -> androidAppIcon ?: AllIcons.Nodes.Module
                isAndroidLibraryModule(moduleDir) -> androidLibraryIcon ?: AllIcons.Nodes.Module
                else -> AllIcons.Nodes.Module
            }
        }
    }

    override fun update(presentation: PresentationData) {
        val displayName = module.name.substringAfterLast(".")
        presentation.clearText()
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        presentation.setIcon(getModuleIcon(getModuleDir()))
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val moduleDir = getModuleDir() ?: return emptyList()
        val psiManager = PsiManager.getInstance(project)
        val fileSettings = ModuleFilesSettings.getInstance(project)
        
        val children = mutableListOf<AbstractTreeNode<*>>()

        // Check if this is a Gradle module (has build.gradle)
        val isGradleModule = moduleDir.findChild("build.gradle") != null ||
                            moduleDir.findChild("build.gradle.kts") != null

        // === SOURCE FOLDERS (sortKey: 00-09) ===
        // Only show Android-style source folders for Gradle modules

        if (fileSettings.showKotlinJava) {
            val javaSources = findSourceDirs(moduleDir, listOf("src/main/java", "src/main/kotlin"))
            if (javaSources.isNotEmpty()) {
                children.add(SortedSourceFolderGroupNode(project, "kotlin+java", AllIcons.Modules.SourceRoot, javaSources, settings, "00"))
            }
        }

        if (fileSettings.showRes) {
            val resDirs = findSourceDirs(moduleDir, listOf("src/main/res"))
            if (resDirs.isNotEmpty()) {
                children.add(SortedSourceFolderGroupNode(project, "res", AllIcons.Modules.ResourcesRoot, resDirs, settings, "01"))
            }
        }

        if (fileSettings.showAssets) {
            val assetsDirs = findSourceDirs(moduleDir, listOf("src/main/assets"))
            if (assetsDirs.isNotEmpty()) {
                children.add(SortedSourceFolderGroupNode(project, "assets", AllIcons.Modules.ResourcesRoot, assetsDirs, settings, "02"))
            }
        }

        // Test sources
        if (fileSettings.showTestSources) {
            val testSources = findSourceDirs(moduleDir, listOf("src/test/java", "src/test/kotlin"))
            if (testSources.isNotEmpty()) {
                children.add(SortedSourceFolderGroupNode(project, "test", AllIcons.Modules.TestRoot, testSources, settings, "03"))
            }
        }

        if (fileSettings.showAndroidTestSources) {
            val androidTestSources = findSourceDirs(moduleDir, listOf("src/androidTest/java", "src/androidTest/kotlin"))
            if (androidTestSources.isNotEmpty()) {
                children.add(SortedSourceFolderGroupNode(project, "androidTest", AllIcons.Modules.TestRoot, androidTestSources, settings, "04"))
            }
        }

        // Show all directories when showAllFolders is enabled
        val shouldShowAllFolders = !isGradleModule || fileSettings.showAllFolders

        // Generated folders - build folder only (only for Gradle modules)
        if (isGradleModule && fileSettings.showGeneratedFolders) {
            moduleDir.findChild("build")?.let { dir ->
                if (dir.isDirectory) {
                    psiManager.findDirectory(dir)?.let { psiDir ->
                        children.add(SortedDirNode(project, psiDir, settings, "05"))
                    }
                }
            }
        }

        // === ALL DIRECTORIES (sortKey: 10-39 for non-Gradle, 40-49 for additional) ===
        if (shouldShowAllFolders) {
            val excludedDirs = mutableSetOf("src", "build")
            // For non-Gradle modules showing all content, don't exclude as much
            if (!isGradleModule) {
                excludedDirs.clear()
                excludedDirs.add("build")  // Still exclude build directory
            }
            
            val allDirs = moduleDir.children
                .filter { it.isDirectory }
                .filter { it.name !in excludedDirs }
                .sortedBy { it.name }

            // Use different sort key ranges based on context
            val sortKeyPrefix = if (!isGradleModule) "10" else "40"
            allDirs.forEachIndexed { index, dir ->
                // Skip if already added as a source folder (for Gradle modules)
                if (isGradleModule && dir.name == "src") return@forEachIndexed
                
                psiManager.findDirectory(dir)?.let { psiDir ->
                    children.add(SortedDirNode(project, psiDir, settings, "${sortKeyPrefix}_${String.format("%03d", index)}"))
                }
            }
        }

        // === CONFIG AND OTHER FILES ===
        
        // .gitignore files
        if (fileSettings.showGitignore) {
            moduleDir.findChild(".gitignore")?.let { file ->
                if (!file.isDirectory) {
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "88"))
                    }
                }
            }
        }

        // Other files (non-config files)
        if (fileSettings.showOtherFiles) {
            val configFilesToExclude = KNOWN_CONFIG_FILES.toMutableSet()
            if (!fileSettings.showGitignore) {
                configFilesToExclude.add(".gitignore")
            }
            
            val otherFiles = moduleDir.children
                .filter { !it.isDirectory }
                .filter { it.name !in configFilesToExclude }
                .filter { !it.name.startsWith("BuildConfig") }
                .sortedBy { it.name }
            
            otherFiles.forEachIndexed { index, file ->
                psiManager.findFile(file)?.let { psiFile ->
                    children.add(SortedFileNode(project, psiFile, settings, null, "55_${String.format("%03d", index)}"))
                }
            }
        }

        // === CONFIG FILES (sortKey: 90-99, BuildConfig always 99) ===
        // Only show these for Gradle modules
        
        if (isGradleModule) {
            // AndroidManifest.xml - direct file at same level as build.gradle
            if (fileSettings.showManifests) {
                findManifestFile(moduleDir)?.let { manifestFile ->
                    psiManager.findFile(manifestFile)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "90"))
                    }
                }
            }

            if (fileSettings.showBuildGradle) {
                val buildGradleFile = moduleDir.findChild("build.gradle.kts") 
                    ?: moduleDir.findChild("build.gradle")
                buildGradleFile?.let { file ->
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "91"))
                    }
                }
            }

            if (fileSettings.showProguardRules) {
                moduleDir.findChild("proguard-rules.pro")?.let { file ->
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "92"))
                    }
                }
            }

            if (fileSettings.showConsumerRules) {
                moduleDir.findChild("consumer-rules.pro")?.let { file ->
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "93"))
                    }
                }
            }

            // BuildConfig.java - ALWAYS LAST with sortKey 99
            if (fileSettings.showBuildConfig) {
                findBuildConfig(moduleDir)?.let { buildConfigFile ->
                    psiManager.findFile(buildConfigFile)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, "generated", "99"))
                    }
                }
            }
        }

        // Sort children by their sort keys
        return children.sortedBy { node ->
            when (node) {
                is SortedSourceGroupNode -> node.sortKey
                is SortedSourceFolderGroupNode -> node.sortKey
                is SortedFileNode -> node.sortKey
                is SortedDirNode -> node.sortKey
                else -> "ZZ"
            }
        }
    }

    /**
     * Gets the module directory using public API.
     * Uses ModuleRootManager to get content roots (the recommended public API approach).
     */
    private fun getModuleDir(): VirtualFile? {
        val contentRoots = ModuleRootManager.getInstance(module).contentRoots
        return contentRoots.firstOrNull()
    }

    private fun findManifestFile(moduleDir: VirtualFile): VirtualFile? {
        return moduleDir.findFileByRelativePath("src/main/AndroidManifest.xml")
    }

    private fun findSourceDirs(moduleDir: VirtualFile, relativePaths: List<String>): List<VirtualFile> {
        return relativePaths.mapNotNull { path ->
            moduleDir.findFileByRelativePath(path)
        }.filter { it.isDirectory }
    }

    private fun findBuildConfig(moduleDir: VirtualFile): VirtualFile? {
        val paths = listOf(
            "build/generated/source/buildConfig/debug",
            "build/generated/source/buildConfig/release"
        )
        for (path in paths) {
            val dir = moduleDir.findFileByRelativePath(path) ?: continue
            return findFileRecursively(dir, "BuildConfig.java")
        }
        return null
    }

    private fun findFileRecursively(dir: VirtualFile, fileName: String): VirtualFile? {
        for (child in dir.children) {
            if (child.name == fileName) return child
            if (child.isDirectory) {
                val found = findFileRecursively(child, fileName)
                if (found != null) return found
            }
        }
        return null
    }

    override fun getVirtualFile(): VirtualFile? = getModuleDir()

    /**
     * Exposes the module content-root directory so that the project view pane's
     * getElementsFromNode can convert it into a PsiDirectory. Without this, the
     * data-context has no PSI_ELEMENT and IDE actions (New Module, New Kotlin File,
     * etc.) are hidden for this node.
     */
    override fun getRoots(): Collection<VirtualFile> {
        val dir = getModuleDir() ?: return emptyList()
        return listOf(dir)
    }

    override fun contains(file: VirtualFile): Boolean {
        val moduleDir = getModuleDir() ?: return false
        return file.path.startsWith(moduleDir.path)
    }
}

// === SORTED NODES WITH EXPLICIT SORT KEYS ===

class SortedSourceGroupNode(
    project: Project,
    private val name: String,
    private val icon: Icon,
    private val files: List<PsiFile>,
    private val settings: ViewSettings?,
    val sortKey: String
) : ProjectViewNode<String>(project, name, settings) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = name
        presentation.setIcon(icon)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        return files.map { PsiFileNode(project, it, settings) }
    }

    override fun contains(file: VirtualFile): Boolean = files.any { it.virtualFile == file }
    override fun getSortKey(): Comparable<*> = sortKey
}

class SortedSourceFolderGroupNode(
    project: Project,
    private val name: String,
    private val icon: Icon,
    private val sourceDirs: List<VirtualFile>,
    private val settings: ViewSettings?,
    val sortKey: String
) : ProjectViewNode<String>(project, name, settings) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = name
        presentation.setIcon(icon)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val psiManager = PsiManager.getInstance(project)
        val children = mutableListOf<AbstractTreeNode<*>>()

        for (dir in sourceDirs) {
            val psiDir = psiManager.findDirectory(dir) ?: continue
            // Folders first (sort key "0_name"), then files (sort key "1_name")
            children.addAll(psiDir.subdirectories.sortedBy { it.name }.map { 
                SortedCompactPackageNode(project, it, settings, "0_${it.name}") 
            })
            children.addAll(psiDir.files.sortedBy { it.name }.map { 
                SortedPsiFileNode(project, it, settings, "1_${it.name}") 
            })
        }
        return children.sortedBy { node ->
            when (node) {
                is SortedCompactPackageNode -> node.sortKey
                is SortedPsiFileNode -> node.sortKey
                else -> "Z_${node.name}"
            }
        }
    }

    override fun contains(file: VirtualFile): Boolean = sourceDirs.any { file.path.startsWith(it.path) }
    override fun getSortKey(): Comparable<*> = sortKey
}

class SortedFileNode(
    project: Project,
    psiFile: PsiFile,
    settings: ViewSettings?,
    private val label: String?,
    val sortKey: String
) : PsiFileNode(project, psiFile, settings) {

    override fun update(presentation: PresentationData) {
        super.update(presentation)
        if (label != null) {
            val file = value ?: return
            presentation.clearText()
            presentation.addText(file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
            presentation.addText(" ($label)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }

    override fun getSortKey(): Comparable<*> = sortKey
}

class SortedDirNode(
    project: Project,
    private val dir: PsiDirectory,
    private val settings: ViewSettings?,
    val sortKey: String
) : ProjectViewNode<PsiDirectory>(project, dir, settings) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = dir.name
        presentation.setIcon(AllIcons.Nodes.Folder)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val children = mutableListOf<AbstractTreeNode<*>>()
        // Folders first (sort key "0_name"), then files (sort key "1_name")
        children.addAll(dir.subdirectories.sortedBy { it.name }.map { 
            SortedPsiDirNode(project, it, settings, "0_${it.name}") 
        })
        children.addAll(dir.files.sortedBy { it.name }.map { 
            SortedPsiFileNode(project, it, settings, "1_${it.name}") 
        })
        return children.sortedBy { node ->
            when (node) {
                is SortedPsiDirNode -> node.sortKey
                is SortedPsiFileNode -> node.sortKey
                else -> "Z_${node.name}"
            }
        }
    }

    override fun contains(file: VirtualFile): Boolean = file.path.startsWith(dir.virtualFile.path)
    override fun getSortKey(): Comparable<*> = sortKey
    override fun getTypeSortWeight(sortByType: Boolean): Int = 10  // Directories have lower weight
}

// === ORIGINAL NODES (for compatibility) ===

class AndroidBuildModuleWithChildrenNode(
    project: Project,
    private val module: Module,
    private val displayName: String,
    private val childHierarchyNodes: List<ModuleHierarchyNode>,
    private val settings: ViewSettings?
) : ProjectViewNode<Module>(project, module, settings) {

    override fun update(presentation: PresentationData) {
        presentation.clearText()
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        // Parent module with children - use Android-specific icon (same as Android view)
        presentation.setIcon(AndroidBuildModuleNode.getModuleIcon(getModuleDir()))
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val moduleDir = getModuleDir()
        val psiManager = PsiManager.getInstance(project)
        val fileSettings = ModuleFilesSettings.getInstance(project)
        
        val children = mutableListOf<AbstractTreeNode<*>>()

        // Child modules first
        children.addAll(childHierarchyNodes.sortedBy { it.displayName }.map { it.toTreeNode() })

        if (moduleDir != null) {
            // Source folders
            if (fileSettings.showKotlinJava) {
                val javaSources = listOf("src/main/java", "src/main/kotlin").mapNotNull { 
                    moduleDir.findFileByRelativePath(it) 
                }.filter { it.isDirectory }
                if (javaSources.isNotEmpty()) {
                    children.add(SortedSourceFolderGroupNode(project, "kotlin+java", AllIcons.Modules.SourceRoot, javaSources, settings, "10"))
                }
            }

            if (fileSettings.showRes) {
                moduleDir.findFileByRelativePath("src/main/res")?.let { resDir ->
                    if (resDir.isDirectory) {
                        children.add(SortedSourceFolderGroupNode(project, "res", AllIcons.Modules.ResourcesRoot, listOf(resDir), settings, "11"))
                    }
                }
            }

            if (fileSettings.showAssets) {
                moduleDir.findFileByRelativePath("src/main/assets")?.let { assetsDir ->
                    if (assetsDir.isDirectory) {
                        children.add(SortedSourceFolderGroupNode(project, "assets", AllIcons.Modules.ResourcesRoot, listOf(assetsDir), settings, "12"))
                    }
                }
            }

            // Test sources
            if (fileSettings.showTestSources) {
                val testSources = listOf("src/test/java", "src/test/kotlin").mapNotNull { 
                    moduleDir.findFileByRelativePath(it) 
                }.filter { it.isDirectory }
                if (testSources.isNotEmpty()) {
                    children.add(SortedSourceFolderGroupNode(project, "test", AllIcons.Modules.TestRoot, testSources, settings, "13"))
                }
            }

            if (fileSettings.showAndroidTestSources) {
                val androidTestSources = listOf("src/androidTest/java", "src/androidTest/kotlin").mapNotNull { 
                    moduleDir.findFileByRelativePath(it) 
                }.filter { it.isDirectory }
                if (androidTestSources.isNotEmpty()) {
                    children.add(SortedSourceFolderGroupNode(project, "androidTest", AllIcons.Modules.TestRoot, androidTestSources, settings, "14"))
                }
            }

            // Generated folders - build folder only
            if (fileSettings.showGeneratedFolders) {
                moduleDir.findChild("build")?.let { dir ->
                    if (dir.isDirectory) {
                        psiManager.findDirectory(dir)?.let { psiDir ->
                            children.add(SortedDirNode(project, psiDir, settings, "15"))
                        }
                    }
                }
            }

            // Config files - AndroidManifest at same level as build.gradle
            if (fileSettings.showManifests) {
                moduleDir.findFileByRelativePath("src/main/AndroidManifest.xml")?.let { manifestFile ->
                    psiManager.findFile(manifestFile)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "90"))
                    }
                }
            }

            if (fileSettings.showBuildGradle) {
                (moduleDir.findChild("build.gradle.kts") ?: moduleDir.findChild("build.gradle"))?.let { file ->
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "91"))
                    }
                }
            }

            if (fileSettings.showProguardRules) {
                moduleDir.findChild("proguard-rules.pro")?.let { file ->
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "92"))
                    }
                }
            }

            if (fileSettings.showConsumerRules) {
                moduleDir.findChild("consumer-rules.pro")?.let { file ->
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(SortedFileNode(project, psiFile, settings, null, "93"))
                    }
                }
            }
        }

        return children.sortedBy { node ->
            when (node) {
                is SortedSourceGroupNode -> node.sortKey
                is SortedSourceFolderGroupNode -> node.sortKey
                is SortedFileNode -> node.sortKey
                else -> "ZZ"
            }
        }
    }

    /**
     * Gets the module directory using public API.
     * Uses ModuleRootManager to get content roots (the recommended public API approach).
     */
    private fun getModuleDir(): VirtualFile? {
        val contentRoots = ModuleRootManager.getInstance(module).contentRoots
        return contentRoots.firstOrNull()
    }

    override fun getVirtualFile(): VirtualFile? = getModuleDir()

    /**
     * Exposes the module content-root directory so that the project view pane's
     * getElementsFromNode can convert it into a PsiDirectory. Without this, the
     * data-context has no PSI_ELEMENT and IDE actions (New Module, New Kotlin File,
     * etc.) are hidden for this node.
     */
    override fun getRoots(): Collection<VirtualFile> {
        val dir = getModuleDir() ?: return emptyList()
        return listOf(dir)
    }

    override fun contains(file: VirtualFile): Boolean {
        val moduleDir = getModuleDir() ?: return false
        return file.path.startsWith(moduleDir.path)
    }
}

// Wrapper not needed - child modules are sorted directly

/**
 * Pure grouping node for multi-layer module hierarchies (e.g. a `feature` directory
 * that contains child modules but has no `build.gradle` of its own).
 *
 * The value type is [ModuleGroup] so that IntelliJ's data-context extraction recognises
 * it for `ModuleGroup.ARRAY_DATA_KEY`. This makes the **New > Module** action visible
 * when the user right-clicks this node (see NewModuleInGroupAction).
 */
class ModuleGroupNode(
    project: Project,
    private val displayName: String,
    private val childHierarchyNodes: List<ModuleHierarchyNode>,
    settings: ViewSettings?,
    /** The filesystem directory this group node corresponds to (e.g. `feature/`). */
    private val groupVirtualFile: VirtualFile? = null,
    moduleGroup: ModuleGroup
) : ProjectViewNode<ModuleGroup>(project, moduleGroup, settings) {

    override fun update(presentation: PresentationData) {
        presentation.clearText()
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        // Pure grouping node (no module content) - use Module icon
        presentation.setIcon(AllIcons.Nodes.Module)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        return childHierarchyNodes.sortedBy { it.displayName }.map { it.toTreeNode() }
    }

    override fun getVirtualFile(): VirtualFile? = groupVirtualFile

    /**
     * Exposes the group directory so that the project view pane's getElementsFromNode
     * can convert it into a PsiDirectory. This populates PSI_ELEMENT in the data-context,
     * enabling IDE actions (New Kotlin File, etc.) for this node.
     */
    override fun getRoots(): Collection<VirtualFile> {
        return if (groupVirtualFile != null) listOf(groupVirtualFile) else emptyList()
    }

    override fun contains(file: VirtualFile): Boolean {
        // Check if any child hierarchy node contains this file
        return children.any { childNode ->
            (childNode as? ProjectViewNode<*>)?.contains(file) == true
        }
    }
}

class RootProjectFilesNode(
    project: Project,
    private val settings: ViewSettings?
) : ProjectViewNode<String>(project, "Gradle Scripts", settings) {

    companion object {
        val ROOT_FILES = listOf(
            ".gitignore", "build.gradle.kts", "build.gradle",
            "gradle.properties", "local.properties", "README.md",
            "settings.gradle.kts", "settings.gradle"
        )
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = "Gradle Scripts"
        presentation.setIcon(AllIcons.Nodes.ConfigFolder)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val basePath = project.basePath ?: return emptyList()
        val psiManager = PsiManager.getInstance(project)
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath)
            ?: return emptyList()

        val children = mutableListOf<AbstractTreeNode<*>>()
        val addedFiles = mutableSetOf<String>()

        for (fileName in ROOT_FILES) {
            val baseName = fileName.removeSuffix(".kts")
            if (baseName != fileName && addedFiles.contains(baseName)) continue
            if (addedFiles.contains("$fileName.kts")) continue

            baseDir.findChild(fileName)?.let { file ->
                if (!file.isDirectory) {
                    psiManager.findFile(file)?.let { psiFile ->
                        children.add(PsiFileNode(project, psiFile, settings))
                        addedFiles.add(fileName)
                    }
                }
            }
        }

        // libs.versions.toml (Gradle version catalog) - typically in gradle/ directory
        baseDir.findFileByRelativePath("gradle/libs.versions.toml")?.let { libsToml ->
            psiManager.findFile(libsToml)?.let { psiFile ->
                children.add(PsiFileNode(project, psiFile, settings))
            }
        }

        // Global gradle.properties
        val userHome = System.getProperty("user.home")
        LocalFileSystem.getInstance()
            .findFileByPath("$userHome/.gradle/gradle.properties")?.let { globalProps ->
                psiManager.findFile(globalProps)?.let { psiFile ->
                    children.add(GlobalGradlePropertiesNode(project, psiFile, settings))
                }
            }

        return children
    }

    override fun contains(file: VirtualFile): Boolean {
        // Check if this file is one of the root project files
        val project = myProject ?: return false
        val basePath = project.basePath ?: return false
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return false
        
        // Check if the file is in the root project directory
        if (file.parent != baseDir && file.parent?.parent != baseDir) return false
        
        // Check if it's one of the known root files
        val fileName = file.name
        return ROOT_FILES.any { it == fileName || "$it.kts" == fileName } ||
               file.path.endsWith("gradle/libs.versions.toml") ||
               file.path.contains(".gradle/gradle.properties")
    }
    override fun getSortKey(): Comparable<*> = "ZZZZ"
    override fun getWeight(): Int = 2000
}

class GlobalGradlePropertiesNode(
    project: Project,
    psiFile: PsiFile,
    settings: ViewSettings?
) : PsiFileNode(project, psiFile, settings) {
    override fun update(presentation: PresentationData) {
        super.update(presentation)
        presentation.clearText()
        presentation.addText("gradle.properties", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.addText(" (Global)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }
}

/**
 * Compact package node with explicit sort key for proper folder-first ordering.
 * Collapses single-child-directory chains into a dotted display name (e.g. "com.a.b.c").
 *
 * IMPORTANT: The superclass [ProjectViewNode] is constructed with [computeFinalDir] so that
 * [getValue] returns the deepest directory in the chain. This ensures "New File/Class" IDE
 * actions target the correct package directory (e.g. com/a/b/c) instead of the root (com).
 * [rootDir] is kept separately only for [contains] checks that must cover the whole chain.
 */
class SortedCompactPackageNode(
    project: Project,
    private val rootDir: PsiDirectory,
    private val settings: ViewSettings?,
    val sortKey: String
) : ProjectViewNode<PsiDirectory>(project, computeFinalDir(rootDir), settings) {

    // Eagerly compute the dotted display name (e.g. "com.a.b.c")
    private val compactedName: String = computeCompactedName(rootDir)

    companion object {
        /**
         * Traverses single-child directory chains and returns the deepest directory.
         * e.g. com → a → b → c (each with one child subdir) returns the "c" directory.
         */
        fun computeFinalDir(dir: PsiDirectory): PsiDirectory {
            var current = dir
            while (true) {
                val subdirs = current.subdirectories
                val files = current.files
                if (files.isNotEmpty() || subdirs.size != 1) break
                current = subdirs.first()
            }
            return current
        }

        /**
         * Returns the dotted display name for a compact chain (e.g. "com.a.b.c").
         */
        fun computeCompactedName(dir: PsiDirectory): String {
            val pathParts = mutableListOf(dir.name)
            var current = dir
            while (true) {
                val subdirs = current.subdirectories
                val files = current.files
                if (files.isNotEmpty() || subdirs.size != 1) break
                current = subdirs.first()
                pathParts.add(current.name)
            }
            return pathParts.joinToString(".")
        }
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = compactedName
        presentation.setIcon(AllIcons.Nodes.Package)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        // value == finalDir (the deepest directory passed to the superclass constructor)
        val finalDir = value
        val children = mutableListOf<AbstractTreeNode<*>>()

        // Folders first (sort key "0_name"), then files (sort key "1_name")
        children.addAll(finalDir.subdirectories.sortedBy { it.name }.map {
            SortedCompactPackageNode(project, it, settings, "0_${it.name}")
        })
        children.addAll(finalDir.files.sortedBy { it.name }.map {
            SortedPsiFileNode(project, it, settings, "1_${it.name}")
        })

        return children.sortedBy { node ->
            when (node) {
                is SortedCompactPackageNode -> node.sortKey
                is SortedPsiFileNode -> node.sortKey
                else -> "Z_${node.name}"
            }
        }
    }

    // rootDir covers the entire compact chain, so contains() works for all files within it
    override fun contains(file: VirtualFile): Boolean =
        file.path.startsWith(rootDir.virtualFile.path)

    override fun getSortKey(): Comparable<*> = sortKey
    override fun getTypeSortWeight(sortByType: Boolean): Int = 10  // Directories have lower weight
}

/**
 * Directory node with explicit sort key for proper folder-first ordering.
 */
class SortedPsiDirNode(
    project: Project,
    private val dir: PsiDirectory,
    private val settings: ViewSettings?,
    val sortKey: String
) : ProjectViewNode<PsiDirectory>(project, dir, settings) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = dir.name
        presentation.setIcon(AllIcons.Nodes.Folder)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val children = mutableListOf<AbstractTreeNode<*>>()
        // Folders first (sort key "0_name"), then files (sort key "1_name")
        children.addAll(dir.subdirectories.sortedBy { it.name }.map { 
            SortedPsiDirNode(project, it, settings, "0_${it.name}") 
        })
        children.addAll(dir.files.sortedBy { it.name }.map { 
            SortedPsiFileNode(project, it, settings, "1_${it.name}") 
        })
        return children.sortedBy { node ->
            when (node) {
                is SortedPsiDirNode -> node.sortKey
                is SortedPsiFileNode -> node.sortKey
                else -> "Z_${node.name}"
            }
        }
    }

    override fun contains(file: VirtualFile): Boolean = file.path.startsWith(dir.virtualFile.path)
    override fun getSortKey(): Comparable<*> = sortKey
    override fun getTypeSortWeight(sortByType: Boolean): Int = 10  // Directories have lower weight
}

/**
 * File node with explicit sort key for proper folder-first ordering.
 */
class SortedPsiFileNode(
    project: Project,
    psiFile: PsiFile,
    settings: ViewSettings?,
    val sortKey: String
) : PsiFileNode(project, psiFile, settings) {
    override fun getSortKey(): Comparable<*> = sortKey
    override fun getTypeSortWeight(sortByType: Boolean): Int = 20  // Files have higher weight than directories
}

/**
 * External Libraries node that shows project dependencies.
 * This delegates to the standard external libraries view from the project structure.
 */
class ExternalLibrariesNode(
    project: Project,
    private val settings: ViewSettings?
) : ProjectViewNode<String>(project, "External Libraries", settings) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = "External Libraries"
        presentation.setIcon(AllIcons.Nodes.PpLibFolder)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val allModules = ModuleManager.getInstance(project).modules

        // Collect unique libraries, keyed by name, preserving the Library object
        val uniqueLibraries = mutableMapOf<String, Library>()

        for (module in allModules) {
            val rootManager = ModuleRootManager.getInstance(module)
            for (orderEntry in rootManager.orderEntries) {
                if (orderEntry is LibraryOrderEntry) {
                    val library = orderEntry.library
                    val libraryName = orderEntry.libraryName
                    if (library != null && libraryName != null) {
                        uniqueLibraries[libraryName] = library
                    }
                }
            }
        }

        return uniqueLibraries.entries
            .sortedBy { it.key }
            .map { (name, library) -> LibraryNode(project, name, library, settings) }
    }

    override fun contains(file: VirtualFile): Boolean = false
    override fun getSortKey(): Comparable<*> = "ZZZZZ"  // After Gradle Scripts
    override fun getWeight(): Int = 3000  // Heavy weight to ensure it's last
}

/**
 * Node representing a named library (e.g. a Gradle/Maven dependency).
 *
 * When expanded it shows one child [LibraryClassRootNode] per JAR/class-root that belongs to
 * the library.  If the library has exactly one root its packages are surfaced directly as
 * children, skipping the intermediate JAR node for a cleaner tree.
 */
class LibraryNode(
    project: Project,
    private val libraryName: String,
    private val library: Library,
    private val settings: ViewSettings?
) : ProjectViewNode<String>(project, libraryName, settings) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = libraryName
        presentation.setIcon(AllIcons.Nodes.PpLib)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val classRoots = library.getFiles(OrderRootType.CLASSES)

        return when {
            classRoots.isEmpty() -> emptyList()
            classRoots.size == 1 -> {
                // Single JAR – inline the package tree directly under the library node
                buildPackageChildren(project, classRoots[0])
            }
            else -> {
                // Multiple JARs – show one LibraryClassRootNode per JAR
                val psiManager = PsiManager.getInstance(project)
                classRoots.mapNotNull { root ->
                    psiManager.findDirectory(root)?.let { psiDir ->
                        // Use the JAR file name (parent of the "!/" virtual root)
                        val jarName = root.parent?.name ?: root.name
                        LibraryClassRootNode(project, jarName, psiDir, settings)
                    }
                }
            }
        }
    }

    /**
     * Builds package/class children directly from a class root [VirtualFile].
     * Sub-directories are shown as compact package nodes; top-level files as file nodes.
     */
    private fun buildPackageChildren(
        project: Project,
        root: VirtualFile
    ): List<AbstractTreeNode<*>> {
        val psiManager = PsiManager.getInstance(project)
        val psiDir = psiManager.findDirectory(root) ?: return emptyList()
        val children = mutableListOf<AbstractTreeNode<*>>()

        psiDir.subdirectories.sortedBy { it.name }.forEachIndexed { i, subDir ->
            children.add(SortedCompactPackageNode(project, subDir, settings, "0_${String.format("%03d", i)}_${subDir.name}"))
        }
        psiDir.files.sortedBy { it.name }.forEachIndexed { i, file ->
            children.add(SortedPsiFileNode(project, file, settings, "1_${String.format("%03d", i)}_${file.name}"))
        }
        return children
    }

    override fun contains(file: VirtualFile): Boolean = false
}

/**
 * Represents a single JAR / class-root belonging to a library.
 * Its children are the top-level packages and classes inside that root.
 */
class LibraryClassRootNode(
    project: Project,
    private val rootName: String,
    private val rootDir: PsiDirectory,
    private val settings: ViewSettings?
) : ProjectViewNode<PsiDirectory>(project, rootDir, settings) {

    override fun update(presentation: PresentationData) {
        presentation.presentableText = rootName
        presentation.setIcon(AllIcons.Nodes.PpJar)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val children = mutableListOf<AbstractTreeNode<*>>()

        rootDir.subdirectories.sortedBy { it.name }.forEachIndexed { i, subDir ->
            children.add(SortedCompactPackageNode(project, subDir, settings, "0_${String.format("%03d", i)}_${subDir.name}"))
        }
        rootDir.files.sortedBy { it.name }.forEachIndexed { i, file ->
            children.add(SortedPsiFileNode(project, file, settings, "1_${String.format("%03d", i)}_${file.name}"))
        }
        return children
    }

    override fun contains(file: VirtualFile): Boolean =
        file.path.startsWith(rootDir.virtualFile.path)
}
