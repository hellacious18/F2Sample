package com.example.f2sample

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.CardParams
import com.stripe.android.model.Token

class PaymentActivity : AppCompatActivity() {

        private lateinit var stripe: Stripe
        private val publishableKey = "pk_test_51R4gG7BFXrlh50H3nLigd1VSxR8Wo1t1d0ZlcTMxmgBnEAJZxofHRxQIpM9ZIpdyNRjSkQJTi2FkI3EBpSfuLqR200GDCmbLqB"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment)

        PaymentConfiguration.init(applicationContext, publishableKey)

        stripe = Stripe(this, publishableKey)
        createTestToken()
    }

    private fun createTestToken() {
        val cardParams = CardParams(
            "4242424242424242", // Test Visa card
            12, 2025, // Expiry Month & Year
            "123" // CVV
        )

        stripe.createCardToken(cardParams, callback = object : ApiResultCallback<Token> {
            override fun onSuccess(result: Token) {
                Log.d("StripeToken", "Token: ${result.id}")
            }

            override fun onError(e: Exception) {
                Log.e("StripeToken", "Error: ${e.localizedMessage}")
            }
        })

    }
}