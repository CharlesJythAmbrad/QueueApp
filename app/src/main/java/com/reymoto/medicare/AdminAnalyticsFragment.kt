package com.reymoto.medicare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminAnalyticsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var currentPeriod = "daily"
    private var selectedDepartment = "Finance"
    
    private lateinit var spinnerDepartment: Spinner
    private lateinit var tvDepartmentTitle: TextView
    private lateinit var tvDepartmentTotal: TextView
    private lateinit var tvDepartmentCompleted: TextView
    private lateinit var tvDepartmentPending: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var tvPeriodInfo: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_analytics, container, false)
        
        db = FirebaseFirestore.getInstance()
        
        // Initialize views
        spinnerDepartment = view.findViewById(R.id.spinnerDepartment)
        tvDepartmentTitle = view.findViewById(R.id.tvDepartmentTitle)
        tvDepartmentTotal = view.findViewById(R.id.tvDepartmentTotal)
        tvDepartmentCompleted = view.findViewById(R.id.tvDepartmentCompleted)
        tvDepartmentPending = view.findViewById(R.id.tvDepartmentPending)
        tvCompletionRate = view.findViewById(R.id.tvCompletionRate)
        tvPeriodInfo = view.findViewById(R.id.tvPeriodInfo)
        
        // Setup department spinner
        setupDepartmentSpinner()
        
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
        
        // Load initial data (Finance by default)
        loadAnalytics()
        
        return view
    }

    private fun setupDepartmentSpinner() {
        val departments = arrayOf("💰 Finance Department", "📄 Registrar Department")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, departments)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDepartment.adapter = adapter
        
        // Set Finance as default (index 0)
        spinnerDepartment.setSelection(0)
        
        spinnerDepartment.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDepartment = when (position) {
                    0 -> "Finance"
                    1 -> "Registrar"
                    else -> "Finance"
                }
                updateDepartmentTitle()
                loadAnalytics()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDepartmentTitle() {
        val (icon, title, color) = when (selectedDepartment) {
            "Finance" -> Triple("💰", "Finance Department", "#1976D2")
            "Registrar" -> Triple("📄", "Registrar Department", "#F57C00")
            else -> Triple("💰", "Finance Department", "#1976D2")
        }
        
        tvDepartmentTitle.text = "$icon $title"
        tvDepartmentTitle.setTextColor(android.graphics.Color.parseColor(color))
    }

    private fun loadAnalytics() {
        updatePeriodInfo()
        
        // Load department-specific statistics only
        loadDepartmentAnalytics(selectedDepartment)
    }

    private fun loadDepartmentAnalytics(department: String) {
        // Load all department data without date filtering for debugging
        db.collection("appointments")
            .whereEqualTo("department", department)
            .get()
            .addOnSuccessListener { docs ->
                // Debug: Log department-specific documents found
                android.util.Log.d("AdminAnalytics", "$department documents found: ${docs.size()}")
                
                if (docs.isEmpty) {
                    // No data for this department
                    tvDepartmentCompleted.text = "0"
                    tvDepartmentPending.text = "0"
                    tvCompletionRate.text = "0%"
                    android.util.Log.d("AdminAnalytics", "No documents found for $department department")
                    return@addOnSuccessListener
                }
                
                // For now, show all data regardless of date to debug
                val allDocs = docs.documents
                
                // Debug: Log some sample department data
                if (allDocs.isNotEmpty()) {
                    val sampleDoc = allDocs[0]
                    android.util.Log.d("AdminAnalytics", "$department sample document: ${sampleDoc.data}")
                }
                
                val total = allDocs.size
                val completed = allDocs.count { it.getString("status") == "Completed" }
                val pending = allDocs.count { it.getString("status") == "Pending" }
                
                // Debug: Log status counts
                android.util.Log.d("AdminAnalytics", "$department - Total: $total, Completed: $completed, Pending: $pending")
                
                // Calculate completion rate
                val completionRate = if (total > 0) {
                    ((completed.toDouble() / total.toDouble()) * 100).toInt()
                } else {
                    0
                }
                
                // Update department-specific UI with total included
                tvDepartmentTotal.text = total.toString()
                tvDepartmentCompleted.text = completed.toString()
                tvDepartmentPending.text = pending.toString()
                tvCompletionRate.text = "$completionRate%"
            }
            .addOnFailureListener { e ->
                // Handle error and log it
                android.util.Log.e("AdminAnalytics", "Error loading $department analytics", e)
                tvDepartmentTotal.text = "0"
                tvDepartmentCompleted.text = "0"
                tvDepartmentPending.text = "0"
                tvCompletionRate.text = "0%"
            }
    }

    private fun getDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        
        when (currentPeriod) {
            "daily" -> {
                // Today only
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time
                
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                return Pair(startDate, calendar.time)
            }
            "monthly" -> {
                // Current month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                return Pair(startDate, calendar.time)
            }
            "yearly" -> {
                // Current year
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startDate = calendar.time
                
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                return Pair(startDate, calendar.time)
            }
            else -> {
                // All time
                calendar.set(2020, Calendar.JANUARY, 1)
                val startDate = calendar.time
                return Pair(startDate, Date())
            }
        }
    }

    private fun updatePeriodInfo() {
        val departmentName = when (selectedDepartment) {
            "Finance" -> "Finance"
            "Registrar" -> "Registrar"
            else -> "Finance"
        }
        
        val info = when (currentPeriod) {
            "daily" -> {
                val today = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
                "Showing $departmentName data for today: $today"
            }
            "monthly" -> {
                val month = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
                "Showing $departmentName data for: $month"
            }
            "yearly" -> {
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                "Showing $departmentName data for year: $year"
            }
            else -> "Showing all $departmentName data"
        }
        tvPeriodInfo.text = info
    }
}
