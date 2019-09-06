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
import android.view.Gravity
import android.widget.TextView
import android.widget.TableLayout
import android.widget.RelativeLayout
import android.widget.LinearLayout
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import java.io.FileWriter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.os.Build
import android.os.Environment
import android.Manifest
import android.content.Context
import android.content.IntentFilter
import java.nio.charset.Charset
import android.view.View
import java.util.*
import android.widget.*

class Acelerometro {
    public var horario: String?
    public var valorX: Double
    public var valorY: Double
    public var valorZ: Double

    init {
        this.horario = null
        this.valorX = 0.0
        this.valorY = 0.0
        this.valorZ = 0.0
    }

    constructor(){ }

    constructor(hor: String, val_x: Double, val_y: Double, val_z: Double){
        this.atualizaValores(hor, val_x, val_y, val_z)
    }

    public fun atualizaValores(hor: String, val_x: Double, val_y: Double, val_z: Double) {
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

    private var eixoX = 0.0
    private var eixoY = 0.0
    private var eixoZ = 0.0
    private var eixoX0 = 0.0
    private var eixoY0 = 0.0
    private var eixoZ0 = 0.0
    private var flagInit = false
    private val handler = Handler()
    private var accAtual = Acelerometro()
    
    private var linhasRegistradas = 0
    private var numLinhasMaximo = 0
    private var intervaloGravacao = 0
    private var modoGravacao = 0 
    private var diretorioArquivo = ""  
    private var extensaoArquivo = ""      
    private var tipoUsuario = 0
    private var isCabecalho = true
    private var isLimiteLinhas = false

    companion object {
        private val REQUEST_ENABLE_BT = 1
        public val MODO_PADRAO = 2
        public val MODO_REMOTO = 3
        public val USER_TERAPEUTA = 4
        public val USER_PACIENTE = 5
        private val BOTAO_PAUSAR = 6
        private val BOTAO_CONTINUAR = 7
    }

    private val adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter()
    private lateinit var progressDialog: AlertDialog
    private var builder: AlertDialog.Builder? = null
    private lateinit var appSensorManager: SensorManager
    private lateinit var bluetoothConnector: BluetoothConnector
    
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

        var modoBtnPausar = BOTAO_PAUSAR
        this.btnPausar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                if (modoBtnPausar == BOTAO_PAUSAR){
                    this@MenuGravacao.pausarGravacao()
                    modoBtnPausar = BOTAO_CONTINUAR
                    btnPausar.text = "Continuar"
                } else {
                    this@MenuGravacao.retomarGravacao()
                    modoBtnPausar = BOTAO_PAUSAR
                    btnPausar.text = "Pausar"
                }
            }
        })
        
        this.appSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        this.appSensorManager.registerListener(this,
            this.appSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)

        val intentRecebido = intent
        this.modoGravacao = intentRecebido.getIntExtra("rec_mode", MODO_PADRAO)
        this.diretorioArquivo = intentRecebido.getStringExtra("file_path") ?: ""
        this.extensaoArquivo = intentRecebido.getStringExtra("file_type") ?: ""
        this.numLinhasMaximo = intentRecebido.getIntExtra("max_lines", 120)
        this.intervaloGravacao = intentRecebido.getIntExtra("rec_delay", 500)
        this.isLimiteLinhas = numLinhasMaximo > 0

        if (this.modoGravacao == MODO_PADRAO) criaLayoutPadrao()
        else {
            if (adaptadorBluetooth == null){
                Toast.makeText(this, "Seu dispositivo não suporta" +
                    "o recurso de Bluetooth.", Toast.LENGTH_LONG).show()
                this.pararGravacao()

            } else if (!adaptadorBluetooth.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }

            this.bluetoothConnector = BluetoothConnector(this)
            val bEscolheUsuario = AlertDialog.Builder(this)
            val modosUsuario = arrayOf("Dispositivo do terapeuta", "Dispositivo do paciente")
            bEscolheUsuario.setTitle("Qual dispositivo você está usando?")
            bEscolheUsuario.setCancelable(false)
            bEscolheUsuario.setItems(modosUsuario, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    tipoUsuario = if (which == 0) USER_TERAPEUTA else USER_PACIENTE

                    if (tipoUsuario == USER_TERAPEUTA){
                        bluetoothConnector.iniciaTerapeuta()

                        ProgressTask().execute()
                        criaLayoutTerapeuta()

                    } else mostraDispositivosPareados()

                    dialog.cancel()
                }
            })
            bEscolheUsuario.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    dialog.cancel()
                    pararGravacao()
                }
            })
            bEscolheUsuario.show()
        }
    }

    override fun onDestroy(){
        this.handler.removeCallbacksAndMessages(null)
        this.appSensorManager.unregisterListener(this)

        super.onDestroy()
    }

    public inner class ProgressTask : AsyncTask<Void, Void?, Void?>() {
        override protected fun onPreExecute(){
            if (builder == null){
                builder = AlertDialog.Builder(this@MenuGravacao)
                builder!!.setTitle("Aguardando conexão com outro dispositivo...")
                builder!!.setCancelable(false)
                builder!!.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        bluetoothConnector.cancelaDescoberta()
                        dialog.cancel()
                        pararGravacao()
                        Toast.makeText(this@MenuGravacao, "Descoberta cancelada!", Toast.LENGTH_SHORT).show()
                    }
                })
                
                val progressBar = ProgressBar(this@MenuGravacao)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                progressBar.layoutParams = layoutParams

                builder!!.setView(progressBar, 0, 10, 0, 0)
                progressDialog = builder!!.create()
            }

            progressDialog.show()
        }

        override protected fun doInBackground(vararg params: Void?): Void? {
            while (bluetoothConnector.getState() != BluetoothConnector.STATE_CONNECTED){ }
            return null;
        }

        override protected fun onPostExecute(result: Void?){
            if (tipoUsuario == USER_TERAPEUTA)
                bluetoothConnector.enviaMensagem("1")

            progressDialog.dismiss()
        }
    }

    private fun mostraDispositivosPareados(){
        val bEscolhePareados = AlertDialog.Builder(this)
        val dispositivosPareados = this.adaptadorBluetooth.getBondedDevices()

        val dispositivos = Array(dispositivosPareados.size){ dispositivosPareados.elementAt(it) }
        val nomesDispositivos = Array(dispositivosPareados.size){ dispositivos[it].name}

        bEscolhePareados.setTitle("Dispositivos pareados")
        bEscolhePareados.setCancelable(false)
        bEscolhePareados.setItems(nomesDispositivos, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                bluetoothConnector.iniciaPaciente(dispositivos[which], true)

                ProgressTask().execute()
                criaLayoutPaciente()

                dialog.dismiss()
            }
        })
        bEscolhePareados.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                dialog.cancel()
                pararGravacao()
            }
        })
        bEscolhePareados.show()
    }

    private fun criaLayoutPadrao(){
        this.iniciarGravacao()
        this.btnParar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                this@MenuGravacao.pararGravacao()
            }
        })
    }

    private fun criaLayoutPaciente(){
        this.recLayout.removeView(this.conLayout)
        this.recLayout.removeView(this.btnPausar)
        this.recLayout.removeView(this.btnParar)
    }

    private fun criaLayoutTerapeuta(){
        var btnPausarTerapeuta = BOTAO_PAUSAR
        this.btnPausar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                if (btnPausarTerapeuta == BOTAO_PAUSAR){
                    btnPausar.text = "Continuar"
                    btnPausarTerapeuta = BOTAO_CONTINUAR
                    bluetoothConnector.enviaMensagem("2")

                } else {
                    btnPausar.text = "Pausar"
                    btnPausarTerapeuta = BOTAO_PAUSAR
                    bluetoothConnector.enviaMensagem("3")
                }
            }
        })

        this.btnParar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                this@MenuGravacao.bluetoothConnector.enviaMensagem("0")
                this@MenuGravacao.pararGravacao()
            }
        })
    }

    public fun iniciarGravacao(){
        if (modoGravacao == MODO_PADRAO)
            Toast.makeText(this, "O registro dos dados foi iniciado! " +
                    "O arquivo será salvo em $diretorioArquivo.", Toast.LENGTH_LONG).show()

        val c = Calendar.getInstance()
        val strData = String.format("%02d/%02d %02d:%02d:%02d", c.get(Calendar.DATE),
            c.get(Calendar.MONTH)+1, c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE), c.get(Calendar.SECOND))

        this.accAtual.atualizaValores(strData, eixoX, eixoY, eixoZ)
        this.ativarHandler(intervaloGravacao)
    }

    public fun pararGravacao(){
        val intentSender = Intent(applicationContext, MainActivity::class.java)
        intentSender.putExtra("act_name", "MenuGravacao")
        this.startActivity(intentSender)
        this.finish()
    }

    public fun pausarGravacao(){
        this.handler.removeCallbacksAndMessages(null)
    }

    public fun retomarGravacao(){
        this.ativarHandler(intervaloGravacao)
    }

    private fun ativarHandler(delay: Int){
        this.handler.postDelayed(object : Runnable {
            override fun run() {
                atualizarTela()
                escreverCSV(accAtual)
                
                handler.postDelayed(this, delay.toLong())
            }
        }, delay.toLong())
    }

    private fun atualizarTela(){
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

        this.tabelaDados.addView(row)
    }

    private fun escreverCSV(acc: Acelerometro){
        try {
            var stringValores = ""

            if (this.isCabecalho){
                stringValores += "HORARIO,EIXO X,EIXO Y,EIXO Z\n"
                this.isCabecalho = false
            }

            val listaValores = arrayOf(acc.horario,
                String.format("%.2f", acc.valorX).replace(',', '.'),
                String.format("%.2f", acc.valorY).replace(',', '.'),
                String.format("%.2f", acc.valorZ).replace(',', '.'))

            stringValores += listaValores.joinToString(",") + '\n'

            if (this.modoGravacao == MODO_PADRAO){
                val writer = FileWriter(this.diretorioArquivo, !this.isCabecalho)
                writer.append(stringValores)
                writer.close()

            } else if (this.modoGravacao == MODO_REMOTO && 
                    this.tipoUsuario == USER_PACIENTE){
                bluetoothConnector.enviaMensagem(stringValores)
            }

            if (this.isLimiteLinhas) {
                this.linhasRegistradas += 1
                if (this.linhasRegistradas == this.numLinhasMaximo) {
                    Toast.makeText(this, "O limite máximo de" +
                         " gravação foi atingido!", Toast.LENGTH_LONG).show()
                    this.pararGravacao()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    public fun escreverCSV(msg: String){
        try {
            val writer = FileWriter(diretorioArquivo, !isCabecalho)
            writer.append(msg)
            writer.close()

            if (this.isCabecalho){
                this.isCabecalho = false
            }

            if (this.isLimiteLinhas) {
                this.linhasRegistradas += 1
                if (this.linhasRegistradas == this.numLinhasMaximo) {
                    Toast.makeText(this, "O limite máximo de" +
                         " gravação foi atingido!", Toast.LENGTH_LONG).show()
                    this.pararGravacao()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int){ }

    override fun onSensorChanged(event: SensorEvent){
        val eixoX1 = event.values[0].toDouble()
        val eixoY1 = event.values[1].toDouble()
        val eixoZ1 = event.values[2].toDouble()
    
        if (!this.flagInit){
            this.eixoX0 = eixoX1
            this.eixoY0 = eixoY1
            this.eixoZ0 = eixoZ1
            this.flagInit = true

        } else {
            this.eixoX = Math.abs(this.eixoX0 - eixoX1)
            this.eixoY = Math.abs(this.eixoY0 - eixoY1)
            this.eixoZ = Math.abs(this.eixoZ0 - eixoZ1)
    
            this.eixoX0 = eixoX1
            this.eixoY0 = eixoY1
            this.eixoZ0 = eixoZ1
            
            val c = Calendar.getInstance()
            val strData = String.format("%02d/%02d %02d:%02d:%02d", c.get(Calendar.DATE),
                c.get(Calendar.MONTH)+1, c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE), c.get(Calendar.SECOND))

            this.accAtual.atualizaValores(strData, eixoX, eixoY, eixoZ)
            this.txtEixoX.text = String.format("%.2f m/s²", accAtual.valorX).replace(',', '.')
            this.txtEixoY.text = String.format("%.2f m/s²", accAtual.valorY).replace(',', '.')
            this.txtEixoZ.text = String.format("%.2f m/s²", accAtual.valorZ).replace(',', '.')
        }
    }

    override protected fun onActivityResult(requestCode: Int, resultCode: Int, dataInt: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "Para prosseguir, é necessário " +
                "que o recurso de Bluetooth seja ativado.", Toast.LENGTH_LONG).show()
            
            this.pararGravacao()
        }
    }
}