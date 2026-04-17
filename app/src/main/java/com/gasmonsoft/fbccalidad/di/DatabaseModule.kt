package com.gasmonsoft.fbccalidad.di

import android.content.Context
import androidx.annotation.Keep
import androidx.room.Room
import com.gasmonsoft.fbccalidad.BuildConfig
import com.gasmonsoft.fbccalidad.data.database.FuelBoxControlDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideContainerDao(database: FuelBoxControlDatabase) = database.containerDao()

    @Provides
    fun provideUserDao(database: FuelBoxControlDatabase) = database.userDao()

    @Keep
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): FuelBoxControlDatabase {
        return Room.databaseBuilder(
            appContext,
            FuelBoxControlDatabase::class.java,
            BuildConfig.DB_NAME
        ).fallbackToDestructiveMigration(true).build()
    }
}