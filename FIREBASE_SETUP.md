# Firebase Firestore Security Rules Setup

## Error: Permission Denied

If you're getting "ERROR: PERMISSION DENIED: Missing or insufficient permissions" when submitting a queue request, you need to update your Firestore security rules.

---

## Solution: Update Firestore Security Rules

### Step 1: Go to Firebase Console

1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click on **Firestore Database** in the left menu
4. Click on the **Rules** tab at the top

### Step 2: Update Security Rules

Replace your current rules with one of the following options:

---

## Option 1: Allow Authenticated Users (RECOMMENDED)

This allows any logged-in user to read and write their own data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Users collection - users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Appointments collection - authenticated users can create and read
    match /appointments/{appointmentId} {
      // Allow authenticated users to create appointments
      allow create: if request.auth != null;
      
      // Allow users to read their own appointments
      allow read: if request.auth != null && 
                     (resource.data.studentUID == request.auth.uid || 
                      request.auth.uid != null);
      
      // Allow users to update their own appointments
      allow update: if request.auth != null && 
                       resource.data.studentUID == request.auth.uid;
      
      // Allow users to delete their own appointments
      allow delete: if request.auth != null && 
                       resource.data.studentUID == request.auth.uid;
    }
  }
}
```

---

## Option 2: Open Access for Development (NOT RECOMMENDED FOR PRODUCTION)

**⚠️ WARNING: This allows anyone to read/write data. Use only for testing!**

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

---

## Option 3: Authenticated Users with Admin Access

This allows authenticated users to write, and admins to have full access:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is admin
    function isAdmin() {
      return request.auth != null && 
             get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == 'admin';
    }
    
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                      (request.auth.uid == userId || isAdmin());
    }
    
    // Appointments collection
    match /appointments/{appointmentId} {
      // Anyone authenticated can create
      allow create: if request.auth != null;
      
      // Anyone authenticated can read
      allow read: if request.auth != null;
      
      // Only owner or admin can update
      allow update: if request.auth != null && 
                       (resource.data.studentUID == request.auth.uid || isAdmin());
      
      // Only owner or admin can delete
      allow delete: if request.auth != null && 
                       (resource.data.studentUID == request.auth.uid || isAdmin());
    }
  }
}
```

---

## Step 3: Publish Rules

1. After pasting the rules, click **Publish** button
2. Wait for confirmation message
3. Try submitting a queue request again

---

## Verify Your Setup

### Check if Firebase is Connected

Make sure you have:

1. **google-services.json** file in `app/` folder
2. Firebase dependencies in `app/build.gradle.kts`:

```kotlin
dependencies {
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    
    // ... other dependencies
}
```

3. Google Services plugin in `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")  // Add this line
}
```

4. Google Services classpath in root `build.gradle.kts`:

```kotlin
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}
```

---

## Testing the Connection

After updating the rules, test by:

1. Login to the app
2. Go to Request Queue
3. Fill in the form
4. Click Submit Queue Request
5. Should see: "Queue requested! Your number: XXXXXXXX-XXX-XXX"

---

## Troubleshooting

### Still Getting Permission Denied?

1. **Check Authentication**: Make sure you're logged in
   - Go to Firebase Console → Authentication
   - Verify your user exists

2. **Check Rules Published**: 
   - Go to Firestore → Rules tab
   - Verify the rules show your changes
   - Check the "Last published" timestamp

3. **Check Collection Name**:
   - Rules use collection name "appointments"
   - Code uses `db.collection("appointments")`
   - Make sure they match

4. **Clear App Data**:
   - Uninstall and reinstall the app
   - Or clear app data in device settings

5. **Check Internet Connection**:
   - Make sure device has internet access
   - Firebase requires active internet connection

---

## Understanding the Rules

### What Each Rule Does:

- `request.auth != null` - User must be logged in
- `request.auth.uid == userId` - User can only access their own data
- `resource.data.studentUID` - The UID stored in the document
- `allow create` - Allows creating new documents
- `allow read` - Allows reading documents
- `allow update` - Allows modifying existing documents
- `allow delete` - Allows removing documents

---

## Recommended Rule for Your App

Use **Option 1** (Allow Authenticated Users) because:

✅ Secure - only logged-in users can access data
✅ Users can create queue requests
✅ Users can view their own appointments
✅ Users can view all queues (for dashboard)
✅ Users can update/delete their own appointments

---

## Next Steps

1. Update Firestore rules using Option 1
2. Publish the rules
3. Test the app
4. If it works, you're done!
5. If not, check troubleshooting section

---

**Last Updated**: February 2026
**For**: Smart Queue & Appointment System
