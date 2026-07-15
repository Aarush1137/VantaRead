package com.example.vantaread.data.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VantaStorageManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var baseUri: Uri? = null

    fun setBaseUri(uriString: String?) {
        baseUri = if (uriString.isNullOrBlank()) null else Uri.parse(uriString)
    }

    private fun getInternalDir(): File {
        val dir = File(context.filesDir, "novels")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(100)
    }

    private fun getNovelDirName(novelUrl: String): String {
        return try {
            val host = URL(novelUrl).host.replace(".", "_")
            val path = URL(novelUrl).path.trim('/').replace("/", "_")
            sanitizeFileName("${host}_$path")
        } catch (e: Exception) {
            sanitizeFileName(novelUrl)
        }
    }

    private fun getChapterFileName(chapterUrl: String): String {
        return try {
            val path = URL(chapterUrl).path.trim('/').replace("/", "_")
            "${sanitizeFileName(path)}.html"
        } catch (e: Exception) {
            "${sanitizeFileName(chapterUrl)}.html"
        }
    }

    suspend fun saveChapter(novelUrl: String, chapterUrl: String, content: String) = withContext(Dispatchers.IO) {
        val uri = baseUri
        if (uri == null) {
            val novelDir = File(getInternalDir(), getNovelDirName(novelUrl))
            if (!novelDir.exists()) novelDir.mkdirs()
            val file = File(novelDir, getChapterFileName(chapterUrl))
            file.writeText(content)
        } else {
            val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext
            val novelDir = rootDoc.findFile(getNovelDirName(novelUrl)) ?: rootDoc.createDirectory(getNovelDirName(novelUrl)) ?: return@withContext
            val fileName = getChapterFileName(chapterUrl)
            val file = novelDir.findFile(fileName) ?: novelDir.createFile("text/html", fileName) ?: return@withContext
            context.contentResolver.openOutputStream(file.uri)?.use { 
                it.write(content.toByteArray())
            }
        }
    }

    suspend fun loadChapter(novelUrl: String, chapterUrl: String): String? = withContext(Dispatchers.IO) {
        val uri = baseUri
        if (uri == null) {
            val novelDir = File(getInternalDir(), getNovelDirName(novelUrl))
            val file = File(novelDir, getChapterFileName(chapterUrl))
            if (file.exists()) file.readText() else null
        } else {
            val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext null
            val novelDir = rootDoc.findFile(getNovelDirName(novelUrl)) ?: return@withContext null
            val file = novelDir.findFile(getChapterFileName(chapterUrl)) ?: return@withContext null
            context.contentResolver.openInputStream(file.uri)?.use { 
                it.bufferedReader().readText()
            }
        }
    }

    suspend fun deleteNovel(novelUrl: String) = withContext(Dispatchers.IO) {
        val uri = baseUri
        if (uri == null) {
            val novelDir = File(getInternalDir(), getNovelDirName(novelUrl))
            novelDir.deleteRecursively()
        } else {
            val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext
            val novelDir = rootDoc.findFile(getNovelDirName(novelUrl))
            novelDir?.delete()
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val uri = baseUri
        if (uri == null) {
            getInternalDir().deleteRecursively()
        } else {
            val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext
            rootDoc.listFiles().forEach { it.delete() }
        }
    }

    suspend fun migrate(oldUriString: String?, newUriString: String?) = withContext(Dispatchers.IO) {
        val oldBase = if (oldUriString.isNullOrBlank()) null else Uri.parse(oldUriString)
        val newBase = if (newUriString.isNullOrBlank()) null else Uri.parse(newUriString)
        
        Log.d("VantaStorage", "Migrating from $oldUriString to $newUriString")

        // Helper to get all novel directories in a base
        fun getNovels(base: Uri?): List<Pair<String, List<String>>> {
            return if (base == null) {
                getInternalDir().listFiles()?.filter { it.isDirectory }?.map { dir ->
                    dir.name to (dir.listFiles()?.map { it.name } ?: emptyList())
                } ?: emptyList()
            } else {
                val doc = DocumentFile.fromTreeUri(context, base) ?: return emptyList()
                doc.listFiles().filter { it.isDirectory }.map { dir ->
                    dir.name!! to (dir.listFiles().map { it.name!! })
                }
            }
        }

        val sourceNovels = getNovels(oldBase)
        for ((novelDirName, chapterFiles) in sourceNovels) {
            for (chapterFileName in chapterFiles) {
                try {
                    val content = if (oldBase == null) {
                        File(File(getInternalDir(), novelDirName), chapterFileName).readText()
                    } else {
                        val rootDoc = DocumentFile.fromTreeUri(context, oldBase)!!
                        val novelDoc = rootDoc.findFile(novelDirName)!!
                        val chapterDoc = novelDoc.findFile(chapterFileName)!!
                        context.contentResolver.openInputStream(chapterDoc.uri)!!.use { it.bufferedReader().readText() }
                    }

                    if (newBase == null) {
                        val novelDir = File(getInternalDir(), novelDirName)
                        if (!novelDir.exists()) novelDir.mkdirs()
                        File(novelDir, chapterFileName).writeText(content)
                    } else {
                        val rootDoc = DocumentFile.fromTreeUri(context, newBase)!!
                        val novelDoc = rootDoc.findFile(novelDirName) ?: rootDoc.createDirectory(novelDirName)!!
                        val chapterDoc = novelDoc.createFile("text/html", chapterFileName)!!
                        context.contentResolver.openOutputStream(chapterDoc.uri)!!.use { it.write(content.toByteArray()) }
                    }
                } catch (e: Exception) {
                    Log.e("VantaStorage", "Migration failed for $novelDirName/$chapterFileName", e)
                }
            }
        }
    }
    
    suspend fun saveCover(novelUrl: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        val uri = baseUri
        val novelDirName = getNovelDirName(novelUrl)
        val fileName = "cover.jpg"
        if (uri == null) {
            val novelDir = File(getInternalDir(), novelDirName)
            if (!novelDir.exists()) novelDir.mkdirs()
            File(novelDir, fileName).writeBytes(bytes)
        } else {
            val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return@withContext
            val novelDir = rootDoc.findFile(novelDirName) ?: rootDoc.createDirectory(novelDirName) ?: return@withContext
            val file = novelDir.findFile(fileName) ?: novelDir.createFile("image/jpeg", fileName) ?: return@withContext
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
        }
    }
    
    fun getCoverUri(novelUrl: String): Uri? {
        val novelDirName = getNovelDirName(novelUrl)
        val fileName = "cover.jpg"
        val uri = baseUri
        return if (uri == null) {
            val file = File(File(getInternalDir(), novelDirName), fileName)
            if (file.exists()) Uri.fromFile(file) else null
        } else {
            val rootDoc = DocumentFile.fromTreeUri(context, uri) ?: return null
            val novelDir = rootDoc.findFile(novelDirName) ?: return null
            val file = novelDir.findFile(fileName) ?: return null
            file.uri
        }
    }
}
