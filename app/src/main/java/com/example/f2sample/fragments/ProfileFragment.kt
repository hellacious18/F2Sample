package com.example.f2sample.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.f2sample.AboutMeActivity
import com.example.f2sample.MainActivity
import com.example.f2sample.PaymentActivity
import com.example.f2sample.adapter.ProfileAdapter
import com.example.f2sample.R
import com.example.f2sample.data.ProfileItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var profileImage: CircleImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: ProfileAdapter

    private lateinit var googleSignInClient: GoogleSignInClient  // Add this


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        profileImage = view.findViewById(R.id.profile_image)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        listView = view.findViewById(R.id.profile_list)

        googleSignInClient = GoogleSignIn.getClient(requireContext(), GoogleSignInOptions.DEFAULT_SIGN_IN)


        loadUserData()

        val items = listOf(
            ProfileItem(R.drawable.account_circle_24px, "About Me"),
            ProfileItem(R.drawable.ar_on_you_24px, "Facial Features"),
            ProfileItem(R.drawable.accessibility_new_24px, "Body Measurements"),
            ProfileItem(R.drawable.redeem_subscription_24px, "Subscription"),
            ProfileItem(R.drawable.settings_24px, "Settings"),
            ProfileItem(R.drawable.logout_24px, "Logout")
        )

        adapter = ProfileAdapter(requireContext(), items)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {startActivity(Intent(context, AboutMeActivity::class.java))}
                1 -> Toast.makeText(context, "Face Details Clicked", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(context, "Body Proportions Clicked", Toast.LENGTH_SHORT).show()
                3 -> {startActivity(Intent(context, PaymentActivity::class.java))}
                4 -> Toast.makeText(context, "Settings Clicked", Toast.LENGTH_SHORT).show()
                5 -> logout()
            }
        }

        return view  // ✅ Return the inflated view, not a new one
    }

    private fun logout() {
        googleSignInClient.signOut().addOnCompleteListener {

            FirebaseAuth.getInstance().signOut()

            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Redirect to LoginActivity
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

            // Load Profile Image using Glide
            Glide.with(this)
                .load(account.photoUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .circleCrop()
                .into(profileImage)
        } else {
            Toast.makeText(context, "No Google Account Found", Toast.LENGTH_SHORT).show()
        }
    }
}
