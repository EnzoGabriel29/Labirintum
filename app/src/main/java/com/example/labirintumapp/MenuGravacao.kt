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

    private lateinit var patientDevice: BluetoothDevice
    
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

        BluetoothServerController(this).start()
    }

    private fun createRemoteRootLayout(){
        val bChoosePaired = AlertDialog.Builder(this)
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val mPairedDevices = mBluetoothAdapter.getBondedDevices()

        var optionsDevicesName = emptyArray<String>()
        var optionsDevicesAddress = emptyArray<String>()
        var contDevice: Int = 0
        for (device in mPairedDevices){
            contDevice += 1
            optionsDevicesName += device.name
            optionsDevicesAddress += device.address
        }
  
        bChoosePaired.setTitle("Dispositivos pareados")
        bChoosePaired.setItems(optionsDevicesName, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                patientDevice = mBluetoothAdapter.getRemoteDevice(optionsDevicesAddress[which])
                escreverLog("ModoGravacao: Choosed device ${optionsDevicesName[which]}")
                BluetoothClient(this@MenuGravacao, patientDevice, "1").start()
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
                BluetoothClient(this@MenuGravacao, patientDevice, "2")
                btnPausar.text = "Continuar"
                btnPausarRootMode = "C"
            } else if (btnPausarRootMode == "C"){
                BluetoothClient(this@MenuGravacao, patientDevice, "3")
                btnPausar.text = "Pausar"
                btnPausarRootMode = "P"
            }
        }

        btnParar.setOnClickListener {
            BluetoothClient(this@MenuGravacao, patientDevice, "0")
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
        val intentSender = Intent(applicationContext, MainActivity::class.java)
        intentSender.putExtra("act_name", "MenuGravacao")
        startActivity(intentSender)
        finish()
    }

    override fun onDestroy(){
        handler.removeCallbacksAndMessages(null)
        sensorManagerRec.unregisterListener(this)
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
}