# GitHub Actions Workflows

This directory contains GitHub Actions workflows for automated building and releasing of the plugin.

## Workflows

### 1. Build Plugin (`build.yml`)

**Triggers:**
- Push to `main` branch
- Pull requests to `main` branch

**Actions:**
- Sets up Java 17 and Gradle
- Builds the plugin
- Runs tests
- Verifies plugin structure
- Uploads build artifacts (available for 30 days)

**Artifacts:**
- `plugin-artifact`: The built plugin ZIP file
- `test-results`: Test execution results

### 2. Release Plugin (`release.yml`)

**Triggers:**
- Git tags matching pattern `v*` (e.g., `v1.0.0`, `v1.0.1`)

**Actions:**
- Builds and verifies the plugin
- Creates a GitHub Release with the plugin ZIP attached
- Generates release notes automatically

**Creating a Release:**

```bash
# Create and push a tag
git tag v1.0.0
git push origin v1.0.0
```

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
