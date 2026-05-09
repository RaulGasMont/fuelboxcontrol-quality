package com.gasmonsoft.fbccalidad.data.datasource.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.gasmonsoft.fbccalidad.data.service.ble.FileReadEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbOtgEventSource @Inject constructor(
    private val usbManager: UsbManager
) {

    private val _events = MutableSharedFlow<FileReadEvent>(
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: Flow<FileReadEvent> = _events.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errors: Flow<String> = _errors.asSharedFlow()

    private var reader: UsbOtgReader? = null
    private var currentScope: CoroutineScope? = null

    // ─── Conectar ──────────────────────────────────────────────────────────

    fun connect(
        device: UsbDevice
    ): Boolean {
        disconnect()

        val newScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        currentScope = newScope

        reader = UsbOtgReader(usbManager).apply {
            onFrameReady = { frame ->
                newScope.launch {
                    _events.emit(
                        FileReadEvent(value = frame)
                    )
                }
            }
            onError = { message ->
                newScope.launch {
                    _errors.emit(message)
                }
            }
            onTransmissionEnd = {
                newScope.launch {}
            }
        }

        return if (reader?.connect(device) == true) {
            reader?.startReading(newScope)
            true
        } else {
            disconnect()
            false
        }
    }

    fun sendCommand(command: String): Boolean {
        return reader?.sendCommand(command) ?: false
    }

    // ─── Flush final ───────────────────────────────────────────────────────

//    fun flushRemaining(type: ReadBinType) {
//        reader?.flushRemaining()?.let { remaining ->
//            currentScope?.launch {
//                _events.emit(
//                    FileReadEvent(value = remaining, type = type)
//                )
//            }
//        }
//    }

    // ─── Estado ────────────────────────────────────────────────────────────

    val isConnected: Boolean
        get() = reader?.isConnected == true

    // ─── Desconectar ───────────────────────────────────────────────────────

    fun disconnect() {
        reader?.disconnect()
        reader = null
        currentScope?.cancel()
        currentScope = null
    }
}