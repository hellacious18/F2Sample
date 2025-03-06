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
import com.google.firebase.firestore.SetOptions
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Calendar

class AboutMeActivity : AppCompatActivity() {

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

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_me)

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

        googleSignInClient = GoogleSignIn.getClient(
            applicationContext,
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )

        loadUserData()

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

        btnSave.setOnClickListener {
            saveUserDetails()
        }
    }

    private fun loadUserData() {
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(applicationContext)
        val user = auth.currentUser

        if (account != null && user != null) {
            name.text = account.displayName ?: "No Name"
            email.text = account.email ?: "No Email"

            Glide.with(this)
                .load(account.photoUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .circleCrop()
                .into(profileImage)

            val emailKey = user.email?.replace(".", "_") ?: "No_Email"
            val userDocRef = firestore.collection("users").document(emailKey)

            userDocRef.collection("basicDetails").document("info").get().addOnSuccessListener { document ->
                if (document.exists()) {
                    dateOfBirth.text = document.getString("birthdate") ?: ""
                    editTextHeight.setText(document.getString("height") ?: "")
                    editTextWeight.setText(document.getString("weight") ?: "")
                    spinnerGender.setSelection(getSpinnerIndex(spinnerGender, document.getString("gender") ?: ""))
                    spinnerHeights.setSelection(getSpinnerIndex(spinnerHeights, document.getString("heightUnit") ?: ""))
                    spinnerWeight.setSelection(getSpinnerIndex(spinnerWeight, document.getString("weightUnit") ?: ""))
                    Log.d("AboutMe", "User basic details loaded successfully!")
                }
            }
        } else {
            Toast.makeText(applicationContext, "No Google Account Found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserDetails() {
        val user = auth.currentUser
        if (user != null) {
            val emailKey = user.email?.replace(".", "_") ?: "No_Email"

            val userDetails = hashMapOf(
                "name" to name.text.toString(),
                "email" to email.text.toString()
            )

            val basicDetails = hashMapOf(
                "birthdate" to dateOfBirth.text.toString(),
                "gender" to spinnerGender.selectedItem.toString(),
                "height" to editTextHeight.text.toString(),
                "heightUnit" to spinnerHeights.selectedItem.toString(),
                "weight" to editTextWeight.text.toString(),
                "weightUnit" to spinnerWeight.selectedItem.toString()
            )

            // Save user info document
            firestore.collection("users").document(emailKey)
                .set(mapOf("info" to userDetails), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("AboutMe", "User info saved successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("AboutMe", "Error saving user info: ${e.message}")
                }

            // Save basic details document
            firestore.collection("users").document(emailKey)
                .set(mapOf("basicDetails" to basicDetails), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("AboutMe", "User basic details saved successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("AboutMe", "Error saving user basic details: ${e.message}")
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getSpinnerIndex(spinner: Spinner, value: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString() == value) {
                return i
            }
        }
        return 0
    }
}
