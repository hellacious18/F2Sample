package com.example.f2sample.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.ChatAdapter
import com.example.f2sample.Message
import com.example.f2sample.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.vertexai.vertexAI
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FashionFragment : Fragment(R.layout.fragment_fashion) {

    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-1.5-flash-001")
    private val firestore = FirebaseFirestore.getInstance()
    private val chatCollection = firestore.collection("chats")

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var inputMessage: EditText
    private lateinit var sendButton: Button
    private val messages = mutableListOf<Message>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        inputMessage = view.findViewById(R.id.inputMessage)
        sendButton = view.findViewById(R.id.sendButton)

        chatAdapter = ChatAdapter(messages)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter

        loadChatHistory()

        sendButton.setOnClickListener {
            val userInput = inputMessage.text.toString().trim()
            if (userInput.isNotEmpty()) {
                sendMessage(userInput)
                inputMessage.text.clear()
            }
        }
    }

    private fun loadChatHistory() {
        chatCollection.orderBy("timestamp").get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val message = doc.getString("message") ?: ""
                    val isUser = doc.getBoolean("isUser") ?: false
                    messages.add(Message(message, isUser))
                }
                chatAdapter.notifyDataSetChanged()
                chatRecyclerView.scrollToPosition(messages.size - 1)
            }
            .addOnFailureListener { e ->
                Log.e("FashionFragment", "Failed to load chat history: ${e.message}")
            }
    }

    private fun saveMessageToFirestore(message: String, isUser: Boolean) {
        val chatData = hashMapOf(
            "message" to message,
            "isUser" to isUser,
            "timestamp" to System.currentTimeMillis()
        )
        chatCollection.add(chatData)
    }

    private fun sendMessage(prompt: String) {
        messages.add(Message(prompt, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        chatRecyclerView.scrollToPosition(messages.size - 1)
        saveMessageToFirestore(prompt, true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val aiResponse = response.text ?: "No response from AI"
                Log.d("FashionFragment", "AI Response: $aiResponse")

                launch(Dispatchers.Main) {
                    messages.add(Message(aiResponse, isUser = false))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    chatRecyclerView.scrollToPosition(messages.size - 1)
                    saveMessageToFirestore(aiResponse, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FashionFragment", "Error: ${e.message}")
            }
        }
    }
}

// Now, messages will persist across sessions! 🚀
