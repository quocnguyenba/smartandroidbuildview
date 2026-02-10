package com.quocnguyen.smartbuildview

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Persistent settings for Module Files view.
 * Stores which file types should be shown or hidden.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "ModuleFilesSettings",
    storages = [Storage("moduleFilesSettings.xml")]
)
class ModuleFilesSettings : PersistentStateComponent<ModuleFilesSettings.State> {

    data class State(
        var showBuildGradle: Boolean = true,
        var showProguardRules: Boolean = true,
        var showConsumerRules: Boolean = true,
        var showBuildConfig: Boolean = true,
        var showManifests: Boolean = true,
        var showKotlinJava: Boolean = true,
        var showRes: Boolean = true,
        var showAssets: Boolean = true,
        var showOtherFiles: Boolean = false  // Show all other files like Project view
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var showBuildGradle: Boolean
        get() = myState.showBuildGradle
        set(value) { myState.showBuildGradle = value }

    var showProguardRules: Boolean
        get() = myState.showProguardRules
        set(value) { myState.showProguardRules = value }

    var showConsumerRules: Boolean
        get() = myState.showConsumerRules
        set(value) { myState.showConsumerRules = value }

    var showBuildConfig: Boolean
        get() = myState.showBuildConfig
        set(value) { myState.showBuildConfig = value }

    var showManifests: Boolean
        get() = myState.showManifests
        set(value) { myState.showManifests = value }

    var showKotlinJava: Boolean
        get() = myState.showKotlinJava
        set(value) { myState.showKotlinJava = value }

    var showRes: Boolean
        get() = myState.showRes
        set(value) { myState.showRes = value }

    var showAssets: Boolean
        get() = myState.showAssets
        set(value) { myState.showAssets = value }

    var showOtherFiles: Boolean
        get() = myState.showOtherFiles
        set(value) { myState.showOtherFiles = value }

    companion object {
        fun getInstance(project: Project): ModuleFilesSettings {
            return project.service<ModuleFilesSettings>()
        }
    }
}
