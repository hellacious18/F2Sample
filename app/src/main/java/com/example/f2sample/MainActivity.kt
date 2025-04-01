package com.example.f2sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private lateinit var videoView: VideoView
    private lateinit var welcomeText: TextView
    private lateinit var appDescription: TextView
    private lateinit var btnGoogleSignIn: SignInButton

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            goToHomeActivity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        // Initialize views
        videoView = findViewById(R.id.background_video)
        welcomeText = findViewById(R.id.welcome_text)
        appDescription = findViewById(R.id.app_description)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)


        val videoPath = "android.resource://" + packageName + "/" + R.raw.main
        videoView.setVideoURI(Uri.parse(videoPath))

        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        mediaController.setVisibility(View.GONE)
        videoView.setMediaController(mediaController)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = false

            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            val screenWidth = videoView.width.toFloat()
            val screenHeight = videoView.height.toFloat()

            // Calculate the smaller dimension between screen width and height
            val smallerDimension = minOf(screenWidth, screenHeight)

            // Set the layout parameters to make the VideoView square
            val layoutParams = videoView.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.width = smallerDimension.toInt()
            layoutParams.height = smallerDimension.toInt()

            // Center the VideoView horizontally and vertically
            layoutParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

            videoView.layoutParams = layoutParams

            videoView.start()

        }

        videoView.setOnErrorListener { mp, what, extra ->
            Log.e("VideoError", "Error during video playback: what=$what, extra=$extra")
            return@setOnErrorListener true
        }


        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            goToHomeActivity()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnGoogleSignIn.setOnClickListener {
            signIn()
        }
    }

    private fun goToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign-in failed", e)
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()

                    val name = user?.displayName ?: "No Name"
                    val email = user?.email ?: "No Email"
                    val emailKey = email

                    val firestore = FirebaseFirestore.getInstance()
                    val userDetails = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "subscription" to "free"
                    )

                    firestore.collection("users").document(emailKey)
                        .set(hashMapOf("info" to userDetails))
                        .addOnSuccessListener {
                            Log.d("UserData", "User data saved to Firestore under users -> $emailKey -> info!")
                        }
                        .addOnFailureListener { e ->
                            Log.e("UserData", "Error saving user data: ${e.message}")
                        }

                    goToHomeActivity()
                } else {
                    Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}