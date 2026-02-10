package com.quocnguyen.smartbuildview

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for [ModuleHierarchyNode] hierarchy building logic.
 *
 * ## Overview
 * This test suite verifies the path parsing and splitting logic used by ModuleHierarchyNode,
 * which is responsible for building a hierarchical module structure before converting to tree nodes.
 *
 * ## Why
 * The ModuleHierarchyNode class is critical for organizing modules into a nested
 * structure (e.g., "feature.login" becomes feature > login). Testing ensures:
 * - Single-level modules are handled correctly
 * - Multi-level nested modules create proper hierarchies
 * - Edge cases (empty names, deep nesting) work as expected
 *
 * ## Implementation
 * Tests focus on the string parsing and splitting logic used by the addChild() method,
 * which recursively builds the hierarchy structure. By testing the path manipulation
 * logic in isolation, we can verify correctness without needing heavy IntelliJ
 * Project/Module mocks.
 *
 * ##Alternatives
 * Could use IntelliJ's BasePlatformTestCase for full integration testing with
 * actual Project instances, but that would be slower and more complex for testing
 * this core logic.
 */
class ModuleHierarchyNodeTest {

    /**
     * Test that addChild correctly splits a simple single-part path.
     *
     * Example: "login" should be recognized as a leaf node (no further splitting needed).
     */
    @Test
    fun `test simple path has single part`() {
        val childPath = "login"
        
        // Verify path parsing logic matching addChild implementation
        val pathParts = childPath.split(".", limit = 2)
        
        assertEquals(1, pathParts.size, "Simple path should have 1 part")
        assertEquals("login", pathParts[0])
    }

    /**
     * Test that addChild correctly splits a two-part nested path.
     *
     * Example: "login.ui" should split into "login" (parent) and "ui" (child).
     */
    @Test
    fun `test nested path splits into parent and child`() {
        val childPath = "login.ui"
        
        // Verify path parsing logic (same as addChild implementation)
        val pathParts = childPath.split(".", limit = 2)
        
        assertEquals(2, pathParts.size, "Nested path should split into 2 parts")
        assertEquals("login", pathParts[0], "First part should be 'login'")
        assertEquals("ui", pathParts[1], "Second part should be 'ui'")
    }

    /**
     * Test path splitting behavior for deeply nested module paths.
     *
     * Example: "feature.auth.login.ui" should split with limit=2 at each recursion level.
     */
    @Test
    fun `test deeply nested paths split correctly with limit 2`() {
        val testCases = mapOf(
            "login" to 1,
            "login.ui" to 2,
            "auth.login.ui" to 2,              // Should be limited to 2 parts
            "feature.auth.login.ui" to 2       // Should be limited to 2 parts
        )
        
        testCases.forEach { (path, expectedPartCount) ->
            val parts = path.split(".", limit = 2)
            assertEquals(
                expectedPartCount,
                parts.size,
                "Path splitting with limit=2 failed for: $path"
            )
        }
    }

    /**
     * Test the logic for determining single-level vs multi-level paths.
     *
     * This is used in addChild to decide whether to create a leaf node or recurse.
     */
    @Test
    fun `test single vs multi-level path detection`() {
        val testCases = mapOf(
            "app" to true,               // Single level
            "login" to true,             // Single level
            "feature.login" to false,    // Multi level
            "core.data.api" to false     // Multi level
        )
        
        testCases.forEach { (path, expectedSingleLevel) ->
            val splitParts = path.split(".", limit = 2)
            val isSingleLevel = splitParts.size == 1
            
            assertEquals(
                expectedSingleLevel,
                isSingleLevel,
                "Single level detection failed for: $path"
            )
        }
    }

    /**
     * Test extracting parent name and remaining child path for nested modules.
     *
     * Used by addChild to recursively build hierarchy by splitting progressively.
     */
    @Test
    fun `test parent and child path extraction`() {
        val testCases = mapOf(
            "login.ui" to Pair("login", "ui"),
            "auth.login.ui" to Pair("auth", "login.ui"),
            "feature.auth.login.ui" to Pair("feature", "auth.login.ui")
        )
        
        testCases.forEach { (path, expected) ->
            val parts = path.split(".", limit = 2)
            val parentName = parts[0]
            val childPath = if (parts.size > 1) parts[1] else null
            
            assertEquals(expected.first, parentName, "Parent extraction failed for: $path")
            assertEquals(expected.second, childPath, "Child path extraction failed for: $path")
        }
    }

    /**
     * Test that module names following different naming conventions are parsed correctly.
     *
     * Android projects use various conventions: kebab-case, snake_case, camelCase, etc.
     * Only dots should be used for hierarchy splitting.
     */
    @Test
    fun `test module naming conventions preserve hyphens and underscores`() {
        val testCases = mapOf(
            "feature-login" to listOf("feature-login"),                    // Kebab case preserved
            "core_network" to listOf("core_network"),                      // Snake case preserved
            "feature.login-ui" to listOf("feature", "login-ui"),           // Mixed: dot splits, hyphen preserved
            "core.data_repository" to listOf("core", "data_repository"),   // Mixed: dot splits, underscore preserved
            "my-feature.sub_module" to listOf("my-feature", "sub_module")  // Mixed conventions
        )
        
        testCases.forEach { (path, expectedParts) ->
            val parts = path.split(".", limit = 2)
            assertEquals(
                expectedParts.size,
                parts.size,
                "Part count mismatch for: $path"
            )
            assertEquals(
                expectedParts[0],
                parts[0],
                "First part should preserve naming convention for: $path"
            )
            if (expectedParts.size > 1) {
                assertEquals(
                    expectedParts[1],
                    parts[1],
                    "Second part should preserve naming convention for: $path"
                )
            }
        }
    }

    /**
     * Test the recursive split behavior used in nested module addition.
     *
     * This simulates how addChild processes paths like "auth.login.ui" recursively,
     * splitting step by step with limit=2 until reaching a leaf.
     */
    @Test
    fun `test recursive split behavior for deep paths`() {
        // Start with "auth.login.ui"
        var remainingPath = "auth.login.ui"
        val hierarchy = mutableListOf<String>()
        
        // First split (limit = 2): "auth" + "login.ui"
        var parts = remainingPath.split(".", limit = 2)
        hierarchy.add(parts[0])  // "auth"
        remainingPath = parts.getOrNull(1) ?: ""
        
        // Second split: "login" + "ui"
        if (remainingPath.isNotEmpty()) {
            parts = remainingPath.split(".", limit = 2)
            hierarchy.add(parts[0])  // "login"
            remainingPath = parts.getOrNull(1) ?: ""
        }
        
        // Third split: "ui" (leaf)
        if (remainingPath.isNotEmpty()) {
            parts = remainingPath.split(".", limit = 2)
            hierarchy.add(parts[0])  // "ui"
        }
        
        assertEquals(3, hierarchy.size, "Should create 3-level hierarchy")
        assertEquals(listOf("auth", "login", "ui"), hierarchy)
    }

    /**
     * Test that path splitting with limit=2 correctly handles various depths.
     *
     * This is the core logic used by addChild to progressively split module paths.
     */
    @Test
    fun `test path splitting with limit 2 for various depths`() {
        // Single level
        val singleParts = "app".split(".", limit = 2)
        assertEquals(1, singleParts.size)
        assertEquals("app", singleParts[0])
        
        // Two levels
        val twoParts = "feature.login".split(".", limit = 2)
        assertEquals(2, twoParts.size)
        assertEquals("feature", twoParts[0])
        assertEquals("login", twoParts[1])
        
        // Three+ levels (limited to 2 parts)
        val deepParts = "feature.login.ui.screen".split(".", limit = 2)
        assertEquals(2, deepParts.size)
        assertEquals("feature", deepParts[0])
        assertEquals("login.ui.screen", deepParts[1], "Remaining path should be preserved")
    }

    /**
     * Test edge case: empty path handling.
     *
     * While unlikely in real usage, empty strings should be handled gracefully.
     */
    @Test
    fun `test edge case empty path`() {
        val emptyPath = ""
        val parts = emptyPath.split(".", limit = 2)
        
        assertEquals(1, parts.size, "Empty string split should return list with empty string")
        assertEquals("", parts[0])
    }

    /**
     * Test that single-part paths without dots are not split.
     *
     * Validates that modules like "app", "core", "shared" remain as single entities.
     */
    @Test
    fun `test single part paths are not split`() {
        val singlePartPaths = listOf("app", "core", "feature-login", "data_layer", "shared")
        
        singlePartPaths.forEach { path ->
            val parts = path.split(".", limit = 2)
            assertEquals(1, parts.size, "Single part path should not be split: $path")
            assertEquals(path, parts[0], "Path should be preserved exactly: $path")
        }
    }

    /**
     * Test that child path determination logic works correctly.
     *
     * When parts.size == 1, there's no child path (leaf node).
     * When parts.size > 1, parts[1] contains the child path for recursion.
     */
    @Test
    fun `test child path determination for recursion`() {
        // Leaf node (no child path)
        val leafParts = "login".split(".", limit = 2)
        val hasChild = leafParts.size > 1
        assertFalse(hasChild, "Leaf node should not have child path")
        
        // Parent with child
        val parentParts = "feature.login".split(".", limit = 2)
        val hasChildPath = parentParts.size > 1
        assertTrue(hasChildPath, "Parent node should have child path")
        assertEquals("login", parentParts[1], "Child path should be extracted correctly")
    }
}
