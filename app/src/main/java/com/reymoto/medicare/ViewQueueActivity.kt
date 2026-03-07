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
import java.text.SimpleDateFormat
import java.util.*

class ViewQueueActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var currentQueueText: TextView
    private lateinit var myQueueText: TextView
    private lateinit var queueAdapter: QueueAdapter
    private val queueList = mutableListOf<Appointment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_queue)

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
        loadQueue()
        loadMyQueue()

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            loadQueue()
            loadMyQueue()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, `DashboardFragment`::class.java))
            finish()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewQueue)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.tvEmptyView)
        currentQueueText = findViewById(R.id.tvCurrentQueue)
        myQueueText = findViewById(R.id.tvMyQueue)
    }

    private fun setupRecyclerView() {
        queueAdapter = QueueAdapter(queueList) { appointment ->
            // Cancel functionality - can be implemented if needed
            Toast.makeText(this, "Cancel not available in this view", Toast.LENGTH_SHORT).show()
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = queueAdapter
    }

    private fun loadQueue() {
        showLoading(true)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        db.collection("appointments")
            .whereEqualTo("appointmentDate", today)
            .whereEqualTo("status", "Pending")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                queueList.clear()

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
                    queueList.add(appointment)
                }

                queueAdapter.notifyDataSetChanged()
                
                if (queueList.isEmpty()) {
                    showEmptyView(true)
                    currentQueueText.text = "Current Queue: None"
                } else {
                    showEmptyView(false)
                    currentQueueText.text = "Current Queue: ${queueList[0].queueNumber}"
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyView(true)
                Toast.makeText(this, "Error loading queue: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadMyQueue() {
        val currentUser = auth.currentUser ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        db.collection("appointments")
            .whereEqualTo("studentUID", currentUser.uid)
            .whereEqualTo("appointmentDate", today)
            .whereEqualTo("status", "Pending")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val myAppointment = documents.documents[0]
                    val queueNumber = myAppointment.getString("queueNumber") ?: ""
                    val position = queueList.indexOfFirst { it.queueNumber == queueNumber } + 1
                    
                    if (position > 0) {
                        myQueueText.text = "Your Queue: $queueNumber (Position: $position)"
                        myQueueText.setTextColor(getColor(R.color.primary_color))
                    } else {
                        myQueueText.text = "Your Queue: $queueNumber"
                    }
                } else {
                    myQueueText.text = "You have no pending appointments today"
                }
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