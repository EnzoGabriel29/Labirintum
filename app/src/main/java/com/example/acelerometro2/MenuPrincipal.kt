package com.example.acelerometro2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.view.View
import android.view.LayoutInflater
import android.widget.*
import java.io.File
import java.io.FileWriter
import java.util.*
import android.widget.Toast
import com.example.acelerometro2.*



class MenuPrincipal : AppCompatActivity(), SensorEventListener {
    // Declara as Views para cada tipo de visualização
    private lateinit var layoutEixoX: LinearLayout
    private lateinit var layoutEixoY: LinearLayout
    private lateinit var layoutEixoZ: LinearLayout
    private lateinit var txtEixoX: TextView
    private lateinit var txtEixoY: TextView
    private lateinit var txtEixoZ: TextView
    private lateinit var graphEixoX: View
    private lateinit var graphEixoY: View
    private lateinit var graphEixoZ: View

    // Define o tipo de modo de visualização
    // 'T' para TEXTO e 'G' para GRAFICO
    private var modo: Char = 'T'

    // Declara as variáveis do sensor do Android
    private var sensorManager: SensorManager? = null
    private var firstIter: Boolean = false
    private var eixoX0: Double = 0.0
    private var eixoY0: Double = 0.0
    private var eixoZ0: Double = 0.0
    private var eixoX: Double = 0.0
    private var eixoY: Double = 0.0
    private var eixoZ: Double = 0.0

    internal lateinit var db: BancoDados


    private var rewrite: Boolean = false


	
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager!!.registerListener(this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)

        db = BancoDados(this)
        for (i in db.getAcc)
            atualizarLista(this, i)

        // O modo de visualização padrão é TEXTO
        val escolhaView = findViewById<RadioGroup>(R.id.modoView)
        escolhaView.check(R.id.checkTexto)
        inflateLayoutTexto(findViewById(R.id.main_layout), true)

        // Infla os layouts dependendo do tipo de visualização escolhido
        escolhaView.setOnCheckedChangeListener { _, _ ->
            val parent = findViewById<LinearLayout>(R.id.main_layout)
            val selectedId = escolhaView.checkedRadioButtonId
            val selectedButton = findViewById<RadioButton>(selectedId).text

            if (selectedButton == "Texto") {
                modo = 'T'
                inflateLayoutTexto(parent, false)
            } else {
                modo = 'G'
                inflateLayoutGrafico(parent)
            }
        }

        val txtIntervalo = findViewById<EditText>(R.id.edtIntervalo)
        val switchSalvar = findViewById<Switch>(R.id.switchSalvarDados)
        val handler = Handler()
        switchSalvar.setOnCheckedChangeListener{_, _ ->
            // Caso o usuário defina a opção de salvar os dados periodicamente
            if (switchSalvar.isChecked){
                val numIntervalo = if (txtIntervalo.text.toString().isEmpty()) 0.toFloat() else txtIntervalo.text.toString().toFloat()
                txtIntervalo.isEnabled = false

                val delay = numIntervalo * 1000

                // Os dados são salvos no Banco de Dados e atualizados no Linear Layout
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
                        db.addAcc(acc)
                        atualizarLista(this@MenuPrincipal, acc)

                        escreverCSV(acc, rewrite)
						rewrite = false

                        handler.postDelayed(this, delay.toLong())
                    }
                }, delay.toLong())
            }
            else{
                handler.removeCallbacksAndMessages(null)
                txtIntervalo.isEnabled = true
            }
        }

        val btnDelete = findViewById<Button>(R.id.btnDeletarDados)
        btnDelete.setOnClickListener {
            rewrite = true
            val arrayAcc = db.getAcc
            for (i in arrayAcc)
                db.deleteAcc(i)
            val parent = findViewById<LinearLayout>(R.id.layoutDadosSalvos)
            parent.removeAllViews()
        }

    }

    private fun atualizarLista(activity: Activity, acc: Acelerometro){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        @SuppressLint("InflateParams") val view = inflater.inflate(R.layout.campo_dados_acelerometro, null)
        val parent = activity.findViewById<LinearLayout>(R.id.layoutDadosSalvos)
        val child = view.findViewById<LinearLayout>(R.id.layout_campo)

        val valorHorario = child.findViewById<TextView>(R.id.txtHorario)
        valorHorario.text = acc.horario
        val valorEixoX = child.findViewById<TextView>(R.id.txtValorX)
        valorEixoX.text = String.format("%.2f", acc.valorX)
        val valorEixoY = child.findViewById<TextView>(R.id.txtValorY)
        valorEixoY.text = String.format("%.2f", acc.valorY)
        val valorEixoZ = child.findViewById<TextView>(R.id.txtValorZ)
        valorEixoZ.text = String.format("%.2f", acc.valorZ)

        parent.addView(child)
    }

    private fun escreverCSV(acc: Acelerometro, rw: Boolean){
        try {
            val filepath = Environment.getExternalStorageDirectory().path + "/valores.csv")
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

    private fun inflateLayoutTexto(parent: LinearLayout, init: Boolean){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        @SuppressLint("InflateParams") val view = inflater.inflate(R.layout.modo_texto, parent, false)
        val child = view.findViewById<LinearLayout>(R.id.text_layout)

        layoutEixoX = child.findViewById(R.id.layoutEixoX)
        layoutEixoY = child.findViewById(R.id.layoutEixoY)
        layoutEixoZ = child.findViewById(R.id.layoutEixoZ)

        txtEixoX = layoutEixoX.findViewById(R.id.valorEixoX)
        txtEixoY = layoutEixoY.findViewById(R.id.valorEixoY)
        txtEixoZ = layoutEixoZ.findViewById(R.id.valorEixoZ)

        if (! init)
            parent.removeViewAt(1)
        parent.addView(child, 1)
    }

    private fun inflateLayoutGrafico(parent: LinearLayout){
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        @SuppressLint("InflateParams") val view = inflater.inflate(R.layout.modo_grafico_barras, parent, false)
        val child = view.findViewById<LinearLayout>(R.id.graph_layout)

        layoutEixoX = child.findViewById(R.id.layoutEixoX)
        layoutEixoY = child.findViewById(R.id.layoutEixoY)
        layoutEixoZ = child.findViewById(R.id.layoutEixoZ)

        txtEixoX = layoutEixoX.findViewById(R.id.valorEixoX)
        txtEixoY = layoutEixoY.findViewById(R.id.valorEixoY)
        txtEixoZ = layoutEixoZ.findViewById(R.id.valorEixoZ)

        graphEixoX = layoutEixoX.findViewById(R.id.eixoX)
        graphEixoY = layoutEixoY.findViewById(R.id.eixoY)
        graphEixoZ = layoutEixoZ.findViewById(R.id.eixoZ)

        parent.removeViewAt(1)
        parent.addView(child, 1)
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

            txtEixoX.text = String.format("%.2f", eixoX)
            txtEixoY.text = String.format("%.2f", eixoY)
            txtEixoZ.text = String.format("%.2f", eixoZ)

            if (modo == 'G'){
                graphEixoX.post(Runnable {
                    val params = graphEixoX.layoutParams
                    params.height = if (eixoX.toInt() * 3 > 300) 300 else eixoX.toInt() * 3
                    graphEixoX.layoutParams = params
                })

                graphEixoY.post(Runnable{
                    val params = graphEixoY.layoutParams
                    params.height = if (eixoY.toInt() * 3 > 300) 300 else eixoY.toInt() * 3
                    graphEixoY.layoutParams = params
                })

                graphEixoZ.post(Runnable{
                    val params = graphEixoZ.layoutParams
                    params.height = if (eixoZ.toInt() * 3 > 300) 300 else eixoZ.toInt() * 3
                    graphEixoZ.layoutParams = params
                })
            }
        }
    }
} 
