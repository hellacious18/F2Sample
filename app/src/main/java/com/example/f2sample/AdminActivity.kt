package com.example.f2sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.adapter.UserAdapter
import com.example.f2sample.data.User
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        recyclerView = findViewById(R.id.rvUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(userList)
        recyclerView.adapter = userAdapter

        fetchUsers()
    }

    private fun fetchUsers() {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                userList.clear() // Avoid duplicates
                for (document in result) {
                    val infoMap = document.get("info") as? Map<String, Any>
                    val detailsMap = document.get("basicDetails") as? Map<String, Any>

                    if (infoMap != null) {
                        val name = infoMap["name"] as? String ?: ""
                        val email = infoMap["email"] as? String ?: ""

                        val birthdate = detailsMap?.get("birthdate") as? String ?: "N/A"
                        val gender = detailsMap?.get("gender") as? String ?: "N/A"
                        val height = detailsMap?.get("height")?.toString() ?: "N/A"
                        val weight = detailsMap?.get("weight")?.toString() ?: "N/A"

                        val user = User(name, email, birthdate, gender, height, weight)
                        userList.add(user)
                    }
                }
                userAdapter.notifyDataSetChanged() // ✅ Corrected
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching users", e)
            }
    }
}
