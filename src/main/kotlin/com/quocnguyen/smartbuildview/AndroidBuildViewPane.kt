package com.quocnguyen.smartbuildview

import com.intellij.icons.AllIcons
import com.intellij.ide.SelectInContext
import com.intellij.ide.SelectInTarget
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import javax.swing.Icon

/**
 * Custom Project View Pane that mimics the Android view but includes build files under each module.
 * Appears as "Android + Build" in the Project view dropdown.
 */
class AndroidBuildViewPane(project: Project) : ProjectViewPane(project), DumbAware {

    companion object {
        const val ID = "AndroidBuildView"
    }

    override fun getTitle(): String = "Android + Build"

    override fun getId(): String = ID

    override fun getIcon(): Icon = AllIcons.Nodes.Module

    override fun getWeight(): Int = 142 // Just after Android view (which is ~141)

    // Hide Scratches and Consoles from this view
    override fun supportsShowScratchesAndConsoles(): Boolean = false

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
