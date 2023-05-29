package com.samilozturk.homemate_matching.data.source.remotestorage.impl

import android.net.Uri
import com.samilozturk.homemate_matching.data.model.ImagePath
import com.samilozturk.homemate_matching.data.source.remotestorage.RemoteStorageRepository
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.random.nextInt

class FakeRemoteStorageRepository : RemoteStorageRepository {
    override suspend fun getDownloadUrl(imagePath: ImagePath): String {
        delay(100)
        return imagePath.value
    }

    override suspend fun uploadImage(uri: Uri, fileName: String): ImagePath {
        delay(1000)
        return ImagePath("https://picsum.photos/id/${Random.nextInt(100..200)}/500/500")
    }
}