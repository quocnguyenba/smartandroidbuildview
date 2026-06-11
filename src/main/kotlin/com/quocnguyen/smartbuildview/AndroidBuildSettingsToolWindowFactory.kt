package com.quocnguyen.smartbuildview

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JComponent

/**
 * Tool window that displays Android + Build view settings on the right bar.
 * Provides quick access to toggle visibility of different file types and folders.
 */
class AndroidBuildSettingsToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val settingsPanel = AndroidBuildSettingsPanel(project)
        val content = ContentFactory.getInstance().createContent(settingsPanel.createComponent(), "", false)
        content.isCloseable = false
        toolWindow.contentManager.addContent(content)
    }

    // Only expose the settings tool window for Android projects, matching the
    // visibility of the "Android + Build" project view it configures.
    override fun shouldBeAvailable(project: Project): Boolean =
        AndroidProjectDetector.isAndroidProject(project)
}

/**
 * Panel containing all settings checkboxes for Android + Build view.
 */
class AndroidBuildSettingsPanel(private val project: Project) {
    
    private val settings = ModuleFilesSettings.getInstance(project)

    fun createComponent(): JComponent {
        // Create the settings panel with DSL
        val settingsContent = panel {
            row {
                label("Android + Build View Settings")
                    .apply { 
                        component.font = component.font.deriveFont(Font.BOLD, component.font.size + 2f)
                    }
            }
            
            group("Source Folders") {
                row {
                    checkBox("Show kotlin+java sources")
                        .applyToComponent { isSelected = settings.showKotlinJava }
                        .applyToComponent {
                            addActionListener {
                                settings.showKotlinJava = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show res folder")
                        .applyToComponent { isSelected = settings.showRes }
                        .applyToComponent {
                            addActionListener {
                                settings.showRes = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show assets folder")
                        .applyToComponent { isSelected = settings.showAssets }
                        .applyToComponent {
                            addActionListener {
                                settings.showAssets = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show test sources")
                        .applyToComponent { isSelected = settings.showTestSources }
                        .applyToComponent {
                            addActionListener {
                                settings.showTestSources = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show androidTest sources")
                        .applyToComponent { isSelected = settings.showAndroidTestSources }
                        .applyToComponent {
                            addActionListener {
                                settings.showAndroidTestSources = isSelected
                                refreshView()
                            }
                        }
                }
            }

            group("Configuration Files") {
                row {
                    checkBox("Show AndroidManifest.xml")
                        .applyToComponent { isSelected = settings.showManifests }
                        .applyToComponent {
                            addActionListener {
                                settings.showManifests = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show build.gradle files")
                        .applyToComponent { isSelected = settings.showBuildGradle }
                        .applyToComponent {
                            addActionListener {
                                settings.showBuildGradle = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show proguard-rules.pro")
                        .applyToComponent { isSelected = settings.showProguardRules }
                        .applyToComponent {
                            addActionListener {
                                settings.showProguardRules = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show consumer-rules.pro")
                        .applyToComponent { isSelected = settings.showConsumerRules }
                        .applyToComponent {
                            addActionListener {
                                settings.showConsumerRules = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show .gitignore")
                        .applyToComponent { isSelected = settings.showGitignore }
                        .applyToComponent {
                            addActionListener {
                                settings.showGitignore = isSelected
                                refreshView()
                            }
                        }
                }
            }

            group("Generated Content") {
                row {
                    checkBox("Show BuildConfig.java")
                        .applyToComponent { isSelected = settings.showBuildConfig }
                        .applyToComponent {
                            addActionListener {
                                settings.showBuildConfig = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show build folder")
                        .applyToComponent { isSelected = settings.showGeneratedFolders }
                        .applyToComponent {
                            addActionListener {
                                settings.showGeneratedFolders = isSelected
                                refreshView()
                            }
                        }
                }
            }

            group("Other") {
                row {
                    checkBox("Show all other folders")
                        .applyToComponent { isSelected = settings.showAllFolders }
                        .applyToComponent {
                            addActionListener {
                                settings.showAllFolders = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show other files")
                        .applyToComponent { isSelected = settings.showOtherFiles }
                        .applyToComponent {
                            addActionListener {
                                settings.showOtherFiles = isSelected
                                refreshView()
                            }
                        }
                }
                row {
                    checkBox("Show external libraries")
                        .applyToComponent { isSelected = settings.showExternalLibraries }
                        .applyToComponent {
                            addActionListener {
                                settings.showExternalLibraries = isSelected
                                refreshView()
                            }
                        }
                }
            }
        }
        
        // Add margins around the content
        settingsContent.border = BorderFactory.createEmptyBorder(
            JBUI.scale(12), // top
            JBUI.scale(12), // left
            JBUI.scale(12), // bottom
            JBUI.scale(12)  // right
        )
        
        // Wrap in scroll pane to make content scrollable
        return JBScrollPane(settingsContent).apply {
            border = null
            isOpaque = false
        }
    }

    private fun refreshView() {
        ProjectView.getInstance(project).refresh()
    }
}