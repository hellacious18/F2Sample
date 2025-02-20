package com.example.f2sample

import android.app.Application
import com.google.firebase.FirebaseApp

class FireBase: Application() {
    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.getApps(this).isEmpty()) {  // Ensure Firebase initializes only once
            FirebaseApp.initializeApp(this)
        }

    }
}