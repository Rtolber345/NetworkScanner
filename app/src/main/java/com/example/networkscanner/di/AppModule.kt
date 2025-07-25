package com.example.networkscanner.di

import android.content.Context
import com.example.networkscanner.data.NetworkScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkScanner(@ApplicationContext context: Context): NetworkScanner {
        return NetworkScanner(context)
    }
}
