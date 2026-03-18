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
    private var selectedDateValue = ""
    
    private lateinit var spinnerDepartment: Spinner
    private lateinit var spinnerPeriodType: Spinner
    private lateinit var spinnerDateValue: Spinner
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
        spinnerPeriodType = view.findViewById(R.id.spinnerPeriodType)
        spinnerDateValue = view.findViewById(R.id.spinnerDateValue)
        tvDepartmentTitle = view.findViewById(R.id.tvDepartmentTitle)
        tvDepartmentTotal = view.findViewById(R.id.tvDepartmentTotal)
        tvDepartmentCompleted = view.findViewById(R.id.tvDepartmentCompleted)
        tvDepartmentPending = view.findViewById(R.id.tvDepartmentPending)
        tvCompletionRate = view.findViewById(R.id.tvCompletionRate)
        tvPeriodInfo = view.findViewById(R.id.tvPeriodInfo)
        
        // Setup spinners
        setupDepartmentSpinner()
        setupPeriodSpinners()
        
        // Load initial data
        loadAnalytics()
        
        return view
    }

    private fun setupPeriodSpinners() {
        // Setup Period Type Spinner
        val periodTypes = arrayOf("📅 Daily", "📅 Monthly", "📅 Yearly")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodTypes)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPeriodType.adapter = periodAdapter
        
        // Set Daily as default
        spinnerPeriodType.setSelection(0)
        
        // Setup Period Type listener
        spinnerPeriodType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentPeriod = when (position) {
                    0 -> "daily"
                    1 -> "monthly"
                    2 -> "yearly"
                    else -> "daily"
                }
                setupDateValueSpinner()
                loadAnalytics()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Initialize date value spinner
        setupDateValueSpinner()
    }
    
    private fun setupDateValueSpinner() {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        when (currentPeriod) {
            "daily" -> {
                // Generate dates for current month
                val dates = mutableListOf<String>()
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                
                // Add today and previous 30 days
                for (i in 0..30) {
                    val date = Calendar.getInstance()
                    date.add(Calendar.DAY_OF_MONTH, -i)
                    dates.add(dateFormat.format(date.time))
                }
                
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dates)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDateValue.adapter = adapter
                
                // Set today as default
                selectedDateValue = dates[0]
                spinnerDateValue.setSelection(0)
            }
            "monthly" -> {
                // Generate months for current and previous years
                val months = mutableListOf<String>()
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                
                // Add current month and previous 12 months
                for (i in 0..12) {
                    val date = Calendar.getInstance()
                    date.add(Calendar.MONTH, -i)
                    months.add(monthFormat.format(date.time))
                }
                
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDateValue.adapter = adapter
                
                // Set current month as default
                selectedDateValue = months[0]
                spinnerDateValue.setSelection(0)
            }
            "yearly" -> {
                // Generate years
                val years = (2020..2030).map { it.toString() }.toTypedArray()
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDateValue.adapter = adapter
                
                // Set current year as default
                val currentYearIndex = years.indexOf(currentYear.toString())
                selectedDateValue = currentYear.toString()
                spinnerDateValue.setSelection(if (currentYearIndex >= 0) currentYearIndex else 0)
            }
        }
        
        // Set date value listener
        spinnerDateValue.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDateValue = parent?.getItemAtPosition(position).toString()
                loadAnalytics()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
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
        val (startDate, endDate) = getDateRange()
        
        // Debug: Log the date range being used
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        android.util.Log.d("AdminAnalytics", "Loading $department analytics from ${dateFormat.format(startDate)} to ${dateFormat.format(endDate)}")
        
        // Query with date filtering
        db.collection("appointments")
            .whereEqualTo("department", department)
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .get()
            .addOnSuccessListener { docs ->
                android.util.Log.d("AdminAnalytics", "$department documents found in date range: ${docs.size()}")
                
                if (docs.isEmpty) {
                    // No data for this department in the selected period
                    tvDepartmentTotal.text = "0"
                    tvDepartmentCompleted.text = "0"
                    tvDepartmentPending.text = "0"
                    tvCompletionRate.text = "0%"
                    android.util.Log.d("AdminAnalytics", "No documents found for $department in selected period")
                    return@addOnSuccessListener
                }
                
                val filteredDocs = docs.documents
                
                // Debug: Log some sample data
                if (filteredDocs.isNotEmpty()) {
                    val sampleDoc = filteredDocs[0]
                    android.util.Log.d("AdminAnalytics", "$department sample document: ${sampleDoc.data}")
                    android.util.Log.d("AdminAnalytics", "Sample timestamp: ${sampleDoc.getTimestamp("timestamp")}")
                }
                
                val total = filteredDocs.size
                val completed = filteredDocs.count { it.getString("status") == "Completed" }
                val pending = filteredDocs.count { it.getString("status") == "Pending" }
                val serving = filteredDocs.count { it.getString("status") == "Serving" }
                
                // Debug: Log status counts
                android.util.Log.d("AdminAnalytics", "$department in selected period - Total: $total, Completed: $completed, Pending: $pending, Serving: $serving")
                
                // Calculate completion rate
                val completionRate = if (total > 0) {
                    ((completed.toDouble() / total.toDouble()) * 100).toInt()
                } else {
                    0
                }
                
                // Update department-specific UI
                tvDepartmentTotal.text = total.toString()
                tvDepartmentCompleted.text = completed.toString()
                tvDepartmentPending.text = (pending + serving).toString() // Combine pending and serving
                tvCompletionRate.text = "$completionRate%"
                
                // Update period info
                updatePeriodInfo()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AdminAnalytics", "Error loading $department analytics for date range", e)
                
                // Fallback: Try without date filtering if timestamp field doesn't exist
                android.util.Log.d("AdminAnalytics", "Trying fallback query without timestamp filtering...")
                loadDepartmentAnalyticsWithoutDateFilter(department)
            }
    }
    
    private fun loadDepartmentAnalyticsWithoutDateFilter(department: String) {
        // Fallback method for data that might not have timestamp field
        db.collection("appointments")
            .whereEqualTo("department", department)
            .get()
            .addOnSuccessListener { docs ->
                android.util.Log.d("AdminAnalytics", "$department fallback query - documents found: ${docs.size()}")
                
                if (docs.isEmpty) {
                    tvDepartmentTotal.text = "0"
                    tvDepartmentCompleted.text = "0"
                    tvDepartmentPending.text = "0"
                    tvCompletionRate.text = "0%"
                    return@addOnSuccessListener
                }
                
                val allDocs = docs.documents
                
                // For fallback, filter by queue number date if available
                val (startDate, endDate) = getDateRange()
                val filteredDocs = if (currentPeriod != "yearly") {
                    // Try to filter by queue number date format (YYYY-MM-DD-DEPT-XXX)
                    allDocs.filter { doc ->
                        val queueNumber = doc.getString("queueNumber") ?: ""
                        try {
                            if (queueNumber.contains("-")) {
                                val datePart = queueNumber.split("-").take(3).joinToString("-")
                                val queueDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(datePart)
                                queueDate != null && queueDate >= startDate && queueDate <= endDate
                            } else {
                                false
                            }
                        } catch (e: Exception) {
                            false
                        }
                    }
                } else {
                    // For yearly, just show all data
                    allDocs
                }
                
                val total = filteredDocs.size
                val completed = filteredDocs.count { it.getString("status") == "Completed" }
                val pending = filteredDocs.count { it.getString("status") == "Pending" }
                val serving = filteredDocs.count { it.getString("status") == "Serving" }
                
                android.util.Log.d("AdminAnalytics", "$department fallback filtered - Total: $total, Completed: $completed, Pending: $pending, Serving: $serving")
                
                val completionRate = if (total > 0) {
                    ((completed.toDouble() / total.toDouble()) * 100).toInt()
                } else {
                    0
                }
                
                tvDepartmentTotal.text = total.toString()
                tvDepartmentCompleted.text = completed.toString()
                tvDepartmentPending.text = (pending + serving).toString()
                tvCompletionRate.text = "$completionRate%"
                
                updatePeriodInfo()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("AdminAnalytics", "Error in fallback query for $department", e)
                tvDepartmentTotal.text = "0"
                tvDepartmentCompleted.text = "0"
                tvDepartmentPending.text = "0"
                tvCompletionRate.text = "0%"
            }
    }

    private fun getDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        
        when (currentPeriod) {
            "daily" -> {
                // Parse selected date
                try {
                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    val selectedDate = dateFormat.parse(selectedDateValue)
                    if (selectedDate != null) {
                        calendar.time = selectedDate
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
                } catch (e: Exception) {
                    android.util.Log.e("AdminAnalytics", "Error parsing daily date: $selectedDateValue", e)
                }
            }
            "monthly" -> {
                // Parse selected month
                try {
                    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val selectedDate = monthFormat.parse(selectedDateValue)
                    if (selectedDate != null) {
                        calendar.time = selectedDate
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val startDate = calendar.time
                        
                        // Last day of the month
                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.DAY_OF_MONTH, -1)
                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                        calendar.set(Calendar.MINUTE, 59)
                        calendar.set(Calendar.SECOND, 59)
                        calendar.set(Calendar.MILLISECOND, 999)
                        return Pair(startDate, calendar.time)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AdminAnalytics", "Error parsing monthly date: $selectedDateValue", e)
                }
            }
            "yearly" -> {
                // Parse selected year
                try {
                    val year = selectedDateValue.toInt()
                    calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val startDate = calendar.time
                    
                    calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                    calendar.set(Calendar.MILLISECOND, 999)
                    return Pair(startDate, calendar.time)
                } catch (e: Exception) {
                    android.util.Log.e("AdminAnalytics", "Error parsing yearly date: $selectedDateValue", e)
                }
            }
        }
        
        // Fallback to all time
        calendar.set(2020, Calendar.JANUARY, 1)
        val startDate = calendar.time
        return Pair(startDate, Date())
    }

    private fun updatePeriodInfo() {
        val departmentName = when (selectedDepartment) {
            "Finance" -> "Finance"
            "Registrar" -> "Registrar"
            else -> "Finance"
        }
        
        val info = when (currentPeriod) {
            "daily" -> "Showing $departmentName data for: $selectedDateValue"
            "monthly" -> "Showing $departmentName data for: $selectedDateValue"
            "yearly" -> "Showing $departmentName data for year: $selectedDateValue"
            else -> "Showing all $departmentName data"
        }
        tvPeriodInfo.text = info
    }
}
