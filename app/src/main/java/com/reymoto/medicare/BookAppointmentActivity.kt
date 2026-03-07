package com.reymoto.medicare

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var selectedDepartment = "Finance"
    private var selectedTransactionType = ""
    private var currentLocation: Location? = null

    // Sambag 1 and Sambag 2, Urgello boundaries
    private val SAMBAG1_CENTER_LAT = 10.3157
    private val SAMBAG1_CENTER_LNG = 123.8854
    private val SAMBAG2_CENTER_LAT = 10.3180
    private val SAMBAG2_CENTER_LNG = 123.8870
    private val ALLOWED_RADIUS_METERS = 600.0

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_appointment)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize views
        val backArrow = findViewById<ImageView>(R.id.ivBackArrow)
        val locationStatusText = findViewById<TextView>(R.id.tvLocationStatus)
        val departmentGroup = findViewById<RadioGroup>(R.id.rgDepartment)
        val transactionTypeGroup = findViewById<RadioGroup>(R.id.rgTransactionType)
        val nameField = findViewById<EditText>(R.id.etName)
        val courseField = findViewById<EditText>(R.id.etCourse)
        val studentIdField = findViewById<EditText>(R.id.etStudentId)
        val mobileField = findViewById<EditText>(R.id.etMobile)
        val paymentDetailsLayout = findViewById<LinearLayout>(R.id.layoutPaymentDetails)
        val cardPaymentCheckbox = findViewById<CheckBox>(R.id.cbCardPayment)
        val cardDetailsLayout = findViewById<LinearLayout>(R.id.layoutCardDetails)
        val submitButton = findViewById<Button>(R.id.btnSubmitQueue)

        // Back button
        backArrow.setOnClickListener {
            finish()
        }

        // Check location
        checkLocationAndUpdateUI(locationStatusText)

        // Load user data
        loadUserData(nameField, courseField, studentIdField, mobileField)

        // Department selection
        departmentGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDepartment = when (checkedId) {
                R.id.rbFinance -> "Finance"
                R.id.rbRegistrar -> "Registrar"
                else -> "Finance"
            }
        }

        // Transaction type selection - show payment details
        transactionTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTransactionType = when (checkedId) {
                R.id.rbDownPayment -> "Down Payment"
                R.id.rbInstallment -> "Installment"
                R.id.rbBalance -> "Balance Payment"
                else -> ""
            }
            
            // Show payment details if a transaction type is selected
            if (selectedTransactionType.isNotEmpty()) {
                paymentDetailsLayout.visibility = View.VISIBLE
            } else {
                paymentDetailsLayout.visibility = View.GONE
            }
        }

        // Card payment checkbox
        cardPaymentCheckbox.setOnCheckedChangeListener { _, isChecked ->
            cardDetailsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup cash denomination calculators
        setupCashDenominationCalculators()

        // Submit button
        submitButton.setOnClickListener {
            submitQueueRequest()
        }
    }

    private fun loadUserData(nameField: EditText, courseField: EditText, studentIdField: EditText, mobileField: EditText) {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    nameField.setText(document.getString("fullName") ?: "")
                    courseField.setText(document.getString("course") ?: "")
                    studentIdField.setText(document.getString("idNumber") ?: "")
                    mobileField.setText(document.getString("contact") ?: "")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCashDenominationCalculators() {
        val denominations = mapOf(
            1000 to Pair(R.id.etPieces1000, R.id.tvAmount1000),
            500 to Pair(R.id.etPieces500, R.id.tvAmount500),
            200 to Pair(R.id.etPieces200, R.id.tvAmount200),
            100 to Pair(R.id.etPieces100, R.id.tvAmount100),
            50 to Pair(R.id.etPieces50, R.id.tvAmount50),
            20 to Pair(R.id.etPieces20, R.id.tvAmount20)
        )

        denominations.forEach { (value, ids) ->
            val piecesField = findViewById<EditText>(ids.first)
            val amountText = findViewById<TextView>(ids.second)
            
            piecesField.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val pieces = s.toString().toIntOrNull() ?: 0
                    val amount = pieces * value
                    amountText.text = String.format("%.2f", amount.toDouble())
                    calculateTotals()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // Coins calculator
        val coinsField = findViewById<EditText>(R.id.etCoins)
        val coinsAmountText = findViewById<TextView>(R.id.tvAmountCoins)
        
        coinsField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val coins = s.toString().toDoubleOrNull() ?: 0.0
                coinsAmountText.text = String.format("%.2f", coins)
                calculateTotals()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun calculateTotals() {
        val pieces1000 = findViewById<EditText>(R.id.etPieces1000).text.toString().toIntOrNull() ?: 0
        val pieces500 = findViewById<EditText>(R.id.etPieces500).text.toString().toIntOrNull() ?: 0
        val pieces200 = findViewById<EditText>(R.id.etPieces200).text.toString().toIntOrNull() ?: 0
        val pieces100 = findViewById<EditText>(R.id.etPieces100).text.toString().toIntOrNull() ?: 0
        val pieces50 = findViewById<EditText>(R.id.etPieces50).text.toString().toIntOrNull() ?: 0
        val pieces20 = findViewById<EditText>(R.id.etPieces20).text.toString().toIntOrNull() ?: 0
        val coins = findViewById<EditText>(R.id.etCoins).text.toString().toDoubleOrNull() ?: 0.0

        val totalCash = (pieces1000 * 1000) + (pieces500 * 500) + (pieces200 * 200) + 
                        (pieces100 * 100) + (pieces50 * 50) + (pieces20 * 20) + coins

        val totalAmount = findViewById<EditText>(R.id.etTotalAmount).text.toString().toDoubleOrNull() ?: 0.0
        val change = totalCash - totalAmount

        findViewById<TextView>(R.id.tvTotalCashTendered).text = String.format("%.2f", totalCash)
        findViewById<TextView>(R.id.tvChange).text = String.format("%.2f", change)
    }

    private fun checkLocationAndUpdateUI(statusTextView: TextView) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            statusTextView.text = "📍 Location permission required"
            statusTextView.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                val isInAllowedArea = isLocationInSambagArea(location)
                
                if (isInAllowedArea) {
                    statusTextView.text = "✅ You are in Sambag 1 or 2, Urgello - Request allowed"
                    statusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                } else {
                    statusTextView.text = "❌ You must be in Sambag 1 or 2, Urgello"
                    statusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            } else {
                statusTextView.text = "📍 Unable to get location. Please enable GPS"
                statusTextView.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
        }
    }

    private fun isLocationInSambagArea(location: Location): Boolean {
        val resultsToSambag1 = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            SAMBAG1_CENTER_LAT,
            SAMBAG1_CENTER_LNG,
            resultsToSambag1
        )
        
        if (resultsToSambag1[0] <= ALLOWED_RADIUS_METERS) {
            return true
        }
        
        val resultsToSambag2 = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            SAMBAG2_CENTER_LAT,
            SAMBAG2_CENTER_LNG,
            resultsToSambag2
        )
        
        return resultsToSambag2[0] <= ALLOWED_RADIUS_METERS
    }

    private fun submitQueueRequest() {
        // Validation
        if (selectedTransactionType.isEmpty()) {
            Toast.makeText(this, "Please select a transaction type", Toast.LENGTH_SHORT).show()
            return
        }

        val mobile = findViewById<EditText>(R.id.etMobile).text.toString().trim()
        if (mobile.isEmpty()) {
            Toast.makeText(this, "Please enter mobile number", Toast.LENGTH_SHORT).show()
            return
        }

        // Check location
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            return
        }

        val submitButton = findViewById<Button>(R.id.btnSubmitQueue)
        submitButton.isEnabled = false
        submitButton.text = "Checking location..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(this, "Unable to get your location. Please enable GPS", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit Queue Request"
                return@addOnSuccessListener
            }

            if (!isLocationInSambagArea(location)) {
                Toast.makeText(this, "You must be in Sambag 1 or 2, Urgello to request queue", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit Queue Request"
                return@addOnSuccessListener
            }

            submitButton.text = "Submitting..."
            proceedWithQueueRequest(location, mobile, submitButton)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
            submitButton.isEnabled = true
            submitButton.text = "Submit Queue Request"
        }
    }

    private fun proceedWithQueueRequest(location: Location, mobile: String, submitButton: Button) {
        generateQueueNumber { queueNumber ->
            val currentUser = auth.currentUser
            
            // Collect payment details if transaction type is selected
            val paymentData = if (selectedTransactionType.isNotEmpty()) {
                hashMapOf(
                    "purpose1" to findViewById<EditText>(R.id.etPurpose1).text.toString(),
                    "purpose2" to findViewById<EditText>(R.id.etPurpose2).text.toString(),
                    "purpose3" to findViewById<EditText>(R.id.etPurpose3).text.toString(),
                    "totalAmount" to findViewById<EditText>(R.id.etTotalAmount).text.toString(),
                    "pieces1000" to findViewById<EditText>(R.id.etPieces1000).text.toString(),
                    "pieces500" to findViewById<EditText>(R.id.etPieces500).text.toString(),
                    "pieces200" to findViewById<EditText>(R.id.etPieces200).text.toString(),
                    "pieces100" to findViewById<EditText>(R.id.etPieces100).text.toString(),
                    "pieces50" to findViewById<EditText>(R.id.etPieces50).text.toString(),
                    "pieces20" to findViewById<EditText>(R.id.etPieces20).text.toString(),
                    "coins" to findViewById<EditText>(R.id.etCoins).text.toString(),
                    "totalCashTendered" to findViewById<TextView>(R.id.tvTotalCashTendered).text.toString(),
                    "change" to findViewById<TextView>(R.id.tvChange).text.toString()
                )
            } else {
                hashMapOf<String, String>()
            }

            // Card payment details
            val cardPaymentCheckbox = findViewById<CheckBox>(R.id.cbCardPayment)
            if (cardPaymentCheckbox.isChecked) {
                val cardType = if (findViewById<RadioButton>(R.id.rbDebit).isChecked) "Debit" else "Credit"
                paymentData["cardPayment"] = "true"
                paymentData["cardType"] = cardType
                paymentData["cardHolderName"] = findViewById<EditText>(R.id.etCardHolderName).text.toString()
                paymentData["bank"] = findViewById<EditText>(R.id.etBank).text.toString()
                paymentData["cardAmount"] = findViewById<EditText>(R.id.etCardAmount).text.toString()
            }

            // Create queue request data
            val queueData = hashMapOf(
                "studentUID" to currentUser?.uid,
                "studentEmail" to currentUser?.email,
                "studentName" to findViewById<EditText>(R.id.etName).text.toString(),
                "course" to findViewById<EditText>(R.id.etCourse).text.toString(),
                "studentId" to findViewById<EditText>(R.id.etStudentId).text.toString(),
                "mobile" to mobile,
                "department" to selectedDepartment,
                "transactionType" to selectedTransactionType,
                "queueNumber" to queueNumber,
                "status" to "Pending",
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "paymentDetails" to paymentData,
                "timestamp" to FieldValue.serverTimestamp()
            )

            // Save to Firestore
            db.collection("appointments")
                .add(queueData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Queue requested! Your number: $queueNumber", Toast.LENGTH_LONG).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    submitButton.isEnabled = true
                    submitButton.text = "Submit Queue Request"
                }
        }
    }

    private fun generateQueueNumber(callback: (String) -> Unit) {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        db.collection("appointments")
            .whereEqualTo("department", selectedDepartment)
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size() + 1
                val queueNumber = "$today-${selectedDepartment.take(3).uppercase()}-${String.format("%03d", count)}"
                callback(queueNumber)
            }
            .addOnFailureListener {
                val random = Random().nextInt(999) + 1
                val queueNumber = "$today-${selectedDepartment.take(3).uppercase()}-${String.format("%03d", random)}"
                callback(queueNumber)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationAndUpdateUI(findViewById(R.id.tvLocationStatus))
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }
}
