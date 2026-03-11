package com.reymoto.medicare

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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
    
    private var currentStep = 1
    private var currentLocation: Location? = null
    
    // Step 1 data
    private var selectedTransactionType = "Finance"
    private var studentName = ""
    private var studentCourse = ""
    private var studentId = ""
    private var studentMobile = ""
    
    // Step 2 data
    private var selectedDate = ""
    private var selectedPurpose = ""
    
    // Step 3 data
    private var selectedPaymentMethod = ""
    private var paymentFieldsData = mutableMapOf<String, String>()

    private val URGELLO_CENTER_LAT = 10.3157
    private val URGELLO_CENTER_LNG = 123.8854
    private val ALLOWED_RADIUS_METERS = 2000.0
    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        
        return loadStep1(inflater, container)
    }

    private fun loadStep1(inflater: LayoutInflater, container: ViewGroup?): View {
        currentStep = 1
        val view = inflater.inflate(R.layout.fragment_book_appointment_step1, container, false)

        val locationStatusText = view.findViewById<TextView>(R.id.tvLocationStatus)
        val transactionTypeGroup = view.findViewById<RadioGroup>(R.id.rgTransactionType)
        val nameField = view.findViewById<EditText>(R.id.etName)
        val courseField = view.findViewById<EditText>(R.id.etCourse)
        val studentIdField = view.findViewById<EditText>(R.id.etStudentId)
        val mobileField = view.findViewById<EditText>(R.id.etMobile)
        val btnNext = view.findViewById<Button>(R.id.btnNext)

        checkLocationAndUpdateUI(locationStatusText)
        loadUserData(nameField, courseField, studentIdField, mobileField)

        transactionTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTransactionType = when (checkedId) {
                R.id.rbRequestRegistrar -> "Registrar"
                R.id.rbRequestFinance -> "Finance"
                else -> "Finance"
            }
        }

        btnNext.setOnClickListener {
            studentName = nameField.text.toString().trim()
            studentCourse = courseField.text.toString().trim()
            studentId = studentIdField.text.toString().trim()
            studentMobile = mobileField.text.toString().trim()

            if (studentName.isEmpty() || studentCourse.isEmpty() || studentId.isEmpty() || studentMobile.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTransactionType == "Registrar") {
                val intent = Intent(requireContext(), RegistrarRequestActivity::class.java)
                startActivity(intent)
                return@setOnClickListener
            }

            replaceWithStep2()
        }

        return view
    }

    private fun replaceWithStep2() {
        val view = layoutInflater.inflate(R.layout.fragment_book_appointment_step2, null)
        (this.view as? ViewGroup)?.removeAllViews()
        (this.view as? ViewGroup)?.addView(view)
        setupStep2(view)
    }

    private fun setupStep2(view: View) {
        currentStep = 2

        val nameField = view.findViewById<EditText>(R.id.etNameStep2)
        val courseField = view.findViewById<EditText>(R.id.etCourseStep2)
        val dateField = view.findViewById<EditText>(R.id.etDate)
        val studentIdField = view.findViewById<EditText>(R.id.etStudentIdStep2)
        val mobileField = view.findViewById<EditText>(R.id.etMobileStep2)
        val purposeSpinner = view.findViewById<Spinner>(R.id.spinnerPurpose)
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        val btnNext = view.findViewById<Button>(R.id.btnNext)

        nameField.setText(studentName)
        courseField.setText(studentCourse)
        studentIdField.setText(studentId)
        mobileField.setText(studentMobile)

        // Auto-fill date with current date
        val calendar = Calendar.getInstance()
        selectedDate = String.format("%02d/%02d/%d", 
            calendar.get(Calendar.DAY_OF_MONTH), 
            calendar.get(Calendar.MONTH) + 1, 
            calendar.get(Calendar.YEAR))
        dateField.setText(selectedDate)

        val purposes = arrayOf("Select Purpose", "Down Payment", "Pay Balance", "Installment")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, purposes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        purposeSpinner.adapter = adapter

        btnBack.setOnClickListener {
            replaceWithStep1()
        }

        btnNext.setOnClickListener {
            selectedPurpose = purposeSpinner.selectedItem.toString()

            if (selectedPurpose == "Select Purpose") {
                Toast.makeText(requireContext(), "Please select purpose of payment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            replaceWithStep3()
        }
    }

    private fun replaceWithStep1() {
        val view = layoutInflater.inflate(R.layout.fragment_book_appointment_step1, null)
        (this.view as? ViewGroup)?.removeAllViews()
        (this.view as? ViewGroup)?.addView(view)
        
        val locationStatusText = view.findViewById<TextView>(R.id.tvLocationStatus)
        val transactionTypeGroup = view.findViewById<RadioGroup>(R.id.rgTransactionType)
        val nameField = view.findViewById<EditText>(R.id.etName)
        val courseField = view.findViewById<EditText>(R.id.etCourse)
        val studentIdField = view.findViewById<EditText>(R.id.etStudentId)
        val mobileField = view.findViewById<EditText>(R.id.etMobile)
        val btnNext = view.findViewById<Button>(R.id.btnNext)

        nameField.setText(studentName)
        courseField.setText(studentCourse)
        studentIdField.setText(studentId)
        mobileField.setText(studentMobile)
        
        if (selectedTransactionType == "Registrar") {
            transactionTypeGroup.check(R.id.rbRequestRegistrar)
        } else {
            transactionTypeGroup.check(R.id.rbRequestFinance)
        }

        checkLocationAndUpdateUI(locationStatusText)

        transactionTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedTransactionType = when (checkedId) {
                R.id.rbRequestRegistrar -> "Registrar"
                R.id.rbRequestFinance -> "Finance"
                else -> "Finance"
            }
        }

        btnNext.setOnClickListener {
            studentName = nameField.text.toString().trim()
            studentCourse = courseField.text.toString().trim()
            studentId = studentIdField.text.toString().trim()
            studentMobile = mobileField.text.toString().trim()

            if (studentName.isEmpty() || studentCourse.isEmpty() || studentId.isEmpty() || studentMobile.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedTransactionType == "Registrar") {
                val intent = Intent(requireContext(), RegistrarRequestActivity::class.java)
                startActivity(intent)
                return@setOnClickListener
            }

            replaceWithStep2()
        }
    }

    private fun replaceWithStep3() {
        val view = layoutInflater.inflate(R.layout.fragment_book_appointment_step3, null)
        (this.view as? ViewGroup)?.removeAllViews()
        (this.view as? ViewGroup)?.addView(view)
        setupStep3(view)
    }

    private fun setupStep3(view: View) {
        currentStep = 3

        val paymentMethodSpinner = view.findViewById<Spinner>(R.id.spinnerPaymentMethod)
        val dynamicFieldsLayout = view.findViewById<LinearLayout>(R.id.layoutDynamicFields)
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)

        val paymentMethods = arrayOf("Select Payment Method", "Gcash", "Cash", "Card", "Cheque")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, paymentMethods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        paymentMethodSpinner.adapter = adapter

        paymentMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPaymentMethod = paymentMethods[position]
                updateDynamicFields(dynamicFieldsLayout, selectedPaymentMethod)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnBack.setOnClickListener {
            replaceWithStep2()
        }

        btnSubmit.setOnClickListener {
            if (selectedPaymentMethod == "Select Payment Method") {
                Toast.makeText(requireContext(), "Please select a payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Collect static field data
            val cashDenomination = view.findViewById<EditText>(R.id.etCashDenomination).text.toString().trim()
            val noPieces = view.findViewById<EditText>(R.id.etNoPieces).text.toString().trim()
            val amount = view.findViewById<EditText>(R.id.etAmount).text.toString().trim()

            if (cashDenomination.isEmpty() || noPieces.isEmpty() || amount.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all amount paid fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Collect dynamic field data
            collectDynamicFieldsData(dynamicFieldsLayout)

            if (paymentFieldsData.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all payment method fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add static fields to payment data
            paymentFieldsData["cashDenomination"] = cashDenomination
            paymentFieldsData["noPieces"] = noPieces
            paymentFieldsData["amount"] = amount

            submitQueueRequest(btnSubmit)
        }
    }

    private fun updateDynamicFields(layout: LinearLayout, paymentMethod: String) {
        layout.removeAllViews()
        paymentFieldsData.clear()

        when (paymentMethod) {
            "Gcash" -> {
                addFieldLabel(layout, "Gcash Name")
                addEditTextField(layout, "etGcashName", "Sunshine")
                addFieldLabel(layout, "Amount")
                addEditTextField(layout, "etGcashAmount", "5000")
            }
            "Cash" -> {
                addFieldLabel(layout, "Gcash Name")
                addEditTextField(layout, "etGcashName", "Sunshine")
                addFieldLabel(layout, "Amount")
                addEditTextField(layout, "etCashAmount", "5000")
            }
            "Card" -> {
                addFieldLabel(layout, "Card Holder Name")
                addEditTextField(layout, "etCardHolderName", "Card Holder Name")
                addFieldLabel(layout, "Bank")
                addEditTextField(layout, "etBank", "Bank Name")
                addFieldLabel(layout, "Amount")
                addEditTextField(layout, "etCardAmount", "5000")
            }
            "Cheque" -> {
                addFieldLabel(layout, "Cheque No.")
                addEditTextField(layout, "etChequeNo", "Cheque Number")
                addFieldLabel(layout, "Bank")
                addEditTextField(layout, "etBank", "Bank Name")
                addFieldLabel(layout, "Amount")
                addEditTextField(layout, "etChequeAmount", "5000")
            }
        }
    }

    private fun addFieldLabel(layout: LinearLayout, labelText: String) {
        val label = TextView(requireContext())
        label.text = labelText
        label.textSize = 14f
        label.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        
        val labelParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        labelParams.setMargins(0, 0, 0, 8)
        label.layoutParams = labelParams
        
        layout.addView(label)
    }

    private fun addEditTextField(layout: LinearLayout, id: String, hint: String) {
        val editText = EditText(requireContext())
        editText.tag = id
        editText.hint = hint
        editText.setPadding(16, 16, 16, 16)
        editText.setBackgroundResource(android.R.drawable.edit_text)
        editText.setTextColor(resources.getColor(android.R.color.black, null))
        editText.setHintTextColor(resources.getColor(android.R.color.darker_gray, null))
        
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            150 // 50dp height
        )
        params.setMargins(0, 0, 0, 32)
        editText.layoutParams = params
        
        layout.addView(editText)
    }

    private fun collectDynamicFieldsData(layout: LinearLayout) {
        paymentFieldsData.clear()
        for (i in 0 until layout.childCount) {
            val child = layout.getChildAt(i)
            if (child is EditText) {
                val key = child.tag.toString()
                val value = child.text.toString().trim()
                paymentFieldsData[key] = value
            }
        }
    }

    private fun loadUserData(nameField: EditText, courseField: EditText, studentIdField: EditText, mobileField: EditText) {
        val currentUser = auth.currentUser ?: return

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
                val isInAllowedArea = isLocationInUrgelloArea(location)
                
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

    private fun isLocationInUrgelloArea(location: Location): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            URGELLO_CENTER_LAT,
            URGELLO_CENTER_LNG,
            results
        )
        return results[0] <= ALLOWED_RADIUS_METERS
    }


    private fun submitQueueRequest(submitButton: Button) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_LONG).show()
            return
        }

        submitButton.isEnabled = false
        submitButton.text = "Checking location..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Toast.makeText(requireContext(), "Unable to get location. Enable GPS and try again", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit"
                return@addOnSuccessListener
            }

            if (!isLocationInUrgelloArea(location)) {
                Toast.makeText(requireContext(), "You must be in Urgello, Cebu City area", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
                submitButton.text = "Submit"
                return@addOnSuccessListener
            }

            submitButton.text = "Submitting..."
            proceedWithQueueRequest(location, submitButton)
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error getting location: ${e.message}", Toast.LENGTH_LONG).show()
            submitButton.isEnabled = true
            submitButton.text = "Submit"
        }
    }

    private fun proceedWithQueueRequest(location: Location, submitButton: Button) {
        generateQueueNumber { queueNumber ->
            val currentUser = auth.currentUser
            
            val queueData = hashMapOf(
                "studentUID" to currentUser?.uid,
                "studentEmail" to currentUser?.email,
                "studentName" to studentName,
                "course" to studentCourse,
                "studentId" to studentId,
                "mobile" to studentMobile,
                "department" to "Finance",
                "transactionType" to "Finance Payment",
                "queueNumber" to queueNumber,
                "status" to "Pending",
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "timestamp" to FieldValue.serverTimestamp(),
                "paymentDate" to selectedDate,
                "paymentPurpose" to selectedPurpose,
                "paymentMethod" to selectedPaymentMethod,
                "paymentDetails" to paymentFieldsData
            )

            db.collection("appointments")
                .add(queueData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Queue requested! Your number: $queueNumber", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    submitButton.isEnabled = true
                    submitButton.text = "Submit"
                }
        }
    }

    private fun generateQueueNumber(callback: (String) -> Unit) {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        db.collection("appointments")
            .whereEqualTo("department", "Finance")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size() + 1
                val queueNumber = "$today-FIN-${String.format("%03d", count)}"
                callback(queueNumber)
            }
            .addOnFailureListener {
                val random = Random().nextInt(999) + 1
                val queueNumber = "$today-FIN-${String.format("%03d", random)}"
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
