package com.quocnguyen.smartbuildview

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile

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

        // Check if our custom pane is currently active
        val projectView = ProjectView.getInstance(project)
        val currentPaneId = projectView.currentViewId

        if (currentPaneId != AndroidBuildViewPane.ID) {
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
        val allModules = ModuleManager.getInstance(project).modules
            .filter { module ->
                val moduleDir = getModuleDir(module)
                moduleDir?.findChild("build.gradle") != null ||
                moduleDir?.findChild("build.gradle.kts") != null
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
        
        // Add Gradle Scripts (root project files) at the bottom
        result.add(RootProjectFilesNode(project, settings))
        
        // Add External Libraries if enabled
        val fileSettings = ModuleFilesSettings.getInstance(project)
        if (fileSettings.showExternalLibraries) {
            result.add(ExternalLibrariesNode(project, settings))
        }
        
        return result
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
 */
class ModuleHierarchyNode(
    val project: Project,
    val displayName: String,
    var module: Module?,
    val settings: ViewSettings?
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
                settings = settings
            )
        } else {
            val childNode = children.getOrPut(childName) {
                ModuleHierarchyNode(
                    project = project,
                    displayName = childName,
                    module = null,
                    settings = settings
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
            ModuleGroupNode(project, displayName, children.values.toList(), settings)
        }
    }
}
