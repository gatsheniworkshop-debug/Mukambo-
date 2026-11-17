package com.example.notifications

import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MukamboMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = Firebase.auth.currentUser?.uid ?: return
        try {
            val db = FirebaseFirestore.getInstance(
                applicationContext.getString(R.string.firestore_database_id)
            )
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "fcmToken" to token.take(512),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync FCM token: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "FCM token sync skipped: ${e.message}")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Mukambo Update"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new marketplace update."
        MukamboNotificationHelper.showLocalNotification(
            context = applicationContext,
            title = title,
            body = body
        )
    }

    private companion object {
        const val TAG = "MukamboFCM"
    }
}
