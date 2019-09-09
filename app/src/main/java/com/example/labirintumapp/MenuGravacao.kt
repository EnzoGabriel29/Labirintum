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
import android.os.Message
import android.os.Looper
import android.Manifest
import android.content.Context
import android.content.IntentFilter
import java.nio.charset.Charset
import android.view.View
import java.util.*
import android.widget.*
import android.util.Log
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class SensorAndroid(){
    public var duracao = 0
    public var accValorX = 0.0
    public var accValorY = 0.0
    public var accValorZ = 0.0
    public var girValorX = 0.0
    public var girValorY = 0.0
    public var girValorZ = 0.0

    public fun aumentaDuracao(dur: Int){
        this.duracao += dur
    }

    public fun atualizaValoresAcc(accX: Double, accY: Double, accZ: Double){
        this.accValorX = accX
        this.accValorY = accY
        this.accValorZ = accZ
    }

    public fun atualizaValoresGir(girX: Double, girY: Double, girZ: Double){
        this.girValorX = girX
        this.girValorY = girY
        this.girValorZ = girZ
    }
}

class MenuGravacao : AppCompatActivity() , SensorEventListener {    
    private lateinit var tabelaDados: TableLayout
    private lateinit var recLayout: RelativeLayout
    private lateinit var conLayout: LinearLayout 
    private lateinit var btnPausar: TextView   
    private lateinit var btnParar: TextView
    private var graficosEixos = emptyArray<GraphView>() 
    private var seriesEixos = emptyArray<LineGraphSeries<DataPoint>>()

    private var accEixoX = 0.0
    private var accEixoY = 0.0
    private var accEixoZ = 0.0
    private var accEixoX0 = 0.0
    private var accEixoY0 = 0.0
    private var accEixoZ0 = 0.0
    private var accFlagInit = false

    private var girEixoX = 0.0
    private var girEixoY = 0.0
    private var girEixoZ = 0.0
    private var girEixoX0 = 0.0
    private var girEixoY0 = 0.0
    private var girEixoZ0 = 0.0
    private var girFlagInit = false

    private lateinit var handler : Handler
    private var flagPararGravacao = true
    private var sensorAtual = SensorAndroid()
    
    private var linhasRegistradas = 0
    private var numLinhasMaximo = 0
    private var intervaloGravacao = 0
    private var modoGravacao = 0 
    private var diretorioArquivo = ""
    private var extensaoArquivo = ""
    private var tipoUsuario = 0
    private var isLimiteLinhas = false

    private var linhasPreGravadas = arrayOf<String>("HORARIO,ACC EIXO X,ACC EIXO Y,ACC EIXO Z,GIR EIXO X,GIR EIXO Y,GIR EIXO Z")

    companion object {
        private val TAG = "LABIRINTUMAPP"
        private val REQUEST_ENABLE_BT = 1
        public val MODO_PADRAO = 2
        public val MODO_REMOTO = 3
        public val USER_TERAPEUTA = 4
        public val USER_PACIENTE = 5
        private val BOTAO_PAUSAR = 6
        private val BOTAO_CONTINUAR = 7
        private val HANDLER_TOAST = 8
    }

    private val adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter()
    private lateinit var progressDialog: AlertDialog
    private var builder: AlertDialog.Builder? = null
    private lateinit var appSensorManager: SensorManager
    private lateinit var bluetoothConnector: BluetoothConnector
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_gravacao)

        tabelaDados = findViewById(R.id.tabelaDados)
        recLayout   = findViewById(R.id.rec_layout)
        conLayout   = findViewById(R.id.controll_layout)
        btnPausar   = findViewById(R.id.btnPausar)
        btnParar    = findViewById(R.id.btnParar)

        graficosEixos += findViewById<GraphView>(R.id.graphAccEixoX)
        graficosEixos += findViewById<GraphView>(R.id.graphAccEixoY)
        graficosEixos += findViewById<GraphView>(R.id.graphAccEixoZ)
        graficosEixos += findViewById<GraphView>(R.id.graphGirEixoX)
        graficosEixos += findViewById<GraphView>(R.id.graphGirEixoY)
        graficosEixos += findViewById<GraphView>(R.id.graphGirEixoZ)

        for (i in 0..5) seriesEixos += LineGraphSeries<DataPoint>()

        for (i in 0..5){
            graficosEixos[i].addSeries(seriesEixos[i])

            graficosEixos[i].getViewport().setXAxisBoundsManual(true)
            graficosEixos[i].getViewport().setMinX(0.0)
            graficosEixos[i].getViewport().setMaxX(40.0)

            graficosEixos[i].getViewport().setYAxisBoundsManual(true)
            graficosEixos[i].getViewport().setMinY(0.0)
            graficosEixos[i].getViewport().setMaxY(20.0)
        }

        var modoBtnPausar = BOTAO_PAUSAR
        btnPausar.setOnClickListener(object : View.OnClickListener {
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
        
        val intentRecebido = intent
        this.modoGravacao = intentRecebido.getIntExtra("rec_mode", MODO_PADRAO)
        this.diretorioArquivo = intentRecebido.getStringExtra("file_path") ?: ""
        this.extensaoArquivo = intentRecebido.getStringExtra("file_type") ?: ""
        this.numLinhasMaximo = intentRecebido.getIntExtra("max_lines", 120)
        this.intervaloGravacao = intentRecebido.getIntExtra("rec_delay", 200)
        this.isLimiteLinhas = numLinhasMaximo > 0

        appSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        appSensorManager.registerListener(this, appSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), this.intervaloGravacao*1000)
        appSensorManager.registerListener(this, appSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), this.intervaloGravacao*1000)

        if (this.modoGravacao == MODO_PADRAO)
            criaLayoutPadrao()

        else {
            if (adaptadorBluetooth == null){
                Toast.makeText(this, "Seu dispositivo não suporta" +
                    "o recurso de Bluetooth.", Toast.LENGTH_LONG).show()
                pararGravacao()

            } else if (!adaptadorBluetooth.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }

            bluetoothConnector = BluetoothConnector(this)
            val bEscolheUsuario = AlertDialog.Builder(this)
            val modosUsuario = arrayOf("Dispositivo do terapeuta", "Dispositivo do paciente")
            bEscolheUsuario.setTitle("Qual dispositivo você está usando?")
            bEscolheUsuario.setCancelable(false)
            bEscolheUsuario.setItems(modosUsuario, object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    tipoUsuario = if (which == 0) USER_TERAPEUTA else USER_PACIENTE

                    if (tipoUsuario == USER_TERAPEUTA){
                        bluetoothConnector.iniciaTerapeuta()

                        TaskProgresso().execute()
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

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what){
                    HANDLER_TOAST -> {
                        val stringRecebida = msg.obj as String
                        Toast.makeText(this@MenuGravacao, stringRecebida, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroy(){
        appSensorManager.unregisterListener(this)
        super.onDestroy()
    }

    public inner class TaskProgresso : AsyncTask<Void, Void?, Void?>() {
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
                        this@TaskProgresso.cancel(true)
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
            return null
        }

        override protected fun onPostExecute(result: Void?){
            if (tipoUsuario == USER_TERAPEUTA)
                bluetoothConnector.enviaMensagem("1")

            progressDialog.dismiss()
        }
    }

    private fun mostraDispositivosPareados(){
        val bEscolhePareados = AlertDialog.Builder(this)
        val dispositivosPareados = adaptadorBluetooth.getBondedDevices()

        val dispositivos = Array(dispositivosPareados.size){ dispositivosPareados.elementAt(it) }
        val nomesDispositivos = Array(dispositivosPareados.size){ dispositivos[it].name}

        bEscolhePareados.setTitle("Dispositivos pareados")
        bEscolhePareados.setCancelable(false)
        bEscolhePareados.setItems(nomesDispositivos, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface, which: Int) {
                bluetoothConnector.iniciaPaciente(dispositivos[which], true)

                TaskProgresso().execute()
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
        iniciarGravacao()
        btnParar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                this@MenuGravacao.pararGravacao()
            }
        })
    }

    private fun criaLayoutPaciente(){
        recLayout.removeView(conLayout)
        recLayout.removeView(btnPausar)
        recLayout.removeView(btnParar)
    }

    private fun criaLayoutTerapeuta(){
        var btnPausarTerapeuta = BOTAO_PAUSAR
        btnPausar.setOnClickListener(object : View.OnClickListener {
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

        btnParar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                this@MenuGravacao.bluetoothConnector.enviaMensagem("0")
                this@MenuGravacao.pararGravacao()
            }
        })
    }

    public fun iniciarGravacao(){
        if (this.modoGravacao == MODO_PADRAO)
            Toast.makeText(this, "O registro dos dados foi iniciado! " +
                    "O arquivo será salvo em $diretorioArquivo.", Toast.LENGTH_LONG).show()

        sensorAtual.atualizaValoresAcc(accEixoX, accEixoY, accEixoZ)
        sensorAtual.atualizaValoresGir(girEixoX, girEixoY, girEixoZ)
        flagPararGravacao = false
    }

    public fun pararGravacao(){
        flagPararGravacao = true

        try {
            val writer = FileWriter(this.diretorioArquivo, false)
            writer.append(this.linhasPreGravadas.joinToString("\n"))
            writer.close()

        } catch (e: Exception){
            val message = Message()
            message.what = HANDLER_TOAST
            message.obj = e.toString()
            handler.sendMessage(message)
            
            e.printStackTrace()
        }

        val intentSender = Intent(applicationContext, MainActivity::class.java)
        intentSender.putExtra("act_name", "MenuGravacao")
        startActivity(intentSender)
        finish()
    }

    public fun pausarGravacao(){
        flagPararGravacao = true
    }

    public fun retomarGravacao(){
        flagPararGravacao = false
    }

    private fun atualizarTela(){
        var row = TableRow(this)

        val lp = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        row.weightSum = 7f
        row.layoutParams =  lp

        var col = TextView(this)
        var trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.layoutParams = trlp
        col.gravity = Gravity.CENTER
        col.text = String.format("%d", sensorAtual.duracao)
        row.addView(col)

        col = TextView(this)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", sensorAtual.accValorX).replace(',', '.')
        row.addView(col)

        col = TextView(this)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", sensorAtual.accValorY).replace(',', '.')
        row.addView(col)

        col = TextView(this)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", sensorAtual.accValorZ).replace(',', '.')
        row.addView(col)

        col = TextView(this)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", sensorAtual.girValorX).replace(',', '.')
        row.addView(col)

        col = TextView(this)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", sensorAtual.girValorY).replace(',', '.')
        row.addView(col)

        col = TextView(this)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", sensorAtual.girValorZ).replace(',', '.')
        row.addView(col)

        tabelaDados.addView(row)
    }

    private fun escreverCSV(){
        val stringValores = String.format(
            "%d,%s,%s,%s,%s,%s,%s".format(sensorAtual.duracao,
            String.format("%.2f", sensorAtual.accValorX).replace(',', '.'),
            String.format("%.2f", sensorAtual.accValorY).replace(',', '.'),
            String.format("%.2f", sensorAtual.accValorZ).replace(',', '.'),
            String.format("%.2f", sensorAtual.girValorX).replace(',', '.'),
            String.format("%.2f", sensorAtual.girValorY).replace(',', '.'),
            String.format("%.2f", sensorAtual.girValorZ).replace(',', '.')))

        if (this.modoGravacao == MODO_PADRAO)
            this.linhasPreGravadas += stringValores

        else if (this.modoGravacao == MODO_REMOTO && this.tipoUsuario == USER_PACIENTE)
            bluetoothConnector.enviaMensagem(stringValores)
        
        if (this.isLimiteLinhas) {
            if (++this.linhasRegistradas == this.numLinhasMaximo) {
                val message = Message()
                message.what = HANDLER_TOAST
                message.obj = "O limite máximo de gravação foi atingido!"
                handler.sendMessage(message)

                pararGravacao()
            }
        }
    }

    private fun atualizarGraficos(){
        val pontoX = sensorAtual.duracao / this.intervaloGravacao.toDouble()

        seriesEixos[0].appendData(DataPoint(pontoX, sensorAtual.accValorX), true, 40)
        seriesEixos[1].appendData(DataPoint(pontoX, sensorAtual.accValorY), true, 40)
        seriesEixos[2].appendData(DataPoint(pontoX, sensorAtual.accValorZ), true, 40)
        seriesEixos[3].appendData(DataPoint(pontoX, sensorAtual.girValorX), true, 40)
        seriesEixos[4].appendData(DataPoint(pontoX, sensorAtual.girValorY), true, 40)
        seriesEixos[5].appendData(DataPoint(pontoX, sensorAtual.girValorZ), true, 40)
    }

    public fun escreverCSV(msg: String){
        this.linhasPreGravadas += msg

        if (this.isLimiteLinhas) {
            if (++this.linhasRegistradas == this.numLinhasMaximo){
                val message = Message()
                message.what = HANDLER_TOAST
                message.obj = "O limite máximo de gravação foi atingido!"
                handler.sendMessage(message)

                pararGravacao()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int){ }

    override fun onSensorChanged(event: SensorEvent){
        if (!flagPararGravacao){
            when (event.sensor.type){
                Sensor.TYPE_GYROSCOPE -> {
                    Log.d(TAG, "GYROSCOPE")
                    val girEixoX1 = event.values[0].toDouble()
                    val girEixoY1 = event.values[1].toDouble()
                    val girEixoZ1 = event.values[2].toDouble()

                    if (!girFlagInit){
                        girEixoX0 = girEixoX1
                        girEixoY0 = girEixoY1
                        girEixoZ0 = girEixoZ1
                        girFlagInit = true

                    } else {
                        girEixoX = Math.abs(girEixoX0 - girEixoX1)
                        girEixoY = Math.abs(girEixoY0 - girEixoY1)
                        girEixoZ = Math.abs(girEixoZ0 - girEixoZ1)
                
                        girEixoX0 = girEixoX1
                        girEixoY0 = girEixoY1
                        girEixoZ0 = girEixoZ1

                        sensorAtual.atualizaValoresGir(girEixoX, girEixoY, girEixoZ)
                    }
                }

                Sensor.TYPE_ACCELEROMETER -> {
                    val accEixoX1 = event.values[0].toDouble()
                    val accEixoY1 = event.values[1].toDouble()
                    val accEixoZ1 = event.values[2].toDouble()

                    if (!accFlagInit){
                        accEixoX0 = accEixoX1
                        accEixoY0 = accEixoY1
                        accEixoZ0 = accEixoZ1
                        accFlagInit = true

                    } else {
                        accEixoX = Math.abs(accEixoX0 - accEixoX1)
                        accEixoY = Math.abs(accEixoY0 - accEixoY1)
                        accEixoZ = Math.abs(accEixoZ0 - accEixoZ1)
                
                        accEixoX0 = accEixoX1
                        accEixoY0 = accEixoY1
                        accEixoZ0 = accEixoZ1

                        sensorAtual.atualizaValoresAcc(accEixoX, accEixoY, accEixoZ)
                    }
                }
            }

            escreverCSV()
            atualizarTela()
            atualizarGraficos()
            sensorAtual.aumentaDuracao(this.intervaloGravacao)
        }
    }

    override protected fun onActivityResult(requestCode: Int, resultCode: Int, dataInt: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "Para prosseguir, é necessário " +
                "que o recurso de Bluetooth seja ativado.", Toast.LENGTH_LONG).show()
            
            pararGravacao()
        }
    }
}