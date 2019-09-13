package com.example.labirintumapp

import android.util.Log
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

class BluetoothConnector(private val activity: MenuGravacao) {
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
        Log.d(TAG, "iniciaTerapeuta called.")
        start()
    }

    public fun iniciaPaciente(device: BluetoothDevice, secure: Boolean){
        Log.d(TAG, "iniciaPaciente called.")
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
            Log.d(TAG, "listenUsingRfcommWithServiceRecord called.")

            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            try {
              if (secure)
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
              else
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE)
            
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: $mSocketType listen() failed", e)
            }

            mmServerSocket = tmp
            mState = STATE_LISTEN
        }

        public override fun run() {
            Log.d(TAG, "accept called.")
            
            var socket: BluetoothSocket? = null

            while (mState !== STATE_CONNECTED){
                try {
                    socket = mmServerSocket!!.accept()

                } catch (e: IOException){
                    Log.e(TAG, "Socket Type: $mSocketType accept() failed", e)
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
                                    Log.e(TAG, "Could not close unwanted socket", e)
                                }

                            else -> Log.d(TAG, "")
                        }
                    }
                }
            }
        }

        public fun cancel() {            
            try {
              mmServerSocket!!.close()
            
            } catch (e: IOException){
                Log.e(TAG, "Socket Type $mSocketType close() of server failed", e)
            }
        }
    }

    private inner class ConnectedThread(socket: BluetoothSocket, socketType: String) : Thread() {
        private val mmSocket: BluetoothSocket
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            Log.d(TAG, "create ConnectedThread: $socketType")
            mmSocket = socket
        
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
      
            try {
                tmpIn = socket.getInputStream()
                tmpOut = socket.getOutputStream()
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
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
                    Log.e(TAG, "disconnected", e)
                    break
                }
            }
        }
      
        public fun write(buffer: ByteArray) {
            try {
                mmOutStream!!.write(buffer)

            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        public fun cancel() {
            try {
                mmSocket.close()
            } catch (e:IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    private inner class ConnectThread (device: BluetoothDevice, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmDevice: BluetoothDevice
        private val mSocketType: String

        init {
            Log.d(TAG, "createRfcommSocketToServiceRecord called.")
            mmDevice = device
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"
        
            try {
                if (secure) tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
                else tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE)
            
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: $mSocketType create() failed", e)
            }
            
            mmSocket = tmp
            mState = STATE_CONNECTING
        }
      
        public override fun run() {
             Log.d(TAG, "connect called.")
            mAdapter.cancelDiscovery()
        
            try {
                mmSocket!!.connect()
        
            } catch (e1: IOException){
                try {
                    mmSocket!!.close()
                
                } catch (e2: IOException){
                    Log.e(TAG, ("unable to close() $mSocketType socket during connection failure"), e2)
                }
                // connectionFailed()
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
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e)
            }
        }
    }
}