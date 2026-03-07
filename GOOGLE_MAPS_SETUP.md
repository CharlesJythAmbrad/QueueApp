# Google Maps Integration Guide
## Sambag 1, Urgello, Cebu City

This guide explains how to set up Google Maps for the Smart Queue System, focused on the Sambag 1, Urgello area in Cebu City.

---

## 📍 Location Details

**Area**: Sambag 1, Urgello, Cebu City  
**Coordinates**: 10.3157° N, 123.8854° E  
**Purpose**: Show Finance Office location and restrict map view to campus area

---

## 🔑 Step 1: Get Google Maps API Key

### 1. Go to Google Cloud Console
Visit: https://console.cloud.google.com/

### 2. Create or Select a Project
- Click "Select a project" → "New Project"
- Name: "Smart Queue System" or your preferred name
- Click "Create"

### 3. Enable Maps SDK for Android
- Go to "APIs & Services" → "Library"
- Search for "Maps SDK for Android"
- Click on it and press "Enable"

### 4. Create API Key
- Go to "APIs & Services" → "Credentials"
- Click "Create Credentials" → "API Key"
- Copy the API key (it will look like: `AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX`)

### 5. Restrict API Key (Recommended)
- Click on the API key you just created
- Under "Application restrictions":
  - Select "Android apps"
  - Click "Add an item"
  - Package name: `com.reymoto.medicare`
  - SHA-1 certificate fingerprint: Get from your keystore
- Under "API restrictions":
  - Select "Restrict key"
  - Check "Maps SDK for Android"
- Click "Save"

---

## 🔧 Step 2: Add API Key to Your App

### Update AndroidManifest.xml

Replace `YOUR_GOOGLE_MAPS_API_KEY_HERE` with your actual API key:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" />
```

**Location**: `app/src/main/AndroidManifest.xml` (already added in the code)

---

## 📱 Step 3: Get SHA-1 Certificate Fingerprint

### For Debug Keystore:

**Windows:**
```cmd
cd C:\Users\YOUR_USERNAME\.android
keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Mac/Linux:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

### For Release Keystore:
```cmd
keytool -list -v -keystore your-release-key.jks -alias your-key-alias
```

Copy the SHA-1 fingerprint and add it to your API key restrictions in Google Cloud Console.

---

## 🗺️ Map Configuration

### Current Settings:

**Center Point**: Sambag 1, Urgello  
- Latitude: 10.3157  
- Longitude: 123.8854

**Map Boundaries** (restricts view to area):
- Southwest: 10.3100, 123.8800
- Northeast: 10.3200, 123.8900

**Zoom Levels**:
- Minimum: 15 (prevents zooming out too far)
- Maximum: 18 (prevents zooming in too close)
- Default: 16 (good overview of area)

**Finance Office Marker**:
- Blue marker at coordinates: 10.3157, 123.8854
- Title: "Finance Office"
- Snippet: "PHINMA University Finance Office"

---

## 🎨 Customization Options

### Change Finance Office Location

Edit `MapFragment.kt`:

```kotlin
private val financeOfficeLocation = LatLng(YOUR_LAT, YOUR_LONG)
```

### Adjust Map Boundaries

Edit the bounds in `MapFragment.kt`:

```kotlin
private val sambag1Bounds = LatLngBounds(
    LatLng(SOUTH_LAT, WEST_LONG),  // Southwest corner
    LatLng(NORTH_LAT, EAST_LONG)   // Northeast corner
)
```

### Change Marker Color

```kotlin
.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
// Options: HUE_RED, HUE_BLUE, HUE_GREEN, HUE_ORANGE, etc.
```

### Change Map Type

```kotlin
googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
// Options: MAP_TYPE_NORMAL, MAP_TYPE_SATELLITE, MAP_TYPE_HYBRID, MAP_TYPE_TERRAIN
```

---

## 🔐 Permissions

The following permissions are already added to AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

Users will be prompted to grant location permission when they first open the map.

---

## 🧪 Testing

### 1. Test on Emulator
- Open Android Studio
- Run the app on an emulator
- Navigate to the Map tab in bottom navigation
- The map should show Sambag 1, Urgello area

### 2. Test on Real Device
- Enable Developer Options on your Android device
- Connect via USB
- Run the app
- Grant location permission when prompted
- Your current location should appear on the map (if you're in the area)

---

## 🎯 Features Implemented

✅ **Map View**: Focused on Sambag 1, Urgello, Cebu City  
✅ **Finance Office Marker**: Shows exact location  
✅ **Boundary Restriction**: Limits map view to campus area  
✅ **Zoom Controls**: Prevents excessive zoom in/out  
✅ **User Location**: Shows current location (with permission)  
✅ **Area Highlight**: Polygon outline of Sambag 1 area  
✅ **Bottom Navigation**: Easy access via Map tab  

---

## 🚀 Advanced Features (Optional)

### Add Multiple Markers

```kotlin
// Add more locations
val library = LatLng(10.3160, 123.8860)
googleMap.addMarker(
    MarkerOptions()
        .position(library)
        .title("Library")
        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
)
```

### Add Directions

```kotlin
// Show route from user location to finance office
// Requires Directions API (additional setup)
```

### Geofencing

```kotlin
// Alert when user enters/exits Sambag 1 area
// Useful for attendance tracking
```

---

## ❗ Troubleshooting

### Map Not Showing
1. Check if API key is correct
2. Verify SHA-1 fingerprint is added to API key restrictions
3. Ensure Maps SDK for Android is enabled
4. Check internet connection

### "Authorization failure" Error
- SHA-1 fingerprint doesn't match
- API key restrictions are too strict
- Wrong package name in restrictions

### Location Not Showing
- Location permission not granted
- GPS is disabled on device
- Not in the Sambag 1 area

---

## 📚 Resources

- [Google Maps Android Documentation](https://developers.google.com/maps/documentation/android-sdk)
- [Maps SDK for Android Guide](https://developers.google.com/maps/documentation/android-sdk/start)
- [API Key Best Practices](https://developers.google.com/maps/api-security-best-practices)

---

## 💡 Tips

1. **Never commit API keys to Git**: Use local.properties or environment variables
2. **Use API key restrictions**: Prevents unauthorized use
3. **Monitor API usage**: Check Google Cloud Console for quota
4. **Test on real device**: Emulator location may not be accurate
5. **Update coordinates**: Verify exact finance office location

---

**Last Updated**: January 2024  
**Map Center**: Sambag 1, Urgello, Cebu City  
**Coordinates**: 10.3157° N, 123.8854° E
