package com.samilozturk.homemate_matching.data.source.remotestorage

import android.net.Uri
import com.samilozturk.homemate_matching.data.model.ImagePath

interface RemoteStorageRepository {
    suspend fun getDownloadUrl(imagePath: ImagePath): String
    suspend fun uploadImage(uri: Uri, fileName: String): ImagePath
}