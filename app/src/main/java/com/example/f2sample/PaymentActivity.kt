package com.example.f2sample

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class PaymentActivity : AppCompatActivity(), PaymentResultListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment)

        // Initialize Razorpay Checkout
        Checkout.preload(applicationContext)

        startPayment() // Start payment process
    }

    private fun startPayment() {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_9PD465v1tKJruT")

        try {
            val options = JSONObject()
            options.put("name", "User1")
            options.put("description", "Test Payment")
            options.put("currency", "INR")
            options.put("amount", "10000") // Amount in paise (10000 = ₹100)
            options.put("prefill.email", "test@example.com")
            options.put("prefill.contact", "9876543210")

            checkout.open(this, options)

        } catch (e: Exception) {
            Log.e("Razorpay", "Error: ${e.localizedMessage}")
            Toast.makeText(this, "Error in Payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        Log.e("razorpayPaymentID", "onPaymentSuccess: $razorpayPaymentID")
        Toast.makeText(this, "Payment Successful: $razorpayPaymentID", Toast.LENGTH_SHORT).show()
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_SHORT).show()
    }
}
