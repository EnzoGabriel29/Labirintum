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

fun showToast(context: Context, msg: String, len: Int) {
    val handler = Handler(Looper.getMainLooper())
    handler.post(object : Runnable {
        override fun run() {
            val dur = if (len == 0) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            Toast.makeText(context, msg, dur).show()
        }
    })
}

class BluetoothServerController(activity: MenuGravacao) : Thread() {
    private var cancelled: Boolean
    private var serverSocket: BluetoothServerSocket?
    private val activity: MenuGravacao = activity
    private lateinit var socket: BluetoothSocket

    init {
        escreverLog("BluetoothServerController: Started thread")
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            try {
                serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("test", uuid)
                escreverLog("BluetoothServerController: Socket created")
                cancelled = false
                showToast(activity, "Aguardando conexão com o " +
                    " dispositivo do terapeuta...", 0)
            } catch (e: IOException) {
                showToast(activity, "Não foi possível iniciar o Bluetooth. "
                    + "Você ativou o Bluetooth no seu dispotivo?", 1)
                serverSocket = null
                cancelled = true
                activity.pararGravacao()
            }
        } else {
            serverSocket = null
            cancelled = true
        }
    }

    override fun run() {
        while(true) {
            if (cancelled) break
            
            try {
                socket = serverSocket!!.accept()
            }
            catch (e: IOException) {
                val errors = StringWriter()
                e.printStackTrace(PrintWriter(errors))
                escreverLog("BluetoothServerController: Error found")
                escreverLog(errors.toString())
                break
            }

            if (!cancelled && socket != null) {
                escreverLog("BluetoothServerController: Connecting")
                BluetoothServer(activity, socket).start()
                escreverLog("BluetoothServer: Started thread") 
            }
        }
    }

    fun cancel() {
        cancelled = true
        serverSocket!!.close()
    }
}

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
            escreverLog("BluetoothServer: Reading")

            inputStream.read(bytes, 0, available)

            text = String(bytes)
            when (text) {
                "1" -> activity.iniciarGravacao()
                "2" -> activity.pausarGravacao()
                "3" -> activity.retomarGravacao()
                "0" -> activity.pararGravacao()
            }

            escreverLog("BluetoothServer: Message received")
            escreverLog("BluetoothServer: Message: $text")
        } catch (e: Exception) {
            escreverLog("BluetoothServer: Cannot read message")
            val errors = StringWriter()
            e.printStackTrace(PrintWriter(errors))
            escreverLog(errors.toString())
        } finally {
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}

class BluetoothClient(act: MenuGravacao, device: BluetoothDevice, msg: String): Thread() {
    private val activity = act
    private val socket = device.createRfcommSocketToServiceRecord(uuid)
    private val message = msg

    override fun run() {
        var socketConnected = false

        escreverLog("BluetoothClient: Connecting")
        try {
            showToast(activity, "Tentando conectar ao dispositivo do paciente...", 0)
            socket.connect()
            socketConnected = true
        } catch(e: Exception) {
            escreverLog("BluetoothClient: Timeout")
            activity.pararGravacao()
            showToast(activity, "Conexão expirada!", 0)
        }

        if (socketConnected) {
            escreverLog("BluetoothClient: Sending")
            val outputStream = socket.outputStream
            val inputStream = socket.inputStream
            
            try {
                outputStream.write(message.toByteArray())
                outputStream.flush()
                escreverLog("BluetoothClient: Message sent")
                escreverLog("BluetoothClient: Message: $message")
            } catch(e: Exception) {
                escreverLog("BluetoothClient: Cannot send message")
            } finally {
                outputStream.close()
                inputStream.close()
                socket.close()
            }
        }
    }
}