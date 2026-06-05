package com.quocnguyen.smartbuildview

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.ModuleGroup
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

/**
 * TreeStructureProvider that replaces the tree structure when using the "Android + Build" view.
 * Shows modules in a hierarchy with build files at the bottom of each module.
 */
class AndroidBuildTreeStructureProvider : TreeStructureProvider, DumbAware {

    override fun modify(
        parent: AbstractTreeNode<*>,
        children: MutableCollection<AbstractTreeNode<*>>,
        settings: ViewSettings?
    ): MutableCollection<AbstractTreeNode<*>> {
        val project = parent.project ?: return children

        // Check if our custom pane is the one currently building its structure
        if (settings !is AndroidBuildViewSettings) {
            return children
        }

        // Only build custom hierarchy at the actual project root node
        if (parent !is ProjectViewProjectNode) {
            return children
        }

        // Build our custom module hierarchy (excludes Scratches and Consoles)
        return buildModuleHierarchy(project, settings)
    }

    /**
     * Builds a hierarchical module structure based on module names.
     * Strips the project prefix (e.g., "What3words.") and creates hierarchy from the rest.
     * Excludes the root project module (just the project name without any submodule).
     */
    private fun buildModuleHierarchy(project: Project, settings: ViewSettings?): MutableCollection<AbstractTreeNode<*>> {
        val fileSettings = ModuleFilesSettings.getInstance(project)
        
        // Collect all module names to check for parent-child relationships
        val allModuleNames = ModuleManager.getInstance(project).modules.map { it.name }.toSet()

        val allModules = ModuleManager.getInstance(project).modules
            .filter { module ->
                // Always exclude source set modules (main, test, androidTest, unitTest, sharedTest)
                // whose parent Gradle module exists. Their content is already shown under the
                // parent module's source folder groupings (kotlin+java, res, androidTest, etc.)
                val moduleName = module.name
                val lastPart = moduleName.substringAfterLast(".")
                val parentName = if (moduleName.contains(".")) moduleName.substringBeforeLast(".") else null
                val isSourceSet = lastPart in setOf("main", "test", "androidTest", "unitTest", "sharedTest")
                val parentExists = parentName != null && parentName in allModuleNames

                if (isSourceSet && parentExists) {
                    false // Always hide source set modules - their content is already visible
                } else {
                    // Only show modules with build.gradle files (Gradle modules)
                    val moduleDir = getModuleDir(module)
                    moduleDir?.findChild("build.gradle") != null ||
                    moduleDir?.findChild("build.gradle.kts") != null
                }
            }
            .sortedBy { it.name }

        // Find the project prefix (root module name)
        val projectPrefix = findProjectPrefix(allModules)
        
        // Exclude the root project module (e.g., "What3words" without any suffix)
        // This is the module that matches exactly the project prefix
        val modules = allModules.filter { module ->
            module.name != projectPrefix
        }

        // Build hierarchy tree
        val rootNodes = mutableMapOf<String, ModuleHierarchyNode>()
        
        for (module in modules) {
            val modulePath = stripPrefix(module.name, projectPrefix)
            val pathParts = modulePath.split(".")
            
            if (pathParts.size == 1) {
                // Root level module
                rootNodes[pathParts[0]] = ModuleHierarchyNode(
                    project = project,
                    displayName = pathParts[0],
                    module = module,
                    settings = settings
                )
            } else {
                // Nested module
                val parentName = pathParts[0]
                val childPath = pathParts.drop(1).joinToString(".")
                
                val parentNode = rootNodes.getOrPut(parentName) {
                    ModuleHierarchyNode(
                        project = project,
                        displayName = parentName,
                        module = null,
                        settings = settings
                    )
                }
                
                parentNode.addChild(childPath, module)
            }
        }

        // Convert to tree nodes and add root project files at the bottom
        val result = mutableListOf<AbstractTreeNode<*>>()
        result.addAll(
            rootNodes.values
                .sortedBy { it.displayName }
                .map { it.toTreeNode() }
        )
        
        findGithubFolder(project, settings)?.let { githubFolder ->
            result.add(githubFolder)
        }

        // Add folders outside of modules if showAllFolders is enabled
        if (fileSettings.showAllFolders) {
            val nonModuleFolders = findNonModuleFolders(project, allModules, settings)
            result.addAll(nonModuleFolders)
        }
        
        // Add Gradle Scripts (root project files) at the bottom
        result.add(RootProjectFilesNode(project, settings))
        
        // Add External Libraries if enabled
        if (fileSettings.showExternalLibraries) {
            result.add(ExternalLibrariesNode(project, settings))
        }
        
        return result
    }

    private fun findGithubFolder(project: Project, viewSettings: ViewSettings?): AbstractTreeNode<*>? {
        val basePath = project.basePath ?: return null
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return null
        val githubDir = baseDir.findChild(".github")?.takeIf { it.isDirectory } ?: return null
        val psiDir = PsiManager.getInstance(project).findDirectory(githubDir) ?: return null
        return GithubFolderNode(project, psiDir, viewSettings, "35_000_.github")
    }

    /**
     * Finds folders in the project root that are not part of any module.
     */
    private fun findNonModuleFolders(project: Project, modules: List<Module>, viewSettings: ViewSettings?): List<AbstractTreeNode<*>> {
        val basePath = project.basePath ?: return emptyList()
        val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath)
            ?: return emptyList()
        
        val psiManager = PsiManager.getInstance(project)
        val moduleRoots = modules.flatMap { ModuleRootManager.getInstance(it).contentRoots.toList() }
        val moduleRootPaths = moduleRoots.map { it.path }.toSet()
        
        val folders = mutableListOf<AbstractTreeNode<*>>()
        
        baseDir.children
            .filter { it.isDirectory }
            .filter { it.path !in moduleRootPaths }
            .filter { it.name !in setOf("build", ".gradle", ".github", ".idea", ".git", "src") }
            .sortedBy { it.name }
            .forEachIndexed { index, dir ->
                psiManager.findDirectory(dir)?.let { psiDir ->
                    folders.add(SortedDirNode(project, psiDir, viewSettings, "35_${String.format("%03d", index)}"))
                }
            }
        
        return folders
    }

    private fun findProjectPrefix(modules: List<Module>): String {
        if (modules.isEmpty()) return ""
        
        val firstParts = modules.map { it.name.split(".").firstOrNull() ?: "" }
        val commonPrefix = firstParts.firstOrNull() ?: ""
        
        return if (firstParts.all { it == commonPrefix }) commonPrefix else ""
    }

    private fun stripPrefix(moduleName: String, prefix: String): String {
        return if (prefix.isNotEmpty() && moduleName.startsWith("$prefix.")) {
            moduleName.removePrefix("$prefix.")
        } else {
            moduleName
        }
    }

    /**
     * Gets the module directory using public API.
     * Uses ModuleRootManager to get content roots (the recommended public API approach).
     */
    private fun getModuleDir(module: Module): VirtualFile? {
        val contentRoots = ModuleRootManager.getInstance(module).contentRoots
        return contentRoots.firstOrNull()
    }
}

/**
 * Helper class to build module hierarchy before converting to tree nodes.
 *
 * @param groupPath The full dot-separated group path from root to this node
 *                  (e.g. `["feature"]` or `["feature", "ui"]`). Used to construct
 *                  a [ModuleGroup] so IntelliJ can show "New > Module" for group nodes.
 */
class ModuleHierarchyNode(
    val project: Project,
    val displayName: String,
    var module: Module?,
    val settings: ViewSettings?,
    private val groupPath: List<String> = listOf(displayName)
) {
    private val children = mutableMapOf<String, ModuleHierarchyNode>()

    fun addChild(path: String, module: Module) {
        val parts = path.split(".", limit = 2)
        val childName = parts[0]
        
        if (parts.size == 1) {
            children[childName] = ModuleHierarchyNode(
                project = project,
                displayName = childName,
                module = module,
                settings = settings,
                groupPath = groupPath + childName
            )
        } else {
            val childNode = children.getOrPut(childName) {
                ModuleHierarchyNode(
                    project = project,
                    displayName = childName,
                    module = null,
                    settings = settings,
                    groupPath = groupPath + childName
                )
            }
            childNode.addChild(parts[1], module)
        }
    }

    fun toTreeNode(): AbstractTreeNode<*> {
        return if (children.isEmpty() && module != null) {
            AndroidBuildModuleNode(project, module!!, settings)
        } else if (module != null && children.isNotEmpty()) {
            AndroidBuildModuleWithChildrenNode(project, module!!, displayName, children.values.toList(), settings)
        } else {
            ModuleGroupNode(
                project, displayName, children.values.toList(), settings,
                findGroupDir(), ModuleGroup(groupPath)
            )
        }
    }

    /**
     * Resolves the filesystem directory that represents this group node (e.g. `feature/`).
     *
     * Strategy: find the first descendant [Module] among [children], get its content-root
     * directory, then return its **parent** — which is the directory owned by this group.
     * For deeply-nested groups the recursion unwinds one level per call, so the correct
     * ancestor directory is always returned.
     *
     * Returns `null` when no descendant module can be found (e.g. empty group, or all
     * content roots are unavailable).
     */
    private fun findGroupDir(): VirtualFile? {
        for (child in children.values) {
            val childModule = child.module
            if (childModule != null) {
                val contentRoots = ModuleRootManager.getInstance(childModule).contentRoots
                val childDir = contentRoots.firstOrNull()
                return childDir?.parent
            }
            // Child itself is a group – recurse one level deeper, then step up one directory
            val nestedDir = child.findGroupDir()
            if (nestedDir != null) return nestedDir.parent
        }
        return null
    }
}
