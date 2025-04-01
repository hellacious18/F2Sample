package com.example.f2sample.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.f2sample.R
import com.example.f2sample.data.Message
import io.noties.markwon.Markwon

class ChatAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageImage: ImageView = itemView.findViewById(R.id.messageImage)
        val messageContainer: LinearLayout = itemView.findViewById(R.id.messageLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val markwon = Markwon.create(holder.itemView.context)

        // Clear existing content
        holder.messageText.text = null
        holder.messageImage.setImageDrawable(null)

        // Message Alignment and Bubble
        if (message.isUser) {
            holder.messageContainer.gravity = Gravity.END
            holder.messageText.setBackgroundResource(R.drawable.user_bubble_bg)
        } else {
            holder.messageContainer.gravity = Gravity.START
            holder.messageText.setBackgroundResource(0)
        }

        if (!message.text.isNullOrEmpty()) {
            holder.messageText.visibility = View.VISIBLE
            if (message.isUser) {
                holder.messageText.text = message.text
            } else {
                markwon.setMarkdown(holder.messageText, message.text)
            }
        } else {
            holder.messageText.visibility = View.GONE
        }

        // Image Handling
        if (!message.imageUrl.isNullOrEmpty()) {
            holder.messageImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(message.imageUrl)
                .placeholder(R.drawable.add_photo_alternate_24px)
                .error(R.drawable.add_photo_alternate_24px)
                .into(holder.messageImage)
        } else {
            holder.messageImage.visibility = View.GONE
        }

    }

    override fun getItemCount(): Int = messages.size
}