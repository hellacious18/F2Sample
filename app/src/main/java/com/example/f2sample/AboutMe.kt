package com.example.f2sample

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Calendar

class AboutMe : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var profileImage: CircleImageView
    private lateinit var name: TextView
    private lateinit var email: TextView
    private lateinit var dateOfBirth: TextView
    private lateinit var spinnerGender: Spinner
    private lateinit var editTextHeight: EditText
    private lateinit var spinnerHeights: Spinner
    private lateinit var editTextWeight: EditText
    private lateinit var spinnerWeight: Spinner
    private lateinit var btnSave: Button

    // Firestore and Auth instances
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_me)

        // Initialize UI elements
        profileImage = findViewById(R.id.aboutMeImage)
        name = findViewById(R.id.aboutMeName)
        email = findViewById(R.id.aboutMeEmail)
        dateOfBirth = findViewById(R.id.dateOfBirth)
        spinnerGender = findViewById(R.id.spinnerGender)
        editTextHeight = findViewById(R.id.editTextHeight)
        spinnerHeights = findViewById(R.id.spinnerHeight)
        editTextWeight = findViewById(R.id.editTextWeight)
        spinnerWeight = findViewById(R.id.spinnerWeight)
        btnSave = findViewById(R.id.aboutMeSave)

        // Initialize Google SignIn client
        googleSignInClient = GoogleSignIn.getClient(
            applicationContext,
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )

        loadUserData()

        // Setup DatePicker for birthdate
        dateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                    val formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    dateOfBirth.text = formattedDate
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        // Save details when Save button is clicked
        btnSave.setOnClickListener {
            saveUserDetails()
        }
    }

    private fun loadUserData() {
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(applicationContext)
        if (account != null) {
            name.text = account.displayName ?: "No Name"
            email.text = account.email ?: "No Email"
            Glide.with(this)
                .load(account.photoUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .circleCrop()
                .into(profileImage)
        } else {
            Toast.makeText(applicationContext, "No Google Account Found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserDetails() {
        val user = auth.currentUser
        if (user != null) {
            // Retrieve values from UI
            val gender = spinnerGender.selectedItem.toString()
            val birthdate = dateOfBirth.text.toString()
            val height = editTextHeight.text.toString()
            val heightUnit = spinnerHeights.selectedItem.toString()
            val weight = editTextWeight.text.toString()
            val weightUnit = spinnerWeight.selectedItem.toString()

            // Create data map
            val userDetails = hashMapOf(
                "name" to name.text.toString(),
                "email" to email.text.toString(),
                "birthdate" to birthdate,
                "gender" to gender,
                "height" to height,
                "heightUnit" to heightUnit,
                "weight" to weight,
                "weightUnit" to weightUnit
            )

            firestore.collection("users").document(user.uid)
                .set(userDetails)
                .addOnSuccessListener {
                    Toast.makeText(this, "User details saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error saving details: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("AboutMe", "Error saving user details", e)
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }
}
