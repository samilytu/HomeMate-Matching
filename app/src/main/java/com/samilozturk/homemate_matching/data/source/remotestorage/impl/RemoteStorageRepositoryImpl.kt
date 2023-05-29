package com.samilozturk.homemate_matching.data.source.remotestorage.impl

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.samilozturk.homemate_matching.data.model.ImagePath
import com.samilozturk.homemate_matching.data.source.remotestorage.RemoteStorageRepository
import kotlinx.coroutines.tasks.await

class RemoteStorageRepositoryImpl(
    private val storage: FirebaseStorage
) : RemoteStorageRepository {
    override suspend fun getDownloadUrl(imagePath: ImagePath): String {
        return storage.reference.child(imagePath.value).downloadUrl.await().toString()
    }

    override suspend fun uploadImage(uri: Uri, fileName: String): ImagePath {
        // insert timestamp to uri name before extension
        val uriName = fileName.substringBeforeLast(".")
        val uriExtension = fileName.substringAfterLast(".")
        val fileName = "$uriName-${System.currentTimeMillis()}.$uriExtension"
        val imageRef = storage.reference.child("images/$fileName")
        val snapshot = imageRef.putFile(uri).await()
        return ImagePath(snapshot.metadata?.path.toString())
    }
}