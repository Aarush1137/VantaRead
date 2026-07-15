package com.example.vantaread.data.repository

import com.example.vantaread.data.db.NovelDao
import com.example.vantaread.data.db.NovelEntity
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepository @Inject constructor(
    private val firebaseServices: FirebaseServices,
    private val novelDao: NovelDao
) {

    suspend fun syncBookmarksToCloud() {
        val auth = firebaseServices.requireAuth()
        val firestore = firebaseServices.requireFirestore()
        val user = requireNotNull(auth.currentUser) { "Sign in to sync bookmarks." }
        val bookmarksRef = firestore.collection("users").document(user.uid).collection("bookmarks")
        val localBookmarks = novelDao.getBookmarkedNovelsSynchronous()
        val localIds = localBookmarks.mapTo(mutableSetOf()) { it.url.stableCloudId() }

        // Firestore batches support at most 500 writes. Sync in bounded chunks and
        // remove cloud documents for bookmarks that were removed locally.
        val remoteIds = bookmarksRef.get().await().documents.mapTo(mutableSetOf()) { it.id }
        val writes = buildList {
            localBookmarks.forEach { novel ->
                add(CloudWrite.Set(bookmarksRef.document(novel.url.stableCloudId()), novel.toCloudMap()))
            }
            (remoteIds - localIds).forEach { id -> add(CloudWrite.Delete(bookmarksRef.document(id))) }
        }

        writes.chunked(MAX_BATCH_WRITES).forEach { chunk ->
            firestore.runBatch { batch ->
                chunk.forEach { write ->
                    when (write) {
                        is CloudWrite.Set -> batch.set(write.reference, write.data)
                        is CloudWrite.Delete -> batch.delete(write.reference)
                    }
                }
            }.await()
        }
    }

    suspend fun syncBookmarksFromCloud() = withContext(Dispatchers.IO) {
        val auth = firebaseServices.requireAuth()
        val firestore = firebaseServices.requireFirestore()
        val user = auth.currentUser ?: return@withContext
        val userRef = firestore.collection("users").document(user.uid)
        val bookmarksRef = userRef.collection("bookmarks")
        
        val snapshot = bookmarksRef.get().await()
        val novels = snapshot.documents.mapNotNull { doc ->
            val url = doc.getString("url") ?: return@mapNotNull null
            NovelEntity(
                    url = url,
                    title = doc.getString("title") ?: "",
                    coverUrl = doc.getString("coverUrl") ?: "",
                    synopsis = doc.getString("synopsis") ?: "",
                    author = doc.getString("author") ?: "",
                    genres = doc.getString("genres") ?: "",
                    status = doc.getString("status") ?: "",
                    latestUpdate = doc.getString("latestUpdate") ?: "",
                    isBookmarked = doc.getBoolean("bookmarked") ?: doc.getBoolean("isBookmarked") ?: true,
                    currentChapterUrl = doc.getString("currentChapterUrl"),
                    currentScrollPosition = doc.getLong("currentScrollPosition")?.toInt() ?: 0,
                    sourceId = doc.getString("sourceId") ?: "wtrlab"
            )
        }

        novels.forEach { novel -> novelDao.insertNovel(novel) }
    }

    private fun NovelEntity.toCloudMap(): Map<String, Any?> = mapOf(
        "url" to url,
        "title" to title,
        "coverUrl" to coverUrl,
        "synopsis" to synopsis,
        "author" to author,
        "genres" to genres,
        "status" to status,
        "latestUpdate" to latestUpdate,
        "isBookmarked" to isBookmarked,
        "currentChapterUrl" to currentChapterUrl,
        "currentScrollPosition" to currentScrollPosition,
        "sourceId" to sourceId
    )

    private fun String.stableCloudId(): String =
        java.util.UUID.nameUUIDFromBytes(toByteArray(Charsets.UTF_8)).toString()

    private fun FirebaseServices.requireAuth() = requireNotNull(auth) {
        "Cloud sync is unavailable because Firebase is not configured in this build."
    }

    private fun FirebaseServices.requireFirestore() = requireNotNull(firestore) {
        "Cloud sync is unavailable because Firebase is not configured in this build."
    }

    private sealed interface CloudWrite {
        data class Set(val reference: com.google.firebase.firestore.DocumentReference, val data: Map<String, Any?>) : CloudWrite
        data class Delete(val reference: com.google.firebase.firestore.DocumentReference) : CloudWrite
    }

    private companion object {
        const val MAX_BATCH_WRITES = 500
    }
}
