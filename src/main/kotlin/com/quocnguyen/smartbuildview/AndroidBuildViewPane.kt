package com.quocnguyen.smartbuildview

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
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

    override fun supportsSortByType(): Boolean = false

    override fun supportsFoldersAlwaysOnTop(): Boolean = true

    // Hide Scratches and Consoles from this view
    override fun supportsShowScratchesAndConsoles(): Boolean = false
}
