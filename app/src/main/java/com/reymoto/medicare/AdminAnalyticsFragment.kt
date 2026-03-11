package com.reymoto.medicare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminAnalyticsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var currentPeriod = "daily"
    
    private lateinit var tvTotalQueues: TextView
    private lateinit var tvCompletedQueues: TextView
    private lateinit var tvFinanceTotal: TextView
    private lateinit var tvFinanceCompleted: TextView
    private lateinit var tvFinanceAnalyticsPending: TextView
    private lateinit var tvFinanceCancelled: TextView
    private lateinit var tvRegistrarTotal: TextView
    private lateinit var tvRegistrarCompleted: TextView
    private lateinit var tvRegistrarAnalyticsPending: TextView
    private lateinit var tvRegistrarCancelled: TextView
    private lateinit var tvPeriodInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_analytics, container, false)
        
        db = FirebaseFirestore.getInstance()
        
        // Initialize views
        tvTotalQueues = view.findViewById(R.id.tvTotalQueues)
        tvCompletedQueues = view.findViewById(R.id.tvCompletedQueues)
        tvFinanceTotal = view.findViewById(R.id.tvFinanceTotal)
        tvFinanceCompleted = view.findViewById(R.id.tvFinanceCompleted)
        tvFinanceAnalyticsPending = view.findViewById(R.id.tvFinanceAnalyticsPending)
        tvFinanceCancelled = view.findViewById(R.id.tvFinanceCancelled)
        tvRegistrarTotal = view.findViewById(R.id.tvRegistrarTotal)
        tvRegistrarCompleted = view.findViewById(R.id.tvRegistrarCompleted)
        tvRegistrarAnalyticsPending = view.findViewById(R.id.tvRegistrarAnalyticsPending)
        tvRegistrarCancelled = view.findViewById(R.id.tvRegistrarCancelled)
        tvPeriodInfo = view.findViewById(R.id.tvPeriodInfo)
        
        val rgPeriod = view.findViewById<RadioGroup>(R.id.rgAnalyticsPeriod)
        
        // Period selection
        rgPeriod.setOnCheckedChangeListener { _, checkedId ->
            currentPeriod = when (checkedId) {
                R.id.rbAnalyticsDaily -> "daily"
                R.id.rbAnalyticsMonthly -> "monthly"
                R.id.rbAnalyticsYearly -> "yearly"
                else -> "daily"
            }
            loadAnalytics()
        }
        
        // Load initial data
        loadAnalytics()
        
        return view
    }

    private fun loadAnalytics() {
        updatePeriodInfo()
        
        // Load overall statistics
        db.collection("appointments")
            .get()
            .addOnSuccessListener { allDocs ->
                val total = allDocs.size()
                val completed = allDocs.documents.count { it.getString("status") == "Completed" }
                
                tvTotalQueues.text = total.toString()
                tvCompletedQueues.text = completed.toString()
            }
        
        // Load Finance analytics
        loadDepartmentAnalytics("Finance")
        
        // Load Registrar analytics
        loadDepartmentAnalytics("Registrar")
    }

    private fun loadDepartmentAnalytics(department: String) {
        db.collection("appointments")
            .whereEqualTo("department", department)
            .get()
            .addOnSuccessListener { docs ->
                val total = docs.size()
                val completed = docs.documents.count { it.getString("status") == "Completed" }
                val pending = docs.documents.count { it.getString("status") == "Pending" }
                val cancelled = docs.documents.count { it.getString("status") == "Cancelled" }
                
                if (department == "Finance") {
                    tvFinanceTotal.text = total.toString()
                    tvFinanceCompleted.text = completed.toString()
                    tvFinanceAnalyticsPending.text = pending.toString()
                    tvFinanceCancelled.text = cancelled.toString()
                } else {
                    tvRegistrarTotal.text = total.toString()
                    tvRegistrarCompleted.text = completed.toString()
                    tvRegistrarAnalyticsPending.text = pending.toString()
                    tvRegistrarCancelled.text = cancelled.toString()
                }
            }
    }

    private fun updatePeriodInfo() {
        val info = when (currentPeriod) {
            "daily" -> {
                val today = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
                "Showing data for today: $today"
            }
            "monthly" -> {
                val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                "Showing data for: $month"
            }
            "yearly" -> {
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                "Showing data for year: $year"
            }
            else -> "Showing all data"
        }
        tvPeriodInfo.text = info
    }
}
