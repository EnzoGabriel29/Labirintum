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
import android.os.Build
import android.preference.PreferenceManager
import android.content.Context
import java.io.File
import android.os.Environment

fun isNumerico(str: String): Boolean {
    try {
        str.toInt()
        return true
    } catch (e: NumberFormatException) {
        return false
    }
}


class MainActivity : AppCompatActivity() , SensorEventListener {
    private lateinit var txtEixoX: TextView
    private lateinit var txtEixoY: TextView      
    private lateinit var txtEixoZ: TextView      
    private lateinit var btnIniciar: TextView
    private lateinit var mainLayout: RelativeLayout 

    private lateinit var sensorManager: SensorManager
    private var firstIter: Boolean = false
    private var eixoX0: Double = 0.0
    private var eixoY0: Double = 0.0
    private var eixoZ0: Double = 0.0
    private var eixoX: Double = 0.0
    private var eixoY: Double = 0.0
    private var eixoZ: Double = 0.0

    private var maxlines: Int    = 0
    private var recdelay: Int    = 0
    private var filetype: String = "0"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)
        txtEixoX = findViewById(R.id.txtEixoX)
        txtEixoY = findViewById(R.id.txtEixoY)
        txtEixoZ = findViewById(R.id.txtEixoZ)
        btnIniciar = findViewById(R.id.btnIniciar)
        mainLayout = findViewById(R.id.main_layout)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val ds = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, ds, SensorManager.SENSOR_DELAY_NORMAL)

        btnIniciar.setOnClickListener {
            val bSetFilename = AlertDialog.Builder(this)
            bSetFilename.setTitle("Qual é o nome do arquivo?")

            val input = EditText(this)
            val lp2 = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
            input.layoutParams = lp2
            input.hint = "Insira o nome do arquivo"
            bSetFilename.setView(input, 20, 0, 20, 0)

            bSetFilename.setPositiveButton("Iniciar", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, id: Int) {
                    var filename = input.text.toString()
                    iniciarGravacao(getSavePath(filename), "N")
                }
            })
            bSetFilename.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.cancel()
                }
            })
            bSetFilename.show()
        }

        updateDefaults()
    }

    private fun updateDefaults(){
        val intentReceiver = intent
        if (intentReceiver == null) return

        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        val prefEditor = pref.edit()

        // se o aplicativo esta sendo executado pela primeira vez
        if (pref.getBoolean("first_run", true)) {
            prefEditor.putBoolean("first_run", false)
            prefEditor.putInt("max_lines", 120)
            prefEditor.putInt("rec_delay", 500)
            prefEditor.putString("file_type", "csv")
            prefEditor.commit()
            return
        }

        // se algum dado foi salvo na activity 'MenuSettings'
        if (intentReceiver.getStringExtra("act_name") == "MenuSettings"){
            if (intentReceiver.getStringExtra("user_action") == "salvar"){
                prefEditor.putInt("max_lines", intentReceiver.getStringExtra("max_lines").toInt())
                prefEditor.putInt("rec_delay", intentReceiver.getStringExtra("rec_delay").toInt())
                prefEditor.putString("file_type", intentReceiver.getStringExtra("file_type"))
                prefEditor.commit()
            }
        }

        maxlines = pref.getInt("max_lines", 120)
        recdelay = pref.getInt("rec_delay", 500)
        filetype = pref.getString("file_type", "csv") ?: return
    }

    override fun onResume(){
        super.onResume()
        val ds = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, ds, SensorManager.SENSOR_DELAY_NORMAL)

        updateDefaults()
    }

    override fun onPause(){
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy(){
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    private fun getSavePath(fname: String): String {
        var filename = fname
        val parentDir = "${Environment.getExternalStorageDirectory().path}/AccelerometerSaveData"
        var intCont = 0
        var strCont = ""
        val sufixo = filename.takeLast(2)

        if (filename.endsWith(filetype))
            filename.dropLast(filetype.length)

        if (isNumerico(sufixo)) {
            intCont = sufixo.toInt()
            strCont = "-%02d".format(intCont)
            filename = filename.dropLastWhile{ it.isDigit() }
            filename = filename.dropLast(1)
        }

        var filepath: String
        while (true) {
            filepath = "$parentDir/$filename$strCont.$filetype"
            var fileObj = File(filepath)

            if (fileObj.isFile()){
                intCont += 1
                strCont = "-%02d".format(intCont)
            } else return filepath
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
            R.id.action_settings -> defineSettingsLayout()
            R.id.action_remote   -> defineRemoteLayout()
        }

        return true
    }

    private fun iniciarGravacao(fname: String, gmode: String){
        val intent = Intent(applicationContext, MenuGravacao::class.java)
        intent.putExtra("act_name", "MainActivity")
        intent.putExtra("file_path", fname)
        intent.putExtra("rec_mode", gmode)
        intent.putExtra("file_type", filetype)
        intent.putExtra("max_lines", maxlines)
        intent.putExtra("rec_delay", recdelay)
        sensorManager.unregisterListener(this)
        startActivity(intent)
        finish()
    }

    private fun defineRemoteLayout(){
        Toast.makeText(this, "O modo remoto foi ativado!", Toast.LENGTH_SHORT).show()
        mainLayout.removeView(btnIniciar)
        iniciarGravacao(getSavePath("bluetoothRec"), "R")
    }

    private fun defineSettingsLayout(){
        val intent = Intent(applicationContext, MenuSettings::class.java)
        intent.putExtra("act_name", "MainActivity")
        intent.putExtra("file_type", filetype.toString())
        intent.putExtra("max_lines", maxlines.toString())
        intent.putExtra("rec_delay", recdelay.toString())
        startActivity(intent)
    }
}