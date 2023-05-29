package com.samilozturk.homemate_matching.di

import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.samilozturk.homemate_matching.data.source.auth.AuthRepository
import com.samilozturk.homemate_matching.data.source.auth.impl.AuthRepositoryImpl
import com.samilozturk.homemate_matching.data.source.db.DbRepository
import com.samilozturk.homemate_matching.data.source.db.impl.DbRepositoryImpl
import com.samilozturk.homemate_matching.data.source.localstorage.LocalStorageRepository
import com.samilozturk.homemate_matching.data.source.localstorage.impl.LocalStorageRepositoryImpl
import com.samilozturk.homemate_matching.data.source.notification.NotificationRepository
import com.samilozturk.homemate_matching.data.source.notification.impl.NotificationRepositoryImpl
import com.samilozturk.homemate_matching.data.source.remotestorage.RemoteStorageRepository
import com.samilozturk.homemate_matching.data.source.remotestorage.impl.RemoteStorageRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideAuthRepository(
        auth: FirebaseAuth,
    ): AuthRepository {
        return AuthRepositoryImpl(auth)
    }

    @Singleton
    @Provides
    fun provideDbRepository(
        db: FirebaseFirestore,
        remoteStorageRepository: RemoteStorageRepository,
        authRepository: AuthRepository,
        notificationRepository: NotificationRepository,
        localStorageRepository: LocalStorageRepository,
    ): DbRepository {
        return DbRepositoryImpl(
            db,
            remoteStorageRepository,
            authRepository,
            notificationRepository,
            localStorageRepository
        )
    }

    @Singleton
    @Provides
    fun provideLocalStorageRepository(
        prefs: SharedPreferences,
    ): LocalStorageRepository {
        return LocalStorageRepositoryImpl(prefs)
    }

    @Singleton
    @Provides
    fun provideRemoteStorageRepository(
        storage: FirebaseStorage,
    ): RemoteStorageRepository {
        return RemoteStorageRepositoryImpl(storage)
    }

    @Singleton
    @Provides
    fun provideNotificationRepository(
        localStorageRepository: LocalStorageRepository
    ): NotificationRepository {
        val client = HttpClient(CIO) {
            install(ContentNegotiation)
        }
        // add plugin to client
        return NotificationRepositoryImpl(client, localStorageRepository)
    }

}