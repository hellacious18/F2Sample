package com.example.f2sample.fragments

import android.R.attr.text
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.collect
import android.graphics.BitmapFactory
import com.google.firebase.vertexai.type.content
import org.commonmark.internal.Bracket.image

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
    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-2.0-flash")
    private val userId = "user1"

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
        val userMessage = inputField.text.toString().trim()
//        if (userMessage.isEmpty() && imageBitmap == null) return

        val message = Message(
            text = userMessage,
            isUser = true
        )
        saveMessageToFirestore(message)
        inputField.text.clear()

        lifecycleScope.launch {
            try {
                val prompt = content {
                    imageBitmap?.let { image(it) }
                    text(userMessage)
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

                imageBitmap = null
                imageView.setImageBitmap(null)

            } catch (e: Exception) {
                e.printStackTrace()
            }
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
