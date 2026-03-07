package com.reymoto.medicare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var appointmentAdapter: AppointmentAdapter
    private val appointmentList = mutableListOf<Appointment>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)
        
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupRecyclerView()
        loadAppointmentHistory()
        
        return view
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.tvEmptyView)
    }

    private fun setupRecyclerView() {
        appointmentAdapter = AppointmentAdapter(appointmentList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
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
                Toast.makeText(requireContext(), "Error loading history: ${exception.message}", Toast.LENGTH_LONG).show()
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