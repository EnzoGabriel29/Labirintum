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
    private val mAdapter: BluetoothAdapter
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    
    companion object {
        private val NAME_SECURE = "BluetoothSecure"
        private val NAME_INSECURE = "BluetoothInsecure"
        private val MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
        private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        val STATE_NONE = 0
        val STATE_LISTEN = 1
        val STATE_CONNECTING = 2
        val STATE_CONNECTED = 3
    }

    init {
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        mNewState = mState
    }

    @Synchronized public fun getState(): Int {
        return mState
    }

    public fun iniciaTerapeuta(){
        start()
    }

    public fun iniciaPaciente(device: BluetoothDevice, secure: Boolean){
        connect(device, secure)
    }

    public fun enviaMensagem(outMsg: String){
        var r: ConnectedThread?

        synchronized (this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }

        val byteMsg = outMsg.toByteArray(Charset.defaultCharset())
        r!!.write(byteMsg)
    }

    public fun cancelaDescoberta(){
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

    @Synchronized public fun start(){
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

    @Synchronized public fun connect(device: BluetoothDevice, secure: Boolean){
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

    @Synchronized fun connected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
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
      
        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread!!.start()
    }

    private inner class AcceptThread (secure: Boolean) : Thread() {
        private var mmServerSocket: BluetoothServerSocket? = null
        private val mSocketType: String
        
        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            try {
              if (secure)
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
              else
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE)
            
            } catch (e: IOException) {
                
            }

            mmServerSocket = tmp
            mState = STATE_LISTEN
        }

        public override fun run() {
            var socket: BluetoothSocket? = null

            while (mState !== STATE_CONNECTED){
                try {
                    socket = mmServerSocket!!.accept()

                } catch (e: IOException){
                    break
                }

                if (socket != null){
                    synchronized (this@BluetoothConnector){
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING ->
                                connected(socket, socket.getRemoteDevice(), mSocketType)

                            STATE_NONE, STATE_CONNECTED ->
                                try {
                                    socket!!.close()
                                
                                } catch (e: IOException){
                                
                                }

                            else -> { }
                        }
                    }
                }
            }
        }

        public fun cancel() {            
            try {
              mmServerSocket!!.close()
            
            } catch (e: IOException){
                
            }
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket, socketType: String) : Thread() {
        private val mmSocket: BluetoothSocket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            mmSocket = socket
        
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
      
            try {
                tmpIn = socket.getInputStream()
                tmpOut = socket.getOutputStream()
            } catch (e: IOException) {

            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        public override fun run(){        
            val buffer = ByteArray(1024)
            var bytes: Int
            
            while (mState === STATE_CONNECTED){
                try {
                    bytes = mmInStream!!.read(buffer)
                    
                    val incomingMessage = String(buffer, 0, bytes)
                    when (incomingMessage){
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
      
        public fun write(buffer: ByteArray) {
            try {
                mmOutStream!!.write(buffer)

            } catch (e: IOException) {

            }
        }

        public fun cancel() {
            try {
                mmSocket.close()
            } catch (e:IOException) {

            }
        }
    }

    private inner class ConnectThread (device: BluetoothDevice, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmDevice: BluetoothDevice
        private val mSocketType: String

        init {
            mmDevice = device
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"
        
            try {
                if (secure) tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
                else tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE)
            
            } catch (e: IOException) {
                
            }
            
            mmSocket = tmp
            mState = STATE_CONNECTING
        }
      
        public override fun run() {
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

            synchronized (this@BluetoothConnector){
                mConnectThread = null
            }
        
            connected(mmSocket, mmDevice, mSocketType)
        }

        public fun cancel() {
            try {
                mmSocket!!.close()
            
            } catch (e: IOException) {
                
            }
        }
    }
}