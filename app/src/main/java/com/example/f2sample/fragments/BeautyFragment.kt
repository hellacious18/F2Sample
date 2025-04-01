package com.example.f2sample.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

class BeautyFragment : Fragment(R.layout.fragment_beauty) {

    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var uploadButton: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var progressBar: ProgressBar
    private var beautyFragmentOverlay: FrameLayout? = null
    private var upgradeButton: Button? = null

    private val PICK_IMAGE_REQUEST = 1
    private var imageBitmap: Bitmap? = null

    // Maintain local conversation history for context memory.
    private val conversationHistory = mutableListOf<Message>()
    private var uploadedImageUrl: String? = null


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
        beautyFragmentOverlay = view.findViewById(R.id.beautyFragmentOverlay)
        upgradeButton = view.findViewById(R.id.upgradeButton)
        progressBar = view.findViewById(R.id.progressBar)

        chatAdapter = ChatAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener { sendMessage() }
        uploadButton.setOnClickListener { pickImageFromGallery() }

        upgradeButton?.setOnClickListener {
            // Open premium upgrade page
            val intent = Intent(requireContext(), PaymentActivity::class.java)
            startActivity(intent)
        }


        listenForMessages()
        getBeautyChatsCount()
        return view
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data ?: return

            val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
            imageView.setImageBitmap(bitmap)
            imageBitmap = bitmap

            // Show progress bar
            progressBar.visibility = View.VISIBLE
            imageView.visibility = View.VISIBLE
            imageView.alpha = 0.5f

            // Upload image immediately
            uploadImageToFirebaseStorage(imageUri) { uploadedUrl ->
                uploadedImageUrl = uploadedUrl // Store uploaded URL
                progressBar.visibility = View.GONE // Hide progress bar
                imageView.alpha = 1.0f // Restore image visibility
            }
        }
    }

    private fun sendMessage() {
        val userMessage = inputField.text.toString().trim()

        // If no text and no uploaded image, return
        if (userMessage.isEmpty() && uploadedImageUrl == null) return

        // Clear input field
        inputField.text.clear()
        imageView.visibility = View.GONE

        val message = Message(
            text = userMessage.ifEmpty { "Analyze image" },
            imageUrl = uploadedImageUrl,
            isUser = true
        )

        saveMessageToFirestore(message)
        conversationHistory.add(message)

        // Get AI response
        getAIResponse(userMessage.ifEmpty { "Analyze image" }, imageBitmap)

        // Reset image data after sending
        imageBitmap = null
        uploadedImageUrl = null
        imageView.setImageBitmap(null)
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri, onUploadComplete: (String?) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    onUploadComplete(uri.toString()) // Store uploaded URL
                }
            }
            .addOnFailureListener {
                onUploadComplete(null) // Handle failure
            }
    }



    private fun getAIResponse(userMessage: String, promptBitmap: Bitmap?) {
        lifecycleScope.launch {
            try {
                // Build a context string from recent messages (if available)
                val contextString = conversationHistory.takeLast(10).joinToString(separator = "\n") { msg ->
                    if (msg.isUser) "User: ${msg.text}" else "${msg.text}"
                }

                val instructions = "Analyze the provided image for facial features and offer brief beauty and skincare suggestions. Provide detailed recommendations only when requested. If no face is detected, inform the user once without repeatedly asking for another image. Keep the conversation focused on beauty, facial analysis, and skincare."

                val fullPromptText = buildString {
                    if (contextString.isNotEmpty()) {
                        append("$contextString\n")
                    }
                    append("User: $userMessage\n")
                    append("AI Instructions: $instructions")
                }

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
        requireActivity().runOnUiThread {
            progressBar.visibility = View.VISIBLE
            imageView.alpha = 0.5f
        }
        val storageRef = storage.reference.child("images/${System.currentTimeMillis()}.jpg")
        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    progressBar.visibility = View.GONE
                    callback(uri.toString())
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                e.printStackTrace()
            }
    }

    private fun saveMessageToFirestore(message: Message) {
        db.collection("users")
            .document(userId)
            .collection("beauty_chats")
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
            .collection("beauty_chats")
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
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun getBeautyChatsCount() {
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { userSnapshot ->
            val info = userSnapshot.get("info") as? Map<*, *>
            val subscription = info?.get("subscription") as? String ?: "Free Plan (Default)"

            userRef.collection("beauty_chats").get()
                .addOnSuccessListener { snapshot ->
                    val count = snapshot.size()

                    requireActivity().runOnUiThread {
                        if (subscription.lowercase() == "Free Plan (Default)"&& count > 20) {
                            beautyFragmentOverlay?.visibility = View.VISIBLE
                        } else {
                            beautyFragmentOverlay?.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener { e -> println("Error fetching beauty_chats: ${e.message}") }
        }.addOnFailureListener { e -> println("Error fetching user data: ${e.message}") }
    }


}
