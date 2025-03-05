package com.example.f2sample.fragments

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.R
import com.example.f2sample.adapters.PostAdapter
import com.example.f2sample.data.Post
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class UserFeedFragment : Fragment(R.layout.fragment_user_feed) {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val postList = mutableListOf<Post>()
    private lateinit var adapter: PostAdapter

    private lateinit var recyclerViewUserFeed: RecyclerView
    private lateinit var floatingButton: FloatingActionButton

    // Image Picker Result
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewUserFeed = view.findViewById(R.id.recyclerViewUserFeed)
        floatingButton = view.findViewById(R.id.floatingButton)

        // Setup RecyclerView
        adapter = PostAdapter(postList)
        recyclerViewUserFeed.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewUserFeed.adapter = adapter

        // Load posts
        loadPosts()

        // Handle Floating Button Click
        floatingButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun uploadImage(uri: Uri) {
        val fileRef = storage.reference.child("images/${UUID.randomUUID()}.jpg")

        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    savePost(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun savePost(imageUrl: String) {
        val post = Post(
            imageUrl = imageUrl,
            userProfileImageUrl = (auth.currentUser?.photoUrl ?: "").toString(),
            userName = auth.currentUser?.displayName ?: "Anonymous",
            likes = 0,
            comments = emptyList(),
            rating = 0f
        )

        db.collection("posts")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post uploaded!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save post", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPosts() {
        db.collection("posts")
            .addSnapshotListener { value, _ ->
                postList.clear()
                value?.toObjects(Post::class.java)?.let {
                    postList.addAll(it)
                    adapter.notifyDataSetChanged()
                }
            }
    }
}
