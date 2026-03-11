package com.reymoto.medicare

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
        
        db = FirebaseFirestore.getInstance()
        
        // Load saved counters
        loadCounters()
        
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
        val btnResetQueue = view.findViewById<Button>(R.id.btnResetQueue)
        
        // Button listeners
        btnFinanceNext.setOnClickListener { callNextQueue("Finance") }
        btnRegistrarNext.setOnClickListener { callNextQueue("Registrar") }
        btnResetQueue.setOnClickListener { showResetConfirmation() }
        
        // Setup real-time listeners for pending count
        setupRealtimeListeners()
        
        return view
    }

    private fun loadCounters() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        financeCounter = prefs.getInt(FINANCE_COUNTER_KEY, 0)
        registrarCounter = prefs.getInt(REGISTRAR_COUNTER_KEY, 0)
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

    private fun showResetConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Reset Queue Counters")
            .setMessage("This will reset both Finance and Registrar counters back to 0.\n\nAre you sure?")
            .setPositiveButton("Reset") { _, _ ->
                resetCounters()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetCounters() {
        financeCounter = 0
        registrarCounter = 0
        
        tvFinanceServing.text = "0"
        tvRegistrarServing.text = "0"
        
        saveCounters()
        
        Toast.makeText(context, "Counters reset to 0", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        financeListener?.remove()
        registrarListener?.remove()
    }
}
