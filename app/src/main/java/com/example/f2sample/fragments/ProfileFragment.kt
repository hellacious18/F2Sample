package com.example.f2sample.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.f2sample.AboutMeActivity
import com.example.f2sample.MainActivity
import com.example.f2sample.PaymentActivity
import com.example.f2sample.PaymentHistoryActivity
import com.example.f2sample.adapter.ProfileAdapter
import com.example.f2sample.R
import com.example.f2sample.data.ProfileItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var profileImage: CircleImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: ProfileAdapter

    private lateinit var googleSignInClient: GoogleSignInClient

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImage = view.findViewById(R.id.profile_image)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        listView = view.findViewById(R.id.profile_list)

        googleSignInClient = GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)

        loadUserData() // Load profile information
        loadAndDisplayUserProfileData() // Load and display preferences from Firestore

        val items = mutableListOf(  // Make the list mutable
            // **Personal Details**
            ProfileItem(null, "Body Profile"),
            ProfileItem(null, "Age"),
            ProfileItem(R.drawable.transgender_24px, "Gender"),
            ProfileItem(null, "Body Size"),
            ProfileItem(R.drawable.circle_24px, "Skin Tone"),

            // **Preferences**
            ProfileItem(null, "Your Preference"),
            ProfileItem(null, "Style"),
//
//            // **Account & Subscription**
//            ProfileItem(R.drawable.redeem_subscription_24px, "Subscription"),
//            ProfileItem(R.drawable.credit_card_clock_24px, "Payment History"),

            // **Settings & Logout**
            ProfileItem(R.drawable.settings_24px, "Settings"),
            ProfileItem(R.drawable.logout_24px, "Logout"),
            ProfileItem(R.drawable.delete_24px, "Delete Account")
        )

        adapter = ProfileAdapter(requireContext(), items)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> startActivity(Intent(context, AboutMeActivity::class.java))
                5 -> startActivity(Intent(context, AboutMeActivity::class.java))
//                7 -> startActivity(Intent(context, PaymentActivity::class.java))
//                8 -> startActivity(Intent(context, PaymentHistoryActivity::class.java))
                8 -> logout()
                9 -> deleteAccount() // Call the deleteAccount function
            }
        }

        return view
    }

    private fun loadAndDisplayUserProfileData() {
        val user = auth.currentUser
        if (user != null) {
            val emailKey = user.email ?: "No_Email"

            firestore.collection("users").document(emailKey)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userProfile = document.get("userProfile") as? Map<String, Any>
                        if (userProfile != null) {
                            // Update the ListView items with the data
                            updateListViewItems(userProfile)
                        } else {
                            Log.d("ProfileFragment", "No userProfile data found")
                        }
                    } else {
                        Log.d("ProfileFragment", "Document does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileFragment", "Error getting document: ${e.message}")
                }
        } else {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateListViewItems(userProfile: Map<String, Any>) {
        val age = userProfile["age"] as? String ?: ""
        val gender = userProfile["gender"] as? String ?: ""
        val bodySize = userProfile["bodySize"] as? String ?: ""
        val skinTone = userProfile["skinTone"] as? String ?: ""
        val stylesList = userProfile["styles"] as? List<String> ?: emptyList()
        val styles = stylesList.joinToString(", ")

        // Determine the gender icon
        val genderIcon = when (gender) {
            "Male" -> R.drawable.male_24px
            "Female" -> R.drawable.female_24px
            "Others" -> R.drawable.transgender_24px
            else -> R.drawable.accessibility_new_24px // Default icon if gender is not recognized
        }

        // Determine the skin tone icon and tint
        val skinToneIcon = R.drawable.circle_24px
        val skinToneColor = when (skinTone) {
            "Fair" -> R.color.skin_tone_fair
            "Light" -> R.color.skin_tone_light
            "Medium" -> R.color.skin_tone_medium
            "Tan" -> R.color.skin_tone_tan
            "Dark" -> R.color.skin_tone_dark
            else -> null // No tint if skin tone is not recognized
        }

        val skinToneTint = skinToneColor?.let { ContextCompat.getColor(requireContext(), it) }

        // Create new list items that include the user data
        val newItems = listOf(
            ProfileItem(null, "Body Profile"), // Keep the "Body Profile" as is
            ProfileItem(null, "Age", age), //Set age to be value instead of the title
            ProfileItem(genderIcon, "Gender", gender), // Set custom icon for gender and gender to be value
            ProfileItem(null, "Body Size", bodySize), //Set body size to be value instead of the title
            ProfileItem(skinToneIcon, "Skin Tone", skinTone, skinToneTint), // Set custom icon and tint for skin tone and skin tone to be value
            ProfileItem(null, "Your Preference"),
            ProfileItem(null, "Style", styles), // Set styles to be value instead of the title
//            ProfileItem(R.drawable.redeem_subscription_24px, "Subscription"),
//            ProfileItem(R.drawable.credit_card_clock_24px, "Payment History"),
            ProfileItem(R.drawable.settings_24px, "Settings"),
            ProfileItem(R.drawable.logout_24px, "Logout"),
            ProfileItem(R.drawable.delete_24px, "Delete Account")
        )

        // Update the adapter with the new items and refresh the ListView
        (listView.adapter as ProfileAdapter).also {
            it.clear()
            it.addAll(newItems)
            it.notifyDataSetChanged()
        }
    }

    private fun logout() {
        googleSignInClient.signOut().addOnCompleteListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadUserData() {
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(requireContext())

        if (account != null) {
            profileName.text = account.displayName ?: "No Name"
            profileEmail.text = account.email ?: "No Email"

            Glide.with(this)
                .load(account.photoUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .circleCrop()
                .into(profileImage)
        } else {
            Toast.makeText(context, "No Google Account Found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteAccount() {
        AlertDialog.Builder(context)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account?")
            .setPositiveButton("Delete") { dialog, which ->
                val user = auth.currentUser
                if (user != null) {
                    // Delete user data from Firestore
                    val emailKey = user.email ?: "No_Email"
                    firestore.collection("users").document(emailKey)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("ProfileFragment", "User data deleted from Firestore")
                            // Delete the Firebase user
                            user.delete()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d("ProfileFragment", "User account deleted from Firebase")
                                        Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(context, MainActivity::class.java))

                                    } else {
                                        Log.e("ProfileFragment", "Error deleting user account: ${task.exception?.message}")
                                        Toast.makeText(context, "Error deleting account", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileFragment", "Error deleting user data from Firestore: ${e.message}")
                            Toast.makeText(context, "Error deleting account data", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}