package com.reymoto.medicare

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var financeListener: ListenerRegistration? = null
    private var registrarListener: ListenerRegistration? = null
    
    private lateinit var tvFinanceServing: TextView
    private lateinit var tvRegistrarServing: TextView
    private lateinit var tvFinancePending: TextView
    private lateinit var tvRegistrarPending: TextView
    
    private var financeCounter = 0
    private var registrarCounter = 0
    
    private val PREFS_NAME = "AdminQueuePrefs"
    private val FINANCE_COUNTER_KEY = "financeCounter"
    private val REGISTRAR_COUNTER_KEY = "registrarCounter"
    private val LAST_RESET_DATE_KEY = "lastResetDate"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
        
        db = FirebaseFirestore.getInstance()
        
        // Initialize views first
        tvFinanceServing = view.findViewById(R.id.tvFinanceServing)
        tvRegistrarServing = view.findViewById(R.id.tvRegistrarServing)
        tvFinancePending = view.findViewById(R.id.tvFinancePending)
        tvRegistrarPending = view.findViewById(R.id.tvRegistrarPending)
        
        // Check if daily reset is needed
        checkAndPerformDailyReset()
        
        // Load saved counters from SharedPreferences
        loadCounters()
        
        // Set initial counter values from local storage
        tvFinanceServing.text = financeCounter.toString()
        tvRegistrarServing.text = registrarCounter.toString()
        
        // Then sync with Firestore (this may update the UI if Firestore has newer values)
        initializeFirestoreCounters()
        
        val btnFinanceNext = view.findViewById<Button>(R.id.btnFinanceNext)
        val btnRegistrarNext = view.findViewById<Button>(R.id.btnRegistrarNext)
        
        // Button listeners
        btnFinanceNext.setOnClickListener { callNextQueue("Finance") }
        btnRegistrarNext.setOnClickListener { callNextQueue("Registrar") }
        
        // Setup real-time listeners for pending count
        setupRealtimeListeners()
        
        return view
    }

    private fun checkAndPerformDailyReset() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val lastResetDate = prefs.getString(LAST_RESET_DATE_KEY, "") ?: ""
        
        android.util.Log.d("AdminDashboard", "Checking daily reset - Today: '$today', Last reset: '$lastResetDate'")
        
        // Only reset if lastResetDate is empty (first time) or different from today
        if (lastResetDate.isEmpty()) {
            android.util.Log.d("AdminDashboard", "First time setup - setting reset date but not resetting counters")
            // First time, just set the date without resetting counters
            prefs.edit().putString(LAST_RESET_DATE_KEY, today).apply()
        } else if (lastResetDate != today) {
            // It's a new day, reset counters
            android.util.Log.d("AdminDashboard", "Performing daily reset - new day detected: '$lastResetDate' -> '$today'")
            resetCountersAutomatically()
            
            // Save today's date as the last reset date
            prefs.edit().putString(LAST_RESET_DATE_KEY, today).apply()
        } else {
            android.util.Log.d("AdminDashboard", "No daily reset needed - same day: '$lastResetDate' == '$today'")
        }
    }
    
    // Method to manually force a reset (for testing purposes)
    fun forceResetCounters() {
        android.util.Log.d("AdminDashboard", "Force resetting counters manually")
        resetCountersAutomatically()
        
        // Update the reset date to today
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        prefs.edit().putString(LAST_RESET_DATE_KEY, today).apply()
        
        // Update UI
        tvFinanceServing.text = "0"
        tvRegistrarServing.text = "0"
    }

    private fun resetCountersAutomatically() {
        android.util.Log.d("AdminDashboard", "Resetting counters automatically for new day")
        
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(FINANCE_COUNTER_KEY, 0)
            putInt(REGISTRAR_COUNTER_KEY, 0)
            apply()
        }
        
        financeCounter = 0
        registrarCounter = 0
        
        // Also reset the Firestore serving counters document
        val servingData = hashMapOf(
            "financeCounter" to 0,
            "registrarCounter" to 0,
            "lastUpdated" to FieldValue.serverTimestamp(),
            "resetDate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        
        db.collection("system").document("servingCounters")
            .set(servingData)
            .addOnSuccessListener {
                android.util.Log.d("AdminDashboard", "Firestore counters reset successfully")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AdminDashboard", "Error resetting serving counters: ${e.message}")
            }
    }

    private fun loadCounters() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        financeCounter = prefs.getInt(FINANCE_COUNTER_KEY, 0)
        registrarCounter = prefs.getInt(REGISTRAR_COUNTER_KEY, 0)
        
        android.util.Log.d("AdminDashboard", "Loaded counters from SharedPreferences - Finance: $financeCounter, Registrar: $registrarCounter")
        
        // Additional debug: check if prefs file exists and has data
        val allPrefs = prefs.all
        android.util.Log.d("AdminDashboard", "All SharedPreferences data: $allPrefs")
    }

    private fun initializeFirestoreCounters() {
        android.util.Log.d("AdminDashboard", "Initializing Firestore counters - Current local values: Finance=$financeCounter, Registrar=$registrarCounter")
        
        // Check if Firestore document exists and is from today
        db.collection("system").document("servingCounters")
            .get()
            .addOnSuccessListener { document ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                
                if (document.exists()) {
                    val resetDate = document.getString("resetDate") ?: ""
                    val lastUpdated = document.getTimestamp("lastUpdated")
                    val docDate = if (lastUpdated != null) {
                        dateFormat.format(lastUpdated.toDate())
                    } else {
                        ""
                    }
                    
                    android.util.Log.d("AdminDashboard", "Firestore document exists - Reset date: '$resetDate', Doc date: '$docDate', Today: '$today'")
                    
                    // Check if document is from today (either by resetDate or lastUpdated)
                    val isFromToday = resetDate == today || docDate == today
                    
                    if (isFromToday) {
                        // Document is from today, use Firestore values as the source of truth
                        val firestoreFinanceCounter = document.getLong("financeCounter")?.toInt() ?: 0
                        val firestoreRegistrarCounter = document.getLong("registrarCounter")?.toInt() ?: 0
                        
                        android.util.Log.d("AdminDashboard", "Document is from today - Firestore values: Finance=$firestoreFinanceCounter, Registrar=$firestoreRegistrarCounter")
                        android.util.Log.d("AdminDashboard", "Local values before sync: Finance=$financeCounter, Registrar=$registrarCounter")
                        
                        // Always use Firestore values for consistency across admin sessions
                        financeCounter = firestoreFinanceCounter
                        registrarCounter = firestoreRegistrarCounter
                        
                        // Update UI and save to SharedPreferences
                        tvFinanceServing.text = financeCounter.toString()
                        tvRegistrarServing.text = registrarCounter.toString()
                        saveCounters()
                        
                        android.util.Log.d("AdminDashboard", "Counters synchronized with Firestore - Finance: $financeCounter, Registrar: $registrarCounter")
                    } else {
                        // Document is from previous day, this should have been handled by daily reset
                        android.util.Log.d("AdminDashboard", "Document is from previous day - using local values and updating Firestore")
                        // Use current local values (which should be 0 from daily reset or preserved from previous session)
                        updateFirestoreCounters()
                    }
                } else {
                    // Document doesn't exist, create it with current local values
                    android.util.Log.d("AdminDashboard", "Firestore document doesn't exist - creating with local values: Finance=$financeCounter, Registrar=$registrarCounter")
                    updateFirestoreCounters()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AdminDashboard", "Error checking serving counters: ${e.message}")
                // Fallback: use local values and try to update Firestore
                android.util.Log.d("AdminDashboard", "Using local values as fallback - Finance: $financeCounter, Registrar: $registrarCounter")
                updateFirestoreCounters()
            }
    }

    private fun updateFirestoreCounters() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        val servingData = hashMapOf(
            "financeCounter" to financeCounter,
            "registrarCounter" to registrarCounter,
            "lastUpdated" to FieldValue.serverTimestamp(),
            "resetDate" to today
        )
        
        android.util.Log.d("AdminDashboard", "Updating Firestore counters - Finance: $financeCounter, Registrar: $registrarCounter")
        
        db.collection("system").document("servingCounters")
            .set(servingData)
            .addOnSuccessListener {
                android.util.Log.d("AdminDashboard", "Firestore counters updated successfully")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AdminDashboard", "Error updating serving counters: ${e.message}")
            }
    }

    private fun saveCounters() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(FINANCE_COUNTER_KEY, financeCounter)
            putInt(REGISTRAR_COUNTER_KEY, registrarCounter)
            apply()
        }
        
        android.util.Log.d("AdminDashboard", "Saved counters to SharedPreferences - Finance: $financeCounter, Registrar: $registrarCounter")
        
        // Verify the save worked
        val savedFinance = prefs.getInt(FINANCE_COUNTER_KEY, -1)
        val savedRegistrar = prefs.getInt(REGISTRAR_COUNTER_KEY, -1)
        android.util.Log.d("AdminDashboard", "Verified saved counters - Finance: $savedFinance, Registrar: $savedRegistrar")
    }

    private fun setupRealtimeListeners() {
        // Get today's date for filtering
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        // Finance pending count - only today's queues
        financeListener = db.collection("appointments")
            .whereEqualTo("department", "Finance")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    android.util.Log.e("AdminDashboard", "Error listening to Finance pending: ${error.message}")
                    return@addSnapshotListener
                }
                
                // Filter to only today's queues
                val todayPendingCount = snapshots?.documents?.count { doc ->
                    val queueNumber = doc.getString("queueNumber") ?: ""
                    queueNumber.startsWith(today)
                } ?: 0
                
                tvFinancePending.text = todayPendingCount.toString()
                android.util.Log.d("AdminDashboard", "Finance pending today: $todayPendingCount")
            }

        // Registrar pending count - only today's queues
        registrarListener = db.collection("appointments")
            .whereEqualTo("department", "Registrar")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    android.util.Log.e("AdminDashboard", "Error listening to Registrar pending: ${error.message}")
                    return@addSnapshotListener
                }
                
                // Filter to only today's queues
                val todayPendingCount = snapshots?.documents?.count { doc ->
                    val queueNumber = doc.getString("queueNumber") ?: ""
                    queueNumber.startsWith(today)
                } ?: 0
                
                tvRegistrarPending.text = todayPendingCount.toString()
                android.util.Log.d("AdminDashboard", "Registrar pending today: $todayPendingCount")
            }
    }

    private fun callNextQueue(department: String) {
        // Get today's date for filtering
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        // Get next pending queue from today only
        db.collection("appointments")
            .whereEqualTo("department", department)
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { pendingDocs ->
                // Filter to only today's queues
                val todayPendingDocs = pendingDocs.documents.filter { doc ->
                    val queueNumber = doc.getString("queueNumber") ?: ""
                    queueNumber.startsWith(today)
                }
                
                if (todayPendingDocs.isEmpty()) {
                    Toast.makeText(context, "No request queue yet for $department department", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Get the oldest pending queue from today (first in queue)
                val doc = todayPendingDocs.minByOrNull { 
                    val queueNumber = it.getString("queueNumber") ?: ""
                    // Extract queue number for sorting (e.g., "001" from "2026-03-23-FIN-001")
                    val parts = queueNumber.split("-")
                    if (parts.size >= 5) parts[4].toIntOrNull() ?: 999 else 999
                }
                
                if (doc == null) {
                    Toast.makeText(context, "No request queue yet for $department department", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                
                val queueNumber = doc.getString("queueNumber") ?: ""
                
                // Increment counter first
                if (department == "Finance") {
                    financeCounter++
                    tvFinanceServing.text = financeCounter.toString()
                } else {
                    registrarCounter++
                    tvRegistrarServing.text = registrarCounter.toString()
                }
                
                saveCounters()
                
                // Update Firestore serving counter document for real-time sync
                updateFirestoreCounters()
                
                // Update status to Serving and store the serving number
                val updates = hashMapOf<String, Any>(
                    "status" to "Serving",
                    "servingNumber" to if (department == "Finance") financeCounter else registrarCounter
                )
                
                doc.reference.update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Now serving: ${if (department == "Finance") financeCounter else registrarCounter} - Queue: $queueNumber", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        // Only show user-friendly message, not technical error
                        Toast.makeText(context, "Unable to call next queue. Please try again.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                // Don't show technical error messages, just show user-friendly message
                Toast.makeText(context, "No request queue yet for $department department", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        financeListener?.remove()
        registrarListener?.remove()
    }
}
