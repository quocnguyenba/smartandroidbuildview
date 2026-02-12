package com.quocnguyen.smartbuildview

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel

/**
 * Toolbar action that shows a settings popup for the Android + Build view.
 * Appears as a gear icon in the Project View toolbar when Android + Build view is active.
 */
class AndroidBuildViewSettingsAction : AnAction(), DumbAware {

    init {
        templatePresentation.text = "Android + Build View Settings"
        templatePresentation.description = "Configure what to show in Android + Build view"
        templatePresentation.icon = AllIcons.General.Settings
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        // Only show this action when Android + Build view is active
        val projectView = ProjectView.getInstance(project)
        val isAndroidBuildView = projectView.currentViewId == AndroidBuildViewPane.ID
        e.presentation.isEnabledAndVisible = isAndroidBuildView
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = ModuleFilesSettings.getInstance(project)

        // Create checkboxes
        val checkboxes = mutableMapOf<String, JBCheckBox>()

        val settingsPanel = panel {
            group("Source Folders") {
                row {
                    checkboxes["showKotlinJava"] = checkBox("kotlin+java sources")
                        .applyToComponent { isSelected = settings.showKotlinJava }
                        .component
                }
                row {
                    checkboxes["showRes"] = checkBox("res folder")
                        .applyToComponent { isSelected = settings.showRes }
                        .component
                }
                row {
                    checkboxes["showAssets"] = checkBox("assets folder")
                        .applyToComponent { isSelected = settings.showAssets }
                        .component
                }
                row {
                    checkboxes["showTestSources"] = checkBox("test sources")
                        .applyToComponent { isSelected = settings.showTestSources }
                        .component
                }
                row {
                    checkboxes["showAndroidTestSources"] = checkBox("androidTest sources")
                        .applyToComponent { isSelected = settings.showAndroidTestSources }
                        .component
                }
            }

            group("Configuration Files") {
                row {
                    checkboxes["showManifests"] = checkBox("AndroidManifest.xml")
                        .applyToComponent { isSelected = settings.showManifests }
                        .component
                }
                row {
                    checkboxes["showBuildGradle"] = checkBox("build.gradle files")
                        .applyToComponent { isSelected = settings.showBuildGradle }
                        .component
                }
                row {
                    checkboxes["showProguardRules"] = checkBox("proguard-rules.pro")
                        .applyToComponent { isSelected = settings.showProguardRules }
                        .component
                }
                row {
                    checkboxes["showConsumerRules"] = checkBox("consumer-rules.pro")
                        .applyToComponent { isSelected = settings.showConsumerRules }
                        .component
                }
            }

            group("Generated Content") {
                row {
                    checkboxes["showBuildConfig"] = checkBox("BuildConfig.java")
                        .applyToComponent { isSelected = settings.showBuildConfig }
                        .component
                }
                row {
                    checkboxes["showGeneratedFolders"] = checkBox("Generated folders (build, .gradle, .idea)")
                        .applyToComponent { isSelected = settings.showGeneratedFolders }
                        .component
                }
            }

            group("Other") {
                row {
                    checkboxes["showOtherFiles"] = checkBox("Other files and folders")
                        .applyToComponent { isSelected = settings.showOtherFiles }
                        .component
                }
                row {
                    checkboxes["showExternalLibraries"] = checkBox("External libraries")
                        .applyToComponent { isSelected = settings.showExternalLibraries }
                        .component
                }
            }
        }

        // Show popup
        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(settingsPanel, null)
            .setTitle("Android + Build View Settings")
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()

        // Add listener to apply settings when popup closes
        popup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                // Apply all settings
                checkboxes["showKotlinJava"]?.let { settings.showKotlinJava = it.isSelected }
                checkboxes["showRes"]?.let { settings.showRes = it.isSelected }
                checkboxes["showAssets"]?.let { settings.showAssets = it.isSelected }
                checkboxes["showTestSources"]?.let { settings.showTestSources = it.isSelected }
                checkboxes["showAndroidTestSources"]?.let { settings.showAndroidTestSources = it.isSelected }
                
                checkboxes["showManifests"]?.let { settings.showManifests = it.isSelected }
                checkboxes["showBuildGradle"]?.let { settings.showBuildGradle = it.isSelected }
                checkboxes["showProguardRules"]?.let { settings.showProguardRules = it.isSelected }
                checkboxes["showConsumerRules"]?.let { settings.showConsumerRules = it.isSelected }
                
                checkboxes["showBuildConfig"]?.let { settings.showBuildConfig = it.isSelected }
                checkboxes["showGeneratedFolders"]?.let { settings.showGeneratedFolders = it.isSelected }
                
                checkboxes["showOtherFiles"]?.let { settings.showOtherFiles = it.isSelected }
                checkboxes["showExternalLibraries"]?.let { settings.showExternalLibraries = it.isSelected }

                // Refresh the project view
                ProjectView.getInstance(project).refresh()
            }
        })

        // Show popup at the component location
        e.inputEvent?.component?.let { component ->
            popup.showUnderneathOf(component)
        } ?: popup.showInFocusCenter()
    }
}
