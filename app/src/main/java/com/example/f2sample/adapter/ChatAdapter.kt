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
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TEXT = 0
        private const val TYPE_IMAGE = 1
    }

    class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }

    class ImageMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.messageImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message, parent, false)
                ImageMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message, parent, false)
                TextMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        if (holder is TextMessageViewHolder) {
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
        } else if (holder is ImageMessageViewHolder) {
            val params = holder.imageView.layoutParams as ViewGroup.MarginLayoutParams
            val imageUrl = message.imageUrl

            if (imageUrl != null) {
                // If there is an image URL, make ImageView visible
                holder.imageView.visibility = View.VISIBLE
                if (message.isUser) {
                    // User image: right side
                    params.marginStart = 80
                    params.marginEnd = 0
                    (holder.imageView.parent as LinearLayout).gravity = Gravity.END
                } else {
                    // AI image: left side
                    params.marginEnd = 80
                    params.marginStart = 0
                    (holder.imageView.parent as LinearLayout).gravity = Gravity.START
                }

                // Load the image using Glide
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .into(holder.imageView)
            } else {
                // If there is no image URL, make ImageView invisible
                holder.imageView.visibility = View.INVISIBLE
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Check if the message contains an image or is just text
        return if (messages[position].imageUrl != null) {
            TYPE_IMAGE
        } else {
            TYPE_TEXT
        }
    }

    override fun getItemCount(): Int = messages.size
}
