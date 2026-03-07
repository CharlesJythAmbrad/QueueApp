package com.reymoto.medicare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReportsAdapter(private val reports: List<Report>) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tvReportTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.tvReportDescription)
        val userEmailTextView: TextView = itemView.findViewById(R.id.tvUserEmail)
        val timestampTextView: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]
        
        holder.titleTextView.text = report.title
        holder.descriptionTextView.text = report.description
        holder.userEmailTextView.text = "Submitted by: ${report.userEmail}"
        holder.timestampTextView.text = report.getFormattedDate()
    }

    override fun getItemCount(): Int = reports.size
}