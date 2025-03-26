package com.example.f2sample.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.PaymentActivity
import com.example.f2sample.R
import com.example.f2sample.adapter.ChatAdapter
import com.example.f2sample.data.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class FashionFragment : Fragment(R.layout.fragment_fashion) {

    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var uploadButton: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private var fashionFragmentOverlay: FrameLayout? = null
    private var upgradeButton: Button? = null

    private val PICK_IMAGE_REQUEST = 1
    private var imageBitmap: Bitmap? = null

    private val conversationHistory = mutableListOf<Message>()

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-2.0-flash")
    private val userId = FirebaseAuth.getInstance().currentUser?.email ?: "guest@example.com"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fashion, container, false)

        inputField = view.findViewById(R.id.inputFieldF)
        sendButton = view.findViewById(R.id.sendButtonF)
        uploadButton = view.findViewById(R.id.uploadButtonF)
        imageView = view.findViewById(R.id.imageViewF)
        recyclerView = view.findViewById(R.id.recyclerViewFashionFragmentF)
        fashionFragmentOverlay = view.findViewById(R.id.fashionFragmentOverlay)
        upgradeButton = view.findViewById(R.id.upgradeButton)

        chatAdapter = ChatAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener { sendMessage() }
        uploadButton.setOnClickListener { pickImageFromGallery() }
        upgradeButton?.setOnClickListener {
            val intent = Intent(requireContext(), PaymentActivity::class.java)
            startActivity(intent)
        }

        listenForMessages()
        getFashionChatsCount()
        return view
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
            imageView.setImageBitmap(bitmap)
            imageBitmap = bitmap
        }
    }

    private fun sendMessage() {
        var userMessage = inputField.text.toString().trim()
        if (userMessage.isEmpty() && imageBitmap == null) return
        if (userMessage.isEmpty() && imageBitmap != null) {
            userMessage = "Analyze outfit"
        }
        inputField.text.clear()

        if (imageBitmap != null) {
            uploadImageToFirebaseStorage { uploadedImageUrl ->
                val imageMessage = Message(
                    text = "Analyze outfit",
                    imageUrl = uploadedImageUrl,
                    isUser = true
                )
                saveMessageToFirestore(imageMessage)
                conversationHistory.add(imageMessage)

                if (userMessage != "Analyze outfit") {
                    val textMessage = Message(
                        text = userMessage,
                        isUser = true
                    )
                    saveMessageToFirestore(textMessage)
                    conversationHistory.add(textMessage)
                }
                getAIResponse(userMessage, imageBitmap)
                imageBitmap = null
                imageView.setImageBitmap(null)
            }
        } else {
            val textMessage = Message(
                text = userMessage,
                isUser = true
            )
            saveMessageToFirestore(textMessage)
            conversationHistory.add(textMessage)
            getAIResponse(userMessage, null)
        }
    }

    private fun getAIResponse(userMessage: String, promptBitmap: Bitmap?) {
        lifecycleScope.launch {
            try {
                val contextString = conversationHistory.takeLast(10).joinToString(separator = "\n") { msg ->
                    if (msg.isUser) "User: ${msg.text}" else "AI: ${msg.text}"
                }

                val instructions = "Analyze the outfit in the image and provide fashion recommendations. Suggest suitable styles, colors, and accessories based on body shape. If no human body is detected, ask for a clearer image."

                val fullPromptText = if (contextString.isNotEmpty()) {
                    "$contextString\nUser: $userMessage\nInstructions: $instructions"
                } else {
                    "User: $userMessage\nInstructions: $instructions"
                }

                val prompt = content {
                    promptBitmap?.let { image(it) }
                    text(fullPromptText)
                }

                val responseBuilder = StringBuilder()
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    chunk.text?.let {
                        responseBuilder.append(it)
                        val streamingMessage = Message(
                            text = responseBuilder.toString(),
                            isUser = false
                        )
                        updateChatUI(listOf(streamingMessage))
                    }
                }

                val finalMessage = Message(
                    text = responseBuilder.toString(),
                    isUser = false
                )
                saveMessageToFirestore(finalMessage)
                conversationHistory.add(finalMessage)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImageToFirebaseStorage(callback: (String) -> Unit) {
        val storageRef = storage.reference.child("fashion_images/${System.currentTimeMillis()}.jpg")
        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun saveMessageToFirestore(message: Message) {
        db.collection("users")
            .document(userId)
            .collection("fashion_chats")
            .add(message)
            .addOnSuccessListener {
                println("Message saved successfully!")
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun listenForMessages() {
        db.collection("users")
            .document(userId)
            .collection("fashion_chats")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.map { doc ->
                    doc.toObject(Message::class.java)!!
                } ?: emptyList()
                conversationHistory.clear()
                conversationHistory.addAll(messages)
                updateChatUI(messages)
            }
    }

    private fun updateChatUI(messages: List<Message>) {
        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun getFashionChatsCount() {
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { userSnapshot ->
            val info = userSnapshot.get("info") as? Map<*, *>
            val subscription = info?.get("subscription") as? String ?: "free"

            userRef.collection("beauty_chats").get()
                .addOnSuccessListener { snapshot ->
                    val count = snapshot.size()

                    requireActivity().runOnUiThread {
                        if (subscription.lowercase() == "free" && count > 20) {
                            fashionFragmentOverlay?.visibility = View.VISIBLE
                        } else {
                            fashionFragmentOverlay?.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { e -> println("Error fetching beauty_chats: ${e.message}") }
        }.addOnFailureListener { e -> println("Error fetching user data: ${e.message}") }
    }

}