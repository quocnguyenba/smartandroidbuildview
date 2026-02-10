# GitHub Actions Workflows

This directory contains GitHub Actions workflows for automated building and releasing of the plugin.

## Workflows

### 1. Build Plugin (`build.yml`)

**Triggers:**
- Push to `main` branch
- Pull requests to `main` branch

**Actions:**
- Sets up Java 17 and Gradle
- Caches IntelliJ SDK downloads (~500MB) for faster builds
- Builds the plugin, runs tests, and verifies in a single optimized step
- Uploads build artifacts (available for 30 days)

**Performance Optimizations:**
- ⚡ **IntelliJ SDK caching**: Reuses downloaded SDK between runs (saves ~2-3 minutes)
- ⚡ **Gradle dependency caching**: Automatic via `setup-gradle@v3`
- ⚡ **Read-only cache for PRs**: Prevents cache pollution from PR builds
- ⚡ **Parallel execution**: Runs tasks concurrently when possible
- ⚡ **Incremental builds**: Single Gradle command leverages incremental compilation

**Artifacts:**
- `plugin-artifact`: The built plugin ZIP file
- `test-results`: Test execution results

### 2. Release Plugin (`release.yml`)

**Triggers:**
- Git tags matching pattern `v*` (e.g., `v1.0.0`, `v1.0.1`)

**Actions:**
- Caches IntelliJ SDK for faster builds
- Builds and verifies the plugin
- Creates a GitHub Release with the plugin ZIP attached
- Generates release notes automatically

**Performance Optimizations:**
- ⚡ Same caching strategies as build workflow
- ⚡ Parallel task execution for faster releases

**Creating a Release:**

```bash
# Create and push a tag
git tag v1.0.0
git push origin v1.0.0
```

## Performance & Caching

### What's Cached

1. **IntelliJ SDK** (`~500MB`)
   - Location: `~/.cache/JetBrains`, `build/idea-sandbox`
   - Saves: 2-3 minutes per build
   - Cache key: Based on `build.gradle.kts` hash

2. **Gradle Dependencies** (automatic via `setup-gradle@v3`)
   - Gradle wrapper
   - Maven/Gradle dependencies
   - Build cache
   - Saves: 1-2 minutes per build

### Cache Strategy

- **Main branch**: Read-write cache (updates cache)
- **Pull requests**: Read-only cache (uses cache, doesn't update)
- **Cache invalidation**: Automatic when `build.gradle.kts` changes

### Typical Build Times

| Scenario | Without Cache | With Cache | Savings |
|----------|---------------|------------|---------|
| First build | ~5-6 minutes | ~5-6 minutes | 0% |
| Subsequent builds | ~5-6 minutes | ~2-3 minutes | 50-60% |
| PR builds | ~5-6 minutes | ~2-3 minutes | 50-60% |

### Publishing to JetBrains Marketplace (Optional)

To enable automatic publishing to JetBrains Marketplace:

1. Get your Marketplace token from [JetBrains Hub](https://plugins.jetbrains.com/author/me/tokens)
2. Add it as a GitHub secret: `Settings → Secrets → Actions → New repository secret`
   - Name: `JETBRAINS_MARKETPLACE_TOKEN`
   - Value: Your token
3. Uncomment the "Publish to JetBrains Marketplace" step in `release.yml`

## Local Testing

You can test the build locally before pushing:

```bash
# Build the plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Verify plugin structure
./gradlew verifyPlugin

# Check the output
ls -lh build/distributions/
```

## Troubleshooting

- **Build fails on JDK version**: Ensure your local Java version matches the CI (Java 17)
- **Gradle wrapper not executable**: Run `chmod +x gradlew` locally
- **Tests failing**: Run `./gradlew test --info` for detailed logs
- **"Post Setup Gradle" error about 'removeUnusedEntriesOlderThan'**: This was a known issue with `gradle/actions/setup-gradle@v3` cache cleanup. Fixed by removing the `gradle-home-cache-cleanup` option (Gradle caching still works automatically)
- **Slow builds after dependency changes**: Cache may need to be cleared. Go to `Actions → Caches` in GitHub repo settings and delete the cache
- **Build fails after upgrading IntelliJ version**: Delete the `intellij-sdk` cache to download the new SDK version
- **Cache hit but build still slow**: Check if IntelliJ SDK version changed in `build.gradle.kts` - this invalidates the cache automatically
