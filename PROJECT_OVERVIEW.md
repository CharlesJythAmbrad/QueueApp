# Smart Queue & Appointment System

## Project Overview
A smart queue and appointment management application designed for students transacting with the school finance office. The system allows students to book appointments for tuition-related concerns, receive queue numbers, and track their turn in real time.

## Key Features

### 1. **Authentication System**
- Firebase Authentication integration
- Secure login and registration
- User session management
- Automatic authentication checks

### 2. **Appointment Booking**
- Transaction type selection:
  - 💰 Down Payment
  - 📅 Installment Payment
  - 💳 Balance Payment
- Date and time selection
- Optional notes field
- Automatic queue number generation
- Real-time Firestore integration

### 3. **Queue Management**
- **Automatic Queue Number Assignment**: Format `YYYYMMDD-XXX`
- **Real-Time Queue Tracking**: Live updates from Firestore
- **Current Queue Display**: Shows who's being served
- **Position Tracking**: Shows your position in queue
- **Refresh Functionality**: Manual queue refresh

### 4. **Appointment History**
- View all past appointments
- Status tracking (Pending, Completed, Cancelled)
- Transaction type history
- Date and time records
- Queue number reference

### 5. **Dashboard**
- Quick access to all features
- User information display
- Clean, intuitive interface
- Easy navigation

## Technical Implementation

### **Firestore Database Structure**

```
appointments/
├── [document-id]/
│   ├── studentUID: String (Firebase Auth UID)
│   ├── studentEmail: String
│   ├── transactionType: String
│   ├── appointmentDate: String (YYYY-MM-DD)
│   ├── appointmentTime: String (HH:MM)
│   ├── notes: String
│   ├── queueNumber: String (YYYYMMDD-XXX)
│   ├── status: String (Pending/Completed/Cancelled)
│   └── timestamp: ServerTimestamp
```

### **Activities**

1. **SplashActivity**: App entry point with branding
2. **LoginActivity**: User authentication
3. **RegisterActivity**: New user registration
4. **DashboardActivity**: Main hub after login
5. **BookAppointmentActivity**: Create new appointments
6. **ViewQueueActivity**: Real-time queue monitoring
7. **AppointmentHistoryActivity**: View past appointments

### **Data Models**

- **Appointment**: Core data model for appointments
  - Includes queue number, transaction type, status
  - Helper methods for formatting and display

### **Adapters**

- **AppointmentAdapter**: RecyclerView adapter for appointment history
- **QueueAdapter**: RecyclerView adapter for queue display

## User Flow

```
Splash Screen
    ↓
Login/Register
    ↓
Dashboard
    ├→ Book Appointment
    │   ├→ Select Transaction Type
    │   ├→ Choose Date & Time
    │   ├→ Add Notes (optional)
    │   └→ Submit (Get Queue Number)
    │
    ├→ View Queue Status
    │   ├→ See Current Queue
    │   ├→ Check Your Position
    │   └→ Refresh Updates
    │
    └→ Appointment History
        └→ View All Past Appointments
```

## Key Benefits

✅ **Reduces Waiting Time**: Students know their queue position  
✅ **Eliminates Confusion**: Clear queue number system  
✅ **Improves Efficiency**: Finance office can manage flow better  
✅ **Convenient Scheduling**: Book appointments in advance  
✅ **Transaction Tracking**: Complete history of all transactions  
✅ **Real-Time Updates**: Live queue status monitoring  

## Security Features

- Firebase Authentication for secure access
- User-specific data isolation
- Server-side timestamp validation
- Secure Firestore rules (to be configured)

## Future Enhancements

📱 **Push Notifications**: Remind users when their turn is near  
🔔 **Alerts**: Notify when queue position changes  
📊 **Analytics**: Track peak hours and transaction patterns  
👨‍💼 **Admin Panel**: Finance office staff management interface  
⏰ **Time Slots**: Limit appointments per time slot  
📧 **Email Confirmations**: Send appointment confirmations  

## Dependencies

- Firebase Authentication
- Cloud Firestore
- AndroidX Libraries
- Material Design Components
- RecyclerView
- CardView

## Installation & Setup

1. Clone the repository
2. Add `google-services.json` to `app/` directory
3. Configure Firebase project
4. Set up Firestore database
5. Build and run the application

## Firestore Security Rules (Recommended)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /appointments/{appointmentId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null && 
                      request.resource.data.studentUID == request.auth.uid;
      allow update, delete: if request.auth != null && 
                              resource.data.studentUID == request.auth.uid;
    }
  }
}
```

## Testing Checklist

- [ ] User registration and login
- [ ] Appointment booking with all transaction types
- [ ] Queue number generation
- [ ] Real-time queue updates
- [ ] Appointment history display
- [ ] Status color coding
- [ ] Navigation between screens
- [ ] Logout functionality
- [ ] Error handling
- [ ] Empty state displays

## Support

For issues or questions, please contact the development team.

---

**Version**: 1.0.0  
**Last Updated**: January 2024  
**Platform**: Android  
**Minimum SDK**: 24 (Android 7.0)  
**Target SDK**: 36
