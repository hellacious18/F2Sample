package com.example.f2sample.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.adapter.ChatAdapter
import com.example.f2sample.data.Message
import com.example.f2sample.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FashionFragment : Fragment(R.layout.fragment_fashion) {

    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-1.5-flash-001")
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var imageButton: ImageButton

    private val messages = mutableListOf<Message>()

    private val PICK_IMAGE_REQUEST = 1

    private val maxMessages = 5
    private val chatContext = StringBuilder()

    private lateinit var userId: String
    private lateinit var chatId: String
    private var chatListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        userId = currentUser?.email?.replace(".", "_") ?: "guest"


//        val currentTime = System.currentTimeMillis()
//        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
//        val formattedTime = dateFormat.format(Date(currentTime))
        // Generate the chatId with the formatted time (hour & minute)

        chatId = "${userId}_fashionChat"

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        inputMessage = view.findViewById(R.id.inputMessage)
        sendButton = view.findViewById(R.id.sendButton)
        imageButton = view.findViewById(R.id.imageButton)

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(context)
        chatRecyclerView.adapter = chatAdapter

        // Listen for new messages in real-time
        startChatListener()

        imageButton.setOnClickListener{
            openImagePicker()
        }

        sendButton.setOnClickListener {
            val userInput = inputMessage.text.toString().trim()
            if (userInput.isNotEmpty()) {
                sendMessage(userInput)
                inputMessage.text.clear()
            }
        }

    }
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun startChatListener() {
        // Real-time listener for Firestore updates
        chatListener = firestore.collection("users").document(userId)
            .collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FashionFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    messages.clear() // Clear existing messages before adding updated ones
                    for (doc in snapshot.documents) {
                        val message = doc.getString("message") ?: ""
                        val isUser = doc.getBoolean("isUser") ?: false
                        messages.add(Message(message, isUser))

                        // Update chat context dynamically
                        if (isUser) {
                            chatContext.append("User: $message\n")
                        } else {
                            chatContext.append("AI: $message\n")
                        }
                    }
                    // Ensure the message list is not too large
                    if (messages.size > maxMessages) {
                        messages.removeAt(0)
                    }

                    chatAdapter.notifyDataSetChanged()
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun saveMessageToFirestore(message: String, isUser: Boolean, imageUrl: String? = null) {
        val chatMessage = hashMapOf(
            "message" to message,
            "isUser" to isUser,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users").document(userId)
            .collection("chats").document(chatId)
            .collection("messages")
            .add(chatMessage)
            .addOnSuccessListener {
                Log.d("FashionFragment", "Message saved successfully!")
            }
            .addOnFailureListener { e ->
                Log.e("FashionFragment", "Error saving message: ${e.message}")
            }
    }

    private fun sendMessage(prompt: String) {
        messages.add(Message(prompt, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
        saveMessageToFirestore(prompt, true)

        // Update chat context with the new user message
        chatContext.append("User: $prompt\n")

        // Ensure the context size does not exceed the limit
        if (messages.size > maxMessages) {
            messages.removeAt(0)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val aiPrompt = getLimitedContext() + "\nAI:"
                val response = generativeModel.generateContent(aiPrompt)
                val aiResponse = response.text ?: "No response from AI"
                Log.d("FashionFragment", "AI Response: $aiResponse")

                launch(Dispatchers.Main) {
                    messages.add(Message(aiResponse, isUser = false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                    saveMessageToFirestore(aiResponse, false)

                    chatContext.append("AI: $aiResponse\n")

                    // Maintain context size limit
                    if (messages.size > maxMessages) {
                        messages.removeAt(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FashionFragment", "Error: ${e.message}")
            }
        }
    }

    private fun getLimitedContext(): String {
        // Get the most recent 'maxMessages' messages
        return messages.takeLast(maxMessages).joinToString("\n") {
            "${if (it.isUser) "User" else "AI"}: ${it.text}"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri = data.data!!

            val storageReference = FirebaseStorage.getInstance().reference
            val imageRef = storageReference.child("chat_images/${System.currentTimeMillis()}.jpg")

            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Get the image URL and send it as part of the message
                        val imageUrl = uri.toString()
                        sendMessageWithImage(imageUrl)
                    }
                }
                .addOnFailureListener {
                    Log.e("FashionFragment", "Error uploading image: ${it.message}")
                }
        }
    }

    private fun sendMessageWithImage(imageUrl: String) {
        val prompt = "Check out this image!"
        val message = Message(prompt, isUser = true, imageUrl = imageUrl)

        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
        saveMessageToFirestore(prompt, true, imageUrl)

        chatContext.append("User: $prompt\n")

        // Ensure the context size does not exceed the limit
        if (messages.size > maxMessages) {
            messages.removeAt(0)
        }

        // Generate AI response
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val aiPrompt = getLimitedContext() + "\nAI:"
                val response = generativeModel.generateContent(aiPrompt)
                val aiResponse = response.text ?: "No response from AI"
                Log.d("FashionFragment", "AI Response: $aiResponse")

                launch(Dispatchers.Main) {
                    messages.add(Message(aiResponse, isUser = false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                    saveMessageToFirestore(aiResponse, false)

                    chatContext.append("AI: $aiResponse\n")

                    // Maintain context size limit
                    if (messages.size > maxMessages) {
                        messages.removeAt(0)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FashionFragment", "Error: ${e.message}")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        chatListener?.remove()
    }

}

