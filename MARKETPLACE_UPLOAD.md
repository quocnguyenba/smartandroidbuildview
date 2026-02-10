# JetBrains Marketplace Upload Guide

This guide walks you through uploading your plugin to JetBrains Marketplace.

## Prerequisites

✅ **License**: Apache 2.0 (added - see `LICENSE` file)  
✅ **Build configuration**: Ready  
✅ **Tests**: 24 tests passing  
✅ **Plugin verified**: Compatible with IntelliJ 2024.1+  

## Step 1: Build the Plugin

```bash
./gradlew buildPlugin verifyPlugin
```

The plugin ZIP will be created at:
```
build/distributions/Android + Build Files View-1.0.0.zip
```

## Step 2: Prepare Marketplace Information

### Required Information

1. **Plugin Name**: Android + Build Files View
2. **Description**: Already in `plugin.xml`
3. **License**: Apache 2.0 ✅
4. **Vendor**: Quoc Nguyen
5. **Email**: ng.baquoc96@gmail.com
6. **Category**: Choose "Project Management" or "User Interface"

### Plugin Tags (Recommended)

Add these tags when uploading:
- `android`
- `project-view`
- `build-files`
- `gradle`
- `productivity`

## Step 3: Upload to JetBrains Marketplace

### Manual Upload

1. Go to [JetBrains Marketplace](https://plugins.jetbrains.com/)
2. Click **"Sign In"** (use your JetBrains account)
3. Go to **"Upload Plugin"**
4. Fill in the form:
   - **Plugin file**: Upload the ZIP from `build/distributions/`
   - **License**: Select **"Apache 2.0"** from dropdown
   - **Category**: Project Management or User Interface
   - **Tags**: Add the recommended tags above
   - **Screenshots**: Optional but highly recommended
   - **Documentation URL**: `https://github.com/quocnguyenba/smartandroidbuildview`

### Automated Upload (GitHub Actions)

Already configured in `.github/workflows/release.yml`. To enable:

1. Get your JetBrains Marketplace token:
   - Go to [https://plugins.jetbrains.com/author/me/tokens](https://plugins.jetbrains.com/author/me/tokens)
   - Click **"Generate New Token"**
   - Give it a name (e.g., "GitHub Actions")
   - Copy the token

2. Add token to GitHub Secrets:
   - Go to your repo: `Settings → Secrets and variables → Actions`
   - Click **"New repository secret"**
   - Name: `JETBRAINS_MARKETPLACE_TOKEN`
   - Value: Paste the token

3. Uncomment the publish step in `.github/workflows/release.yml`:
   ```yaml
   - name: Publish to JetBrains Marketplace
     run: ./gradlew publishPlugin --no-daemon
     env:
       PUBLISH_TOKEN: ${{ secrets.JETBRAINS_MARKETPLACE_TOKEN }}
   ```

4. Create a release:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

## Step 4: Add Screenshots (Highly Recommended)

Screenshots significantly increase downloads. Capture:

1. **The project view dropdown** showing "Android + Build" option
2. **The view itself** showing modules with build files visible
3. **Before/After comparison** (Android view vs Android + Build view)

### Recommended Screenshot Sizes
- Width: 1200px - 2400px
- Height: 800px - 1600px
- Format: PNG or JPEG

## Step 5: Post-Upload Checklist

After uploading:

- [ ] Verify plugin appears in your account
- [ ] Check that all information is correct
- [ ] Test installation from marketplace in a fresh IDE
- [ ] Monitor the approval process (usually 1-3 business days)

## License Declaration for Marketplace

When asked about the license on JetBrains Marketplace:

1. **Select**: Apache License 2.0
2. **License URL**: `https://github.com/quocnguyenba/smartandroidbuildview/blob/main/LICENSE`

The `LICENSE` file in your repository contains the full Apache 2.0 license text.

## Important Notes

### Version Numbers

Follow semantic versioning:
- `1.0.0` - Initial release
- `1.0.1` - Bug fixes
- `1.1.0` - New features (backward compatible)
- `2.0.0` - Breaking changes

Update version in:
1. `build.gradle.kts` (line 10)
2. `plugin.xml` (line 4)

### Change Notes

Update `plugin.xml` with change notes for each release:

```xml
<change-notes><![CDATA[
<h2>Version 1.0.0</h2>
<ul>
  <li>Initial release</li>
  <li>Android + Build project view</li>
  <li>Build files visible under each module</li>
  <li>Customizable file visibility settings</li>
</ul>
]]></change-notes>
```

## Support & Updates

After publishing:

1. **Monitor issues**: Check plugin reviews and GitHub issues
2. **Regular updates**: Keep compatible with new IntelliJ versions
3. **Respond to feedback**: Engage with users who report issues
4. **Version updates**: Test with new IntelliJ releases

## Useful Links

- [JetBrains Plugin Repository](https://plugins.jetbrains.com/)
- [Plugin Publishing Guidelines](https://plugins.jetbrains.com/docs/marketplace/plugin-overview-page.html)
- [Your Marketplace Profile](https://plugins.jetbrains.com/author/me)
- [Plugin Analytics](https://plugins.jetbrains.com/author/me/downloads) (after publishing)

## Troubleshooting

### "License is required"
- ✅ Fixed! The `LICENSE` file and Apache 2.0 declaration are now in place

### "Version already exists"
- Increment version number in both `build.gradle.kts` and `plugin.xml`

### "Plugin verification failed"
- Run `./gradlew verifyPlugin` locally first
- Check compatibility range in `plugin.xml`

### "Upload size too large"
- Typical plugin size: 50KB - 500KB
- Your plugin: ~100KB (well within limits)

## Next Steps

1. ✅ License added (Apache 2.0)
2. Build the plugin: `./gradlew buildPlugin`
3. Upload to JetBrains Marketplace
4. (Optional) Take screenshots for better visibility
5. (Optional) Set up automated publishing via GitHub Actions

Good luck with your plugin launch! 🚀
