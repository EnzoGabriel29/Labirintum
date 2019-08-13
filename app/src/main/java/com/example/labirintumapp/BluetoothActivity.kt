package com.example.labirintumapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID
import android.os.SystemClock
import android.widget.Toast

public class BluetoothConnectionService (act: MenuGravacao){
    private val appName = "MYAPP"
    private val TAG = "BluetoothConnectionServ"
    private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    private val activity: MenuGravacao
    private val mBluetoothAdapter: BluetoothAdapter
    
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private lateinit var mmDevice: BluetoothDevice
    private lateinit var deviceUUID: UUID
    private lateinit var mConnectedThread: ConnectedThread

    private var isConnected = false

    init {
        activity = act
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        start()
    }

    private inner class AcceptThread() : Thread() {
        private val mmServerSocket: BluetoothServerSocket?

        init {
            var tmp: BluetoothServerSocket? = null

            try{
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE)
                escreverLog("AcceptThread: Setting up Server using: " + MY_UUID_INSECURE)
            } catch (e: IOException){
                escreverLog("AcceptThread: IOException")
            }

            mmServerSocket = tmp ?: null
        }

        override fun run(){
            escreverLog("run: AcceptThread Running.")
            var socket: BluetoothSocket? = null

            try{
                escreverLog("run: RFCOM server socket start.....")

                socket = mmServerSocket!!.accept()

                escreverLog("run: RFCOM server socket accepted connection.")

            } catch (e: IOException){
                escreverLog("AcceptThread: IOException:")
            }

            if (socket != null){
                connected(socket, mmDevice)
            }

            escreverLog("END mAcceptThread ")
        }

        fun cancel() {
            escreverLog("cancel: Canceling AcceptThread.")
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
                escreverLog("cancel: Close of AcceptThread ServerSocket failed.")
            }
        }
    }

    private inner class ConnectThread (device: BluetoothDevice, uuid: UUID) : Thread() {
        private var mmSocket: BluetoothSocket? = null

        init {
            escreverLog("ConnectThread: started.")
            mmDevice = device
            deviceUUID = uuid
        }

        override fun run(){
            var tmp: BluetoothSocket? = null
            escreverLog("RUN mConnectThread ")

            try {
                escreverLog("ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + MY_UUID_INSECURE )
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID)
            } catch (e: IOException) {
                escreverLog("ConnectThread: Could not create InsecureRfcommSocket")
            }

            mmSocket = tmp ?: null
            mBluetoothAdapter.cancelDiscovery()

            try {
                mmSocket!!.connect()
                escreverLog("run: ConnectThread connected.")

            } catch (e: IOException) {
                try {
                    mmSocket!!.close()
                    escreverLog("run: Closed Socket.")
                } catch (e1: IOException) {
                    escreverLog("mConnectThread: run: Unable to close connection in socket")
                }
                escreverLog("run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE )
            }

            connected(mmSocket!!, mmDevice)
        }

        fun cancel() {
            try {
                escreverLog("cancel: Closing Client Socket.")
                mmSocket!!.close()
            } catch (e: IOException) {
                escreverLog("cancel: close() of mmSocket in Connectthread failed.")
            }
        }
    }

    @Synchronized fun start() {
        escreverLog("start")
        var tmp = mConnectThread
        if (tmp != null){
            tmp.cancel()
            tmp = null
            mConnectThread = tmp
        }
        
        var tmp2 = mInsecureAcceptThread
        if (tmp2 == null){
            tmp2 = AcceptThread()
            tmp2.start()
            mInsecureAcceptThread = tmp2
        }
    }

    public fun startClient(device: BluetoothDevice, uuid: UUID){
        escreverLog("startClient: Started.")

        mConnectThread = ConnectThread(device, uuid)
        var tmp3 = mConnectThread
        if (tmp3 != null){
            tmp3.start()
            mConnectThread = tmp3
        }
    }

    private inner class ConnectedThread (socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            escreverLog("ConnectedThread: Starting.")

            mmSocket = socket ?: null
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = mmSocket!!.getInputStream()
                tmpOut = mmSocket!!.getOutputStream()
            } catch (e: IOException) {
                escreverLog("falhou")
            }

            mmInStream = tmpIn ?: null
            mmOutStream = tmpOut ?: null
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            while (true) {
                /*
                try {     
                    bytes = mmInStream!!.read(buffer)
                    val incomingMessage = String(buffer, 0, bytes)
                    escreverLog("InputStream: $incomingMessage")

                    when (incomingMessage) {
                        "1" -> this@BluetoothConnectionService.activity.iniciarGravacao()
                        "2" -> this@BluetoothConnectionService.activity.pausarGravacao()
                        "3" -> this@BluetoothConnectionService.activity.retomarGravacao()
                        "0" -> this@BluetoothConnectionService.activity.pararGravacao()
                    }
                } catch (e: IOException) {
                  escreverLog("write: Error reading Input Stream.")
                  break
                }
                */
                try {
                    if (mmInStream!!.available() > 0){
                        bytes = mmInStream!!.read(buffer)
                        val incomingMessage = String(buffer, 0, bytes)
                        escreverLog("InputStream: $incomingMessage")

                        when (incomingMessage) {
                            "1" -> this@BluetoothConnectionService.activity.iniciarGravacao()
                            "2" -> this@BluetoothConnectionService.activity.pausarGravacao()
                            "3" -> this@BluetoothConnectionService.activity.retomarGravacao()
                            "0" -> this@BluetoothConnectionService.activity.pararGravacao()
                        }
                    } else SystemClock.sleep(100)
                } catch (e: IOException) {
                    cancel()
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        fun write(bytes: ByteArray) {
            val text = String(bytes, Charset.defaultCharset())
            escreverLog("write: Writing to outputstream: " + text)
            
            try {
                mmOutStream!!.write(bytes)
            } catch (e: IOException) {
                escreverLog("write: Error writing to output stream.")
            }
        }

        // Call this from the main activity to shutdown the connection
        fun cancel() {
            try { mmSocket!!.close()
            } catch (e: IOException) { }
        }
    }

    private fun connected(mmSocket: BluetoothSocket, mmDevice: BluetoothDevice) {
        escreverLog("connected: Starting.")

        mConnectedThread = ConnectedThread(mmSocket)
        isConnected = true
        mConnectedThread.start()
    }

    fun write (outMsg: ByteArray) {
        val r: ConnectedThread
        escreverLog("write: Write Called.")

        if (!isConnected) {
            Toast.makeText(activity, "Não foi possível conectar" +
                " a esse dispositivo. Verifique se o dispositivo" +
                " está disponível para pareamento.", Toast.LENGTH_LONG).show()
            activity.pararGravacao()
        } else mConnectedThread.write(outMsg)
    }

    fun cancel () {
        val r: ConnectedThread
        escreverLog("cancel: Cancel Called.")

        if (isConnected) mConnectedThread.cancel()
    }
}