package com.gasmonsoft.fuelboxcontrol.data.service.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.gasmonsoft.fuelboxcontrol.data.model.wifi.WifiConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

interface WifiStateObserver {
    fun observe(): Flow<WifiConnectionState>
}

class WifiStateObserverImpl @Inject constructor(
    @ApplicationContext context: Context
) : WifiStateObserver {

    private val appContext = context.applicationContext

    private val connectivityManager: ConnectivityManager =
        appContext.getSystemService(ConnectivityManager::class.java)

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun observe(): Flow<WifiConnectionState> = callbackFlow {
        trySend(readCurrentState())

        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                // No leer propiedades síncronas aquí.
                // Esperamos a onCapabilitiesChanged() para tener info consistente.
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(networkCapabilities.toWifiConnectionState())
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onLost(network: Network) {
                trySend(readCurrentState())
            }

            override fun onUnavailable() {
                trySend(WifiConnectionState())
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        awaitClose {
            runCatching {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }.distinctUntilChanged()

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun readCurrentState(): WifiConnectionState {
        val network = connectivityManager.activeNetwork ?: return WifiConnectionState()
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return WifiConnectionState()

        return capabilities.toWifiConnectionState()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun NetworkCapabilities.toWifiConnectionState(): WifiConnectionState {
        val isWifi = hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (!isWifi) {
            return WifiConnectionState(
                connected = true,
                wifi = false,
                validated = isValidated,
                ssid = null,
                rssi = null,
                signalLevel = null
            )
        }

        val wifiInfo = transportInfo as? WifiInfo

        val ssid = wifiInfo?.ssid
            ?.removePrefix("\"")
            ?.removeSuffix("\"")
            ?.takeUnless { it.equals("<unknown ssid>", ignoreCase = true) }

        val rssi = wifiInfo?.rssi?.takeIf { it > -127 }
        val level = rssi?.let(::mapSignalLevel)

        val levelMessage = when (level) {
            4 -> "Señal Excelente"
            3 -> "Señal Buena"
            2 -> "Señal Aceptable"
            1 -> "Señal Débil"
            else -> ""
        }

        return WifiConnectionState(
            connected = true,
            wifi = true,
            validated = isValidated,
            ssid = ssid,
            rssi = rssi,
            signalLevel = level,
            signalMessage = levelMessage
        )
    }

    private fun mapSignalLevel(rssi: Int): Int = when {
        rssi >= -55 -> 4
        rssi >= -65 -> 3
        rssi >= -75 -> 2
        rssi >= -85 -> 1
        else -> 0
    }
}