package com.quocnguyen.smartbuildview

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for ModuleFilesSettings state management.
 */
class ModuleFilesSettingsTest {

    private lateinit var settings: ModuleFilesSettings

    @BeforeEach
    fun setUp() {
        settings = ModuleFilesSettings()
    }

    @Test
    fun `test default state values`() {
        val state = settings.state
        
        assertTrue(state.showBuildGradle, "Build gradle files should be shown by default")
        assertTrue(state.showProguardRules, "Proguard rules should be shown by default")
        assertTrue(state.showConsumerRules, "Consumer rules should be shown by default")
        assertTrue(state.showBuildConfig, "Build config should be shown by default")
        assertTrue(state.showManifests, "Manifests should be shown by default")
        assertTrue(state.showKotlinJava, "Kotlin/Java files should be shown by default")
        assertTrue(state.showRes, "Resources should be shown by default")
        assertTrue(state.showAssets, "Assets should be shown by default")
        assertFalse(state.showOtherFiles, "Other files should be hidden by default")
    }

    @Test
    fun `test setting and getting individual properties`() {
        // Test showBuildGradle
        settings.showBuildGradle = false
        assertFalse(settings.showBuildGradle)
        assertFalse(settings.state.showBuildGradle)
        
        // Test showProguardRules
        settings.showProguardRules = false
        assertFalse(settings.showProguardRules)
        
        // Test showConsumerRules
        settings.showConsumerRules = false
        assertFalse(settings.showConsumerRules)
        
        // Test showBuildConfig
        settings.showBuildConfig = false
        assertFalse(settings.showBuildConfig)
        
        // Test showManifests
        settings.showManifests = false
        assertFalse(settings.showManifests)
        
        // Test showKotlinJava
        settings.showKotlinJava = false
        assertFalse(settings.showKotlinJava)
        
        // Test showRes
        settings.showRes = false
        assertFalse(settings.showRes)
        
        // Test showAssets
        settings.showAssets = false
        assertFalse(settings.showAssets)
        
        // Test showOtherFiles
        settings.showOtherFiles = true
        assertTrue(settings.showOtherFiles)
    }

    @Test
    fun `test state persistence through loadState and getState`() {
        // Modify settings
        settings.showBuildGradle = false
        settings.showProguardRules = false
        settings.showOtherFiles = true
        
        // Get current state
        val savedState = settings.state
        
        // Create new settings instance and load saved state
        val newSettings = ModuleFilesSettings()
        newSettings.loadState(savedState)
        
        // Verify state was preserved
        assertFalse(newSettings.showBuildGradle)
        assertFalse(newSettings.showProguardRules)
        assertTrue(newSettings.showOtherFiles)
        
        // Verify unchanged defaults are still correct
        assertTrue(newSettings.showBuildConfig)
        assertTrue(newSettings.showManifests)
    }

    @Test
    fun `test state immutability - changes affect both property and state`() {
        settings.showBuildGradle = false
        
        // Both the property and state should reflect the change
        assertFalse(settings.showBuildGradle)
        assertFalse(settings.state.showBuildGradle)
    }

    @Test
    fun `test loading custom state replaces default state`() {
        val customState = ModuleFilesSettings.State(
            showBuildGradle = false,
            showProguardRules = false,
            showConsumerRules = false,
            showBuildConfig = false,
            showManifests = false,
            showKotlinJava = false,
            showRes = false,
            showAssets = false,
            showOtherFiles = true
        )
        
        settings.loadState(customState)
        
        assertFalse(settings.showBuildGradle)
        assertFalse(settings.showProguardRules)
        assertFalse(settings.showConsumerRules)
        assertFalse(settings.showBuildConfig)
        assertFalse(settings.showManifests)
        assertFalse(settings.showKotlinJava)
        assertFalse(settings.showRes)
        assertFalse(settings.showAssets)
        assertTrue(settings.showOtherFiles)
    }

    @Test
    fun `test multiple property changes are reflected in state`() {
        settings.showBuildGradle = false
        settings.showProguardRules = true
        settings.showOtherFiles = true
        
        val state = settings.state
        assertFalse(state.showBuildGradle)
        assertTrue(state.showProguardRules)
        assertTrue(state.showOtherFiles)
    }
}
