package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class RegistrarRequestActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private var currentStep = 1
    
    // Step 1 data
    private var familyName = ""
    private var firstName = ""
    private var middleName = ""
    private var courseYear = ""
    private var studentNumber = ""
    private var lastTerm = ""
    
    // Step 2 data
    private var selectedDocument = ""
    private var otherDocument = ""
    
    // Step 3 data
    private var selectedPurpose = ""
    private var otherPurpose = ""
    
    // Step 4 data
    private var selectedDate = ""
    private var contactNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        loadStep1()
    }

    private fun loadStep1() {
        currentStep = 1
        setContentView(R.layout.activity_registrar_request_step1)
        
        val etFamilyName = findViewById<EditText>(R.id.etFamilyName)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etMiddleName = findViewById<EditText>(R.id.etMiddleName)
        val etCourseYear = findViewById<EditText>(R.id.etCourseYear)
        val etStudentNumber = findViewById<EditText>(R.id.etStudentNumber)
        val spinnerLastTerm = findViewById<Spinner>(R.id.spinnerLastTerm)
        val btnNext = findViewById<Button>(R.id.btnNext)
        
        // Fetch user data from Firestore
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        familyName = document.getString("lastName") ?: ""
                        firstName = document.getString("firstName") ?: ""
                        courseYear = document.getString("course") ?: ""
                        studentNumber = document.getString("idNumber") ?: ""
                        contactNumber = document.getString("contact") ?: ""
                        
                        etFamilyName.setText(familyName)
                        etFirstName.setText(firstName)
                        etCourseYear.setText(courseYear)
                        etStudentNumber.setText(studentNumber)
                    }
                }
        }
        
        // Setup Last Term spinner
        val terms = arrayOf(
            "Select Term",
            "1st Semester 2023-2024",
            "2nd Semester 2023-2024",
            "Summer 2024",
            "1st Semester 2024-2025",
            "2nd Semester 2024-2025",
            "Summer 2025",
            "1st Semester 2025-2026"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, terms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLastTerm.adapter = adapter
        
        btnNext.setOnClickListener {
            middleName = etMiddleName.text.toString().trim()
            lastTerm = spinnerLastTerm.selectedItem.toString()
            
            if (middleName.isEmpty()) {
                Toast.makeText(this, "Please enter middle name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (lastTerm == "Select Term") {
                Toast.makeText(this, "Please select last term", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            loadStep2()
        }
    }

    private fun loadStep2() {
        currentStep = 2
        setContentView(R.layout.activity_registrar_request_step2)
        
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupDocuments)
        val etOthers = findViewById<EditText>(R.id.etOthers)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnNext = findViewById<Button>(R.id.btnNext)
        
        btnBack.setOnClickListener {
            loadStep1()
        }
        
        btnNext.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            
            if (selectedId == -1 && etOthers.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Please select a document or specify in Others", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedId != -1) {
                val radioButton = findViewById<RadioButton>(selectedId)
                selectedDocument = radioButton.text.toString()
            } else {
                selectedDocument = "Others"
                otherDocument = etOthers.text.toString().trim()
            }
            
            loadStep3()
        }
    }

    private fun loadStep3() {
        currentStep = 3
        setContentView(R.layout.activity_registrar_request_step3)
        
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupPurpose)
        val etOthersPurpose = findViewById<EditText>(R.id.etOthersPurpose)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnNext = findViewById<Button>(R.id.btnNext)
        
        btnBack.setOnClickListener {
            loadStep2()
        }
        
        btnNext.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            
            if (selectedId == -1 && etOthersPurpose.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Please select a purpose or specify in Others", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedId != -1) {
                val radioButton = findViewById<RadioButton>(selectedId)
                selectedPurpose = radioButton.text.toString()
            } else {
                selectedPurpose = "Others"
                otherPurpose = etOthersPurpose.text.toString().trim()
            }
            
            loadStep4()
        }
    }

    private fun loadStep4() {
        currentStep = 4
        setContentView(R.layout.activity_registrar_request_step4)
        
        val spinnerDate = findViewById<Spinner>(R.id.spinnerDate)
        val etContactNumber = findViewById<EditText>(R.id.etContactNumber)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        
        // Pre-fill contact number
        etContactNumber.setText(contactNumber)
        
        // Setup Date spinner (next 7 days)
        val dates = mutableListOf<String>()
        dates.add("Select Date")
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        for (i in 0..6) {
            dates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dates)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDate.adapter = adapter
        
        btnBack.setOnClickListener {
            loadStep3()
        }
        
        btnSubmit.setOnClickListener {
            selectedDate = spinnerDate.selectedItem.toString()
            contactNumber = etContactNumber.text.toString().trim()
            
            if (selectedDate == "Select Date") {
                Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (contactNumber.isEmpty() || contactNumber.length < 11) {
                Toast.makeText(this, "Please enter a valid contact number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            submitRequest()
        }
    }

    private fun submitRequest() {
        val currentUser = auth.currentUser ?: return
        
        // Generate queue number
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        db.collection("appointments")
            .whereEqualTo("appointmentDate", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
            .whereEqualTo("department", "Registrar")
            .get()
            .addOnSuccessListener { documents ->
                val queueNumber = "$today-REG-${String.format("%03d", documents.size() + 1)}"
                
                val requestData = hashMapOf(
                    "studentUID" to currentUser.uid,
                    "studentEmail" to currentUser.email,
                    "department" to "Registrar",
                    "transactionType" to "Document Request",
                    "queueNumber" to queueNumber,
                    "status" to "Pending",
                    "timestamp" to Timestamp.now(),
                    "appointmentDate" to SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    "appointmentTime" to SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    
                    // Personal Data
                    "familyName" to familyName,
                    "firstName" to firstName,
                    "middleName" to middleName,
                    "courseYear" to courseYear,
                    "studentNumber" to studentNumber,
                    "lastTerm" to lastTerm,
                    
                    // Document
                    "documentType" to selectedDocument,
                    "otherDocument" to otherDocument,
                    
                    // Purpose
                    "purpose" to selectedPurpose,
                    "otherPurpose" to otherPurpose,
                    
                    // Contact
                    "requestDate" to selectedDate,
                    "contactNumber" to contactNumber
                )
                
                db.collection("appointments")
                    .add(requestData)
                    .addOnSuccessListener {
                        showSuccessDialog(queueNumber)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
    }

    private fun showSuccessDialog(queueNumber: String) {
        AlertDialog.Builder(this)
            .setTitle("✓ Request Submitted")
            .setMessage("Your registrar document request has been submitted successfully!\n\nQueue Number: $queueNumber\n\nYou can track your request in the Queue History.")
            .setPositiveButton("Go to Dashboard") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onBackPressed() {
        when (currentStep) {
            1 -> super.onBackPressed()
            2 -> loadStep1()
            3 -> loadStep2()
            4 -> loadStep3()
        }
    }
}
