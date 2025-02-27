package com.example.f2sample

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon

class ChatAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]

        val params = holder.messageText.layoutParams as ViewGroup.MarginLayoutParams
        val markwon = Markwon.create(holder.itemView.context)

        if (message.isUser) {
            // User message: right side with bubble
            params.marginStart = 80
            params.marginEnd = 0
            (holder.messageText.parent as LinearLayout).gravity = Gravity.END
            holder.messageText.setBackgroundResource(R.drawable.user_bubble_bg)

            holder.messageText.text = message.text
        } else {
            // AI message: left side, slightly in the middle
            params.marginEnd = 80
            params.marginStart = 0
            (holder.messageText.parent as LinearLayout).gravity = Gravity.START
            holder.messageText.setBackgroundResource(0)
            // Render AI response with Markdown directly
            markwon.setMarkdown(holder.messageText, message.text)
        }
    }


    override fun getItemCount(): Int = messages.size
}
