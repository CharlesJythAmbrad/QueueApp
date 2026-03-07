# Location Validation for Booking System
## Geofencing for Sambag 1 & 2, Urgello, Cebu City

This guide explains how the location validation system works to ensure appointments can only be booked from within the Sambag 1 or Sambag 2, Urgello area.

---

## 🎯 Purpose

**Security Feature**: Prevents students from booking appointments when they're not physically on campus.

**Use Case**: Ensures students are actually present at the university when booking finance office appointments.

---

## 📍 Geofencing Configuration

### Location Parameters

```kotlin
// Sambag 1 center point
SAMBAG1_CENTER_LAT = 10.3157
SAMBAG1_CENTER_LNG = 123.8854

// Sambag 2 center point
SAMBAG2_CENTER_LAT = 10.3180
SAMBAG2_CENTER_LNG = 123.8870

// Allowed radius for each area (600 meters = 0.6 km)
ALLOWED_RADIUS_METERS = 600.0
```

### Coverage Areas

- **Sambag 1**: 600-meter radius from Finance Office location
- **Sambag 2**: 600-meter radius from Sambag 2 center
- **Total Coverage**: Both barangays in Urgello area

---

## 🔒 How It Works

### 1. Location Check on Page Load

When the booking page opens:
- Automatically requests user's current location
- Checks if location is within Sambag 1 OR Sambag 2 area
- Displays status message:
  - ✅ Green: "You are in Sambag 1 or 2, Urgello - Booking allowed"
  - ❌ Red: "You must be in Sambag 1 or 2, Urgello to book"
  - 📍 Orange: "Location permission required" or "Unable to get location"

### 2. Location Validation on Submit

When user clicks "Book Appointment":
1. Requests fresh location data
2. Calculates distance from both Sambag 1 and Sambag 2 centers
3. Validates if within 600m radius of either location
4. If valid: Proceeds with booking
5. If invalid: Shows error and blocks booking

### 3. Distance Calculation

Uses Android's `Location.distanceBetween()` method for both areas:
```kotlin
// Check Sambag 1
Location.distanceBetween(
    userLatitude, userLongitude,
    SAMBAG1_CENTER_LAT, SAMBAG1_CENTER_LNG,
    resultsToSambag1
)

// Check Sambag 2
Location.distanceBetween(
    userLatitude, userLongitude,
    SAMBAG2_CENTER_LAT, SAMBAG2_CENTER_LNG,
    resultsToSambag2
)

// Valid if within either radius
return resultsToSambag1[0] <= 600 || resultsToSambag2[0] <= 600
```

Returns distance in meters for precise validation.

---

## 🔐 Security Features

### ✅ What's Protected

1. **Dual-Area Validation**: Checks both Sambag 1 and Sambag 2 locations
2. **Real-time Validation**: Checks location at booking time, not just on page load
3. **GPS Required**: Must have GPS enabled to book
4. **Permission Required**: Location permission must be granted
5. **Distance-based**: Uses actual GPS coordinates, not IP or network location
6. **Stored Location**: Saves booking location (lat/lng) in Firestore for audit

### ❌ What's Blocked

- Booking from home
- Booking from other barangays
- Booking without GPS enabled
- Booking without location permission
- Booking with mock/fake locations (can be enhanced)

---

## 📱 User Experience Flow

### Scenario 1: User is in Sambag 1 or 2

1. Opens booking page
2. Sees: ✅ "You are in Sambag 1 or 2, Urgello - Booking allowed"
3. Fills form
4. Clicks "Book Appointment"
5. Button shows: "Checking location..."
6. Button shows: "Booking..."
7. Success: "Appointment booked! Queue #XXXXXX"

### Scenario 2: User is Outside Both Areas

1. Opens booking page
2. Sees: ❌ "You must be in Sambag 1 or 2, Urgello to book"
3. Fills form
4. Clicks "Book Appointment"
5. Button shows: "Checking location..."
6. Error: "You must be in Sambag 1 or 2, Urgello, Cebu City to book an appointment"
7. Button resets: "Book Appointment"

### Scenario 3: GPS Disabled

1. Opens booking page
2. Sees: 📍 "Unable to get location. Please enable GPS"
3. Clicks "Book Appointment"
4. Error: "Unable to get your location. Please enable GPS and try again."

### Scenario 4: Permission Denied

1. Opens booking page
2. Sees: 📍 "Location permission required"
3. Clicks "Book Appointment"
4. Shows permission dialog
5. If denied: "Location permission is required to book appointments"

---

## 🗄️ Data Stored in Firestore

Each appointment now includes location data:

```javascript
{
  studentUID: "xxx",
  studentEmail: "xxx@student.edu",
  transactionType: "Down Payment",
  appointmentDate: "2024-01-15",
  appointmentTime: "10:00",
  notes: "...",
  queueNumber: "20240115-001",
  status: "Pending",
  latitude: 10.3157,      // User's location when booked
  longitude: 123.8854,    // User's location when booked
  timestamp: ServerTimestamp
}
```

### Benefits of Storing Location

- **Audit Trail**: Verify where appointment was booked
- **Analytics**: Track booking patterns by location (Sambag 1 vs Sambag 2)
- **Dispute Resolution**: Proof of on-campus booking
- **Security**: Detect suspicious booking patterns

---

## 🗺️ Map Visualization

The Map tab shows both allowed areas:
- **Blue circle**: Sambag 1 area (600m radius)
- **Green circle**: Sambag 2 area (600m radius)
- **Blue marker**: Finance Office (Sambag 1)
- **Azure marker**: Sambag 2 reference point
- **User location**: Your current position (blue dot)

---

## ⚙️ Configuration Options

### Adjust Allowed Radius

Edit `BookAppointmentFragment.kt`:

```kotlin
// Make radius larger (800m)
private val ALLOWED_RADIUS_METERS = 800.0

// Make radius smaller (400m)
private val ALLOWED_RADIUS_METERS = 400.0
```

### Change Center Points

```kotlin
// Update to exact coordinates
private val SAMBAG1_CENTER_LAT = 10.3160
private val SAMBAG1_CENTER_LNG = 123.8855

private val SAMBAG2_CENTER_LAT = 10.3185
private val SAMBAG2_CENTER_LNG = 123.8875
```

### Add More Allowed Locations

```kotlin
private fun isLocationInAllowedArea(location: Location): Boolean {
    // Check Sambag 1
    if (isNearLocation(location, SAMBAG1_CENTER_LAT, SAMBAG1_CENTER_LNG, 600.0)) {
        return true
    }
    
    // Check Sambag 2
    if (isNearLocation(location, SAMBAG2_CENTER_LAT, SAMBAG2_CENTER_LNG, 600.0)) {
        return true
    }
    
    // Add Sambag 3 if needed
    if (isNearLocation(location, 10.3200, 123.8900, 600.0)) {
        return true
    }
    
    return false
}
```

---

## 🧪 Testing

### Test on Real Device (Recommended)

1. **In Sambag 1 Test**:
   - Go to Sambag 1, Urgello
   - Open app and navigate to Book Appointment
   - Should see green checkmark
   - Try booking - should succeed

2. **In Sambag 2 Test**:
   - Go to Sambag 2, Urgello
   - Open app and navigate to Book Appointment
   - Should see green checkmark
   - Try booking - should succeed

3. **Outside Both Areas Test**:
   - Go somewhere else in Cebu
   - Open app and navigate to Book Appointment
   - Should see red X
   - Try booking - should fail with error

### Test on Emulator

1. Open Android Studio
2. Run app on emulator
3. Open Extended Controls (⋮ button)
4. Go to Location tab
5. Set custom location:
   - **Sambag 1**: Lat: 10.3157, Lng: 123.8854 ✅
   - **Sambag 2**: Lat: 10.3180, Lng: 123.8870 ✅
   - **Outside**: Lat: 10.3000, Lng: 123.8700 ❌
6. Test booking with all three locations

---

## 🚨 Troubleshooting

### Location Always Shows "Unable to get location"

**Solutions**:
1. Enable GPS on device
2. Grant location permission
3. Go outside (GPS works better outdoors)
4. Wait a few seconds for GPS to acquire signal
5. Check if Google Play Services is installed

### Location Shows Wrong Area

**Solutions**:
1. Verify center coordinates are correct for both Sambag 1 and 2
2. Check if radius is appropriate (600m)
3. Wait for GPS to get accurate fix (not network location)
4. Test outdoors for better GPS accuracy

### Permission Dialog Not Showing

**Solutions**:
1. Check if permission already denied permanently
2. Go to App Settings → Permissions → Location → Allow
3. Uninstall and reinstall app
4. Clear app data

---

## 🔒 Security Enhancements (Optional)

### 1. Detect Mock Locations

```kotlin
// Add to location check
if (location.isFromMockProvider) {
    Toast.makeText(context, "Mock locations are not allowed", Toast.LENGTH_LONG).show()
    return
}
```

### 2. Require High Accuracy

```kotlin
if (location.accuracy > 50) { // More than 50 meters accuracy
    Toast.makeText(context, "Location accuracy too low. Please wait for better GPS signal", Toast.LENGTH_LONG).show()
    return
}
```

### 3. Time-based Validation

```kotlin
val locationAge = System.currentTimeMillis() - location.time
if (locationAge > 60000) { // Older than 1 minute
    Toast.makeText(context, "Location data is too old. Please refresh", Toast.LENGTH_LONG).show()
    return
}
```

---

## 📊 Analytics & Monitoring

### Track Booking Attempts by Area

```kotlin
// Determine which area user is in
val area = when {
    isNearSambag1(location) -> "Sambag 1"
    isNearSambag2(location) -> "Sambag 2"
    else -> "Outside"
}

// Log to Firestore for analytics
val attemptData = hashMapOf(
    "studentUID" to currentUser?.uid,
    "latitude" to location.latitude,
    "longitude" to location.longitude,
    "area" to area,
    "allowed" to isInAllowedArea,
    "timestamp" to FieldValue.serverTimestamp()
)
db.collection("booking_attempts").add(attemptData)
```

### Generate Reports

- Bookings by area (Sambag 1 vs Sambag 2)
- Failed booking attempts
- Most common booking locations
- Off-campus booking attempts

---

## ✅ Benefits

1. **Expanded Coverage**: Accepts bookings from both Sambag 1 and Sambag 2
2. **Security**: Prevents remote/fraudulent bookings
3. **Fairness**: Ensures equal access for on-campus students
4. **Accountability**: Audit trail of booking locations
5. **Compliance**: Enforces on-campus booking policy
6. **Analytics**: Understand student booking patterns by area

---

## 📝 Notes

- Location accuracy depends on GPS signal strength
- Indoor locations may have reduced accuracy
- First GPS fix may take 10-30 seconds
- Battery usage is minimal (only checks on booking)
- Works offline once location is acquired
- Both Sambag 1 and Sambag 2 are equally valid for booking

---

**Last Updated**: February 2026  
**Geofence Centers**:  
- Sambag 1: 10.3157° N, 123.8854° E  
- Sambag 2: 10.3180° N, 123.8870° E  
**Allowed Radius**: 600 meters (each area)  
**Coverage**: Sambag 1 & 2, Urgello, Cebu City
