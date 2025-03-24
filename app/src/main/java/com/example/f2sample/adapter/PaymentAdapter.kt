package com.example.f2sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.R
import com.example.f2sample.data.Payment

class PaymentAdapter(private val paymentList: List<Payment>) :
    RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    class PaymentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val paymentId: TextView = view.findViewById(R.id.paymentId)
        val amount: TextView = view.findViewById(R.id.amount)
        val currency: TextView = view.findViewById(R.id.currency)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_payment, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = paymentList[position]
        holder.paymentId.text = "ID: ${payment.paymentId}"
        holder.amount.text = "₹${payment.amount}"
        holder.currency.text = "Currency: ${payment.currency}"
        holder.timestamp.text = "Date: ${payment.timestamp?.toDate()}"
    }

    override fun getItemCount(): Int = paymentList.size
}
