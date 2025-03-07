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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(
    private val postList: MutableList<Post>,
    private val onDeleteClick: (Post) -> Unit // Callback for deleting a post
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val auth = FirebaseAuth.getInstance()

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImageView: ImageView = itemView.findViewById(R.id.postImageView)
        val postUserPicture: CircleImageView = itemView.findViewById(R.id.userProfilePicture)
        val postUserName: TextView = itemView.findViewById(R.id.postUserName)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val likeButton: ImageButton = itemView.findViewById(R.id.likeButton)
        val likeCountText: TextView = itemView.findViewById(R.id.likeCountText)
        val commentButton: ImageButton = itemView.findViewById(R.id.commentButton)
        val shareButton: ImageButton = itemView.findViewById(R.id.shareButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deletePostButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        val currentUser = auth.currentUser?.uid ?: return
        // Load image with Glide
        Glide.with(holder.itemView.context).load(post.imageUrl).into(holder.postImageView)

        // Set user profile picture
        Glide.with(holder.itemView.context).load(post.userProfileImageUrl).into(holder.postUserPicture)

        // Set user name and rating
        holder.postUserName.text = post.userName
        holder.ratingBar.rating = post.rating

        holder.likeCountText.text = post.likes.toString()
        val isLiked = post.likedBy.contains(currentUser)
        holder.likeButton.setImageResource(if (isLiked) R.drawable.liked_24px else R.drawable.favorite_24px)

        // Check if the current user is the owner and show/hide delete button accordingly
        if (post.userId == currentUser) {
            holder.deleteButton.visibility = View.VISIBLE
        } else {
            holder.deleteButton.visibility = View.GONE
        }
        holder.likeButton.setOnClickListener {
            toggleLike(post, currentUser, holder)
        }

        // Handle delete button click
        holder.deleteButton.setOnClickListener {
            onDeleteClick(post) // Call the delete function
        }
    }

    override fun getItemCount() = postList.size

    // Function to remove a post from the list
    fun removePost(post: Post) {
        val position = postList.indexOf(post)
        if (position != -1) {
            postList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    private fun toggleLike(post: Post, userId: String, holder: PostViewHolder) {
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document(post.postId)

        val isLiked = post.likedBy.contains(userId)
        val updatedLikes = if (isLiked) post.likes - 1 else post.likes + 1
        val updatedLikedBy = if (isLiked) post.likedBy - userId else post.likedBy + userId

        postRef.update(mapOf(
            "likes" to updatedLikes,
            "likedBy" to updatedLikedBy
        )).addOnSuccessListener {
            post.likes = updatedLikes
            post.likedBy = updatedLikedBy

            // Update UI instantly
            holder.likeCountText.text = post.likes.toString()
            holder.likeButton.setImageResource(if (updatedLikedBy.contains(userId)) R.drawable.liked_24px else R.drawable.favorite_24px)
        }
    }
}