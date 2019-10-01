package com.example.labirintumapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.UUID

class BluetoothConnector(private val activity: MenuGravacao){
    private var mState: Int = 0
    private var mNewState: Int = 0
    private val mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    
    companion object {
        private const val NAME_SECURE = "BluetoothSecure"
        private const val NAME_INSECURE = "BluetoothInsecure"
        private val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }

    init {
        mState = STATE_NONE
        mNewState = mState
    }

    @Synchronized fun getState(): Int {
        return mState
    }

    fun iniciaTerapeuta(){
        start()
    }

    fun iniciaPaciente(device: BluetoothDevice, secure: Boolean){
        connect(device, secure)
    }

    fun enviaMensagem(outMsg: String){
        var r: ConnectedThread?

        synchronized(this){
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }

        val byteMsg = outMsg.toByteArray(Charset.defaultCharset())
        r!!.write(byteMsg)
    }

    fun cancelaDescoberta(){
        if (mConnectThread != null){
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null){
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
      
        if (mSecureAcceptThread != null){
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }

        if (mInsecureAcceptThread != null){
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }
    }

    @Synchronized fun start(){
        if (mConnectThread != null){
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null){
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread == null){
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread!!.start()
        }

        if (mInsecureAcceptThread == null){
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread!!.start()
        }
    }

    @Synchronized fun connect(device: BluetoothDevice, secure: Boolean){
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        mConnectThread = ConnectThread(device, secure)
        mConnectThread!!.start()
    }

    @Synchronized fun connected(socket: BluetoothSocket){
        if (mConnectThread != null){
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null){
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
      
        if (mSecureAcceptThread != null){
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }

        if (mInsecureAcceptThread != null){
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }
      
        mConnectedThread = ConnectedThread(socket)
        mConnectedThread!!.start()
    }

    private inner class AcceptThread (secure: Boolean) : Thread() {
        private var mmServerSocket: BluetoothServerSocket? = null
        private val mSocketType: String
        
        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            try {
                tmp = if (secure)
                    mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
                else
                    mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE)
            
            } catch (e: IOException) {
                
            }

            mmServerSocket = tmp
            mState = STATE_LISTEN
        }

        override fun run() {
            var socket: BluetoothSocket?

            while (mState != STATE_CONNECTED){
                try {
                    socket = mmServerSocket!!.accept()

                } catch (e: IOException){
                    break
                }

                if (socket != null){
                    synchronized (this@BluetoothConnector){
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING ->
                                connected(socket)

                            STATE_NONE, STATE_CONNECTED ->
                                try {
                                    socket.close()
                                
                                } catch (e: IOException){
                                
                                }

                            else -> { }
                        }
                    }
                }
            }
        }

        fun cancel() {
            try {
              mmServerSocket!!.close()
            
            } catch (e: IOException){
                
            }
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket = socket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {

            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
      
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {

            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        override fun run(){
            val buffer = ByteArray(1024)
            var bytes: Int
            
            while (mState == STATE_CONNECTED){
                try {
                    bytes = mmInStream!!.read(buffer)

                    when (val incomingMessage = String(buffer, 0, bytes)){
                        "0" -> this@BluetoothConnector.activity.pararGravacao()
                        "1" -> this@BluetoothConnector.activity.iniciarGravacao()
                        "2" -> this@BluetoothConnector.activity.pausarGravacao()
                        "3" -> this@BluetoothConnector.activity.retomarGravacao()
                        else -> this@BluetoothConnector.activity.escreverCSV(incomingMessage)
                    }

           
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream!!.write(buffer)

            } catch (e: IOException) {

            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e:IOException) {

            }
        }
    }

    private inner class ConnectThread (device: BluetoothDevice, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"
        
            try {
                tmp = if (secure) device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
                else device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE)
            
            } catch (e: IOException) {
                
            }
            
            mmSocket = tmp
            mState = STATE_CONNECTING
        }

        override fun run() {
            mAdapter.cancelDiscovery()
        
            try {
                mmSocket!!.connect()
        
            } catch (e1: IOException){
                try {
                    mmSocket!!.close()
                
                } catch (e2: IOException){
                    
                }
                return
            }

            synchronized(this@BluetoothConnector){
                mConnectThread = null
            }
        
            connected(mmSocket)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            
            } catch (e: IOException) {
                
            }
        }
    }
}