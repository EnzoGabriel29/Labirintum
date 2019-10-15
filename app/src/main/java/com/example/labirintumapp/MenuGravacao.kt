package com.example.labirintumapp

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.FileWriter
import kotlin.math.abs

class SensorAndroid {
    var duracao = 0
    var accValorX = 0.0
    var accValorY = 0.0
    var accValorZ = 0.0
    var girValorX = 0.0
    var girValorY = 0.0
    var girValorZ = 0.0

    fun aumentaDuracao(dur: Int){
        this.duracao += dur
    }

    fun atualizaValoresAcc(accX: Double, accY: Double, accZ: Double){
        this.accValorX = accX
        this.accValorY = accY
        this.accValorZ = accZ
    }

    fun atualizaValoresGir(girX: Double, girY: Double, girZ: Double){
        this.girValorX = girX
        this.girValorY = girY
        this.girValorZ = girZ
    }
}

class MenuGravacao : AppCompatActivity() , SensorEventListener {    
    private lateinit var tabelaDados: TableLayout
    private lateinit var recLayout: RelativeLayout
    private lateinit var conLayout: LinearLayout
    private lateinit var graphLayout: LinearLayout
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
    private var modoCalculo = ""
    private var tipoUsuario = 0
    private var graficosVisiveis = 0
    private var isLimiteLinhas = false
    private var isGraficoAcc = false
    private var isGraficoGir = false
    private var isPausarThreads = false
    private var isPararThreads = false
    private var minEixoY = 0.0
    private var maxEixoY = 20.0


    private var linhasPreGravadas = arrayOf("HORARIO,ACC EIXO X,ACC EIXO Y,ACC EIXO Z,GIR EIXO X,GIR EIXO Y,GIR EIXO Z")

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        const val MODO_PADRAO = 2
        const val MODO_REMOTO = 3
        const val USER_TERAPEUTA = 4
        const val USER_PACIENTE = 5
        private const val BOTAO_PAUSAR = 6
        private const val BOTAO_CONTINUAR = 7
        private const val HANDLER_TOAST = 8
    }

    private val adaptadorBluetooth = BluetoothAdapter.getDefaultAdapter()
    private lateinit var progressDialog: AlertDialog
    private var builder: AlertDialog.Builder? = null
    private lateinit var appSensorManager: SensorManager
    private lateinit var bluetoothConnector: BluetoothConnector
    
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_gravacao)

        val intentRecebido = intent
        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)

        modoGravacao = intentRecebido.getIntExtra("KEY_MODO_GRAVACAO", MODO_PADRAO)
        diretorioArquivo = intentRecebido.getStringExtra("KEY_DIRETORIO_ARQUIVO") ?: ""
        extensaoArquivo = pref.getString("KEY_EXTENSAO_ARQUIVO", "csv") ?: "csv"
        numLinhasMaximo = pref.getInt("KEY_NUM_MAX_LINHAS", 120)
        intervaloGravacao = pref.getInt("KEY_DELAY_GRAVACAO", 200)
        graficosVisiveis = pref.getInt("KEY_GRAFICOS_VISIVEIS", 3)
        isLimiteLinhas = pref.getBoolean("KEY_IS_NUM_MAX_LINHAS", true)
        modoCalculo = pref.getString("KEY_MODO_CALCULO", "var") ?: "var"

        tabelaDados = findViewById(R.id.tabelaDados)
        recLayout   = findViewById(R.id.rec_layout)
        conLayout   = findViewById(R.id.controll_layout)
        graphLayout = findViewById(R.id.layoutGraphs)
        btnPausar   = findViewById(R.id.btnPausar)
        btnParar    = findViewById(R.id.btnParar)

        isGraficoAcc = graficosVisiveis == 1 || graficosVisiveis == 3
        isGraficoGir = graficosVisiveis == 2 || graficosVisiveis == 3

        if (isGraficoAcc){
            graficosEixos += findViewById<GraphView>(R.id.graphAccEixoX)
            graficosEixos += findViewById<GraphView>(R.id.graphAccEixoY)
            graficosEixos += findViewById<GraphView>(R.id.graphAccEixoZ)
        
        } else graphLayout.removeViews(0, 3)

        if (isGraficoGir){
            graficosEixos += findViewById<GraphView>(R.id.graphGirEixoX)
            graficosEixos += findViewById<GraphView>(R.id.graphGirEixoY)
            graficosEixos += findViewById<GraphView>(R.id.graphGirEixoZ)
        
        } else if (!isGraficoAcc) graphLayout.removeViews(0, 3)
        else graphLayout.removeViews(3, 3)

        for (i in graficosEixos.indices){
            seriesEixos += LineGraphSeries()

            graficosEixos[i].addSeries(seriesEixos[i])

            graficosEixos[i].viewport.isXAxisBoundsManual = true
            graficosEixos[i].viewport.setMinX(0.0)
            if (!isLimiteLinhas) graficosEixos[i].viewport.setMaxX(40.0)
            else graficosEixos[i].viewport.setMaxX(numLinhasMaximo.toDouble())

            graficosEixos[i].viewport.isYAxisBoundsManual = true
            graficosEixos[i].viewport.setMinY(0.0)
            graficosEixos[i].viewport.setMaxY(maxEixoY)
        }

        var modoBtnPausar = BOTAO_PAUSAR
        btnPausar.setOnClickListener {
            if (modoBtnPausar == BOTAO_PAUSAR){
                pausarGravacao()
                modoBtnPausar = BOTAO_CONTINUAR
                btnPausar.text = getString(R.string.btn_continuar)

            } else {
                retomarGravacao()
                modoBtnPausar = BOTAO_PAUSAR
                btnPausar.text = getString(R.string.btn_pausar)
            }
        }

        appSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        appSensorManager.registerListener(this, appSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), this.intervaloGravacao*1000)
        appSensorManager.registerListener(this, appSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), this.intervaloGravacao*1000)

        if (modoGravacao == MODO_PADRAO)
            criaLayoutPadrao()

        else {
            if (adaptadorBluetooth == null){
                Toast.makeText(this, "Seu dispositivo não suporta" +
                    "o recurso de Bluetooth.", Toast.LENGTH_LONG).show()
                pararGravacao()

            } else if (!adaptadorBluetooth.isEnabled){
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }

            bluetoothConnector = BluetoothConnector(this)
            val bEscolheUsuario = AlertDialog.Builder(this)
            val modosUsuario = arrayOf("Dispositivo do terapeuta", "Dispositivo do paciente")
            bEscolheUsuario.setTitle("Qual dispositivo você está usando?")
            bEscolheUsuario.setCancelable(false)

            bEscolheUsuario.setItems(modosUsuario){
                dialog: DialogInterface, which: Int ->
                    tipoUsuario = if (which == 0) USER_TERAPEUTA else USER_PACIENTE

                    if (tipoUsuario == USER_TERAPEUTA){
                        bluetoothConnector.iniciaTerapeuta()

                        TaskProgresso().execute()
                        criaLayoutTerapeuta()

                    } else mostraDispositivosPareados()

                    dialog.dismiss()
            }

            bEscolheUsuario.setNegativeButton("Cancelar"){
                dialog: DialogInterface, _: Int ->
                    dialog.cancel()
                    pararGravacao()
            }

            bEscolheUsuario.show()
        }

        handler = object : Handler(Looper.getMainLooper()){
            override fun handleMessage(msg: Message){
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

    inner class TaskProgresso : AsyncTask<Void, Void?, Void?>(){
        override fun onPreExecute(){
            if (builder == null){
                builder = AlertDialog.Builder(this@MenuGravacao)
                builder!!.setTitle("Aguardando conexão com outro dispositivo...")
                builder!!.setCancelable(false)

                builder!!.setNegativeButton("Cancelar"){
                    dialog: DialogInterface, _: Int ->
                        dialog.cancel()
                        bluetoothConnector.cancelaDescoberta()
                        pararGravacao()
                        this@TaskProgresso.cancel(true)
                        Toast.makeText(this@MenuGravacao, "Descoberta cancelada!", Toast.LENGTH_SHORT).show()
                }
                
                val progressBar = ProgressBar(this@MenuGravacao)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                progressBar.layoutParams = layoutParams

                builder!!.setView(progressBar)
                progressDialog = builder!!.create()
            }

            progressDialog.show()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            while (bluetoothConnector.getState() != BluetoothConnector.STATE_CONNECTED){ }
            return null
        }

        override fun onPostExecute(result: Void?){
            if (tipoUsuario == USER_TERAPEUTA)
                bluetoothConnector.enviaMensagem("1")

            progressDialog.dismiss()
        }
    }

    private fun mostraDispositivosPareados(){
        val bEscolhePareados = AlertDialog.Builder(this)
        val dispositivosPareados = adaptadorBluetooth.bondedDevices

        val dispositivos = Array(dispositivosPareados.size){ dispositivosPareados.elementAt(it) }
        val nomesDispositivos = Array(dispositivosPareados.size){ dispositivos[it].name}

        bEscolhePareados.setTitle("Dispositivos pareados")
        bEscolhePareados.setCancelable(false)

        bEscolhePareados.setItems(nomesDispositivos){
            dialog: DialogInterface, which: Int ->
                bluetoothConnector.iniciaPaciente(dispositivos[which], true)

                TaskProgresso().execute()
                criaLayoutPaciente()

                dialog.dismiss()
        }

        bEscolhePareados.setNegativeButton("Cancelar"){
            dialog: DialogInterface, _: Int ->
                dialog.cancel()
                pararGravacao()
        }

        bEscolhePareados.show()
    }

    private fun criaLayoutPadrao(){
        iniciarGravacao()
        btnParar.setOnClickListener {
            pararGravacao()
        }
    }

    private fun criaLayoutPaciente(){
        recLayout.removeAllViews()

        /*
        val textoEspera = TextView(this)
        textoEspera.text = "Os dados estão sendo enviados ao seu terapeuta."
        val rllp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT)
        textoEspera.layoutParams = rllp
        textoEspera.gravity = Gravity.CENTER
        */

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)  
    }

    private fun criaLayoutTerapeuta(){
        var btnPausarTerapeuta = BOTAO_PAUSAR
        btnPausar.setOnClickListener {
            if (btnPausarTerapeuta == BOTAO_PAUSAR){
                btnPausar.text = getString(R.string.btn_continuar)
                btnPausarTerapeuta = BOTAO_CONTINUAR
                bluetoothConnector.enviaMensagem("2")

            } else {
                btnPausar.text = getString(R.string.btn_pausar)
                btnPausarTerapeuta = BOTAO_PAUSAR
                bluetoothConnector.enviaMensagem("3")
            }
        }

        btnParar.setOnClickListener {
            bluetoothConnector.enviaMensagem("0")
            pararGravacao()
        }
    }

    private inner class ThreadFiltro : Thread() {
        override fun run(){
            var tempoInicial = System.currentTimeMillis()
            while (true){
                if (!isPausarThreads){
                    val tempoFinal = System.currentTimeMillis()
                    val duracao = tempoFinal - tempoInicial
                    
                    if (duracao > intervaloGravacao){
                        flagPararGravacao = false
                        tempoInicial = tempoFinal
                    }
                }

                if (isPararThreads) break
            }
        }
    }

    fun iniciarGravacao(){
        if (this.modoGravacao == MODO_PADRAO)
            Toast.makeText(this, "O registro dos dados foi iniciado! " +
                    "O arquivo será salvo em $diretorioArquivo.", Toast.LENGTH_LONG).show()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sensorAtual.atualizaValoresAcc(accEixoX, accEixoY, accEixoZ)
        sensorAtual.atualizaValoresGir(girEixoX, girEixoY, girEixoZ)
        flagPararGravacao = false
        isPararThreads = false
        ThreadFiltro().start()
    }

    fun pararGravacao(){
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        isPararThreads = true
        flagPararGravacao = true

        if (this.linhasPreGravadas.size > 1){
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
        }

        val intentSender = Intent(applicationContext, MenuPrincipal::class.java)
        intentSender.putExtra("KEY_NOME_ACTIVITY", "MenuGravacao")
        startActivity(intentSender)
        finish()
    }

    fun pausarGravacao(){
        isPausarThreads = true
        flagPararGravacao = true
    }

    fun retomarGravacao(){
        flagPararGravacao = false
        isPausarThreads = false
    }

    private fun atualizarTela(){
        val row = TableRow(this)

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
        
        if (this.isLimiteLinhas){
            if (++this.linhasRegistradas == this.numLinhasMaximo){
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

        val pontosEixoX = if (isLimiteLinhas) numLinhasMaximo else 40
        val isGraficoFixo = isLimiteLinhas

        val valoresEixos = arrayOf(
            sensorAtual.accValorX, sensorAtual.accValorY,
            sensorAtual.accValorZ, sensorAtual.girValorX,
            sensorAtual.girValorY, sensorAtual.girValorZ)

        if (valoresEixos.filter{it > maxEixoY}.any()){
            val novoMax = valoresEixos.max() ?: 0.0

            for (grafico in graficosEixos)
                grafico.viewport.setMaxY(novoMax + 10.0)

            maxEixoY = novoMax
        }

        if (valoresEixos.filter{it < minEixoY}.any()){
            val novoMin = valoresEixos.min() ?: 0.0

            for (grafico in graficosEixos)
                grafico.viewport.setMinY(novoMin - 10.0)

            minEixoY = novoMin
        }

        if (isGraficoAcc && isGraficoGir){
            seriesEixos[0].appendData(DataPoint(pontoX, sensorAtual.accValorX), !isGraficoFixo, pontosEixoX)
            seriesEixos[1].appendData(DataPoint(pontoX, sensorAtual.accValorY), !isGraficoFixo, pontosEixoX)
            seriesEixos[2].appendData(DataPoint(pontoX, sensorAtual.accValorZ), !isGraficoFixo, pontosEixoX)
            seriesEixos[3].appendData(DataPoint(pontoX, sensorAtual.girValorX), !isGraficoFixo, pontosEixoX)
            seriesEixos[4].appendData(DataPoint(pontoX, sensorAtual.girValorY), !isGraficoFixo, pontosEixoX)
            seriesEixos[5].appendData(DataPoint(pontoX, sensorAtual.girValorZ), !isGraficoFixo, pontosEixoX)

        } else if (isGraficoAcc){
            seriesEixos[0].appendData(DataPoint(pontoX, sensorAtual.accValorX), !isGraficoFixo, pontosEixoX)
            seriesEixos[1].appendData(DataPoint(pontoX, sensorAtual.accValorY), !isGraficoFixo, pontosEixoX)
            seriesEixos[2].appendData(DataPoint(pontoX, sensorAtual.accValorZ), !isGraficoFixo, pontosEixoX)

        } else if (isGraficoGir){
            seriesEixos[0].appendData(DataPoint(pontoX, sensorAtual.girValorX), !isGraficoFixo, pontosEixoX)
            seriesEixos[1].appendData(DataPoint(pontoX, sensorAtual.girValorY), !isGraficoFixo, pontosEixoX)
            seriesEixos[2].appendData(DataPoint(pontoX, sensorAtual.girValorZ), !isGraficoFixo, pontosEixoX)
        }
    }

    fun escreverCSV(msg: String){
        this.linhasPreGravadas += msg

        val camposString = msg.split(",")
        val camposDouble = Array(7){ camposString[it].toDouble() }
        val pontoX = camposDouble[0] / 40
        val pontosEixoX = if (isLimiteLinhas) numLinhasMaximo else 40
        val isGraficoFixo = isLimiteLinhas || linhasRegistradas < 40

        if (isGraficoAcc && isGraficoGir){
            seriesEixos[0].appendData(DataPoint(pontoX, camposDouble[1]), !isGraficoFixo, pontosEixoX)
            seriesEixos[1].appendData(DataPoint(pontoX, camposDouble[2]), !isGraficoFixo, pontosEixoX)
            seriesEixos[2].appendData(DataPoint(pontoX, camposDouble[3]), !isGraficoFixo, pontosEixoX)
            seriesEixos[3].appendData(DataPoint(pontoX, camposDouble[4]), !isGraficoFixo, pontosEixoX)
            seriesEixos[4].appendData(DataPoint(pontoX, camposDouble[5]), !isGraficoFixo, pontosEixoX)
            seriesEixos[5].appendData(DataPoint(pontoX, camposDouble[6]), !isGraficoFixo, pontosEixoX)

        } else if (isGraficoAcc){
            seriesEixos[0].appendData(DataPoint(pontoX, camposDouble[1]), !isGraficoFixo, pontosEixoX)
            seriesEixos[1].appendData(DataPoint(pontoX, camposDouble[2]), !isGraficoFixo, pontosEixoX)
            seriesEixos[2].appendData(DataPoint(pontoX, camposDouble[3]), !isGraficoFixo, pontosEixoX)

        } else if (isGraficoGir){
            seriesEixos[0].appendData(DataPoint(pontoX, camposDouble[4]), !isGraficoFixo, pontosEixoX)
            seriesEixos[1].appendData(DataPoint(pontoX, camposDouble[5]), !isGraficoFixo, pontosEixoX)
            seriesEixos[2].appendData(DataPoint(pontoX, camposDouble[6]), !isGraficoFixo, pontosEixoX)
        }

        if (isLimiteLinhas){
            if (++linhasRegistradas == numLinhasMaximo){
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
        when (event.sensor.type){
            Sensor.TYPE_GYROSCOPE -> {
                val girEixoX1 = event.values[0].toDouble()
                val girEixoY1 = event.values[1].toDouble()
                val girEixoZ1 = event.values[2].toDouble()

                if (modoCalculo == "var"){
                    if (!girFlagInit) {
                        girEixoX0 = girEixoX1
                        girEixoY0 = girEixoY1
                        girEixoZ0 = girEixoZ1
                        girFlagInit = true

                    } else {
                        girEixoX = abs(girEixoX0 - girEixoX1)
                        girEixoY = abs(girEixoY0 - girEixoY1)
                        girEixoZ = abs(girEixoZ0 - girEixoZ1)

                        girEixoX0 = girEixoX1
                        girEixoY0 = girEixoY1
                        girEixoZ0 = girEixoZ1

                        sensorAtual.atualizaValoresGir(girEixoX, girEixoY, girEixoZ)
                    }

                } else sensorAtual.atualizaValoresGir(girEixoX1, girEixoY1, girEixoZ1)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val accEixoX1 = event.values[0].toDouble()
                val accEixoY1 = event.values[1].toDouble()
                val accEixoZ1 = event.values[2].toDouble()

                if (modoCalculo == "var"){
                    if (!accFlagInit) {
                        accEixoX0 = accEixoX1
                        accEixoY0 = accEixoY1
                        accEixoZ0 = accEixoZ1
                        accFlagInit = true

                    } else {
                        accEixoX = abs(accEixoX0 - accEixoX1)
                        accEixoY = abs(accEixoY0 - accEixoY1)
                        accEixoZ = abs(accEixoZ0 - accEixoZ1)

                        accEixoX0 = accEixoX1
                        accEixoY0 = accEixoY1
                        accEixoZ0 = accEixoZ1

                        sensorAtual.atualizaValoresAcc(accEixoX, accEixoY, accEixoZ)
                    }

                } else sensorAtual.atualizaValoresAcc(accEixoX1, accEixoY1, accEixoZ1)
            }
        }

        // Apenas efetua o registro dos dados caso o tempo
        // em milissegundos definido tenha sido alcançado.
        if (!flagPararGravacao){
            escreverCSV()
            atualizarTela()
            if (isGraficoAcc || isGraficoGir) atualizarGraficos()
            sensorAtual.aumentaDuracao(this.intervaloGravacao)
            flagPararGravacao = true
        }
    }

    // Recebe o resultado do Intent que solicita a
    // ativação do Bluetooth no dispositivo do usuário.
    override fun onActivityResult(requestCode: Int, resultCode: Int, dataInt: Intent?){
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED){
            Toast.makeText(this, "Para prosseguir, é necessário " +
                "que o recurso de Bluetooth seja ativado.", Toast.LENGTH_LONG).show()
            
            pararGravacao()
        }
    }
}