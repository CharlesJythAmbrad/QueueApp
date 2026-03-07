package com.reymoto.medicare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AppointmentHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val appointmentList = mutableListOf<Appointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_history)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        loadAppointmentHistory()

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, `DashboardFragment`::class.java))
            finish()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewHistory)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.tvEmptyView)
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(appointmentList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = appointmentAdapter
    }

    private fun loadAppointmentHistory() {
        showLoading(true)

        val currentUser = auth.currentUser ?: return

        db.collection("appointments")
            .whereEqualTo("studentUID", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                appointmentList.clear()

                for (document in documents) {
                    val appointment = Appointment(
                        id = document.id,
                        studentEmail = document.getString("studentEmail") ?: "",
                        transactionType = document.getString("transactionType") ?: "",
                        appointmentDate = document.getString("appointmentDate") ?: "",
                        appointmentTime = document.getString("appointmentTime") ?: "",
                        queueNumber = document.getString("queueNumber") ?: "",
                        status = document.getString("status") ?: "",
                        timestamp = document.getTimestamp("timestamp")
                    )
                    appointmentList.add(appointment)
                }

                appointmentAdapter.notifyDataSetChanged()
                
                if (appointmentList.isEmpty()) {
                    showEmptyView(true)
                } else {
                    showEmptyView(false)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyView(true)
                Toast.makeText(this, "Error loading history: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEmptyView(show: Boolean) {
        emptyView.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
}