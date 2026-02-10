package com.quocnguyen.smartbuildview

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for string utility functions used in module name processing.
 * These test the logic that would be used in AndroidBuildTreeStructureProvider.
 */
class StringUtilsTest {

    @Test
    fun `test module name prefix stripping`() {
        // Test cases matching the stripPrefix logic in AndroidBuildTreeStructureProvider
        val testCases = mapOf(
            "What3words.app" to "app",
            "What3words.feature.login" to "feature.login",
            "What3words.core.network" to "core.network",
            "MyProject.main" to "main",
            "SingleModule" to "SingleModule"  // No prefix to strip
        )
        
        testCases.forEach { (input, expected) ->
            val prefix = input.substringBefore(".", "")
            val result = stripPrefix(input, prefix)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `test empty prefix handling`() {
        val result = stripPrefix("module.name", "")
        assertEquals("module.name", result)
    }

    @Test
    fun `test prefix not at start is not stripped`() {
        val result = stripPrefix("app.What3words.feature", "What3words")
        assertEquals("app.What3words.feature", result)
    }

    @Test
    fun `test exact match is not stripped`() {
        val result = stripPrefix("What3words", "What3words")
        assertEquals("What3words", result)
    }

    @Test
    fun `test finding common prefix in module names`() {
        // Test the logic for finding project prefix
        val moduleNames = listOf(
            "What3words.app",
            "What3words.feature.login",
            "What3words.core.network"
        )
        
        val firstParts = moduleNames.map { it.split(".").firstOrNull() ?: "" }
        val commonPrefix = firstParts.firstOrNull() ?: ""
        val hasCommonPrefix = firstParts.all { it == commonPrefix }
        
        assertEquals("What3words", commonPrefix)
        assertTrue(hasCommonPrefix)
    }

    @Test
    fun `test no common prefix when modules differ`() {
        val moduleNames = listOf(
            "ProjectA.app",
            "ProjectB.feature",
            "ProjectC.core"
        )
        
        val firstParts = moduleNames.map { it.split(".").firstOrNull() ?: "" }
        val commonPrefix = firstParts.firstOrNull() ?: ""
        val hasCommonPrefix = firstParts.all { it == commonPrefix }
        
        assertFalse(hasCommonPrefix)
    }

    @Test
    fun `test path splitting for nested modules`() {
        val testCases = mapOf(
            "feature.login" to listOf("feature", "login"),
            "core.network.api" to listOf("core", "network", "api"),
            "app" to listOf("app")
        )
        
        testCases.forEach { (input, expected) ->
            val parts = input.split(".")
            assertEquals(expected, parts, "Failed for input: $input")
        }
    }

    // Helper method matching AndroidBuildTreeStructureProvider logic
    private fun stripPrefix(moduleName: String, prefix: String): String {
        return if (prefix.isNotEmpty() && moduleName.startsWith("$prefix.")) {
            moduleName.removePrefix("$prefix.")
        } else {
            moduleName
        }
    }
}
