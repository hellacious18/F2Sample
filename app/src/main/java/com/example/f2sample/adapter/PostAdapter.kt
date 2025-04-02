package com.example.f2sample.adapter

import android.app.AlertDialog
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
import com.example.f2sample.data.Comment
import com.example.f2sample.data.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

private const val VIEW_TYPE_LINEAR = 0
private const val VIEW_TYPE_GRID = 1

class PostAdapter(
    private val postList: MutableList<Post>,
    var isLinearLayoutManager: Boolean = true,
    private val onDeleteClick: (Post) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val auth = FirebaseAuth.getInstance()

    override fun getItemViewType(position: Int): Int {
        return if (isLinearLayoutManager) VIEW_TYPE_LINEAR else VIEW_TYPE_GRID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_LINEAR -> {
                val view = inflater.inflate(R.layout.item_post_linear, parent, false) // Use item_post for linear
                LinearViewHolder(view)
            }
            VIEW_TYPE_GRID -> {
                val view = inflater.inflate(R.layout.item_post_grid, parent, false)
                GridViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = postList[position]
        val currentUser = auth.currentUser?.uid ?: return

        when (holder.itemViewType) {
            VIEW_TYPE_LINEAR -> {
                val linearHolder = holder as LinearViewHolder
                val isLiked = post.likedBy.contains(currentUser)
                val isRated = post.ratings.containsKey(currentUser)

                Glide.with(linearHolder.itemView.context).load(post.imageUrl).into(linearHolder.postImageView)
                Glide.with(linearHolder.itemView.context).load(post.userProfileImageUrl).into(linearHolder.postUserPicture)

                linearHolder.postUserName.text = post.userName
                linearHolder.likeCountText.text = post.likes.toString()

                linearHolder.likeButton.setImageResource(if (isLiked) R.drawable.like_red_24px else R.drawable.like_24px)

                linearHolder.postMenu.visibility = if (post.userId == currentUser) View.VISIBLE else View.GONE

                loadComments(post.postId, linearHolder)
                loadAverageRating(post, linearHolder)

                linearHolder.postMenu.setOnClickListener { view ->
                    val popupMenu = androidx.appcompat.widget.PopupMenu(linearHolder.itemView.context, view)
                    popupMenu.inflate(R.menu.vertical_menu)

                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.action_delete_post -> {
                                showDeleteConfirmationDialog(linearHolder.itemView, post)
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }


                linearHolder.likeButton.setOnClickListener { toggleLike(post, currentUser, linearHolder) }
                linearHolder.commentButton.setOnClickListener {
                    if (linearHolder.commentSection.visibility == View.VISIBLE) {
                        linearHolder.commentSection.visibility = View.GONE
                    } else {
                        linearHolder.commentSection.visibility = View.VISIBLE
                        linearHolder.ratingBar.visibility = View.GONE
                        loadComments(post.postId, linearHolder)
                    }
                }
                linearHolder.postCommentButton.setOnClickListener {
                    val commentText = linearHolder.editTextComment.text.toString().trim()
                    if (commentText.isNotEmpty()) {
                        addComment(post.postId, commentText, linearHolder)
                        linearHolder.editTextComment.text.clear()
                    }
                }

                linearHolder.recyclerViewComment.scrollToPosition(0)

                linearHolder.ratingImageButton.setImageResource(if (isRated) R.drawable.star_yellow_24dp else R.drawable.star_24px)
                linearHolder.ratingImageButton.setOnClickListener {
                    if (linearHolder.ratingBar.visibility == View.VISIBLE) {
                        linearHolder.ratingBar.visibility = View.GONE
                    } else {
                        linearHolder.ratingBar.visibility = View.VISIBLE
                        linearHolder.commentSection.visibility = View.GONE
                        loadComments(post.postId, linearHolder)
                    }
                }
                linearHolder.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
                    saveRating(post.postId, currentUser, rating)
                }
            }
            VIEW_TYPE_GRID -> {
                val gridHolder = holder as GridViewHolder
                loadAverageRating(post, gridHolder)
                Glide.with(gridHolder.itemView.context).load(post.imageUrl).into(gridHolder.postImageView)
            }
        }
    }

    private fun loadAverageRating(post: Post, holder: LinearViewHolder) {
        val ratings = post.ratings.values
        if (ratings.isNotEmpty()) {
            val averageRating = ratings.sum() / ratings.size
            val ratingCount = ratings.size
            holder.ratingCount.text = ratingCount.toString()
            holder.ratingAverage.text = String.format("%.1f", averageRating)
        } else {
            holder.ratingAverage.text = "0"
        }
    }

    private fun loadAverageRating(post: Post, holder: GridViewHolder) {
        val ratings = post.ratings.values
        if (ratings.isNotEmpty()) {
            val averageRating = ratings.sum() / ratings.size
            val ratingCount = ratings.size
            holder.ratingCount.text = String.format("(%d)",ratingCount)
            holder.ratingAverage.text = String.format("%.1f", averageRating)
        } else {
            holder.ratingAverage.text = "0"
        }
    }


    private fun saveRating(postId: String, userId: String, rating: Float) {
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document(postId)

        postRef.update("ratings.$userId", rating)
            .addOnSuccessListener {
                println("Rating updated!")
            }
            .addOnFailureListener { e ->
                println("Failed to update rating: ${e.message}")
            }
    }


    override fun getItemCount() = postList.size

    fun removePost(post: Post) {
        val position = postList.indexOf(post)
        if (position != -1) {
            postList.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun setLayout(layout:Boolean){
        this.isLinearLayoutManager = layout;
        notifyDataSetChanged()
    }

    private fun toggleLike(post: Post, userId: String, holder: LinearViewHolder) {
        // Update UI immediately
        val isLiked = post.likedBy.contains(userId)
        val updatedLikedBy = if (isLiked) {
            post.likedBy - userId
        } else {
            post.likedBy + userId
        }

        // Update local post object
        post.likedBy = updatedLikedBy
        post.likes = updatedLikedBy.size

        // Update UI
        holder.likeCountText.text = post.likes.toString()
        holder.likeButton.setImageResource(
            if (!isLiked) R.drawable.like_red_24px else R.drawable.like_24px
        )

        // Update Firestore
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document(post.postId)

        postRef.update(mapOf(
            "likes" to post.likes,
            "likedBy" to updatedLikedBy
        ))
    }

    private fun addComment(postId: String, commentText: String, holder: LinearViewHolder) {
        val db = FirebaseFirestore.getInstance()
        val commentRef = db.collection("posts").document(postId).collection("comments").document()

        val comment = Comment(
            userId = auth.currentUser?.uid ?: "",
            userName = auth.currentUser?.displayName ?: "Anonymous",
            userProfileImageUrl = auth.currentUser?.photoUrl.toString(),
            commentText = commentText,
            timestamp = System.currentTimeMillis()
        )

        // Update UI immediately
        holder.editTextComment.text.clear()

        commentRef.set(comment)
    }

    private fun loadComments(postId: String, holder: LinearViewHolder) {
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


    private fun showDeleteConfirmationDialog(view: View, post: Post) {
        AlertDialog.Builder(view.context).apply {
            setTitle("Delete Post")
            setMessage("Are you sure you want to delete this post?")
            setPositiveButton("Delete") { _, _ ->
                deletePost(post)
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun deletePost(post: Post) {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts").document(post.postId)
            .delete()
            .addOnSuccessListener {
                removePost(post)
            }
            .addOnFailureListener { e ->
                println("Failed todelete post: ${e.message}")
            }
    }


    class LinearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImageView: ImageView = itemView.findViewById(R.id.postImageView)
        val postUserPicture: CircleImageView = itemView.findViewById(R.id.userProfilePicture)
        val postUserName: TextView = itemView.findViewById(R.id.postUserName)
        val postMenu: ImageButton = itemView.findViewById(R.id.post_menu)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val ratingImageButton: ImageButton = itemView.findViewById(R.id.ratingImageButton)
        val ratingCount: TextView = itemView.findViewById(R.id.ratingCount)
        var ratingAverage: TextView = itemView.findViewById(R.id.textViewAvgRatings)
        val likeButton: ImageButton = itemView.findViewById(R.id.likeButton)
        val likeCountText: TextView = itemView.findViewById(R.id.likeCountText)
        val commentButton: ImageButton = itemView.findViewById(R.id.commentButton)
        val postCommentButton: ImageButton = itemView.findViewById(R.id.postCommentButton)
        val editTextComment: EditText = itemView.findViewById(R.id.editTextComment)
        val commentSection: View = itemView.findViewById(R.id.commentSection)
        val commentCountText: TextView = itemView.findViewById(R.id.commentCountText)
        val recyclerViewComment: RecyclerView = itemView.findViewById(R.id.recyclerViewComment)
    }

    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImageView: ImageView = itemView.findViewById(R.id.postImageView)
        var ratingAverage: TextView = itemView.findViewById(R.id.textViewAvgRatings)
        var ratingCount: TextView = itemView.findViewById(R.id.ratingCount)
    }
}