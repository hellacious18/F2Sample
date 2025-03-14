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
    private lateinit var sendButton: ImageButton
    private lateinit var uploadButton: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    private val PICK_IMAGE_REQUEST = 1
    private var imageBitmap: Bitmap? = null

    // Maintain local conversation history for context memory.
    private val conversationHistory = mutableListOf<Message>()

    // Firebase services
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    // Using gemini-2.0-flash model; adjust as needed.
    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-2.0-flash")
    // Save chats using the current user's email (or default)
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
        // Get user input; if empty but image is provided, default to "Analyze image".
        var userMessage = inputField.text.toString().trim()
        if (userMessage.isEmpty() && imageBitmap == null) return
        if (userMessage.isEmpty() && imageBitmap != null) {
            userMessage = "Analyze image"
        }
        inputField.text.clear()

        // Save user message in Firestore (and update local conversation history)
        if (imageBitmap != null) {
            // Upload image first
            uploadImageToFirebaseStorage { uploadedImageUrl ->
                val imageMessage = Message(
                    text = "Analyze image", // Default caption if no text provided
                    imageUrl = uploadedImageUrl,
                    isUser = true
                )
                saveMessageToFirestore(imageMessage)
                // Update local context:
                conversationHistory.add(imageMessage)

                if (userMessage != "Analyze image") {
                    val textMessage = Message(
                        text = userMessage,
                        isUser = true
                    )
                    saveMessageToFirestore(textMessage)
                    conversationHistory.add(textMessage)
                }
                // Now, include context when getting AI response
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
                // Build a context string from recent messages (if available)
                val contextString = conversationHistory.takeLast(10).joinToString(separator = "\n") { msg ->
                    if (msg.isUser) "User: ${msg.text}" else "AI: ${msg.text}"
                }

                // Define default instructions for the AI
                val instructions = "Please analyze the facial features of the human in detail and recommend ways to enhance them. If the user asks, provide skincare routine and product suggestions based on the skin analysis. If you cannot detect a human face in the image, kindly ask the user to send another picture."

                // Build the full prompt by combining context, the user message, and the instructions.
                // If there is no context, it will still send the instructions.
                val fullPromptText = if (contextString.isNotEmpty()) {
                    "$contextString\nUser: $userMessage\nInstructions: $instructions"
                } else {
                    "User: $userMessage\nInstructions: $instructions"
                }

                // Create the prompt using the content block. If an image is provided, include it.
                val prompt = content {
                    promptBitmap?.let { image(it) }
                    text(fullPromptText)
                }

                val responseBuilder = StringBuilder()
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    chunk.text?.let {
                        responseBuilder.append(it)
                        // Optionally update UI with streaming response
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
                // Update local conversation history as well
                conversationHistory.clear()
                conversationHistory.addAll(messages)
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
