package com.example.f2sample.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class BeautyFragment : Fragment() {

    private lateinit var inputField: EditText
    private lateinit var sendButton: Button
    private lateinit var uploadButton: Button
    private lateinit var imageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    private val PICK_IMAGE_REQUEST = 1
    private var imageBitmap: Bitmap? = null

    // Firebase services
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    // Using gemini-2.0-flash model; adjust as needed.
    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-2.0-flash")
    // Save chats using the current user's email (or a default if not available)
    private val userId = FirebaseAuth.getInstance().currentUser?.email ?: "guest@example.com"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_beauty, container, false)

        inputField = view.findViewById(R.id.inputField)
        sendButton = view.findViewById(R.id.sendButton)
        uploadButton = view.findViewById(R.id.uploadButton)
        imageView = view.findViewById(R.id.imageView)
        recyclerView = view.findViewById(R.id.recylerViewBeautyFragment)

        chatAdapter = ChatAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener { sendMessage() }
        uploadButton.setOnClickListener { pickImageFromGallery() }

        listenForMessages()
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
        // Get the user input. If empty but image is provided, default to "Analyze image".
        var userMessage = inputField.text.toString().trim()
        if (userMessage.isEmpty() && imageBitmap == null) return
        if (userMessage.isEmpty() && imageBitmap != null) {
            userMessage = "Analyze image"
        }

        inputField.text.clear()

        // If an image is present, upload it first
        if (imageBitmap != null) {
            // Save the image message first so it shows in the chat
            uploadImageToFirebaseStorage { uploadedImageUrl ->
                val imageMessage = Message(
                    text = "Analyze image", // Default caption if no text provided
                    imageUrl = uploadedImageUrl,
                    isUser = true
                )
                saveMessageToFirestore(imageMessage)
                // Also, if user typed text (other than default), save it as a separate message
                if (userMessage != "Analyze image") {
                    val textMessage = Message(
                        text = userMessage,
                        isUser = true
                    )
                    saveMessageToFirestore(textMessage)
                }
                // Now get AI response using the image and the user message
                getAIResponse(userMessage, imageBitmap)
                imageBitmap = null
                imageView.setImageBitmap(null)
            }
        } else {
            // No image; simply send the text and get AI response
            val textMessage = Message(
                text = userMessage,
                isUser = true
            )
            saveMessageToFirestore(textMessage)
            getAIResponse(userMessage, null)
        }
    }

    private fun getAIResponse(userMessage: String, promptBitmap: Bitmap?) {
        lifecycleScope.launch {
            try {
                val prompt = content {
                    promptBitmap?.let { image(it) }
                    // Use default text if userMessage is empty (shouldn't happen now)
                    text(userMessage.ifEmpty { "Analyze facial features & give details:" })
                }

                val responseBuilder = StringBuilder()
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    chunk.text?.let {
                        responseBuilder.append(it)
                        // Optionally update UI as streaming response
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun uploadImageToFirebaseStorage(callback: (String) -> Unit) {
        val storageRef = storage.reference.child("images/${System.currentTimeMillis()}.jpg")
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
            .collection("chats")
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
            .collection("chats")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.map { doc ->
                    doc.toObject(Message::class.java)!!
                } ?: emptyList()
                updateChatUI(messages)
            }
    }

    private fun updateChatUI(messages: List<Message>) {
        chatAdapter = ChatAdapter(messages)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = chatAdapter
        recyclerView.scrollToPosition(messages.size - 1)
    }
}
