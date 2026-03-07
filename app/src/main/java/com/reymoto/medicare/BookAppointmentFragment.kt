package com.reymoto.medicare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookAppointmentFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var selectedDepartment = "Finance" // Default to Finance
    private var selectedTransactionType = ""
    private var currentLocation: Location? = null

    // Urgello, Cebu City area - University campus
    // Coordinates for Southwestern University PHINMA
    private val URGELLO_CENTER_LAT = 10.3157  // SWU PHINMA coordinates
    private val URGELLO_CENTER_LNG = 123.8854
    private val ALLOWED_RADIUS_METERS = 2000.0  // 2km radius to cover entire campus and surrounding area

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_appointment, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize views
        val locationStatusText = view.findViewById<TextView>(R.id.tvLocationStatus)
        val requestRegistrarButton = view.findViewById<Button>(R.id.btnRequestRegistrar)
        val transactionTypeGroup = view.findViewById<RadioGroup>(R.id.rgTransactionType)
        val nameField = view.findViewById<EditText>(R.id.etName)
        val courseField = view.findViewById<EditText>(R.id.etCourse)
        val studentIdField = view.findViewById<EditText>(R.id.etStudentId)
        val mobileField = view.findViewById<EditText>(R.id.etMobile)
        val paymentDetailsLayout = view.findViewById<LinearLayout>(R.id.layoutPaymentDetails)
        val cardPaymentCheckbox = view.findViewById<CheckBox>(R.id.cbCardPayment)
        val cardDetailsLayout = view.findViewById<LinearLayout>(R.id.layoutCardDetails)
        val submitButton = view.findViewById<Button>(R.id.btnSubmitQueue)

        // Check location and validate Urgello area
        checkLocationAndUpdateUI(locationStatusText)

        // Load user data
        loadUserData(nameField, courseField, studentIdField, mobileField)

        // Request Registrar Queue button - Launch multi-step form
        requestRegistrarButton.setOnClickListener {
            val intent = Intent(requireContext(), RegistrarRequestActivity::class.java)
            startActivity(intent)
        }

        // Transaction type selection - show payment details and set to Finance
        transactionTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDepartment = "Finance" // Finance transactions
            selectedTransactionType = when (checkedId) {
                R.id.rbDownPayment -> "Down Payment"
                R.id.rbInstallment -> "Installment"
                R.id.rbBalance -> "Balance Payment"
                else -> ""
            }
            
            // Show payment details if a transaction type is selected
            if (selectedTransactionType.isNotEmpty()) {
                paymentDetailsLayout.visibility = View.VISIBLE
                
                // Auto-fill purpose of payment with transaction type
                view.findViewById<EditText>(R.id.etPurpose1).setText(selectedTransactionType)
            } else {
                paymentDetailsLayout.visibility = View.GONE
            }
        }

        // Card payment checkbox
        cardPaymentCheckbox.setOnCheckedChangeListener { _, isChecked ->
            cardDetailsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Setup cash denomination calculators
        setupCashDenominationCalculators(view)

        // Submit button
        submitButton.setOnClickListener {
            submitQueueRequest(view)
        }

        return view
    }

    private fun loadUserData(nameField: EditText, courseField: EditText, studentIdField: EditText, mobileField: EditText) {
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupCashDenominationCalculators(view: View) {
        val denominations = mapOf(
            1000 to Pair(R.id.etPieces1000, R.id.tvAmount1000),
            500 to Pair(R.id.etPieces500, R.id.tvAmount500),
            200 to Pair(R.id.etPieces200, R.id.tvAmount200),
            100 to Pair(R.id.etPieces100, R.id.tvAmount100),
            50 to Pair(R.id.etPieces50, R.id.tvAmount50),
            20 to Pair(R.id.etPieces20, R.id.tvAmount20)
        )

        denominations.forEach { (value, ids) ->
            val piecesField = view.findViewById<EditText>(ids.first)
            val amountText = view.findViewById<TextView>(ids.second)
            
            piecesField.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val pieces = s.toString().toIntOrNull() ?: 0
                    val amount = pieces * value
                    amountText.text = String.format("%.2f", amount.toDouble())
                    calculateTotals(view)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // Coins calculator
        val coinsField = view.findViewById<EditText>(R.id.etCoins)
        val coinsAmountText = view.findViewById<TextView>(R.id.tvAmountCoins)
        
        coinsField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val coins = s.toString().toDoubleOrNull() ?: 0.0
                coinsAmountText.text = String.format("%.2f", coins)
                calculateTotals(view)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun calculateTotals(view: View) {
        val pieces1000 = view.findViewById<EditText>(R.id.etPieces1000).text.toString().toIntOrNull() ?: 0
        val pieces500 = view.findViewById<EditText>(R.id.etPieces500).text.toString().toIntOrNull() ?: 0
        val pieces200 = view.findViewById<EditText>(R.id.etPieces200).text.toString().toIntOrNull() ?: 0
        val pieces100 = view.findViewById<EditText>(R.id.etPieces100).text.toString().toIntOrNull() ?: 0
        val pieces50 = view.findViewById<EditText>(R.id.etPieces50).text.toString().toIntOrNull() ?: 0
        val pieces20 = view.findViewById<EditText>(R.id.etPieces20).text.toString().toIntOrNull() ?: 0
        val coins = view.findViewById<EditText>(R.id.etCoins).text.toString().toDoubleOrNull() ?: 0.0

        val totalCash = (pieces1000 * 1000) + (pieces500 * 500) + (pieces200 * 200) + 
                        (pieces100 * 100) + (pieces50 * 50) + (pieces20 * 20) + coins

        val totalAmount = view.findViewById<EditText>(R.id.etTotalAmount).text.toString().toDoubleOrNull() ?: 0.0
        val change = totalCash - totalAmount

        view.findViewById<TextView>(R.id.tvTotalCashTendered).text = String.format("%.2f", totalCash)
        view.findViewById<TextView>(R.id.tvChange).text = String.format("%.2f", change)
    }

    private fun checkLocationAndUpdateUI(statusTextView: TextView) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
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
                    statusTextView.text = "✅ You are in Urgello area - Request allowed"
                    statusTextView.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                } else {
                    statusTextView.text = "❌ You must be in Urgello, Cebu City area"
                    statusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
            } else {
                statusTextView.text = "📍 Unable to get location. Please enable GPS"
                statusTextView.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
        }
    }

    private fun isLocationInSambagArea(location: Location): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            URGELLO_CENTER_LAT,
            URGELLO_CENTER_LNG,
            results
        )
        
        val distance = results[0]
        
        // Debug: Log the distance for troubleshooting
        android.util.Log.d("LocationCheck", "Your location: ${location.latitude}, ${location.longitude}")
        android.util.Log.d("LocationCheck", "Distance from Urgello center: ${distance}m (allowed: ${ALLOWED_RADIUS_METERS}m)")
        
        // Return true if within 2km of Urgello center
        return distance <= ALLOWED_RADIUS_METERS
    }

    private fun submitQueueRequest(view: View) {
        // Validation
        if (selectedTransactionType.isEmpty() && selectedDepartment != "Registrar") {
            Toast.makeText(requireContext(), "Please select a transaction type or request Registrar queue", Toast.LENGTH_SHORT).show()
            return
        }

        val mobile = view.findViewById<EditText>(R.id.etMobile).text.toString().trim()
        if (mobile.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter mobile number", Toast.LENGTH_SHORT).show()
            return
        }

        // Check location permission
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission is required to submit queue request", Toast.LENGTH_LONG).show()
            return
        }

        val submitButton = view.findViewById<Button>(R.id.btnSubmitQueue)
        submitButton.isEnabled = false
        submitButton.text = "Checking location..."

        // Get current location and validate
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(requireContext(), "Unable to get your location. Please enable GPS and try again", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit Queue Request"
                return@addOnSuccessListener
            }

            // Check if in Urgello area
            if (!isLocationInSambagArea(location)) {
                Toast.makeText(requireContext(), "You must be in Urgello, Cebu City area to submit queue request", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit Queue Request"
                return@addOnSuccessListener
            }

            submitButton.text = "Submitting..."
            proceedWithQueueRequest(view, location, mobile, submitButton)
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
            submitButton.isEnabled = true
            submitButton.text = "Submit Queue Request"
        }
    }

    private fun proceedWithQueueRequest(view: View, location: Location?, mobile: String, submitButton: Button) {
        generateQueueNumber { queueNumber ->
            val currentUser = auth.currentUser
            
            // Collect payment details if transaction type is selected (Finance only)
            val paymentData = if (selectedDepartment == "Finance" && selectedTransactionType.isNotEmpty()) {
                hashMapOf(
                    "purpose1" to view.findViewById<EditText>(R.id.etPurpose1).text.toString(),
                    "purpose2" to view.findViewById<EditText>(R.id.etPurpose2).text.toString(),
                    "purpose3" to view.findViewById<EditText>(R.id.etPurpose3).text.toString(),
                    "totalAmount" to view.findViewById<EditText>(R.id.etTotalAmount).text.toString(),
                    "pieces1000" to view.findViewById<EditText>(R.id.etPieces1000).text.toString(),
                    "pieces500" to view.findViewById<EditText>(R.id.etPieces500).text.toString(),
                    "pieces200" to view.findViewById<EditText>(R.id.etPieces200).text.toString(),
                    "pieces100" to view.findViewById<EditText>(R.id.etPieces100).text.toString(),
                    "pieces50" to view.findViewById<EditText>(R.id.etPieces50).text.toString(),
                    "pieces20" to view.findViewById<EditText>(R.id.etPieces20).text.toString(),
                    "coins" to view.findViewById<EditText>(R.id.etCoins).text.toString(),
                    "totalCashTendered" to view.findViewById<TextView>(R.id.tvTotalCashTendered).text.toString(),
                    "change" to view.findViewById<TextView>(R.id.tvChange).text.toString()
                )
            } else {
                hashMapOf<String, String>()
            }

            // Card payment details
            val cardPaymentCheckbox = view.findViewById<CheckBox>(R.id.cbCardPayment)
            if (cardPaymentCheckbox.isChecked) {
                val cardType = if (view.findViewById<RadioButton>(R.id.rbDebit).isChecked) "Debit" else "Credit"
                paymentData["cardPayment"] = "true"
                paymentData["cardType"] = cardType
                paymentData["cardHolderName"] = view.findViewById<EditText>(R.id.etCardHolderName).text.toString()
                paymentData["bank"] = view.findViewById<EditText>(R.id.etBank).text.toString()
                paymentData["cardAmount"] = view.findViewById<EditText>(R.id.etCardAmount).text.toString()
            }

            // Create queue request data
            val queueData = hashMapOf(
                "studentUID" to currentUser?.uid,
                "studentEmail" to currentUser?.email,
                "studentName" to view.findViewById<EditText>(R.id.etName).text.toString(),
                "course" to view.findViewById<EditText>(R.id.etCourse).text.toString(),
                "studentId" to view.findViewById<EditText>(R.id.etStudentId).text.toString(),
                "mobile" to mobile,
                "department" to selectedDepartment,
                "transactionType" to selectedTransactionType,
                "queueNumber" to queueNumber,
                "status" to "Pending",
                "latitude" to (location?.latitude ?: 0.0),
                "longitude" to (location?.longitude ?: 0.0),
                "paymentDetails" to paymentData,
                "timestamp" to FieldValue.serverTimestamp()
            )

            // Save to Firestore
            db.collection("appointments")
                .add(queueData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Queue requested! Your number: $queueNumber", Toast.LENGTH_LONG).show()
                    
                    // Go back to dashboard
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
                view?.findViewById<TextView>(R.id.tvLocationStatus)?.let { statusView ->
                    checkLocationAndUpdateUI(statusView)
                }
            } else {
                Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }
}
