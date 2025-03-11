package com.example.f2sample.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.R
import com.example.f2sample.adapter.PostAdapter
import com.example.f2sample.data.Post
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class UserFeedFragment : Fragment(R.layout.fragment_user_feed) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postList = mutableListOf<Post>()
    private lateinit var adapter: PostAdapter

    private lateinit var blurView: View
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewUserFeed: RecyclerView
    private lateinit var floatingButton: FloatingActionButton

    // Image Picker Result
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        blurView = view.findViewById(R.id.blur_view)
        progressBar = view.findViewById(R.id.progress_bar)
        recyclerViewUserFeed = view.findViewById(R.id.recyclerViewUserFeed)
        floatingButton = view.findViewById(R.id.floatingButton)

        // Setup RecyclerView with delete functionality
        adapter = PostAdapter(postList) { post -> deletePost(post) }
        recyclerViewUserFeed.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewUserFeed.adapter = adapter

        showLoading()
        // Load posts
        loadPosts()
        hideLoading()

        // Handle Floating Button Click
        floatingButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun showLoading() {
        blurView.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        blurView.bringToFront()
        progressBar.bringToFront()
    }

    private fun hideLoading() {
        blurView.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    private fun uploadImage(uri: Uri) {
        showLoading()
        val fileRef = storage.reference.child("images/${UUID.randomUUID()}.jpg")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    savePost(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun savePost(imageUrl: String) {
        val postRef = db.collection("posts").document()
        val post = Post(
            postId = postRef.id, // Store Firestore document ID
            imageUrl = imageUrl,
            userProfileImageUrl = (auth.currentUser?.photoUrl ?: "").toString(),
            userName = auth.currentUser?.displayName ?: "Anonymous",
            likes = 0,
            userId = auth.currentUser?.uid ?: "",
            likedBy = emptyList()
        )

        postRef.set(post)
            .addOnSuccessListener {
                hideLoading()

                Toast.makeText(requireContext(), "Post uploaded!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(requireContext(), "Failed to save post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPosts() {
        db.collection("posts")
            .addSnapshotListener { value, _ ->
                value?.documentChanges?.forEach { change ->
                    val post = change.document.toObject(Post::class.java)
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            if (!postList.any { it.postId == post.postId }) {
                                postList.add(0, post)
                                adapter.notifyItemInserted(0)
                            }
                        }
                        DocumentChange.Type.MODIFIED -> {
                            val index = postList.indexOfFirst { it.postId == post.postId }
                            if (index != -1) {
                                postList[index] = post
                                adapter.notifyItemChanged(index)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val index = postList.indexOfFirst { it.postId == post.postId }
                            if (index != -1) {
                                postList.removeAt(index)
                                adapter.notifyItemRemoved(index)
                            }
                        }
                    }
                }
            }
    }

    private fun deletePost(post: Post) {

        val currentUserUid = auth.currentUser?.uid ?: return
        // Ensure only the owner can delete the post
        if (post.userId != currentUserUid) {
            Toast.makeText(requireContext(), "You can only delete your own posts!", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("posts").document(post.postId)
            .delete()
            .addOnSuccessListener {
                val imageRef = storage.getReferenceFromUrl(post.imageUrl)
                imageRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Post deleted!", Toast.LENGTH_SHORT).show()
                        adapter.removePost(post)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to delete image", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete post", Toast.LENGTH_SHORT).show()
            }
    }
}