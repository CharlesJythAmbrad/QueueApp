package com.reymoto.medicare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppointmentAdapter(private val appointments: List<Appointment>) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val queueNumberTextView: TextView = itemView.findViewById(R.id.tvQueueNumber)
        val transactionTypeTextView: TextView = itemView.findViewById(R.id.tvTransactionType)
        val dateTimeTextView: TextView = itemView.findViewById(R.id.tvDateTime)
        val statusTextView: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        
        holder.queueNumberTextView.text = "Queue #${appointment.queueNumber}"
        holder.transactionTypeTextView.text = appointment.transactionType
        holder.dateTimeTextView.text = appointment.getFormattedDateTime()
        holder.statusTextView.text = appointment.status
        holder.statusTextView.setTextColor(holder.itemView.context.getColor(appointment.getStatusColor()))
    }

    override fun getItemCount(): Int = appointments.size
}