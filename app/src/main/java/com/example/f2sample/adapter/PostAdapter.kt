package com.example.f2sample.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.f2sample.R
import com.example.f2sample.data.Post
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(private val postList: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImageView: ImageView = itemView.findViewById(R.id.postImageView)
        val postUserPicture: CircleImageView = itemView.findViewById(R.id.userProfilePicture)
        val postUserName: TextView = itemView.findViewById(R.id.postUserName)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val likeButton: ImageButton = itemView.findViewById(R.id.likeButton)
        val commentButton: ImageButton = itemView.findViewById(R.id.commentButton)
        val shareButton: ImageButton = itemView.findViewById(R.id.shareButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }


    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]

        // Load image with Glide
        Glide.with(holder.itemView.context)
            .load(post.imageUrl)
            .into(holder.postImageView)

        //Set user photo
        Glide.with(holder.itemView.context)
            .load(post.userProfileImageUrl)
            .into(holder.postUserPicture)


        // Set user name
        holder.postUserName.text = post.userName

        // Set rating
        holder.ratingBar.rating = post.rating

        // Handle like button click
        holder.likeButton.setOnClickListener {
            // Handle like action (increment likes in Firestore)
        }

        // Handle comment button click
        holder.commentButton.setOnClickListener {
            // Open comment section
        }

        // Handle share button click
        holder.shareButton.setOnClickListener {
            // Share post link
        }
    }

    override fun getItemCount() = postList.size
}
