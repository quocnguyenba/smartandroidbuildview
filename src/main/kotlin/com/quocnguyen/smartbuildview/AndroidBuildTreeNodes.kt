package com.quocnguyen.smartbuildview

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
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
            } catch (e: Exception) {
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

        // === SOURCE FOLDERS (sortKey: 00-09) ===

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

        // === OTHER DIRECTORIES AND FILES (sortKey: 50-59) ===
        // Folders first (50_xxx), then files (55_xxx) - matching Android view behavior
        if (fileSettings.showOtherFiles) {
            val otherDirs = moduleDir.children
                .filter { it.isDirectory }
                .filter { it.name !in setOf("src", "build", ".gradle", ".idea") }
                .sortedBy { it.name }

            otherDirs.forEachIndexed { index, dir ->
                psiManager.findDirectory(dir)?.let { psiDir ->
                    children.add(SortedDirNode(project, psiDir, settings, "50_${String.format("%03d", index)}"))
                }
            }

            val otherFiles = moduleDir.children
                .filter { !it.isDirectory }
                .filter { it.name !in KNOWN_CONFIG_FILES }
                .filter { !it.name.startsWith("BuildConfig") }
                .sortedBy { it.name }
            
            otherFiles.forEachIndexed { index, file ->
                psiManager.findFile(file)?.let { psiFile ->
                    children.add(SortedFileNode(project, psiFile, settings, null, "55_${String.format("%03d", index)}"))
                }
            }
        }

        // === CONFIG FILES (sortKey: 90-99, BuildConfig always 99) ===
        
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

    override fun contains(file: VirtualFile): Boolean {
        val moduleDir = getModuleDir() ?: return false
        return file.path.startsWith(moduleDir.path)
    }
}

// === SORTED NODES WITH EXPLICIT SORT KEYS ===

class SortedSourceGroupNode(
    project: Project,
    private val name: String,
    private val icon: javax.swing.Icon,
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
        // Parent module with children - use ModuleGroup icon
        presentation.setIcon(AllIcons.Nodes.ModuleGroup)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
        val moduleDir = getModuleDir()
        val psiManager = PsiManager.getInstance(project)
        val fileSettings = ModuleFilesSettings.getInstance(project)
        
        val children = mutableListOf<AbstractTreeNode<*>>()

        // Child modules first
        val childModules = childHierarchyNodes.sortedBy { it.displayName }.map { it.toTreeNode() }

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

    override fun contains(file: VirtualFile): Boolean {
        val moduleDir = getModuleDir() ?: return false
        return file.path.startsWith(moduleDir.path)
    }
}

// Wrapper not needed - child modules are sorted directly

class ModuleGroupNode(
    project: Project,
    private val displayName: String,
    private val childHierarchyNodes: List<ModuleHierarchyNode>,
    private val settings: ViewSettings?
) : ProjectViewNode<String>(project, displayName, settings) {

    override fun update(presentation: PresentationData) {
        presentation.clearText()
        presentation.addText(displayName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        // Pure grouping node (no module content) - use ModuleGroup icon
        presentation.setIcon(AllIcons.Nodes.ModuleGroup)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        return childHierarchyNodes.sortedBy { it.displayName }.map { it.toTreeNode() }
    }

    override fun contains(file: VirtualFile): Boolean = false
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

    override fun contains(file: VirtualFile): Boolean = false
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
 */
class SortedCompactPackageNode(
    project: Project,
    private val rootDir: PsiDirectory,
    private val settings: ViewSettings?,
    val sortKey: String
) : ProjectViewNode<PsiDirectory>(project, rootDir, settings) {

    private val compactedInfo: Pair<String, PsiDirectory> by lazy {
        computeCompactedPath(rootDir)
    }

    private val compactedName: String get() = compactedInfo.first
    private val finalDir: PsiDirectory get() = compactedInfo.second

    private fun computeCompactedPath(dir: PsiDirectory): Pair<String, PsiDirectory> {
        val pathParts = mutableListOf(dir.name)
        var currentDir = dir

        while (true) {
            val subdirs = currentDir.subdirectories
            val files = currentDir.files
            if (files.isNotEmpty() || subdirs.size != 1) {
                break
            }
            currentDir = subdirs.first()
            pathParts.add(currentDir.name)
        }

        return Pair(pathParts.joinToString("."), currentDir)
    }

    override fun update(presentation: PresentationData) {
        presentation.presentableText = compactedName
        presentation.setIcon(AllIcons.Nodes.Package)
    }

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val project = myProject ?: return emptyList()
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

    override fun contains(file: VirtualFile): Boolean = 
        file.path.startsWith(rootDir.virtualFile.path)
    
    override fun getSortKey(): Comparable<*> = sortKey
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
}
