package com.example.labirintumapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import android.widget.TextView
import android.widget.TableLayout
import android.widget.TableRow
import androidx.fragment.app.FragmentManager
import android.content.Intent
import android.view.Gravity
import java.io.File
import java.util.Scanner
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import android.os.AsyncTask
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import android.view.Menu
import android.view.MenuItem
import java.net.URLConnection
import android.net.Uri

class FragmentoGraficos() : Fragment() {
    private var diretorioArquivo = ""

    public fun defineDiretorio(dir: String){
        this.diretorioArquivo = dir
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
    }
 
    override fun onCreateView(inflater: LayoutInflater,
    container: ViewGroup?, savedInstanceState: Bundle?): View {
        val itemView = inflater.inflate(R.layout.fragmento_graficos, container, false)
        
        val graphAccEixoX = itemView.findViewById<GraphView>(R.id.graphAccEixoX)
        val graphAccEixoY = itemView.findViewById<GraphView>(R.id.graphAccEixoY)
        val graphAccEixoZ = itemView.findViewById<GraphView>(R.id.graphAccEixoZ)
        val graphGirEixoX = itemView.findViewById<GraphView>(R.id.graphGirEixoX)
        val graphGirEixoY = itemView.findViewById<GraphView>(R.id.graphGirEixoY)
        val graphGirEixoZ = itemView.findViewById<GraphView>(R.id.graphGirEixoZ)

        var valoresAccEixoX = emptyArray<Double>()
        var valoresAccEixoY = emptyArray<Double>()
        var valoresAccEixoZ = emptyArray<Double>()
        var valoresGirEixoX = emptyArray<Double>()
        var valoresGirEixoY = emptyArray<Double>()
        var valoresGirEixoZ = emptyArray<Double>()

        val arquivoRegistro = File(this.diretorioArquivo)
        val scanner = Scanner(arquivoRegistro)

        scanner.nextLine()
        while (scanner.hasNextLine()) {
            val linhaLida = scanner.nextLine()
            val valores = linhaLida.split(",")

            valoresAccEixoX += valores[1].toDouble()
            valoresAccEixoY += valores[2].toDouble()
            valoresAccEixoZ += valores[3].toDouble()
            valoresGirEixoX += valores[4].toDouble()
            valoresGirEixoY += valores[5].toDouble()
            valoresGirEixoZ += valores[6].toDouble()
        }
        scanner.close()

        atualizaGrafico(graphAccEixoX, valoresAccEixoX)
        atualizaGrafico(graphAccEixoY, valoresAccEixoY)
        atualizaGrafico(graphAccEixoZ, valoresAccEixoZ)
        atualizaGrafico(graphGirEixoX, valoresGirEixoX)
        atualizaGrafico(graphGirEixoY, valoresGirEixoY)
        atualizaGrafico(graphGirEixoZ, valoresGirEixoZ)

        return itemView
    }

    private fun atualizaGrafico(grafico: GraphView, pontos: Array<Double>){
        grafico.getViewport().setXAxisBoundsManual(true)
        grafico.getViewport().setMinX(0.0)
        grafico.getViewport().setMaxX(pontos.size.toDouble())

        grafico.getViewport().setYAxisBoundsManual(true)
        grafico.getViewport().setMinY(0.0)
        grafico.getViewport().setMaxY(pontos.max()!!+10.0)

        grafico.getViewport().setScalable(true)

        var pontosArquivo = emptyArray<DataPoint>()
        for (x in 0 until pontos.size)
            pontosArquivo += DataPoint(x.toDouble(), pontos[x])
        val seriesArquivo = LineGraphSeries<DataPoint>(pontosArquivo)
        grafico.addSeries(seriesArquivo)
    }
}

class FragmentoTexto() : Fragment() {
    private var diretorioArquivo = ""
    private lateinit var activity: MenuRegistrosAnteriores

    public fun inicializaFragmento(dir: String, act: MenuRegistrosAnteriores){
        this.diretorioArquivo = dir
        this.activity = act
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
    }
 
    override fun onCreateView(inflater: LayoutInflater,
    container: ViewGroup?, savedInstanceState: Bundle?): View {
        val itemView = inflater.inflate(R.layout.fragmento_texto, container, false)

        val tabelaDados = itemView.findViewById<TableLayout>(R.id.tabelaDados)

        val arquivoRegistro = File(this.diretorioArquivo)
        val scanner = Scanner(arquivoRegistro)

        scanner.nextLine()
        while (scanner.hasNextLine()) {
            val linhaLida = scanner.nextLine()
            val valores = linhaLida.split(",")
            atualizaTabela(tabelaDados, valores.map{it.toDouble()}.toTypedArray())
        }
        scanner.close()

        return itemView
    }

    private fun atualizaTabela(tabela: TableLayout, linha: Array<Double>){
        var row = TableRow(this.activity)

        val lp = TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        row.weightSum = 7f
        row.layoutParams =  lp

        var col = TextView(this.activity)
        var trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.layoutParams = trlp
        col.gravity = Gravity.CENTER
        col.text = String.format("%d", linha[0].toInt())
        row.addView(col)

        col = TextView(this.activity)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", linha[1]).replace(',', '.')
        row.addView(col)

        col = TextView(this.activity)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", linha[2]).replace(',', '.')
        row.addView(col)

        col = TextView(this.activity)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", linha[3]).replace(',', '.')
        row.addView(col)

        col = TextView(this.activity)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", linha[4]).replace(',', '.')
        row.addView(col)

        col = TextView(this.activity)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", linha[5]).replace(',', '.')
        row.addView(col)

        col = TextView(this.activity)
        trlp = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1f)
        col.gravity = Gravity.CENTER
        col.layoutParams = trlp
        col.text = String.format("%.2f", linha[6]).replace(',', '.')
        row.addView(col)

        tabela.addView(row)
    }
}

class MenuRegistrosAnteriores : AppCompatActivity() {
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var progressDialog: AlertDialog
    private var builder: AlertDialog.Builder? = null

    private var diretorioArquivo = ""
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_registros_anteriores)

        val intentRecebido = intent
        diretorioArquivo = intentRecebido.getStringExtra("KEY_DIRETORIO_ARQUIVO") ?: ""
        setTitle(diretorioArquivo.split("/").last())
        
        TaskCarregaArquivo().execute()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.toolbar_registros_anteriores, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.item_compartilhar -> {
                val intentCompartilhar = Intent(Intent.ACTION_SEND)
                val arquivoEnviar = File(diretorioArquivo)

                if (arquivoEnviar.exists()){
                    intentCompartilhar.setType(URLConnection
                        .guessContentTypeFromName(arquivoEnviar.name))
                    
                    intentCompartilhar.putExtra(Intent.EXTRA_STREAM,
                        Uri.parse("file://" + diretorioArquivo))

                    intentCompartilhar.putExtra(Intent.EXTRA_SUBJECT,
                        "Compartilhar arquivo")

                    intentCompartilhar.putExtra(Intent.EXTRA_TEXT,
                        "Compartilhar arquivo")

                    startActivity(Intent.createChooser(
                        intentCompartilhar, "Compartilhar arquivo"))
                }

                return true
            }

            else -> return super.onOptionsItemSelected(item);
        }
    }

    public inner class ViewPagerAdapter : FragmentPagerAdapter {
        private val mFragmentList = arrayListOf<Fragment>()
        private val mFragmentTitleList = arrayListOf<String>()
        
        constructor(manager: FragmentManager) : super(manager){ }
 
        override fun getCount() = mFragmentList.size
        override fun getItem(pos: Int) = mFragmentList[pos] 
        override fun getPageTitle(pos: Int) = mFragmentTitleList[pos]
 
        public fun adicionaFragmento(fragmento: Fragment, nome: String){
            mFragmentList.add(fragmento)
            mFragmentTitleList.add(nome)
        }
    }

    public inner class TaskCarregaArquivo : AsyncTask<Void, Void?, ViewPagerAdapter>(){
        override protected fun onPreExecute(){
            if (builder == null){
                builder = AlertDialog.Builder(this@MenuRegistrosAnteriores)
                builder!!.setTitle("Carregando arquivo...")
                builder!!.setCancelable(false)
                
                val progressBar = ProgressBar(this@MenuRegistrosAnteriores)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                progressBar.layoutParams = layoutParams

                builder!!.setView(progressBar, 0, 10, 0, 0)
                progressDialog = builder!!.create()
            }

            progressDialog.show()
        }

        override protected fun doInBackground(vararg params: Void?): ViewPagerAdapter {
            viewPager = findViewById<ViewPager>(R.id.viewpager)
            val adapter = ViewPagerAdapter(getSupportFragmentManager())
            
            val fragGraf = FragmentoGraficos()
            fragGraf.defineDiretorio(diretorioArquivo)

            val fragText = FragmentoTexto()
            fragText.inicializaFragmento(diretorioArquivo, this@MenuRegistrosAnteriores)

            adapter.adicionaFragmento(fragGraf, "GRÁFICOS")
            adapter.adicionaFragmento(fragText, "TEXTO")

            return adapter
        }

        override protected fun onPostExecute(result: ViewPagerAdapter){
            viewPager.setAdapter(result)
 
            tabLayout = findViewById<TabLayout>(R.id.tabs)
            tabLayout.setupWithViewPager(viewPager)

            progressDialog.dismiss()
        }
    }
}