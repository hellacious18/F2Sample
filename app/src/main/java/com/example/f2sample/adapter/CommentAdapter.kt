package com.example.f2sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.f2sample.R
import com.example.f2sample.data.Comment
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentAdapter(
    private val commentList: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: CircleImageView = itemView.findViewById(R.id.commentUserProfilePicture)
        val userName: TextView = itemView.findViewById(R.id.commentUserName)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val commentTime: TextView = itemView.findViewById(R.id.commentTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]

        holder.userName.text = comment.userName
        holder.commentText.text = comment.commentText

        Glide.with(holder.itemView.context)
            .load(comment.userProfileImageUrl)
            .placeholder(R.drawable.account_circle_24px)
            .into(holder.userImage)

        // Format timestamp
        val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        val date = Date(comment.timestamp)
        holder.commentTime.text = dateFormat.format(date)
    }

    override fun getItemCount() = commentList.size
}
