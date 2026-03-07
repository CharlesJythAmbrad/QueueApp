# QUICK FIX: Permission Denied Error

## Problem
Getting error: "ERROR: PERMISSION DENIED: Missing or insufficient permissions"

## Solution (2 Minutes)

### Step 1: Open Firebase Console
1. Go to https://console.firebase.google.com/
2. Select your project
3. Click **Firestore Database** (left menu)
4. Click **Rules** tab (top)

### Step 2: Copy & Paste These Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    match /appointments/{appointmentId} {
      allow create: if request.auth != null;
      allow read: if request.auth != null;
      allow update: if request.auth != null && resource.data.studentUID == request.auth.uid;
      allow delete: if request.auth != null && resource.data.studentUID == request.auth.uid;
    }
  }
}
```

### Step 3: Click Publish

### Step 4: Test App
- Login to app
- Go to Request Queue
- Fill form
- Click Submit
- Should work now! ✅

---

## What This Does
- Allows logged-in users to create queue requests
- Allows users to read all appointments (for dashboard)
- Allows users to update/delete only their own appointments
- Blocks access for non-logged-in users

---

## Still Not Working?

### Option A: Temporary Open Access (Testing Only)
⚠️ **Use only for testing! Not secure for production!**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

### Option B: Check These:
1. Are you logged in? (Check Firebase Console → Authentication)
2. Did you click Publish after pasting rules?
3. Is your internet working?
4. Try uninstalling and reinstalling the app

---

**That's it! Your app should work now.**
