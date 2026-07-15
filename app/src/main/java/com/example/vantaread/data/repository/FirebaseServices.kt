package com.example.vantaread.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Firebase is an optional integration for local and open-source builds. The reader must remain
 * usable in guest mode when a developer has not supplied google-services.json.
 */
class FirebaseServices private constructor(
    val auth: FirebaseAuth?,
    val firestore: FirebaseFirestore?
) {
    val isAuthConfigured: Boolean
        get() = auth != null

    val isCloudSyncConfigured: Boolean
        get() = auth != null && firestore != null

    companion object {
        fun create(context: Context): FirebaseServices {
            val app = runCatching { FirebaseApp.getInstance() }.getOrNull()
                ?: FirebaseApp.initializeApp(context)

            return FirebaseServices(
                auth = app?.let(FirebaseAuth::getInstance),
                firestore = app?.let(FirebaseFirestore::getInstance)
            )
        }
    }
}
