package com.example.f2sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.f2sample.adapter.SubscriptionAdapter
import com.example.f2sample.data.Subscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class PaymentActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedSubscription: Subscription? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment)

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewSubscriptions)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val subscriptions = listOf(
            Subscription("Free Plan (Default)", "₹0", "• Limited AI-generated recommendations\n• Ads displayed"),
            Subscription("Basic Plan (Low-cost)", "₹99/month", "• AI-generated recommendations with more details\n• Ad-free experience"),
            Subscription("Pro Plan (Mid-tier)", "₹199/month", "• Full AI-powered beauty & fashion analysis\n• Priority access to new features"),
            Subscription("Elite Plan (High-tier)", "₹299/month", "• All Pro features + advanced AI insights\n• One-on-one AI styling consultation")
        )

        val adapter = SubscriptionAdapter(subscriptions) { selectedSubscription ->
            this.selectedSubscription = selectedSubscription
            handleSubscriptionSelection(selectedSubscription)
        }

        recyclerView.adapter = adapter
    }

    private fun handleSubscriptionSelection(subscription: Subscription) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (subscription.name == "Free Plan (Default)") {
            updateSubscription(user.email!!, "Free Plan (Default)")
        } else {
            startPayment(subscription)
        }
    }

    private fun startPayment(subscription: Subscription) {
        val checkout = Checkout()
        checkout.setKeyID("RAZORPAY_KEY_ID")

        val user = auth.currentUser
        val email = user?.email ?: "test@example.com"
        val phoneNumber = user?.phoneNumber ?: "9876543210"
        val displayName = user?.displayName ?: "User1"

        try {
            val options = JSONObject()
            options.put("name", displayName)
            options.put("description", subscription.name)
            options.put("currency", "INR")
            options.put("amount", subscription.price.replace("₹", "").replace("/month", "").toInt() * 100)
            options.put("prefill.email", email)
            options.put("prefill.contact", phoneNumber)

            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("Razorpay", "Error: ${e.localizedMessage}")
            Toast.makeText(this, "Error in Payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Update Firestore with paid subscription
        selectedSubscription?.let { subscription ->
            updateSubscription(user.email!!, subscription.name)

            val paymentData = hashMapOf(
                "paymentId" to razorpayPaymentID,
                "subscription" to subscription.name,
                "amount" to subscription.price.replace("₹", "").replace("/month", "").toInt(),
                "currency" to "INR",
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("users")
                .document(user.email!!)
                .collection("payment history")
                .add(paymentData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Payment & Subscription Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error saving payment: ${e.localizedMessage}")
                    Toast.makeText(this, "Failed to save payment", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateSubscription(email: String, subscription: String) {
        val userRef = db.collection("users").document(email)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                userRef.update("info.subscription", subscription)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Subscription Updated: $subscription", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error updating subscription: ${e.localizedMessage}")
                    }
            } else {
                val userData = hashMapOf("info" to hashMapOf("subscription" to subscription))
                userRef.set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Subscription Initialized: $subscription", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error initializing subscription: ${e.localizedMessage}")
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Error fetching user: ${e.localizedMessage}")
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_SHORT).show()
    }
}
