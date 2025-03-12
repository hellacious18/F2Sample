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
                    .inflate(R.layout.item_image_message, parent, false)
                ImageMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_text_message, parent, false)
                TextMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is TextMessageViewHolder -> bindTextMessage(holder, message)
            is ImageMessageViewHolder -> {
                if (!message.imageUrl.isNullOrEmpty()) {
                    bindImageMessage(holder, message)
                } else {
                    holder.imageView.visibility = View.GONE
                }
            }
        }
    }


    private fun bindTextMessage(holder: TextMessageViewHolder, message: Message) {
        val params = holder.messageText.layoutParams as ViewGroup.MarginLayoutParams
        val markwon = Markwon.create(holder.itemView.context)

        // Set gravity and margins based on whether the message is from the user
        if (message.isUser) {
            params.marginStart = 80
            params.marginEnd = 0
            (holder.messageText.parent as LinearLayout).gravity = Gravity.END
            holder.messageText.setBackgroundResource(R.drawable.user_bubble_bg)
        } else {
            params.marginEnd = 80
            params.marginStart = 0
            (holder.messageText.parent as LinearLayout).gravity = Gravity.START
            holder.messageText.setBackgroundResource(0)
        }

        // Render text (with Markdown for AI messages)
        message.text?.let { text ->
            if (message.isUser) {
                holder.messageText.text = text
            } else {
                markwon.setMarkdown(holder.messageText, text)
            }
        }
    }

    private fun bindImageMessage(holder: ImageMessageViewHolder, message: Message) {
        val params = holder.imageView.layoutParams as ViewGroup.MarginLayoutParams

        // Set gravity and margins based on whether the message is from the user
        if (message.isUser) {
            params.marginStart = 80
            params.marginEnd = 0
            (holder.imageView.parent as LinearLayout).gravity = Gravity.END
        } else {
            params.marginEnd = 80
            params.marginStart = 0
            (holder.imageView.parent as LinearLayout).gravity = Gravity.START
        }



        // Load image using Glide
        message.imageUrl?.let { imageUrl ->
            holder.imageView.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .error(R.drawable.add_photo_alternate_24px) // Add an error image
                .into(holder.imageView)
        } ?: run {
            holder.imageView.visibility = View.INVISIBLE
        }

    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].imageUrl != null) {
            TYPE_IMAGE
        } else {
            TYPE_TEXT
        }
    }

    override fun getItemCount(): Int = messages.size
}