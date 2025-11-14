package com.github.warnastrophy.core.ui.firebase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseTest {
  fun initToEmulators() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    auth.useEmulator("10.0.2.2", 9099)
    db.useEmulator("10.0.2.2", 8080)
  }
}
