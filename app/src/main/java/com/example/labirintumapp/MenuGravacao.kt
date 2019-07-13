package com.example.labirintumapp

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
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
import java.io.File

class Acelerometro {
    var horario: String? = null
    var valorX: Double = 0.0
    var valorY: Double = 0.0
    var valorZ: Double = 0.0

    constructor (){ }

    constructor (Horario: String, ValorX: Double, ValorY: Double, ValorZ: Double){
        this.horario = Horario
        this.valorX = ValorX
        this.valorY = ValorY
        this.valorZ = ValorZ
    }
}

class MenuGravacao : AppCompatActivity() , SensorEventListener {
    
        // layout init
        private lateinit var txtEixoX: TextView        
        private lateinit var txtEixoY: TextView        
        private lateinit var txtEixoZ: TextView        
        private lateinit var tabelaDados: TableLayout 
        private lateinit var recLayout: RelativeLayout
        private lateinit var conLayout: LinearLayout  
        private lateinit var btnPausar: TextView      
        private lateinit var btnParar: TextView        

        // accelerometer init
        private var sensorManager: SensorManager? = null
        private var firstIter: Boolean = false
        private var eixoX0: Double     = 0.0
        private var eixoY0: Double     = 0.0
        private var eixoZ0: Double     = 0.0
        private var eixoX: Double      = 0.0
        private var eixoY: Double      = 0.0
        private var eixoZ: Double      = 0.0
        private val handler: Handler = Handler()

        // save file init
        private var filename: String = ""
        private var filetype: String = ""
        private var recmode: String  = ""
        private var filepath: String = ""
        private var usertype: String = ""

        // bluetooth init
        private lateinit var patientDevice: BluetoothDevice
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_gravacao)

        txtEixoX = findViewById(R.id.txtEixoX)
        txtEixoY = findViewById(R.id.txtEixoY)
        txtEixoZ = findViewById(R.id.txtEixoZ)
        tabelaDados = findViewById(R.id.tabelaDados)
        recLayout = findViewById(R.id.rec_layout)
        conLayout = findViewById(R.id.controll_layout)
        btnPausar = findViewById(R.id.btnPausar)
        btnParar = findViewById(R.id.btnParar)

        var btnPausarMode = 'P'
        btnPausar.setOnClickListener {
            if (btnPausarMode == 'P') {
                handler.removeCallbacksAndMessages(null)
                btnPausarMode = 'C'
                btnPausar.text = "Continuar"
            }
            else {
                val numIntervalo = 0.5
                ativarHandler(numIntervalo)
                btnPausarMode = 'P'
                btnPausar.text = "Pausar"
            }
        }
        
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)

        val intentMain = intent
        filename = intentMain.getStringExtra("fileName")
        filetype = intentMain.getStringExtra("fileType")
        recmode = intentMain.getStringExtra("recMode")
        val fileparent = Environment.getExternalStorageDirectory().path

        if (recmode == "N")
            createDefaultLayout()
        else {
            // initialize usertype variable
            val bChooseUser = AlertDialog.Builder(this)
            val optionsUsers = arrayOf("Dispositivo do terapeuta", "Dispositivo do paciente")
            bChooseUser.setTitle("Qual dispositivo você está usando?")
            bChooseUser.setItems(optionsUsers, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    usertype = if (which == 0) "root" else "user"

                    Toast.makeText(applicationContext, "recmode: $recmode, usertype: $usertype", Toast.LENGTH_SHORT).show()

                    // create layout based on recmode/usertype
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
                }
            })
            bChooseUser.show()
        }

        // handle filename already exists
        var cont = 0
        while (true) {
            filepath = "$fileparent/$filename.$filetype"
            var fileObj = File(filepath)

            if (fileObj.isFile()){
                cont += 1
                filename += "$cont"
            }
            else break
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

        if (contDevice == 0)
            Toast.makeText(this, "ITS EMPTY", Toast.LENGTH_SHORT).show()
       
                    
        bChoosePaired.setTitle("Dispositivos pareados")
        bChoosePaired.setItems(optionsDevicesName, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                patientDevice = mBluetoothAdapter.getRemoteDevice(optionsDevicesAddress[which])
                BluetoothClient(patientDevice, "1").start()
                dialog.dismiss()
            }
        })

        bChoosePaired.show()

        btnParar.setOnClickListener {
            BluetoothClient(patientDevice, "0")
        }
    }

    public fun iniciarGravacao(){
        Toast.makeText(this, "O registro dos dados foi iniciado! " +
                "O arquivo será salvo em $filepath.", Toast.LENGTH_LONG).show()

        val numIntervalo = 0.5
        ativarHandler(numIntervalo)
    }

    public fun pararGravacao(){
        handler.removeCallbacksAndMessages(null)
        val intentMain2 = Intent(applicationContext, MainActivity::class.java)
        startActivity(intentMain2)
        finish()
    }

    private fun ativarHandler(int: Double) {
        val delay = int * 1000

        handler.postDelayed(object : Runnable {
            override fun run() {
                val c = Calendar.getInstance()
                var strData = ""
                strData += String.format("%02d", c.get(Calendar.DATE)) + "/"
                strData += String.format("%02d", c.get(Calendar.MONTH)) + " "
                strData += String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":"
                strData += String.format("%02d", c.get(Calendar.MINUTE)) + ":"
                strData += String.format("%02d", c.get(Calendar.SECOND))

                val acc = Acelerometro(strData, eixoX, eixoY, eixoZ)

                atualizarLista(tabelaDados, acc)
                escreverCSV(acc, false)

                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())
    }

    private fun atualizarLista(t: TableLayout, acc: Acelerometro) {
        val row = TableRow(applicationContext)
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
        col1.text = acc.horario
        row.addView(col1)

        val col2 = TextView(this)
        val trlp2 = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col2.gravity = Gravity.CENTER
        col2.layoutParams = trlp2
        col2.text = String.format("%.2f", eixoX).replace(',', '.')
        row.addView(col2)

        val col3 = TextView(this)
        val trlp3 = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col3.gravity = Gravity.CENTER
        col3.layoutParams = trlp3
        col3.text = String.format("%.2f", eixoY).replace(',', '.')
        row.addView(col3)

        val col4 = TextView(this)
        val trlp4 = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col4.gravity = Gravity.CENTER
        col4.layoutParams = trlp4
        col4.text = String.format("%.2f", eixoZ).replace(',', '.')
        row.addView(col4)

        t.addView(row)
    }

    private fun escreverCSV(acc: Acelerometro, rw: Boolean) {
        try {
            val writer = FileWriter(filepath, !rw)
            if (rw) writer.append("HORÁRIO,EIXO X,EIXOY,EIXOZ\n")

            val dadosList = arrayOf(acc.horario,
                String.format("%.2f", acc.valorX).replace(',', '.'),
                String.format("%.2f", acc.valorY).replace(',', '.'),
                String.format("%.2f", acc.valorZ).replace(',', '.'))

            val stringData = dadosList.joinToString(",")
            writer.append(stringData)
            writer.append('\n')
            writer.close()

        } catch (erMsg: Exception) {
            Toast.makeText(this, erMsg.toString(), Toast.LENGTH_LONG).show()
            erMsg.printStackTrace()
        }
    }

    override fun onAccuracyChanged(arg0: Sensor, arg1: Int){ }

    override fun onSensorChanged(event: SensorEvent){
        val eixoX1 = event.values[0].toDouble()
        val eixoY1 = event.values[1].toDouble()
        val eixoZ1 = event.values[2].toDouble()

        if (!firstIter){
            eixoX0 = eixoX1; eixoY0 = eixoY1; eixoZ0 = eixoZ1
            firstIter = true
        }
        else{
            eixoX = Math.abs(eixoX0 - eixoX1)
            eixoY = Math.abs(eixoY0 - eixoY1)
            eixoZ = Math.abs(eixoZ0 - eixoZ1)

            eixoX0 = eixoX1
            eixoY0 = eixoY1
            eixoZ0 = eixoZ1

            txtEixoX.text = String.format("%.2f m/s²", eixoX).replace(',', '.')
            txtEixoY.text = String.format("%.2f m/s²", eixoY).replace(',', '.')
            txtEixoZ.text = String.format("%.2f m/s²", eixoZ).replace(',', '.')
        }
    }
}