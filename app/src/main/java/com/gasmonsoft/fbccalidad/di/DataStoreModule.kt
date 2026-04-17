package com.gasmonsoft.fbccalidad.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gasmonsoft.fbccalidad.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fbccalidad.data.repository.datastore.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideSettingsRepo(dataStore: DataStore<Preferences>): DataStoreRepository =
        DataStoreRepository(dataStore)
}