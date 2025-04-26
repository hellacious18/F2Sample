package com.example.f2sample.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
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
import java.util.UUID

class FashionFragment : Fragment(R.layout.fragment_fashion) {

    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var uploadButton: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var progressBar: ProgressBar
    private var fashionFragmentOverlay: FrameLayout? = null
    private var upgradeButton: Button? = null
    private lateinit var backgroundImageView: ImageView
    private lateinit var verticalMenu: ImageView // Declare the ImageView

    private var imageBitmap: Bitmap? = null
    private var uploadedImageUrl: String? = null

    // User profile context fields
    private var profAge = ""
    private var profGender = ""
    private var profBodySize = ""
    private var profSkinTone = ""
    private var profStyles = ""

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

    private val imageResources = intArrayOf( // Add this array
        R.drawable.fashion1,
        R.drawable.fashion2,
        R.drawable.fashion3,
        R.drawable.fashion4,
        R.drawable.fashion5,
        R.drawable.fashion6
    )
    private var currentImageIndex = 0 // Add this variable
    private val handler = Handler() // Add this Handler
    private val interval: Long = 3000 // 3 seconds // Add this variable

    private val imageSwitcherRunnable = object : Runnable { // Add this Runnable
        override fun run() {
            // Fade out animation
            backgroundImageView.animate().alpha(0f).setDuration(1000).withEndAction {
                // Change image
                currentImageIndex = (currentImageIndex + 1) % imageResources.size
                backgroundImageView.setImageResource(imageResources[currentImageIndex])

                // Fade in animation
                backgroundImageView.animate().alpha(0.3f).setDuration(1000).start()
            }.start()
            handler.postDelayed(this, interval)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_fashion, container, false)

        inputField = view.findViewById(R.id.inputField)
        sendButton = view.findViewById(R.id.sendButton)
        uploadButton = view.findViewById(R.id.uploadButton)
        imageView = view.findViewById(R.id.imageView)
        recyclerView = view.findViewById(R.id.recylerViewFashionFragment)
        fashionFragmentOverlay = view.findViewById(R.id.fashionFragmentOverlay)
        upgradeButton = view.findViewById(R.id.upgradeButton)
        progressBar = view.findViewById(R.id.progressBar)
        backgroundImageView = view.findViewById(R.id.backgroundImageView) // Initialize here
        verticalMenu = view.findViewById(R.id.verticalMenu) // Initialize the verticalMenu

        chatAdapter = ChatAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener { sendMessage() }
        uploadButton.setOnClickListener { pickImageFromGallery() }

        upgradeButton?.setOnClickListener {
            // Open premium upgrade page
            val intent = Intent(requireContext(), PaymentActivity::class.java)
            startActivity(intent)
        }
        verticalMenu.setOnClickListener { showPopupMenu(verticalMenu) } // Setup click listener


        // Initialize ActivityResultLauncher
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                imageUri?.let { uri ->
                    handleImageSelection(uri)
                }
            }
        }

        loadProfileContext()

        listenForMessages()
        getFashionChatsCount()

        backgroundImageView.setImageResource(imageResources[0]) // Set initial image
        backgroundImageView.alpha = 0.3f // Initial alpha value
        handler.post(imageSwitcherRunnable) // Start the image switching

        return view
    }
    private fun loadProfileContext() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val profile = doc.get("userProfile") as? Map<*, *> ?: emptyMap<Any,Any>()
                profAge = profile["age"] as? String ?: ""
                profGender = profile["gender"] as? String ?: ""
                profBodySize = profile["bodySize"] as? String ?: ""
                profSkinTone = profile["skinTone"] as? String ?: ""
                profStyles = (profile["styles"] as? List<*>)?.joinToString(", ") ?: ""

                sendIntroMessage()
            }
            .addOnFailureListener { e ->
                Log.e("FashionFragment", "Error loading profile: ${e.message}")
                sendIntroMessage()
            }
    }

    private fun sendIntroMessage() {
        val name = FirebaseAuth.getInstance().currentUser?.displayName ?: "there"
        val intro = buildString {
            append("✨ Hello, $name! ✨\n\n")

            var hasDetails = false

            if (profBodySize.isNotEmpty()) {
                append("Body size: $profBodySize. ")
                hasDetails = true
            }
            if (profStyles.isNotEmpty()) {
                append("Style mood: $profStyles. ")
                hasDetails = true
            }

            if (!hasDetails) {
                append("👗 Your fashion profile is still waiting for your input. Please fill it out in the *Profile Section* to get your personalized outfit recommendations. ✨\n\n")
            }

            append("\n💃 Just a heads-up: I’m here for full-body analysis and the latest outfit trends. For beauty tips and face glam, head over to the *Beauty Chat*.\n\n")
            append("Ready to serve looks? Let’s curate an outfit that speaks your style. 🌟👠")
        }

        val introMsg = Message(text = intro, imageUrl = null, isUser = false)
        conversationHistory.add(introMsg)
        chatAdapter.addMessage(introMsg)
        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
    }


    override fun onDestroyView() { // Override onDestroyView
        super.onDestroyView()
        handler.removeCallbacks(imageSwitcherRunnable) // Stop the animation
    }


    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun showPopupMenu(view: View) { // Add popup menu
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.vertical_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete_post -> {
                    // Delete the chat
                    deleteAllMessages()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
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
                    Log.d("FashionFragment", "Uploaded Image URL: $uploadedImageUrl")
                }
            } catch (e: Exception) {
                Log.e("FashionFragment", "Error handling image selection: ${e.message}", e)
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
            text = userMessage.ifEmpty { "Analyze my outfit" },
            imageUrl = uploadedImageUrl,
            isUser = true
        )

        saveMessageToFirestore(message)
        conversationHistory.add(message)

        chatAdapter.addMessage(message)
        recyclerView.scrollToPosition(chatAdapter.itemCount - 1)

        updateChatUI(conversationHistory)

        getAIResponse(userMessage.ifEmpty { "Analyze my outfit" }, imageBitmap)

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
                Log.e("FashionFragment", "Error uploading image: ${e.message}", e)
                onUploadComplete(null)
            }
        }
    }

    private fun getAIResponse(userMessage: String, promptBitmap: Bitmap?) {
        lifecycleScope.launch {
            try {

                val profileContext = buildString {
                    if (profAge.isNotEmpty()) append("Age: $profAge; ")
                    if (profGender.isNotEmpty()) append("Gender: $profGender; ")
                    if (profBodySize.isNotEmpty()) append("Body size: $profBodySize; ")
                    if (profSkinTone.isNotEmpty()) append("Skin tone: $profSkinTone; ")
                    if (profStyles.isNotEmpty()) append("Styles: $profStyles; ")
                }

                val historyContext = conversationHistory.takeLast(10).joinToString("\n") { msg ->
                    if (msg.isUser) "User: ${msg.text}" else "AI: ${msg.text}"
                }

                val instructions = "Analyze the provided image for full-body features and offer stylish outfit recommendations. Provide detailed fashion suggestions only when explicitly requested by the user. If no clear full-body image is detected, inform the user once without repeatedly asking for another image. Keep the conversation focused on fashion, outfit styling, and body shape analysis."

                val fullPromptText = buildString {
                    append(profileContext).append("\n")
                    append(historyContext).append("\n")
                    append("User: $userMessage\n")
                    append("AI Instructions: $instructions")
                }

                val prompt = content {
                    promptBitmap?.let { image(it) }
                    text(fullPromptText)
                }

                val responseBuilder = StringBuilder()

                // Create a placeholder message and add to the chat (only once)
                var streamingMessage: Message? = null
                if (conversationHistory.isEmpty() || conversationHistory.last().text!!.isEmpty()) {
                    streamingMessage = Message(
                        text = "",  // Empty message initially
                        imageUrl = null,
                        isUser = false
                    )
                    conversationHistory.add(streamingMessage)
                    chatAdapter.addMessage(streamingMessage)
                    recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }

                // Streaming content handling
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    chunk.text?.let {
                        responseBuilder.append(it)
                        // Only update the last message (streamingMessage) with the new content
                        streamingMessage?.let { msg ->
                            chatAdapter.updateLastMessageText(responseBuilder.toString())
                            recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                        }
                    }
                }

                // Final AI response (only add once, without "AI:" prefix)
                val aiResponseMessage = Message(
                    text = responseBuilder.toString(),  // No "AI:" prefix here
                    imageUrl = null,
                    isUser = false
                )

                saveMessageToFirestore(aiResponseMessage)
                conversationHistory.add(aiResponseMessage)
                chatAdapter.addMessage(aiResponseMessage)
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)

            } catch (e: Exception) {
                Log.e("FashionFragment", "Error getting AI response: ${e.message}", e)
                val errorMessage = Message(
                    text = "Error: ${e.message}",
                    imageUrl = null,
                    isUser = false
                )
                saveMessageToFirestore(errorMessage)
                conversationHistory.add(errorMessage)
                chatAdapter.addMessage(errorMessage)  // Add error message
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun deleteAllMessages() {
        lifecycleScope.launch {
            try {
                val batch = db.batch()
                val messagesRef = db.collection("users")
                    .document(userId)
                    .collection("fashion_chats")

                messagesRef.get().await().documents.forEach { document ->
                    batch.delete(document.reference)
                }
                batch.commit().await()

                conversationHistory.clear()
                updateChatUI(emptyList())

                Log.d("FashionFragment", "All messages deleted successfully!")
            } catch (e: Exception) {
                Log.e("FashionFragment", "Error deleting messages: ${e.message}", e)
                Toast.makeText(requireContext(), "Error deleting messages", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveMessageToFirestore(message: Message) {
        lifecycleScope.launch {
            try {
                db.collection("users")
                    .document(userId)
                    .collection("fashion_chats")
                    .add(message)
                    .await()
                Log.d("FashionFragment", "Message saved successfully!")
            } catch (e: Exception) {
                Log.e("FashionFragment", "Error saving message: ${e.message}", e)
            }
        }
    }

    private fun listenForMessages() {
        db.collection("users")
            .document(userId)
            .collection("fashion_chats")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FashionFragment", "Listen failed: ${e.message}", e)
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
                // Directly update the adapter's message list and notify the changes
                val diff = messages.size - chatAdapter.itemCount

                // If there are new messages, add them to the adapter
                if (diff > 0) {
                    chatAdapter.messages.addAll(messages.subList(chatAdapter.itemCount, messages.size))
                    chatAdapter.notifyItemRangeInserted(chatAdapter.itemCount, diff)
                }

                // Scroll to the latest message
                recyclerView.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun getFashionChatsCount() {
        val userRef = db.collection("users").document(userId)

        var subscription: String? = null

        userRef.get().addOnSuccessListener { userSnapshot ->
            val info = userSnapshot.get("info") as? Map<*, *>
            subscription = info?.get("subscription") as? String ?: "Free Plan (Default)"

            userRef.collection("fashion_chats").get()
                .addOnSuccessListener { snapshot ->
                    val count = snapshot.size()

                    if (isAdded) { // Check if Fragment is attached
                        requireActivity().runOnUiThread {
                            fashionFragmentOverlay?.isVisible =
                                subscription?.lowercase() == "free plan (default)" && count > 20
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FashionFragment", "Error fetching fashion_chats: ${e.message}", e)
                }
        }.addOnFailureListener { e ->
            Log.e("FashionFragment", "Error fetching user data: ${e.message}", e)
        }
    }
}
