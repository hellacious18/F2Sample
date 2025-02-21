package com.example.f2sample.fragments

import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.f2sample.R
import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.vertexAI
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FashionFragment : Fragment(R.layout.fragment_fashion) {

    private lateinit var generativeModel: GenerativeModel
    private lateinit var textViewResult: TextView
    private lateinit var btnGenerate: Button
    private lateinit var prompt: EditText

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewResult = view.findViewById(R.id.responseTextView)
        btnGenerate = view.findViewById(R.id.sendButton)
        prompt = view.findViewById(R.id.prompt)

        // Initialize Vertex AI
        generativeModel = Firebase.vertexAI.generativeModel("gemini-1.5-flash-001")

        btnGenerate.setOnClickListener {
            val userPrompt = prompt.text.toString()
            if (userPrompt.isNotEmpty()) {
                generateFashionAdvice(userPrompt)
            } else {
                textViewResult.text = "Please enter a prompt!"
            }
        }
    }

    private fun generateFashionAdvice(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val result = response.text ?: "No response from AI"
                Log.d("FashionFragment", "Response: $result")

                launch(Dispatchers.Main) {
                    val markwon = Markwon.create(requireContext())
                    markwon.setMarkdown(textViewResult, result) // Apply Markdown Formatting
                }

            } catch (e: Exception) {
                Log.e("FashionFragment", "Error: ${e.message}", e)

                launch(Dispatchers.Main) {
                    textViewResult.text = "Error: ${e.message}"
                }
            }
        }
    }
}
