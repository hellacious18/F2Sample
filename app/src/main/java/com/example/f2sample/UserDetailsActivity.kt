package com.example.f2sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UserDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_details)

        val name = intent.getStringExtra("name")
        val email = intent.getStringExtra("email")
        val birthdate = intent.getStringExtra("birthdate")
        val gender = intent.getStringExtra("gender")
        val height = intent.getStringExtra("height")
        val weight = intent.getStringExtra("weight")

        findViewById<TextView>(R.id.nameText).text = name
        findViewById<TextView>(R.id.emailText).text = email
        findViewById<TextView>(R.id.birthdateText).text = "Birthdate: $birthdate"
        findViewById<TextView>(R.id.genderText).text = "Gender: $gender"
        findViewById<TextView>(R.id.heightText).text = "Height: $height"
        findViewById<TextView>(R.id.weightText).text = "Weight: $weight"
    }
}
