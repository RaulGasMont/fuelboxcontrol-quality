package com.gasmonsoft.fuelboxcontrol.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import com.gasmonsoft.fuelboxcontrol.BuildConfig
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorBLEReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.service.FuelSoftwareService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(55, TimeUnit.SECONDS)
            .build()
    }

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.URL_API)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Singleton
    @Provides
    fun provideFuelSoftwareService(retrofit: Retrofit): FuelSoftwareService {
        return retrofit.create(FuelSoftwareService::class.java)
    }

    @Provides
    @Singleton
    fun provideSensorReceiveManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter,
    ): SensorReceiveManager {
        return SensorBLEReceiveManager(bluetoothAdapter, context)
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }
}