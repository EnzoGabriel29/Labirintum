package com.example.labirintumapp

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.FileWriter
import java.util.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.os.Build
import android.os.Environment
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import java.nio.charset.Charset
import java.util.UUID

fun escreverLog(msg: String){
    val filepath = Environment.getExternalStorageDirectory().path + "/log.txt"
    val writer = FileWriter(filepath, true)
    val c = Calendar.getInstance()
    var strData = ""
    strData += String.format("%02d", c.get(Calendar.DATE)) + "/"
    strData += String.format("%02d", c.get(Calendar.MONTH)) + " "
    strData += String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":"
    strData += String.format("%02d", c.get(Calendar.MINUTE)) + ":"
    strData += String.format("%02d", c.get(Calendar.SECOND))
    
    val logString = "$strData - $msg"
    writer.append(logString)
    writer.append('\n')
    writer.close()
}

class Acelerometro {
    var horario: String? = null
    var valorX: Double = 0.0
    var valorY: Double = 0.0
    var valorZ: Double = 0.0

    constructor () { }

    constructor(hor: String, val_x: Double, val_y: Double, val_z: Double){
        this.horario = hor
        this.valorX  = val_x
        this.valorY  = val_y
        this.valorZ  = val_z
    }

    public fun update(hor: String, val_x: Double, val_y: Double, val_z: Double) {
        this.horario = hor
        this.valorX  = val_x
        this.valorY  = val_y
        this.valorZ  = val_z
    }
}

class MenuGravacao : AppCompatActivity() , SensorEventListener {    
    private lateinit var txtEixoX: TextView        
    private lateinit var txtEixoY: TextView        
    private lateinit var txtEixoZ: TextView        
    private lateinit var tabelaDados: TableLayout 
    private lateinit var recLayout: RelativeLayout
    private lateinit var conLayout: LinearLayout  
    private lateinit var btnPausar: TextView      
    private lateinit var btnParar: TextView 

    private var eixoX0: Double = 0.0
    private var eixoY0: Double = 0.0
    private var eixoZ0: Double = 0.0
    private var eixoX: Double = 0.0
    private var eixoY: Double = 0.0
    private var eixoZ: Double = 0.0
    private var firstIter: Boolean = false
    private val handler: Handler = Handler()
    private var accAtual: Acelerometro = Acelerometro()
    private lateinit var sensorManagerRec: SensorManager

    private var lineCont: Int = 0
    private var maxlines: Int = 0
    private var recdelay: Int = 0
    private var recmode: String = "" 
    private var filepath: String = ""  
    private var filetype: String = ""      
    private var usertype: String = ""
    private var maxLinesOn: Boolean = false
    private var header: Boolean = true

    private var bluetoothConnected = false
    private var broadcastsRegistered = arrayOf(false, false, false, false)
    private var mBluetoothConnection: BluetoothConnectionService? = null
    private val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var myDevice: BluetoothDevice
    private var listDevices = emptyArray<BluetoothDevice>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_gravacao)

        txtEixoX    = findViewById(R.id.txtEixoX)
        txtEixoY    = findViewById(R.id.txtEixoY)
        txtEixoZ    = findViewById(R.id.txtEixoZ)
        tabelaDados = findViewById(R.id.tabelaDados)
        recLayout   = findViewById(R.id.rec_layout)
        conLayout   = findViewById(R.id.controll_layout)
        btnPausar   = findViewById(R.id.btnPausar)
        btnParar    = findViewById(R.id.btnParar)

        var btnPausarMode = 'P'
        btnPausar.setOnClickListener {
            if (btnPausarMode == 'P') {
                pausarGravacao()
                btnPausarMode = 'C'
                btnPausar.text = "Continuar"
            }
            else {
                retomarGravacao()
                btnPausarMode = 'P'
                btnPausar.text = "Pausar"
            }
        }
        
        sensorManagerRec = getSystemService(SENSOR_SERVICE) as SensorManager
        val ds = sensorManagerRec.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManagerRec.registerListener(this, ds, SensorManager.SENSOR_DELAY_NORMAL)

        val intentReceiver = intent
        recmode = intentReceiver.getStringExtra("rec_mode")
        filepath = intentReceiver.getStringExtra("file_path")
        filetype = intentReceiver.getStringExtra("file_type")
        maxlines = intentReceiver.getIntExtra("max_lines", 120)
        recdelay = intentReceiver.getIntExtra("rec_delay", 500)

        maxLinesOn = maxlines > 0

        if (recmode == "N")
            createDefaultLayout()
        else {

            val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            registerReceiver(mBroadcastReceiver4, filter)
            broadcastsRegistered[3] = true


            if (mBluetoothAdapter == null){
                Toast.makeText(this, "O seu dispositivo não " +
                    "possui o recurso de Bluetooth.", Toast.LENGTH_LONG)
                pararGravacao()
            }
            
            if (!mBluetoothAdapter.isEnabled()){
                Toast.makeText(this, "Ativando o Bluetooth...", Toast.LENGTH_SHORT)
                
                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivity(enableBTIntent)
                
                val BTIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                registerReceiver(mBroadcastReceiver1, BTIntent)
                broadcastsRegistered[0] = true 

                Toast.makeText(this, "Bluetooth ativado!", Toast.LENGTH_SHORT)
            }

            val bChooseUser = AlertDialog.Builder(this)
            val optionsUsers = arrayOf("Dispositivo do terapeuta", "Dispositivo do paciente")
            bChooseUser.setTitle("Qual dispositivo você está usando?")
            bChooseUser.setItems(optionsUsers, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    usertype = if (which == 0) "root" else "user"

                    escreverLog("MenuGravacao: Choosed $usertype mode")

                    if (recmode == "N")
                        createDefaultLayout()
                    else if (recmode == "R" && usertype == "user")
                        createRemoteUserLayout()
                    else if (recmode == "R" && usertype == "root")
                        createRemoteRootLayout()

                    dialog.cancel()
                }
            })
            bChooseUser.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.cancel()
                    pararGravacao()
                }
            })
            bChooseUser.show()
        }
    }

    private fun createDefaultLayout(){
        iniciarGravacao()
        btnParar.setOnClickListener {
            pararGravacao()
        }
    }

    private fun createRemoteUserLayout(){
        recLayout.removeView(conLayout)
        recLayout.removeView(btnPausar)
        recLayout.removeView(btnParar)

        escreverLog("btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.")
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        startActivity(discoverableIntent)
        // aki
        val intentFilter = IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        registerReceiver(mBroadcastReceiver2, intentFilter)
        broadcastsRegistered[1] = true
    }

    private fun createRemoteRootLayout(){
        val bChoosePaired = AlertDialog.Builder(this)
        escreverLog("btnDiscover: Looking for unpaired devices.")
        checkBTPermissions()

        /*
        mBluetoothAdapter.startDiscovery()
        val discoverDevicesIntent = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mBroadcastReceiver3, discoverDevicesIntent)
        broadcastsRegistered[2] = true

        var optionsDevicesName = emptyArray<String>()
        for (device in listDevices) {
            optionsDevicesName += device.name
        }
        */
        val mPairedDevices = mBluetoothAdapter.getBondedDevices()

        var optionsDevicesName = emptyArray<String>()
        var optionsDevicesAddress = emptyArray<String>()
        var contDevice: Int = 0
        for (device in mPairedDevices){
            contDevice += 1
            optionsDevicesName += device.name
            optionsDevicesAddress += device.address
            listDevices += device
        }

        bChoosePaired.setTitle("Dispositivos pareados")
        bChoosePaired.setItems(optionsDevicesName, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                mBluetoothAdapter.cancelDiscovery()

                escreverLog("onItemClick: You Clicked on a device.")
                val deviceName = listDevices[which].name
                val deviceAddress = listDevices[which].address

                escreverLog("onItemClick: deviceName = $deviceName")
                escreverLog("onItemClick: deviceAddress = $deviceAddress")

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    escreverLog("Trying to pair with $deviceName")
                    listDevices[which].createBond()
                    myDevice = listDevices[which]
                    mBluetoothConnection = BluetoothConnectionService(this@MenuGravacao)
                }

                bluetoothConnected = true
                escreverLog("startBTConnection: Initializing RFCOM Bluetooth Connection.")
                mBluetoothConnection!!.startClient(myDevice, MY_UUID_INSECURE)
                mBluetoothConnection!!.write("1".toByteArray(Charset.defaultCharset()))

                dialog.dismiss()
            }
        })
        bChoosePaired.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
                pararGravacao()
            }
        })
        bChoosePaired.show()

        var btnPausarRootMode = "P"
        btnPausar.setOnClickListener {
            if (btnPausarRootMode == "P") {
                mBluetoothConnection!!.write("2".toByteArray(Charset.defaultCharset()))
                btnPausar.text = "Continuar"
                btnPausarRootMode = "C"
            } else if (btnPausarRootMode == "C"){
                mBluetoothConnection!!.write("3".toByteArray(Charset.defaultCharset()))
                btnPausar.text = "Pausar"
                btnPausarRootMode = "P"
            }
        }

        btnParar.setOnClickListener {
            mBluetoothConnection!!.write("0".toByteArray(Charset.defaultCharset()))
        }
    }

    public fun iniciarGravacao(){
        Toast.makeText(this, "O registro dos dados foi iniciado! " +
                "O arquivo será salvo em $filepath.", Toast.LENGTH_LONG).show()

        val c = Calendar.getInstance()
        val strData = String.format("%02d/%02d %02d:%02d:%02d", c.get(Calendar.DATE),
            c.get(Calendar.MONTH)+1, c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE), c.get(Calendar.SECOND))

        accAtual.update(strData, eixoX, eixoY, eixoZ)

        ativarHandler(recdelay)
    }

    public fun pararGravacao(){
        if (recmode == "R" && bluetoothConnected){
            mBluetoothConnection!!.cancel()
        }

        val intentSender = Intent(applicationContext, MainActivity::class.java)
        intentSender.putExtra("act_name", "MenuGravacao")
        startActivity(intentSender)
        finish()
    }

    override fun onDestroy(){
        handler.removeCallbacksAndMessages(null)
        sensorManagerRec.unregisterListener(this)

        if (recmode == "R"){
            if (broadcastsRegistered[0])
                unregisterReceiver(mBroadcastReceiver1)
            if (broadcastsRegistered[1])
                unregisterReceiver(mBroadcastReceiver2)
            if (broadcastsRegistered[2])
                unregisterReceiver(mBroadcastReceiver3)
            if (broadcastsRegistered[3])
                unregisterReceiver(mBroadcastReceiver4)
        }

        super.onDestroy()
    }

    public fun pausarGravacao(){
        handler.removeCallbacksAndMessages(null)
    }

    public fun retomarGravacao(){
        ativarHandler(recdelay)
    }

    private fun ativarHandler(delay: Int){
        handler.postDelayed(object : Runnable {
            override fun run() {
                atualizarLista()
                escreverCSV()
                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())
    }

    private fun atualizarLista(){
        var row = TableRow(this)

        val layParams = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        row.weightSum = 4f
        row.layoutParams = layParams

        val col1 = TextView(this)
        val trlp1 = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col1.layoutParams = trlp1
        col1.gravity = Gravity.CENTER
        col1.text = accAtual.horario
        row.addView(col1)

        val col2 = TextView(this)
        val trlp2 = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col2.gravity = Gravity.CENTER
        col2.layoutParams = trlp2
        col2.text = String.format("%.2f", accAtual.valorX).replace(',', '.')
        row.addView(col2)

        val col3 = TextView(this)
        val trlp3 = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col3.gravity = Gravity.CENTER
        col3.layoutParams = trlp3
        col3.text = String.format("%.2f", accAtual.valorY).replace(',', '.')
        row.addView(col3)

        val col4 = TextView(this)
        val trlp4 = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col4.gravity = Gravity.CENTER
        col4.layoutParams = trlp4
        col4.text = String.format("%.2f", accAtual.valorZ).replace(',', '.')
        row.addView(col4)

        tabelaDados.addView(row)
    }

    private fun escreverCSV(){
        try {
            val writer = FileWriter(filepath, !header)
            if (header){
                writer.append("HORARIO,EIXO X,EIXO Y,EIXO Z\n")
                header = false
            }

            val dadosList = arrayOf(accAtual.horario,
                String.format("%.2f", accAtual.valorX).replace(',', '.'),
                String.format("%.2f", accAtual.valorY).replace(',', '.'),
                String.format("%.2f", accAtual.valorZ).replace(',', '.'))

            val stringData = dadosList.joinToString(",")
            writer.append(stringData)
            writer.append('\n')
            writer.close()

            if (maxLinesOn) {
                lineCont += 1
                if (lineCont == maxlines) {
                    Toast.makeText(this, "O limite máximo de" +
                         " gravação foi atingido!", Toast.LENGTH_LONG).show()
                    pararGravacao()
                }
            }

        } catch (erMsg: Exception) {
            Toast.makeText(this, erMsg.toString(), Toast.LENGTH_LONG).show()
            erMsg.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int){

    }

    override fun onSensorChanged(event: SensorEvent){
        val eixoX1 = event.values[0].toDouble()
        val eixoY1 = event.values[1].toDouble()
        val eixoZ1 = event.values[2].toDouble()
    
        if (!firstIter){
            eixoX0 = eixoX1
            eixoY0 = eixoY1
            eixoZ0 = eixoZ1
            firstIter = true

        } else {
            eixoX = Math.abs(eixoX0 - eixoX1)
            eixoY = Math.abs(eixoY0 - eixoY1)
            eixoZ = Math.abs(eixoZ0 - eixoZ1)
    
            eixoX0 = eixoX1
            eixoY0 = eixoY1
            eixoZ0 = eixoZ1
            
            val c = Calendar.getInstance()
            val strData = String.format("%02d/%02d %02d:%02d:%02d", c.get(Calendar.DATE),
                c.get(Calendar.MONTH)+1, c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE), c.get(Calendar.SECOND))

            accAtual.update(strData, eixoX, eixoY, eixoZ)
            txtEixoX.text = String.format("%.2f m/s²", accAtual.valorX).replace(',', '.')
            txtEixoY.text = String.format("%.2f m/s²", accAtual.valorY).replace(',', '.')
            txtEixoZ.text = String.format("%.2f m/s²", accAtual.valorZ).replace(',', '.')
        }
    }

    // broadcasts
    private val mBroadcastReceiver1 = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getAction()

            // When discovery finds a device sqr(aki)
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED){
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_OFF -> escreverLog("onReceive: STATE OFF")
                    BluetoothAdapter.STATE_TURNING_OFF -> escreverLog("mBroadcastReceiver1: STATE TURNING OFF")
                    BluetoothAdapter.STATE_ON -> escreverLog("mBroadcastReceiver1: STATE ON")
                    BluetoothAdapter.STATE_TURNING_ON -> escreverLog("mBroadcastReceiver1: STATE TURNING ON")
                }
            }
        }
    }

    private val mBroadcastReceiver2 = object : BroadcastReceiver() {
        override fun onReceive (context: Context, intent: Intent) {
            val action = intent.getAction()
            if (action == BluetoothAdapter.ACTION_SCAN_MODE_CHANGED){
                val mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                when (mode) {
                    //Device is in Discoverable Mode
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> escreverLog("mBroadcastReceiver2: Discoverability Enabled.")
                    //Device not in discoverable mode
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE -> escreverLog("mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.")
                    BluetoothAdapter.SCAN_MODE_NONE -> escreverLog("mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.")
                    BluetoothAdapter.STATE_CONNECTING -> escreverLog("mBroadcastReceiver2: Connecting....")
                    BluetoothAdapter.STATE_CONNECTED -> escreverLog("mBroadcastReceiver2: Connected.")
                }
            }
        }
    }

    private val mBroadcastReceiver3 = object : BroadcastReceiver() {
        override fun onReceive (context: Context, intent: Intent) {
            val action = intent.getAction()
            escreverLog("onReceive: ACTION FOUND.")
            if (action == BluetoothDevice.ACTION_FOUND){
                val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
                listDevices += device
                escreverLog("onReceive: ${device.name}: ${device.address}")
            }
        }
    }

    private val mBroadcastReceiver4 = object : BroadcastReceiver() {
        override fun onReceive (context: Context, intent: Intent) {
            val action = intent.getAction()
            if (action == BluetoothDevice.ACTION_BOND_STATE_CHANGED){
                val mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as BluetoothDevice
                
                // Case 1: bonded already
                if (mDevice.getBondState() === BluetoothDevice.BOND_BONDED){
                    escreverLog("BroadcastReceiver: BOND_BONDED.")
                    myDevice = mDevice
                }
                // Case 2: creating a bond
                if (mDevice.getBondState() === BluetoothDevice.BOND_BONDING){
                    escreverLog("BroadcastReceiver: BOND_BONDING.")
                }
                // Case 3: Breaking a bond
                if (mDevice.getBondState() === BluetoothDevice.BOND_NONE){
                    escreverLog("BroadcastReceiver: BOND_NONE.")
                }
            }
        }
    }

    // verifica permissoes de bluetooth
    private fun checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            var permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION")
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
            if (permissionCheck != 0){
                this.requestPermissions(arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001) //Any number
            }
        }
        else {
            escreverLog("checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.")
        }
    }

    companion object {
        private val TAG = "MainActivity"
        private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }
}