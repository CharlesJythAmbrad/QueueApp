package com.reymoto.medicare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class QueueAdapter(
    private val queue: List<Appointment>,
    private val onCancelClick: (Appointment) -> Unit
) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {

    class QueueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val queueNumberTextView: TextView = itemView.findViewById(R.id.tvQueueNumber)
        val transactionTypeTextView: TextView = itemView.findViewById(R.id.tvTransactionType)
        val dateTextView: TextView = itemView.findViewById(R.id.tvDate)
        val statusTextView: TextView = itemView.findViewById(R.id.tvStatus)
        val cancelButton: Button = itemView.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_queue, parent, false)
        return QueueViewHolder(view)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val appointment = queue[position]
        
        holder.queueNumberTextView.text = appointment.queueNumber
        holder.transactionTypeTextView.text = appointment.transactionType
        
        // Hide date for registrar items, show for finance items
        val department = if (appointment.queueNumber.isNotEmpty()) {
            val parts = appointment.queueNumber.split("-")
            if (parts.size >= 4) {
                when (parts[3]) {
                    "REG" -> "Registrar"
                    "FIN" -> "Finance"
                    else -> "Unknown"
                }
            } else {
                "Unknown"
            }
        } else {
            "Unknown"
        }
        
        if (department == "Registrar") {
            holder.dateTextView.visibility = View.GONE
        } else {
            holder.dateTextView.visibility = View.VISIBLE
            holder.dateTextView.text = appointment.appointmentDate
        }
        
        holder.statusTextView.text = appointment.status
        
        // Set status badge color
        val statusColor = when (appointment.status) {
            "Pending" -> android.R.color.holo_orange_dark
            "Serving" -> android.R.color.holo_blue_dark
            "Served" -> android.R.color.holo_blue_dark
            "Completed" -> android.R.color.holo_green_dark
            "Cancelled" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
        holder.statusTextView.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, statusColor)
        )
        
        // Show cancel button only for Pending status
        if (appointment.status == "Pending") {
            holder.cancelButton.visibility = View.VISIBLE
            holder.cancelButton.setOnClickListener {
                onCancelClick(appointment)
            }
        } else {
            holder.cancelButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = queue.size
}