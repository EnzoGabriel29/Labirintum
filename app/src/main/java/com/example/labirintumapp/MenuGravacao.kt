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
    init {
        // layout init
        private val txtEixoX: TextView        = findViewById(R.id.txtEixoX)
        private val txtEixoY: TextView        = findViewById(R.id.txtEixoY)
        private val txtEixoZ: TextView        = findViewById(R.id.txtEixoZ)
        private val tabelaDados: TableLayout  = findViewById(R.id.tabelaDados)
        private val recLayout: RelativeLayout = findViewById(R.id.rec_layout)
        private val conLayout: LinearLayout   = findViewById(R.id.controll_layout)
        private val btnPausar: TextView       = findViewById(R.id.btnPausar)
        private val btnParar: TextView        = findViewById(R.id.btnParar)

        var btnPausarMode = 'P'
        btnPausar.setOnClickListener {
            if (btnPausarMode == 'P') {
                handler.removeCallbacksAndMessages(null)
                btnPausarMode = 'C'
                btnPausar.text = "Continuar"
            }
            else {
                ativarHandler(handler, numIntervalo)
                btnPausarMode = 'P'
                btnPausar.text = "Pausar"
            }
        }

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
        private var filepath: String = ""
        private var recmode: String  = ""
        private lateinit var usertype: String

        // bluetooth init
        private lateinit var patientDevice: BluetoothDevice
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_gravacao)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)

        val intentMain = intent
        filename = intentMain.getStringExtra("fileName")
        filetype = intentMain.getStringExtra("fileType")
        recmode = intentMain.getStringExtra("recMode")
        fileparent = Environment.getExternalStorageDirectory().path

        // initialize usertype variable
        val bChooseUser = AlertDialog.Builder(this)
        val optionsUsers = arrayOf("Dispositivo do terapeuta", "Dispositivo do paciente")
        bChooseUser.setTitle("Qual dispositivo você está usando?")
        bChooseUser.setItems(optionsUsers, object : DialogInterface.OnClickListener() {
            override fun onClick(dialog: DialogInterface, which: Int) {
                usertype = if (which == 0) "root" else "user"
                bChooseUser.cancel()
            }
        })
        bChooseUser.setNegativeButton(getString(R.string.cancel), object : DialogInterface.OnClickListener() {
            override fun onClick(dialog: DialogInterface, which: Int) {
                bChooseUser.cancel()
            }
        })
        bChooseUser.show()

        // handle filename already exists
        var cont = 0
        while (true) {
            var filepath = "$fileparent/$filename.$filetype"
            var fileObj = File(filepath)

            if (fileObj.isFile()){
                cont += 1
                filename += "$cont"
            }
            else break
        }

        // create layout based on recmode/usertype
        if (recmode == "N")
            createDefaultLayout()
        else if (recmode == "R" && usertype == "user")
            createRemoteUserLayout()
        else if (recmode == "R" && usertype == "root")
            createRemoteRootLayout()

        Toast.makeText(this, "O registro dos dados foi iniciado! " +
                "O arquivo será salvo em $filepath.", Toast.LENGTH_LONG).show()

    }

    private fun createDefaultLayout(){
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

        var optionsDevices = emptyArray<String>()
        for (device in mPairedDevices) 
            optionsDevices += device.name
                    
        bChoosePaired.setTitle("Dispositivos pareados")
        bChoosePaired.setItems(optionsDevices, object : DialogInterface.OnClickListener() {
            override fun onClick(dialog: DialogInterface, which: Int) {
                patientDevice = mPairedDevices[which]
                BluetoothClient(patientDevice, "1").start()
                bChoosePaired.cancel()
            }
        })

        bChoosePaired.show()

        btnParar.setOnClickListener {
            BluetoothClient(patientDevice, "0")
        }
    }

    public fun iniciarGravacao(){
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
                escreverCSV(acc, rewrite)

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