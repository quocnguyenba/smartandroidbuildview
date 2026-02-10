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
    
    private var showBuildConfigCheckBox: JBCheckBox? = null
    private var showGeneratedFoldersCheckBox: JBCheckBox? = null
    
    private var showOtherFilesCheckBox: JBCheckBox? = null
    private var showExternalLibrariesCheckBox: JBCheckBox? = null

    override fun getDisplayName(): String = "Android + Build View"

    override fun createComponent(): JComponent {
        return panel {
            group("Source Folders") {
                row {
                    showKotlinJavaCheckBox = checkBox("Show kotlin+java sources")
                        .comment("Display main Kotlin and Java source directories")
                        .component
                }
                row {
                    showResCheckBox = checkBox("Show res folder")
                        .comment("Display Android resource directories")
                        .component
                }
                row {
                    showAssetsCheckBox = checkBox("Show assets folder")
                        .comment("Display Android assets directories")
                        .component
                }
                row {
                    showTestSourcesCheckBox = checkBox("Show test sources")
                        .comment("Display unit test source directories (src/test)")
                        .component
                }
                row {
                    showAndroidTestSourcesCheckBox = checkBox("Show androidTest sources")
                        .comment("Display instrumented test source directories (src/androidTest)")
                        .component
                }
            }

            group("Configuration Files") {
                row {
                    showManifestsCheckBox = checkBox("Show AndroidManifest.xml")
                        .comment("Display Android manifest files")
                        .component
                }
                row {
                    showBuildGradleCheckBox = checkBox("Show build.gradle files")
                        .comment("Display Gradle build scripts")
                        .component
                }
                row {
                    showProguardRulesCheckBox = checkBox("Show proguard-rules.pro")
                        .comment("Display ProGuard configuration files")
                        .component
                }
                row {
                    showConsumerRulesCheckBox = checkBox("Show consumer-rules.pro")
                        .comment("Display consumer ProGuard rules (for libraries)")
                        .component
                }
            }

            group("Generated Content") {
                row {
                    showBuildConfigCheckBox = checkBox("Show BuildConfig.java")
                        .comment("Display generated BuildConfig files")
                        .component
                }
                row {
                    showGeneratedFoldersCheckBox = checkBox("Show generated folders")
                        .comment("Display all generated source directories (build/generated)")
                        .component
                }
            }

            group("Other") {
                row {
                    showOtherFilesCheckBox = checkBox("Show other files and folders")
                        .comment("Display additional files and directories not in standard categories")
                        .component
                }
                row {
                    showExternalLibrariesCheckBox = checkBox("Show external libraries")
                        .comment("Display external dependencies and libraries")
                        .component
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
                showBuildConfigCheckBox?.isSelected != settings.showBuildConfig ||
                showGeneratedFoldersCheckBox?.isSelected != settings.showGeneratedFolders ||
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
        
        showBuildConfigCheckBox?.let { settings.showBuildConfig = it.isSelected }
        showGeneratedFoldersCheckBox?.let { settings.showGeneratedFolders = it.isSelected }
        
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
        
        showBuildConfigCheckBox?.isSelected = settings.showBuildConfig
        showGeneratedFoldersCheckBox?.isSelected = settings.showGeneratedFolders
        
        showOtherFilesCheckBox?.isSelected = settings.showOtherFiles
        showExternalLibrariesCheckBox?.isSelected = settings.showExternalLibraries
    }
}
