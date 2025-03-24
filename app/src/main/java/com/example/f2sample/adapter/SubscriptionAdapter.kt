package com.example.f2sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.R
import com.example.f2sample.data.Subscription

class SubscriptionAdapter(
    private val subscriptions: List<Subscription>,
    private val onSubscriptionSelected: (Subscription) -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>() {

    class SubscriptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.subscriptionName)
        val price: TextView = view.findViewById(R.id.subscriptionPrice)
        val features: TextView = view.findViewById(R.id.subscriptionFeatures)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription, parent, false)
        return SubscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        val subscription = subscriptions[position]
        holder.name.text = subscription.name
        holder.price.text = subscription.price
        holder.features.text = subscription.features

        holder.itemView.setOnClickListener {
            onSubscriptionSelected(subscription) // Trigger callback when tapped
        }
    }

    override fun getItemCount() = subscriptions.size
}
