# Smart Queue Admin Web Panel - Setup Guide

## Overview
This is a web-based admin panel for Finance and Registrar staff to manage the queue system. Staff can call the next queue, complete queues, and monitor real-time statistics.

## Features
- ✅ Real-time queue monitoring
- ✅ Call next queue button
- ✅ Complete queue button
- ✅ Skip queue functionality
- ✅ View pending queues list
- ✅ Daily statistics (Total, Completed, Pending)
- ✅ Separate access for Finance and Registrar departments
- ✅ Auto-updates when queues change

## Files Included
1. `index.html` - Main HTML structure
2. `styles.css` - Styling and layout
3. `app.js` - JavaScript logic and Firebase integration
4. `SETUP_GUIDE.md` - This file

## Setup Instructions

### 🚀 Quick Deploy to GitHub Pages (Recommended)

**For online access from any device, see [GITHUB_PAGES_DEPLOYMENT.md](../GITHUB_PAGES_DEPLOYMENT.md) for complete deployment instructions.**

This will give you a live URL like: `https://yourusername.github.io/your-repo/`

### Manual Setup (Alternative)

### Step 1: Configure Firebase

1. Open `app.js` file
2. Find the `firebaseConfig` object at the top
3. Replace the placeholder values with your Firebase project credentials:

```javascript
const firebaseConfig = {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
    appId: "YOUR_APP_ID"
};
```

**Where to find these values:**
- Go to Firebase Console (https://console.firebase.google.com)
- Select your project
- Click the gear icon ⚙️ > Project settings
- Scroll down to "Your apps" section
- Copy the config values

### Step 2: Update Firestore Security Rules

Add these rules to allow admin web access:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users collection
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Appointments collection
    match /appointments/{appointmentId} {
      // Allow anyone to create
      allow create: if request.auth != null;
      
      // Allow anyone to read (for dashboard and admin panel)
      allow read: if true;
      
      // Allow users to update/delete their own appointments
      allow update, delete: if request.auth != null && 
        (resource.data.studentUID == request.auth.uid || 
         request.resource.data.status in ['Serving', 'Completed', 'Pending']);
    }
  }
}
```

### Step 3: Deploy the Web Panel

#### Option A: Local Testing
1. Simply open `index.html` in a web browser
2. No server required for testing

#### Option B: Firebase Hosting (Recommended)
1. Install Firebase CLI:
   ```bash
   npm install -g firebase-tools
   ```

2. Login to Firebase:
   ```bash
   firebase login
   ```

3. Initialize Firebase Hosting:
   ```bash
   firebase init hosting
   ```
   - Select your project
   - Set public directory to current folder
   - Configure as single-page app: No
   - Don't overwrite index.html

4. Deploy:
   ```bash
   firebase deploy --only hosting
   ```

5. Access your admin panel at: `https://YOUR_PROJECT_ID.web.app`

#### Option C: Any Web Server
Upload all files to any web hosting service (GitHub Pages, Netlify, Vercel, etc.)

### Step 4: Admin Credentials

Default passwords (change these in `app.js`):
- **Finance Office:** `finance2026`
- **Registrar Office:** `registrar2026`

To change passwords, edit the `ADMIN_PASSWORDS` object in `app.js`:

```javascript
const ADMIN_PASSWORDS = {
    'Finance': 'your_new_finance_password',
    'Registrar': 'your_new_registrar_password'
};
```

## How to Use

### For Finance/Registrar Staff:

1. **Login**
   - Open the admin panel URL
   - Select your department (Finance or Registrar)
   - Enter the admin password
   - Click "Login"

2. **Call Next Queue**
   - The "Next in Queue" section shows the upcoming queue number
   - Click "📢 Call Next" button
   - The queue will move to "Now Serving" section
   - The mobile app will update automatically for students

3. **Complete Queue**
   - After serving the student, click "✓ Complete" button
   - The queue status changes to "Completed"
   - Next queue automatically becomes available

4. **Skip Queue**
   - If student is not present, click "⏭ Skip" button
   - Queue moves back to pending status
   - Next queue can be called

5. **Monitor Statistics**
   - View total queues for today
   - See completed count
   - Check pending count

## Troubleshooting

### Issue: "Permission Denied" Error
**Solution:** Update Firestore security rules (see Step 2)

### Issue: Queues Not Showing
**Solution:** 
- Check Firebase configuration in `app.js`
- Verify you're logged into correct department
- Check browser console for errors (F12)

### Issue: Can't Update Queue Status
**Solution:**
- Verify Firestore rules allow updates
- Check internet connection
- Refresh the page

### Issue: Real-time Updates Not Working
**Solution:**
- Check Firebase configuration
- Verify Firestore is enabled in Firebase Console
- Check browser console for connection errors

## Security Notes

⚠️ **Important Security Considerations:**

1. **Change Default Passwords:** The default passwords are for testing only
2. **Use HTTPS:** Always deploy on HTTPS (Firebase Hosting provides this automatically)
3. **Implement Proper Auth:** For production, consider using Firebase Authentication instead of hardcoded passwords
4. **Restrict Access:** Use Firebase Hosting rewrites to restrict access by IP if needed

## Advanced: Implementing Firebase Authentication

For better security, replace password authentication with Firebase Auth:

1. Enable Email/Password authentication in Firebase Console
2. Create admin accounts in Firebase Authentication
3. Update `app.js` to use `firebase.auth().signInWithEmailAndPassword()`
4. Add admin role to user documents in Firestore

## Support

For issues or questions:
1. Check browser console (F12) for error messages
2. Verify Firebase configuration
3. Check Firestore security rules
4. Ensure internet connection is stable

## Features Roadmap

Future enhancements:
- [ ] Audio notification when calling next queue
- [ ] Queue history view
- [ ] Export daily reports
- [ ] Multiple admin users with roles
- [ ] SMS notifications to students
- [ ] Queue time analytics

---

**Version:** 1.0  
**Last Updated:** March 2026  
**Compatible with:** Smart Queue Mobile App v1.0
