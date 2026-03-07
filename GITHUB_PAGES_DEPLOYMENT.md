# GitHub Pages Deployment Guide

This guide will help you deploy the Admin Web Panel to GitHub Pages so Finance and Registrar staff can access it online.

## Prerequisites

1. A GitHub account
2. Your project pushed to a GitHub repository
3. Firebase project already set up (with the config in `admin-web/app.js`)

## Step-by-Step Deployment

### 1. Push Your Code to GitHub

If you haven't already, create a repository and push your code:

```bash
git init
git add .
git commit -m "Initial commit with admin panel"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
git push -u origin main
```

### 2. Enable GitHub Pages

1. Go to your repository on GitHub
2. Click **Settings** (top menu)
3. Click **Pages** (left sidebar)
4. Under "Build and deployment":
   - Source: Select **GitHub Actions**
5. Click **Save**

### 3. Trigger the Deployment

The workflow will automatically deploy when you:
- Push changes to the `admin-web/` folder
- Or manually trigger it from the Actions tab

To manually trigger:
1. Go to **Actions** tab in your repository
2. Click **Deploy Admin Panel to GitHub Pages**
3. Click **Run workflow** → **Run workflow**

### 4. Access Your Admin Panel

After deployment completes (1-2 minutes):
- Your admin panel will be available at: `https://YOUR_USERNAME.github.io/YOUR_REPO_NAME/`
- Example: `https://johnsmith.github.io/medicare-queue-system/`

### 5. Share the URL

Give this URL to Finance and Registrar staff:
- Finance staff: Use password `finance2026`
- Registrar staff: Use password `registrar2026`

## Important Notes

### Security Considerations

⚠️ **Your Firebase config is public** when deployed to GitHub Pages. This is normal for client-side apps, but:
- Make sure your Firestore rules are properly configured (already done in `FIRESTORE_RULES.txt`)
- The rules prevent unauthorized data access
- Only status updates are allowed without authentication

### Updating the Admin Panel

Whenever you make changes to files in `admin-web/`:
1. Commit and push to GitHub
2. The workflow automatically redeploys
3. Changes appear in 1-2 minutes

```bash
git add admin-web/
git commit -m "Update admin panel"
git push
```

### Custom Domain (Optional)

If you want a custom domain like `queue.yourschool.edu`:
1. Go to Settings → Pages
2. Add your custom domain
3. Follow GitHub's DNS configuration instructions

## Troubleshooting

### Deployment Failed
- Check the **Actions** tab for error details
- Ensure GitHub Pages is enabled in Settings

### Admin Panel Not Loading
- Check browser console for errors (F12)
- Verify Firebase config in `admin-web/app.js` is correct
- Ensure Firestore rules are published

### Can't Update Queue Status
- Verify Firestore rules match `FIRESTORE_RULES.txt`
- Check browser console for permission errors
- Ensure Firebase project is active

## Alternative: Local Network Access

If you prefer not to use GitHub Pages, you can run locally:

```bash
cd admin-web
python -m http.server 8000
```

Then access at: `http://localhost:8000`

For network access, use your computer's IP address:
`http://192.168.1.XXX:8000`

## Support

If you encounter issues:
1. Check the Actions tab for deployment logs
2. Verify Firebase configuration
3. Test locally first using the local server method above
