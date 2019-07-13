package com.example.labirintumapp

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.widget.*
import java.util.*

class MainActivity : AppCompatActivity() , SensorEventListener {
    private lateinit var txtEixoX: TextView
    private lateinit var txtEixoY: TextView      
    private lateinit var txtEixoZ: TextView      
    private lateinit var btnIniciar: TextView      
    private lateinit var mainLayout: RelativeLayout 

    private var sensorManager: SensorManager? = null
    private var firstIter: Boolean = false
    private var eixoX0: Double = 0.0
    private var eixoY0: Double = 0.0
    private var eixoZ0: Double = 0.0
    private var eixoX: Double = 0.0
    private var eixoY: Double = 0.0
    private var eixoZ: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)

        txtEixoX = findViewById(R.id.txtEixoX)
        txtEixoY = findViewById(R.id.txtEixoY)
        txtEixoZ = findViewById(R.id.txtEixoZ)
        btnIniciar = findViewById(R.id.btnIniciar)
        mainLayout = findViewById(R.id.main_layout)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)
       

        btnIniciar.setOnClickListener {
            val bSetFilename = AlertDialog.Builder(this)
            bSetFilename.setTitle("Qual é o nome do arquivo?")

            // inflate layout (?)
            val input = EditText(this)
            val lp2 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
            lp2.leftMargin = 20
            input.layoutParams = lp2
            bSetFilename.setView(input)

            bSetFilename.setPositiveButton("Iniciar", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    var filename = input.text.toString()
                    if (!filename.endsWith(".csv", true)) filename += ".csv"
                    iniciarGravacao(filename, "csv", "N")
                }
            })

            bSetFilename.show()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> Toast.makeText(this, "Settings", Toast.LENGTH_LONG).show()
            R.id.action_remote -> defineRemoteLayout()
            // R.id.action_speech -> defineSpeechLayout()
        }

        return true
    }

    fun iniciarGravacao(fname: String, ftype: String, gmode: String){
        val intent = Intent(applicationContext, MenuGravacao::class.java)
        intent.putExtra("fileName", fname)
        intent.putExtra("fileType", ftype)
        intent.putExtra("recMode", gmode)
        startActivity(intent)
        finish()
    }

    private fun defineRemoteLayout(){
        Toast.makeText(this, "O modo remoto foi ativado!", Toast.LENGTH_SHORT).show()
        mainLayout.removeView(btnIniciar)
        iniciarGravacao("bluetoothRec", "csv", "R")
    }


}