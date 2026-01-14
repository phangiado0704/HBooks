package com.example.hbooks.di

import com.example.hbooks.data.repository.AuthRepository
import com.example.hbooks.data.repository.BookRepository
import com.example.hbooks.util.FirebaseStorageFetcher
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): AuthRepository {
        return AuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideBookRepository(): BookRepository {
        return BookRepository()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorageFetcher(): FirebaseStorageFetcher {
        return FirebaseStorageFetcher()
    }
}
