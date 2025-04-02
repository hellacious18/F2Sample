package com.example.f2sample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AboutMeActivity : AppCompatActivity() {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private lateinit var ageChipGroup: ChipGroup
    private lateinit var genderChipGroup: ChipGroup
    private lateinit var bodySizeChipGroup: ChipGroup
    private lateinit var skinToneChipGroup: ChipGroup
    private lateinit var styleChipGroup: ChipGroup
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_me)

        // Initialize views
        ageChipGroup = findViewById(R.id.ageChipGroup)
        genderChipGroup = findViewById(R.id.genderChipGroup)
        bodySizeChipGroup = findViewById(R.id.bodySizeChipGroup)
        skinToneChipGroup = findViewById(R.id.skinToneChipGroup)
        styleChipGroup = findViewById(R.id.styleChipGroup)
        saveButton = findViewById(R.id.saveButton)

        loadExistingData()

        saveButton.setOnClickListener {
            saveUserDetails()
        }

    }

    private fun loadExistingData() {
        val user = auth.currentUser
        if (user != null) {
            val emailKey = user.email ?: "No_Email"

            firestore.collection("users").document(emailKey)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userProfile = document.get("userProfile") as? Map<String, Any>
                        if (userProfile != null) {
                            // Set the chips based on the loaded data
                            setSelectedChip(ageChipGroup, userProfile["age"] as? String)
                            setSelectedChip(genderChipGroup, userProfile["gender"] as? String)
                            setSelectedChip(bodySizeChipGroup, userProfile["bodySize"] as? String)
                            setSelectedChip(skinToneChipGroup, userProfile["skinTone"] as? String)
                            setSelectedChips(styleChipGroup, userProfile["styles"] as? List<String>)
                        } else {
                            Log.d("AboutMeActivity", "No userProfile data found")
                        }
                    } else {
                        Log.d("AboutMeActivity", "Document does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AboutMeActivity", "Error getting document: ${e.message}")
                }
        }
    }

    private fun setSelectedChip(chipGroup: ChipGroup, value: String?) {
        if (value != null) {
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                if (chip.text.toString() == value) {
                    chip.isChecked = true
                    break
                }
            }
        }
    }

    private fun setSelectedChips(chipGroup: ChipGroup, values: List<String>?) {
        if (values != null) {
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as Chip
                if (values.contains(chip.text.toString())) {
                    chip.isChecked = true
                }
            }
        }
    }

    private fun saveUserDetails() {
        val user = auth.currentUser
        if (user != null) {
            val emailKey = user.email ?: "No_Email"

            val age = getSelectedChipText(ageChipGroup)
            val gender = getSelectedChipText(genderChipGroup)
            val bodySize = getSelectedChipText(bodySizeChipGroup)
            val skinTone = getSelectedChipText(skinToneChipGroup)
            val styles = getSelectedChipTexts(styleChipGroup)  // Get multiple selected styles

            val userProfile = hashMapOf(  // Changed variable name
                "age" to age,
                "gender" to gender,
                "bodySize" to bodySize,
                "skinTone" to skinTone,
                "styles" to styles
            )

            firestore.collection("users").document(emailKey)
                .set(mapOf("userProfile" to userProfile), SetOptions.merge())  // Changed field name
                .addOnSuccessListener {
                    Log.d("AboutMe", "User details saved successfully under userProfile!")
                    Toast.makeText(this, "Details saved!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                }
                .addOnFailureListener { e ->
                    Log.e("AboutMe", "Error saving user details under userProfile: ${e.message}")
                    Toast.makeText(this, "Error saving details.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedChipText(chipGroup: ChipGroup): String {
        val checkedChipId = chipGroup.checkedChipId
        return if (checkedChipId != View.NO_ID) {
            val chip = chipGroup.findViewById<Chip>(checkedChipId)
            chip.text.toString()
        } else {
            ""
        }
    }

    private fun getSelectedChipTexts(chipGroup: ChipGroup): List<String> {
        val selectedChips = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            if (chip.isChecked) {
                selectedChips.add(chip.text.toString())
            }
        }
        return selectedChips
    }
}