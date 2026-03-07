package com.reymoto.medicare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class QueueFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var queueAdapter: QueueAdapter
    private val queueList = mutableListOf<Appointment>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_queue, container, false)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerView()
        loadMyQueueHistory()

        view.findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            loadMyQueueHistory()
        }
        
        return view
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewQueue)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.tvEmptyView)
    }

    private fun setupRecyclerView() {
        queueAdapter = QueueAdapter(queueList) { appointment ->
            showCancelConfirmation(appointment)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = queueAdapter
    }

    private fun loadMyQueueHistory() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        db.collection("appointments")
            .whereEqualTo("studentUID", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
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
                        status = document.getString("status") ?: "Pending",
                        timestamp = document.getTimestamp("timestamp")
                    )
                    queueList.add(appointment)
                }

                queueAdapter.notifyDataSetChanged()
                
                if (queueList.isEmpty()) {
                    showEmptyView(true)
                } else {
                    showEmptyView(false)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                showEmptyView(true)
                Toast.makeText(requireContext(), "Error loading queue: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showCancelConfirmation(appointment: Appointment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Queue")
            .setMessage("Are you sure you want to cancel queue ${appointment.queueNumber}?")
            .setPositiveButton("Yes") { _, _ ->
                cancelQueue(appointment)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelQueue(appointment: Appointment) {
        db.collection("appointments")
            .document(appointment.id)
            .update("status", "Cancelled")
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Queue cancelled successfully", Toast.LENGTH_SHORT).show()
                loadMyQueueHistory()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error cancelling queue: ${exception.message}", Toast.LENGTH_LONG).show()
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