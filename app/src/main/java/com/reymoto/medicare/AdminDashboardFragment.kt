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
        
        // Check if daily reset is needed
        checkAndPerformDailyReset()
        
        // Load saved counters
        loadCounters()
        
        // Initialize Firestore serving counters document with current values
        initializeFirestoreCounters()
        
        // Initialize views
        tvFinanceServing = view.findViewById(R.id.tvFinanceServing)
        tvRegistrarServing = view.findViewById(R.id.tvRegistrarServing)
        tvFinancePending = view.findViewById(R.id.tvFinancePending)
        tvRegistrarPending = view.findViewById(R.id.tvRegistrarPending)
        
        // Set initial counter values
        tvFinanceServing.text = financeCounter.toString()
        tvRegistrarServing.text = registrarCounter.toString()
        
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
        val lastResetDate = prefs.getString(LAST_RESET_DATE_KEY, "")
        
        if (lastResetDate != today) {
            // It's a new day, reset counters
            resetCountersAutomatically()
            
            // Save today's date as the last reset date
            prefs.edit().putString(LAST_RESET_DATE_KEY, today).apply()
        }
    }

    private fun resetCountersAutomatically() {
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
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        
        db.collection("system").document("servingCounters")
            .set(servingData)
            .addOnFailureListener { e ->
                android.util.Log.e("AdminDashboard", "Error resetting serving counters: ${e.message}")
            }
    }

    private fun loadCounters() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        financeCounter = prefs.getInt(FINANCE_COUNTER_KEY, 0)
        registrarCounter = prefs.getInt(REGISTRAR_COUNTER_KEY, 0)
    }

    private fun initializeFirestoreCounters() {
        // Check if Firestore document exists and is from today
        db.collection("system").document("servingCounters")
            .get()
            .addOnSuccessListener { document ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                
                if (document.exists()) {
                    val lastUpdated = document.getTimestamp("lastUpdated")
                    val docDate = if (lastUpdated != null) {
                        dateFormat.format(lastUpdated.toDate())
                    } else {
                        ""
                    }
                    
                    // Only update if document is from a different day or counters don't match
                    if (docDate != today) {
                        // Document is from previous day, update with current (reset) values
                        updateFirestoreCounters()
                    } else {
                        // Document is from today, sync local counters with Firestore
                        val firestoreFinanceCounter = document.getLong("financeCounter")?.toInt() ?: 0
                        val firestoreRegistrarCounter = document.getLong("registrarCounter")?.toInt() ?: 0
                        
                        // Use the higher value between local and Firestore (in case of multiple admin sessions)
                        financeCounter = maxOf(financeCounter, firestoreFinanceCounter)
                        registrarCounter = maxOf(registrarCounter, firestoreRegistrarCounter)
                        
                        // Update UI and save to SharedPreferences
                        tvFinanceServing.text = financeCounter.toString()
                        tvRegistrarServing.text = registrarCounter.toString()
                        saveCounters()
                        
                        // Update Firestore if local values are higher
                        if (financeCounter > firestoreFinanceCounter || registrarCounter > firestoreRegistrarCounter) {
                            updateFirestoreCounters()
                        }
                    }
                } else {
                    // Document doesn't exist, create it
                    updateFirestoreCounters()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AdminDashboard", "Error checking serving counters: ${e.message}")
                // Fallback: create/update document
                updateFirestoreCounters()
            }
    }

    private fun updateFirestoreCounters() {
        val servingData = hashMapOf(
            "financeCounter" to financeCounter,
            "registrarCounter" to registrarCounter,
            "lastUpdated" to FieldValue.serverTimestamp()
        )
        
        db.collection("system").document("servingCounters")
            .set(servingData)
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
    }

    private fun setupRealtimeListeners() {
        financeListener = db.collection("appointments")
            .whereEqualTo("department", "Finance")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                tvFinancePending.text = (snapshots?.size() ?: 0).toString()
            }

        registrarListener = db.collection("appointments")
            .whereEqualTo("department", "Registrar")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                tvRegistrarPending.text = (snapshots?.size() ?: 0).toString()
            }
    }

    private fun callNextQueue(department: String) {
        // Get next pending queue
        db.collection("appointments")
            .whereEqualTo("department", department)
            .whereEqualTo("status", "Pending")
            .orderBy("timestamp")
            .limit(1)
            .get()
            .addOnSuccessListener { pendingDocs ->
                if (pendingDocs.isEmpty) {
                    Toast.makeText(context, "No pending queues to call", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val doc = pendingDocs.documents[0]
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
                        Toast.makeText(context, "Error updating queue: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching queues: ${e.message}", Toast.LENGTH_LONG).show()
                
                // If orderBy fails due to missing index, try without ordering
                db.collection("appointments")
                    .whereEqualTo("department", department)
                    .whereEqualTo("status", "Pending")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { pendingDocs ->
                        if (pendingDocs.isEmpty) {
                            Toast.makeText(context, "No pending queues to call", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val doc = pendingDocs.documents[0]
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
                            .addOnFailureListener { e2 ->
                                Toast.makeText(context, "Error updating queue: ${e2.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e2 ->
                        Toast.makeText(context, "Error: ${e2.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        financeListener?.remove()
        registrarListener?.remove()
    }
}
