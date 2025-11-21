package com.example.hbooks

import android.app.Application
import com.google.firebase.FirebaseApp

class HBooksApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
