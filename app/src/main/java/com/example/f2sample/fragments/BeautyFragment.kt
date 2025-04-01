package com.example.f2sample.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
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
import kotlinx.coroutines.tasks.await
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

    private var imageBitmap: Bitmap? = null
    private var uploadedImageUrl: String? = null

    // Maintain local conversation history for context memory.
    private val conversationHistory = mutableListOf<Message>()

    // Firebase services
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    // Using gemini-2.0-flash model; adjust as needed.
    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-2.0-flash")
    // Save chats using the current user's email (or default)
    private val userId = FirebaseAuth.getInstance().currentUser?.email ?: "guest@example.com"

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

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

        // Initialize ActivityResultLauncher
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                imageUri?.let { uri ->
                    handleImageSelection(uri)
                }
            }
        }

        listenForMessages()
        getBeautyChatsCount()
        return view
    }


    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }


    private fun handleImageSelection(imageUri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                imageBitmap = bitmap
                imageView.setImageBitmap(bitmap)
                imageView.isVisible = true
                imageView.alpha = 0.5f
                progressBar.isVisible = true

                uploadImageToFirebaseStorage(imageUri) { uploadedUrl ->
                    uploadedImageUrl = uploadedUrl
                    progressBar.isVisible = false
                    imageView.alpha = 1.0f
                    Log.d("BeautyFragment", "Uploaded Image URL: $uploadedImageUrl")
                }
            } catch (e: Exception) {
                Log.e("BeautyFragment", "Error handling image selection: ${e.message}", e)
                Toast.makeText(requireContext(), "Error selecting image", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun sendMessage() {
        val userMessage = inputField.text.toString().trim()

        if (userMessage.isEmpty() && uploadedImageUrl == null) return

        inputField.text.clear()
        imageView.isVisible = false

        val message = Message(
            text = userMessage.ifEmpty { "Analyze image" },
            imageUrl = uploadedImageUrl,
            isUser = true
        )

        saveMessageToFirestore(message)
        conversationHistory.add(message)
        updateChatUI(conversationHistory)

        getAIResponse(userMessage.ifEmpty { "Analyze image" }, imageBitmap)

        imageBitmap = null
        uploadedImageUrl = null
        imageView.setImageBitmap(null)
    }


    private fun uploadImageToFirebaseStorage(imageUri: Uri, onUploadComplete: (String?) -> Unit) {
        lifecycleScope.launch {
            try {
                val storageRef = storage.reference.child("images/${UUID.randomUUID()}.jpg")
                val uploadTask = storageRef.putFile(imageUri)
                uploadTask.await() // Wait for upload to complete

                val uri = storageRef.downloadUrl.await() // Get the download URL
                onUploadComplete(uri.toString())
            } catch (e: Exception) {
                Log.e("BeautyFragment", "Error uploading image: ${e.message}", e)
                onUploadComplete(null)
            }
        }
    }


    private fun getAIResponse(userMessage: String, promptBitmap: Bitmap?) {
        lifecycleScope.launch {
            try {
                val contextString = conversationHistory.takeLast(10).joinToString(separator = "\n") { msg ->
                    if (msg.isUser) "User: ${msg.text}" else "AI: ${msg.text}"
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
                        val streamingMessage = Message(
                            text = responseBuilder.toString(),
                            imageUrl = null,
                            isUser = false
                        )
                        val updatedList = conversationHistory + streamingMessage
                        updateChatUI(updatedList)
                    }
                }

                val aiResponseMessage = Message(
                    text = responseBuilder.toString(),
                    imageUrl = null,
                    isUser = false
                )

                saveMessageToFirestore(aiResponseMessage)
                conversationHistory.add(aiResponseMessage)
                updateChatUI(conversationHistory)

            } catch (e: Exception) {
                Log.e("BeautyFragment", "Error getting AI response: ${e.message}", e)
                val errorMessage = Message(
                    text = "Error: ${e.message}",
                    imageUrl = null,
                    isUser = false
                )
                saveMessageToFirestore(errorMessage)
                conversationHistory.add(errorMessage)
                updateChatUI(conversationHistory)
            }
        }
    }


    private fun saveMessageToFirestore(message: Message) {
        lifecycleScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("beauty_chats")
                    .add(message)
                    .await()
                Log.d("BeautyFragment", "Message saved successfully!")
            } catch (e: Exception) {
                Log.e("BeautyFragment", "Error saving message: ${e.message}", e)
            }
        }
    }


    private fun listenForMessages() {
        db.collection("users")
            .document(userId)
            .collection("beauty_chats")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("BeautyFragment", "Listen failed: ${e.message}", e)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()

                conversationHistory.clear()
                conversationHistory.addAll(messages)
                updateChatUI(messages)
            }
    }


    private fun updateChatUI(messages: List<Message>) {
        if (isAdded) {
            requireActivity().runOnUiThread {
                chatAdapter = ChatAdapter(messages)
                recyclerView.layoutManager = LinearLayoutManager(context)
                recyclerView.adapter = chatAdapter
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun getBeautyChatsCount() {
        val userRef = db.collection("users").document(userId)

        var subscription: String? = null

        userRef.get().addOnSuccessListener { userSnapshot ->
            val info = userSnapshot.get("info") as? Map<*, *>
            subscription = info?.get("subscription") as? String ?: "Free Plan (Default)"

            userRef.collection("beauty_chats").get()
                .addOnSuccessListener { snapshot ->
                    val count = snapshot.size()

                    if (isAdded) { // Check if Fragment is attached
                        requireActivity().runOnUiThread {
                            beautyFragmentOverlay?.isVisible =
                                subscription?.lowercase() == "Free Plan (Default)" && count > 20
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BeautyFragment", "Error fetching beauty_chats: ${e.message}", e)
                }
        }.addOnFailureListener { e ->
            Log.e("BeautyFragment", "Error fetching user data: ${e.message}", e)
        }
    }

}