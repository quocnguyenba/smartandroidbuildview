package com.quocnguyen.smartbuildview

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Settings panel for Android + Build view.
 * Allows users to control which file types and folders are shown.
 */
class AndroidBuildViewConfigurable(private val project: Project) : Configurable {

    private var showKotlinJavaCheckBox: JBCheckBox? = null
    private var showResCheckBox: JBCheckBox? = null
    private var showAssetsCheckBox: JBCheckBox? = null
    private var showTestSourcesCheckBox: JBCheckBox? = null
    private var showAndroidTestSourcesCheckBox: JBCheckBox? = null
    
    private var showManifestsCheckBox: JBCheckBox? = null
    private var showBuildGradleCheckBox: JBCheckBox? = null
    private var showProguardRulesCheckBox: JBCheckBox? = null
    private var showConsumerRulesCheckBox: JBCheckBox? = null
    private var showGitignoreCheckBox: JBCheckBox? = null
    
    private var showBuildConfigCheckBox: JBCheckBox? = null
    private var showGeneratedFoldersCheckBox: JBCheckBox? = null
    
    private var showAllFoldersCheckBox: JBCheckBox? = null
    private var showOtherFilesCheckBox: JBCheckBox? = null
    private var showExternalLibrariesCheckBox: JBCheckBox? = null
    override fun getDisplayName(): String = "Android + Build View"

    override fun createComponent(): JComponent {
        return panel {
            group("Source Folders") {
                row {
                    showKotlinJavaCheckBox = checkBox("Show kotlin+java sources").component
                }
                row {
                    showResCheckBox = checkBox("Show res folder").component
                }
                row {
                    showAssetsCheckBox = checkBox("Show assets folder").component
                }
                row {
                    showTestSourcesCheckBox = checkBox("Show test sources").component
                }
                row {
                    showAndroidTestSourcesCheckBox = checkBox("Show androidTest sources").component
                }
            }

            group("Configuration Files") {
                row {
                    showManifestsCheckBox = checkBox("Show AndroidManifest.xml").component
                }
                row {
                    showBuildGradleCheckBox = checkBox("Show build.gradle files").component
                }
                row {
                    showProguardRulesCheckBox = checkBox("Show proguard-rules.pro").component
                }
                row {
                    showConsumerRulesCheckBox = checkBox("Show consumer-rules.pro").component
                }
                row {
                    showGitignoreCheckBox = checkBox("Show .gitignore").component
                }
            }

            group("Generated Content") {
                row {
                    showBuildConfigCheckBox = checkBox("Show BuildConfig.java").component
                }
                row {
                    showGeneratedFoldersCheckBox = checkBox("Show build folder").component
                }
            }

            group("Other") {
                row {
                    showAllFoldersCheckBox = checkBox("Show all other folders").component
                }
                row {
                    showOtherFilesCheckBox = checkBox("Show other files").component
                }
                row {
                    showExternalLibrariesCheckBox = checkBox("Show external libraries").component
                }
            }
        }
    }

    override fun isModified(): Boolean {
        val settings = ModuleFilesSettings.getInstance(project)
        return showKotlinJavaCheckBox?.isSelected != settings.showKotlinJava ||
                showResCheckBox?.isSelected != settings.showRes ||
                showAssetsCheckBox?.isSelected != settings.showAssets ||
                showTestSourcesCheckBox?.isSelected != settings.showTestSources ||
                showAndroidTestSourcesCheckBox?.isSelected != settings.showAndroidTestSources ||
                showManifestsCheckBox?.isSelected != settings.showManifests ||
                showBuildGradleCheckBox?.isSelected != settings.showBuildGradle ||
                showProguardRulesCheckBox?.isSelected != settings.showProguardRules ||
                showConsumerRulesCheckBox?.isSelected != settings.showConsumerRules ||
                showGitignoreCheckBox?.isSelected != settings.showGitignore ||
                showBuildConfigCheckBox?.isSelected != settings.showBuildConfig ||
                showGeneratedFoldersCheckBox?.isSelected != settings.showGeneratedFolders ||
                showAllFoldersCheckBox?.isSelected != settings.showAllFolders ||
                showOtherFilesCheckBox?.isSelected != settings.showOtherFiles ||
                showExternalLibrariesCheckBox?.isSelected != settings.showExternalLibraries
    }

    override fun apply() {
        val settings = ModuleFilesSettings.getInstance(project)
        
        showKotlinJavaCheckBox?.let { settings.showKotlinJava = it.isSelected }
        showResCheckBox?.let { settings.showRes = it.isSelected }
        showAssetsCheckBox?.let { settings.showAssets = it.isSelected }
        showTestSourcesCheckBox?.let { settings.showTestSources = it.isSelected }
        showAndroidTestSourcesCheckBox?.let { settings.showAndroidTestSources = it.isSelected }
        
        showManifestsCheckBox?.let { settings.showManifests = it.isSelected }
        showBuildGradleCheckBox?.let { settings.showBuildGradle = it.isSelected }
        showProguardRulesCheckBox?.let { settings.showProguardRules = it.isSelected }
        showConsumerRulesCheckBox?.let { settings.showConsumerRules = it.isSelected }
        showGitignoreCheckBox?.let { settings.showGitignore = it.isSelected }
        
        showBuildConfigCheckBox?.let { settings.showBuildConfig = it.isSelected }
        showGeneratedFoldersCheckBox?.let { settings.showGeneratedFolders = it.isSelected }
        
        showAllFoldersCheckBox?.let { settings.showAllFolders = it.isSelected }
        showOtherFilesCheckBox?.let { settings.showOtherFiles = it.isSelected }
        showExternalLibrariesCheckBox?.let { settings.showExternalLibraries = it.isSelected }

        // Refresh the project view to show changes
        ProjectView.getInstance(project).refresh()
    }

    override fun reset() {
        val settings = ModuleFilesSettings.getInstance(project)
        
        showKotlinJavaCheckBox?.isSelected = settings.showKotlinJava
        showResCheckBox?.isSelected = settings.showRes
        showAssetsCheckBox?.isSelected = settings.showAssets
        showTestSourcesCheckBox?.isSelected = settings.showTestSources
        showAndroidTestSourcesCheckBox?.isSelected = settings.showAndroidTestSources
        
        showManifestsCheckBox?.isSelected = settings.showManifests
        showBuildGradleCheckBox?.isSelected = settings.showBuildGradle
        showProguardRulesCheckBox?.isSelected = settings.showProguardRules
        showConsumerRulesCheckBox?.isSelected = settings.showConsumerRules
        showGitignoreCheckBox?.isSelected = settings.showGitignore
        
        showBuildConfigCheckBox?.isSelected = settings.showBuildConfig
        showGeneratedFoldersCheckBox?.isSelected = settings.showGeneratedFolders
        
        showAllFoldersCheckBox?.isSelected = settings.showAllFolders
        showOtherFilesCheckBox?.isSelected = settings.showOtherFiles
        showExternalLibrariesCheckBox?.isSelected = settings.showExternalLibraries
    }
}