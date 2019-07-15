package com.example.labirintumapp

import java.util.UUID
import java.lang.Thread
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.util.Log
import java.io.IOException
import android.widget.Toast
import java.io.FileWriter
import java.io.StringWriter
import java.io.PrintWriter
import android.os.Handler
import android.os.Looper
import android.content.Context

val uuid = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")

fun showToast(context: Context, msg: String) {
    val handler = Handler(Looper.getMainLooper())
    handler.post(object : Runnable {
        public override fun run() {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    })
}

// controla o usuário que receberá os dados via bluetooth
class BluetoothServerController(activity: MenuGravacao) : Thread() {
    private var cancelled: Boolean
    private val serverSocket: BluetoothServerSocket?
    private val activity: MenuGravacao = activity
    private lateinit var socket: BluetoothSocket

    init {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("test", uuid)
            cancelled = false
        } else {
            serverSocket = null
            cancelled = true
        }
    }

    override fun run() {
        while(true) {
            if (cancelled) break
            
            try {
                showToast(activity, "Aguardando a conexão do dispositivo do terapeuta...")
                socket = serverSocket!!.accept()
            }
            catch (e: IOException) {
                break
            }

            if (!cancelled && socket != null) {
                BluetoothServer(activity, socket).start()
            }
        }
    }

    fun cancel() {
        cancelled = true
        serverSocket!!.close()
    }
}

// usuário que receberá os dados via bluetooth
class BluetoothServer(act: MenuGravacao, soc: BluetoothSocket): Thread() {
    private val activity: MenuGravacao = act
    private val socket = soc
    private val inputStream = socket.inputStream
    private val outputStream = socket.outputStream
    private var text: String = ""

    override fun run() {
        try {
            val available = inputStream.available()
            val bytes = ByteArray(available)

            inputStream.read(bytes, 0, available)

            text = String(bytes)
            when (text) {
                "1" -> activity.iniciarGravacao()
                "2" -> activity.pausarGravacao()
                "3" -> activity.retomarGravacao()
                "0" -> activity.pararGravacao()
            }

        } catch (e: Exception) {
        } finally {
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}

// usuário que enviará os dados via bluetooth
class BluetoothClient(act: MenuGravacao, device: BluetoothDevice, msg: String): Thread() {
    private val activity = act
    private val socket = device.createRfcommSocketToServiceRecord(uuid)
    private val message = msg

    override fun run() {
        var socketConnected = false

        try {
            showToast(activity, "Tentando conectar ao dispositivo do paciente...")
            socket.connect()
            socketConnected = true
        } catch(e: Exception) {
            activity.pararGravacao()
            showToast(activity, "Conexão expirada!")
        }

        if (socketConnected) {
            val outputStream = socket.outputStream
            val inputStream = socket.inputStream
            
            try {
                outputStream.write(message.toByteArray())
                outputStream.flush()
            } catch(e: Exception) {
            } finally {
                outputStream.close()
                inputStream.close()
                socket.close()
            }
        }
        

        
    }
}
