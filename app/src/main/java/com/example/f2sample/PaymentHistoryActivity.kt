package com.example.f2sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.adapter.PaymentAdapter
import com.example.f2sample.data.Payment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PaymentHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var paymentAdapter: PaymentAdapter
    private val paymentList = mutableListOf<Payment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_history)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        paymentAdapter = PaymentAdapter(paymentList)
        recyclerView.adapter = paymentAdapter

        fetchPaymentHistory()
    }

    private fun fetchPaymentHistory() {
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email

        if (email == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(email)
            .collection("payment history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                paymentList.clear()
                for (document in documents) {
                    val payment = document.toObject(Payment::class.java)
                    paymentList.add(payment)
                }
                paymentAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching payment history: ${e.localizedMessage}")
                Toast.makeText(this, "Failed to load payments", Toast.LENGTH_SHORT).show()
            }
    }
}
