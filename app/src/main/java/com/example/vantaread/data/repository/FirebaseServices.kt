package com.example.vantaread.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class FirebaseConfigInfo(
    val hasGoogleServicesJson: Boolean,
    val hasGoogleAppId: Boolean,
    val hasApiKey: Boolean,
    val hasWebClientId: Boolean,
    val hasProjectId: Boolean,
    val packageName: String,
    val appIdPreview: String? = null
)

/**
 * Firebase is an optional integration for local and open-source builds. The reader must remain
 * usable in guest mode when a developer has not supplied google-services.json.
 */
class FirebaseServices private constructor(
    val auth: FirebaseAuth?,
    val firestore: FirebaseFirestore?,
    val configInfo: FirebaseConfigInfo
) {
    val isAuthConfigured: Boolean
        get() = auth != null

    val isCloudSyncConfigured: Boolean
        get() = auth != null && firestore != null

    companion object {
        private const val TAG = "FirebaseServices"

        fun create(context: Context): FirebaseServices {
            val res = context.resources
            val packageName = context.packageName
            
            val googleAppIdId = res.getIdentifier("google_app_id", "string", packageName)
            val googleApiKeyId = res.getIdentifier("google_api_key", "string", packageName)
            val webClientIdId = res.getIdentifier("default_web_client_id", "string", packageName)
            val projectIdId = res.getIdentifier("project_id", "string", packageName)

            val googleAppId = if (googleAppIdId != 0) res.getString(googleAppIdId) else null
            
            val info = FirebaseConfigInfo(
                hasGoogleServicesJson = googleAppIdId != 0,
                hasGoogleAppId = googleAppIdId != 0,
                hasApiKey = googleApiKeyId != 0,
                hasWebClientId = webClientIdId != 0,
                hasProjectId = projectIdId != 0,
                packageName = packageName,
                appIdPreview = googleAppId?.let { 
                    if (it.length > 10) "${it.take(5)}...${it.takeLast(5)}" else it 
                }
            )

            if (!info.hasGoogleAppId) {
                Log.w(TAG, "google_app_id resource not found. Firebase features are disabled.")
                return FirebaseServices(null, null, info)
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
                },
                configInfo = info
            )
        }
    }
}
