package com.example.vantaread.data.repository

import android.content.Context
import android.util.Log
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
        private const val TAG = "FirebaseServices"

        fun create(context: Context): FirebaseServices {
            // Check for the existence of google-services.json generated strings
            val res = context.resources
            val packageName = context.packageName
            val googleAppId = res.getIdentifier("google_app_id", "string", packageName)
            
            if (googleAppId == 0) {
                Log.w(TAG, "google_app_id resource not found. Firebase is likely not configured.")
                return FirebaseServices(null, null)
            }

            val app = try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
            }

            if (app == null) {
                Log.w(TAG, "FirebaseApp could not be initialized despite finding resources.")
            }

            return FirebaseServices(
                auth = app?.let { 
                    runCatching { FirebaseAuth.getInstance(it) }.getOrNull() 
                },
                firestore = app?.let { 
                    runCatching { FirebaseFirestore.getInstance(it) }.getOrNull() 
                }
            )
        }
    }
}
