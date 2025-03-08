package com.example.f2sample.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.f2sample.R
import com.example.f2sample.adapter.CommentAdapter
import com.example.f2sample.data.Comment
import com.example.f2sample.data.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(
    private val postList: MutableList<Post>,
    private val onDeleteClick: (Post) -> Unit
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
        val postCommentButton: ImageButton = itemView.findViewById(R.id.postCommentButton)
        val editTextComment: EditText = itemView.findViewById(R.id.editTextComment)
        val commentSection: View = itemView.findViewById(R.id.commentSection)
        val commentCountText: TextView = itemView.findViewById(R.id.commentCountText)
        val recyclerViewComment: RecyclerView = itemView.findViewById(R.id.recyclerViewComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        val currentUser = auth.currentUser?.uid ?: return

        Glide.with(holder.itemView.context).load(post.imageUrl).into(holder.postImageView)
        Glide.with(holder.itemView.context).load(post.userProfileImageUrl).into(holder.postUserPicture)

        holder.postUserName.text = post.userName
        holder.ratingBar.rating = post.rating
        holder.likeCountText.text = post.likes.toString()

        val isLiked = post.likedBy.contains(currentUser)
        holder.likeButton.setImageResource(if (isLiked) R.drawable.liked_24px else R.drawable.favorite_24px)

        holder.deleteButton.visibility = if (post.userId == currentUser) View.VISIBLE else View.GONE

        loadComments(post.postId, holder)

        holder.likeButton.setOnClickListener { toggleLike(post, currentUser, holder) }
        holder.commentButton.setOnClickListener {
            if (holder.commentSection.visibility == View.VISIBLE) {
                holder.commentSection.visibility = View.GONE
            } else {
                holder.commentSection.visibility = View.VISIBLE
                loadComments(post.postId, holder)
            }
        }
        holder.postCommentButton.setOnClickListener {
            val commentText = holder.editTextComment.text.toString().trim()
            if (commentText.isNotEmpty()) {
                addComment(post.postId, commentText, holder)
                holder.editTextComment.text.clear()
            }
        }

        holder.deleteButton.setOnClickListener { onDeleteClick(post) }
    }

    override fun getItemCount() = postList.size

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

        val updatedLikedBy = post.likedBy.toMutableSet().apply {
            if (contains(userId)) remove(userId) else add(userId)
        }
        val updatedLikes = updatedLikedBy.size

        postRef.update(mapOf(
            "likes" to updatedLikes,
            "likedBy" to updatedLikedBy
        )).addOnSuccessListener {
            post.likes = updatedLikes
            post.likedBy = updatedLikedBy.toList()

            holder.likeCountText.text = post.likes.toString()
            holder.likeButton.setImageResource(if (updatedLikedBy.contains(userId)) R.drawable.liked_24px else R.drawable.favorite_24px)
        }
    }

    private fun loadComments(postId: String, holder: PostViewHolder) {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val comments = value?.toObjects(Comment::class.java) ?: emptyList()
                val commentAdapter = CommentAdapter(comments)
                holder.commentCountText.text = comments.size.toString() // Update comment count

                holder.recyclerViewComment.layoutManager = LinearLayoutManager(holder.itemView.context)
                holder.recyclerViewComment.adapter = commentAdapter
            }
    }


    private fun addComment(postId: String, commentText: String, holder: PostViewHolder) {
        val db = FirebaseFirestore.getInstance()
        val commentRef = db.collection("posts").document(postId).collection("comments").document()

        val comment = Comment(
            userId = auth.currentUser?.uid ?: "",
            userName = auth.currentUser?.displayName ?: "Anonymous",
            userProfileImageUrl = auth.currentUser?.photoUrl.toString(),
            commentText = commentText
        )

        commentRef.set(comment)
            .addOnSuccessListener {
                loadComments(postId, holder) // Refresh comments
            }
    }

}
