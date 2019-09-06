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
import android.view.View
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat

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

    private lateinit var appSensorManager: SensorManager
    private var flagInit = false
    private var eixoX0 = 0.0
    private var eixoY0 = 0.0
    private var eixoZ0 = 0.0
    private var eixoX = 0.0
    private var eixoY = 0.0
    private var eixoZ = 0.0

    private var numLinhasMaximo = 0
    private var intervaloGravacao = 0
    private var extensaoArquivo = ""

    companion object {
        private val MY_PERMISSIONS_REQUEST_WRITE = 1;
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)
        this.txtEixoX = findViewById(R.id.txtEixoX)
        this.txtEixoY = findViewById(R.id.txtEixoY)
        this.txtEixoZ = findViewById(R.id.txtEixoZ)
        this.btnIniciar = findViewById(R.id.btnIniciar)
        this.mainLayout = findViewById(R.id.main_layout)

        this.appSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        this.appSensorManager.registerListener(this, 
            this.appSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)

        this.btnIniciar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                val bDefineNomeArquivo = AlertDialog.Builder(this@MainActivity)
                bDefineNomeArquivo.setTitle("Qual é o nome do arquivo?")

                val input = EditText(this@MainActivity)
                val lp2 = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
                input.layoutParams = lp2
                input.hint = "Insira o nome do arquivo"
                bDefineNomeArquivo.setView(input, 20, 0, 20, 0)

                bDefineNomeArquivo.setPositiveButton("Iniciar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, id: Int) {
                        var nomeArquivo = input.text.toString()
                        val diretorioGravacao = this@MainActivity.geraDiretorioGravacao(nomeArquivo)
                        this@MainActivity.iniciarGravacao(diretorioGravacao, MenuGravacao.MODO_PADRAO)
                    }
                })
                bDefineNomeArquivo.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        dialog.cancel()
                    }
                })
                bDefineNomeArquivo.show()
            }
        })

        this.atualizaPadroes()
    }

    private fun atualizaPadroes(){
        val intentRecebido = intent
        if (intentRecebido == null) return

        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        val prefEditor = pref.edit()

        if (pref.getBoolean("first_run", true)) {
            prefEditor.putBoolean("first_run", false)
            prefEditor.putInt("max_lines", 120)
            prefEditor.putInt("rec_delay", 500)
            prefEditor.putString("file_type", "csv")
            prefEditor.commit()
            return
        }

        if (intentRecebido.getStringExtra("act_name") == "MenuSettings"){
            if (intentRecebido.getStringExtra("user_action") == "salvar"){
                prefEditor.putInt("max_lines", intentRecebido.getStringExtra("max_lines").toInt())
                prefEditor.putInt("rec_delay", intentRecebido.getStringExtra("rec_delay").toInt())
                prefEditor.putString("file_type", intentRecebido.getStringExtra("file_type"))
                prefEditor.commit()
            }
        }

        numLinhasMaximo = pref.getInt("max_lines", 120)
        intervaloGravacao = pref.getInt("rec_delay", 500)
        extensaoArquivo = pref.getString("file_type", "csv") ?: return
    }

    override fun onResume(){
        super.onResume()

        this.appSensorManager.registerListener(this, 
            appSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)

        this.atualizaPadroes()
    }

    override fun onPause(){
        super.onPause()
        appSensorManager.unregisterListener(this)
    }

    override fun onDestroy(){
        super.onDestroy()
        appSensorManager.unregisterListener(this)
    }

    private fun geraDiretorioGravacao(nomeArq: String): String {
        var nomeArquivo = nomeArq
        val diretorioPai = "${Environment.getExternalStorageDirectory().path}/AccelerometerSaveData"
        var intCont = 0
        var strCont = ""
        val sufixo = nomeArquivo.takeLast(2)

        if (nomeArquivo.endsWith(extensaoArquivo))
            nomeArquivo.dropLast(extensaoArquivo.length)

        if (isNumerico(sufixo)) {
            intCont = sufixo.toInt()
            strCont = "-%02d".format(intCont)
            nomeArquivo = nomeArquivo.dropLastWhile{ it.isDigit() }
            nomeArquivo = nomeArquivo.dropLast(1)
        }

        val pasta = File(diretorioPai)
        if (!pasta.exists()){
            pasta.mkdirs()
        }

        var diretorioArquivo: String
        while (true) {
            diretorioArquivo = "${diretorioPai}/${nomeArquivo}${strCont}.${extensaoArquivo}"
            var arquivo = File(diretorioArquivo)

            if (arquivo.isFile()){
                intCont += 1
                strCont = "-%02d".format(intCont)
            } else return diretorioArquivo
        }
    }

    override fun onAccuracyChanged(arg0: Sensor, arg1: Int){ }

    override fun onSensorChanged(event: SensorEvent){
        val eixoX1 = event.values[0].toDouble()
        val eixoY1 = event.values[1].toDouble()
        val eixoZ1 = event.values[2].toDouble()

        if (!this.flagInit){
            this.eixoX0 = eixoX1
            this.eixoY0 = eixoY1
            this.eixoZ0 = eixoZ1
            
            this.flagInit = true
        }
        else{
            this.eixoX = Math.abs(this.eixoX0 - eixoX1)
            this.eixoY = Math.abs(this.eixoY0 - eixoY1)
            this.eixoZ = Math.abs(this.eixoZ0 - eixoZ1)

            this.eixoX0 = eixoX1
            this.eixoY0 = eixoY1
            this.eixoZ0 = eixoZ1

            this.txtEixoX.text = String.format("%.2f m/s²", this.eixoX).replace(',', '.')
            this.txtEixoY.text = String.format("%.2f m/s²", this.eixoY).replace(',', '.')
            this.txtEixoZ.text = String.format("%.2f m/s²", this.eixoZ).replace(',', '.')
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> defineLayoutConfiguracoes()
            R.id.action_remote -> defineLayoutRemoto()
        }

        return true
    }

    private fun iniciarGravacao(diretorioArquivo: String, modoGravacao: Int){
        if (isPermissaoEscrita()){
            val intent = Intent(applicationContext, MenuGravacao::class.java)
            intent.putExtra("act_name", "MainActivity")
            intent.putExtra("file_path", diretorioArquivo)
            intent.putExtra("rec_mode", modoGravacao)
            intent.putExtra("file_type", this.extensaoArquivo)
            intent.putExtra("max_lines", this.numLinhasMaximo)
            intent.putExtra("rec_delay", this.intervaloGravacao)
            this.appSensorManager.unregisterListener(this)
            this.startActivity(intent)
            this.finish()
        } else verificarPermissaoEscrita()
    }

    private fun defineLayoutRemoto(){
        Toast.makeText(this, "O modo remoto foi ativado!", Toast.LENGTH_SHORT).show()
        mainLayout.removeView(this.btnIniciar)
        val diretorioGravacao = this.geraDiretorioGravacao("bluetoothRec")
        this.iniciarGravacao(diretorioGravacao, MenuGravacao.MODO_REMOTO)
    }

    private fun defineLayoutConfiguracoes(){
        val intent = Intent(applicationContext, MenuSettings::class.java)
        intent.putExtra("act_name", "MainActivity")
        intent.putExtra("file_type", this.extensaoArquivo.toString())
        intent.putExtra("max_lines", this.numLinhasMaximo.toString())
        intent.putExtra("rec_delay", this.intervaloGravacao.toString())
        this.startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Não foi possível ativar a " + 
                        "funcionalidade de escrita.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            else -> { }       
        }
    }

    private fun isPermissaoEscrita(): Boolean {
        var permissaoEscrita = Manifest.permission.WRITE_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, permissaoEscrita) == PackageManager.PERMISSION_GRANTED
    }

    private fun verificarPermissaoEscrita(){
        ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE)
    }
}