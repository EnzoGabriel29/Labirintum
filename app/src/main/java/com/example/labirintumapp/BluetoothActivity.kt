val uuid = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")

class BluetoothServerController(activity: MainActivity) : Thread() {
    private var cancelled: Boolean
    private val serverSocket: BluetoothServerSocket?
    private val activity = activity
    private lateinit val socket: BluetoothSocketWrapper

    init {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            // create a server socket, identified by the uuid
            serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("test", uuid)
            cancelled = false
        } else {
            serverSocket = null
            cancelled = true
        }
    }

    override fun run() {
        var socket: BluetoothSocket

        while(true) {
            if (cancelled) break
            
            try {
                // once thread execution started, wait for the
                // client connections using accept() method
                socket = serverSocket!!.accept() }
            catch (e: IOException) { break }

            if (!cancelled && socket != null) {
                Log.i("server", "Connecting")
                // once client established connection, accept()
                // method returns a BluetoothSocket reference
                // that gives access to the input and output streams
                BluetoothServer(activity, socket).start() 
            }
        }
    }

    fun cancel() {
        cancelled = true
        serverSocket!!.close()
    }
}

class BluetoothServer(act: Activity, soc: BluetoothSocket): Thread() {
    private val activity = act
    private val socket = soc
    private val inputStream = socket.inputStream
    private val outputStream = socket.outputStream
    var text: String

    override fun run() {
        try {
            val available = inputStream.available()
            val bytes = ByteArray(available)
            Log.i("server", "Reading")
            inputStream.read(bytes, 0, available)

            val text = String(bytes)
            when (text) {
                "1" -> activity.iniciarGravacao()
                "0" -> activity.pararGravacao()
            }

            Log.i("server", "Message received")
            Log.i("server", text)
            // Toast.makeText(activity, text, Toast.LENGHT_SHORT).show()
        } catch (e: Exception) {
            Log.e("client", "Cannot read data", e)
        } finally {
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}

class BluetoothClient(device: BluetoothDevice, msg: String): Thread() {
    private val socket = device.createRfcommSocketToServiceRecord(uuid)
    private val message = msg

    override fun run() {
        Log.i("client", "Connecting")
        socket.connect()

        Log.i("client", "Sending")
        val outputStream = socket.outputStream
        val inputStream = socket.inputStream
        
        try {
            outputStream.write(message.toByteArray())
            outputStream.flush()
            Log.i("client", "Sent")
        } catch(e: Exception) {
            Log.e("client", "Cannot send", e)
        } finally {
            outputStream.close()
            inputStream.close()
            socket.close()
        }
    }
}