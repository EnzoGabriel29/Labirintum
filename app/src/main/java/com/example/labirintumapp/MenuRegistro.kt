package com.example.labirintumapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.io.File
import java.net.URLConnection
import java.util.Scanner

class FragmentoGraficos : Fragment() {
    private var diretorioArquivo = ""
    private var maxValores = 0.0

    fun defineDiretorio(dir: String){
        this.diretorioArquivo = dir
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

        maxValores = arrayOf(valoresAccEixoX.max() ?: 0.0,
            valoresAccEixoY.max() ?: 0.0, valoresAccEixoZ.max() ?: 0.0,
            valoresGirEixoX.max() ?: 0.0, valoresGirEixoY.max() ?: 0.0,
            valoresGirEixoZ.max() ?: 0.0).max() ?: 0.0

        atualizaGrafico(graphAccEixoX, valoresAccEixoX)
        atualizaGrafico(graphAccEixoY, valoresAccEixoY)
        atualizaGrafico(graphAccEixoZ, valoresAccEixoZ)
        atualizaGrafico(graphGirEixoX, valoresGirEixoX)
        atualizaGrafico(graphGirEixoY, valoresGirEixoY)
        atualizaGrafico(graphGirEixoZ, valoresGirEixoZ)

        return itemView
    }

    private fun atualizaGrafico(grafico: GraphView, pontos: Array<Double>){
        grafico.viewport.isXAxisBoundsManual = true
        grafico.viewport.setMinX(0.0)
        grafico.viewport.setMaxX(pontos.size.toDouble())

        grafico.viewport.isYAxisBoundsManual = true
        grafico.viewport.setMinY(0.0)
        grafico.viewport.setMaxY(maxValores+10.0)

        grafico.viewport.isScalable = true

        var pontosArquivo = emptyArray<DataPoint>()
        for (x in 0 until pontos.size)
            pontosArquivo += DataPoint(x.toDouble(), pontos[x])
        val seriesArquivo = LineGraphSeries(pontosArquivo)
        grafico.addSeries(seriesArquivo)
    }
}

class FragmentoTexto : Fragment() {
    private var diretorioArquivo = ""
    private lateinit var activity: MenuRegistrosAnteriores

    fun inicializaFragmento(dir: String, act: MenuRegistrosAnteriores){
        this.diretorioArquivo = dir
        this.activity = act
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
        val row = TableRow(this.activity)

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
        title = diretorioArquivo.split("/").last()
        
        TaskCarregaArquivo().execute()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_registros_anteriores, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.item_compartilhar -> {
                val arquivo = File(diretorioArquivo)

                val intent = Intent(Intent.ACTION_SEND)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                val uri = FileProvider.getUriForFile(this, "com.example.labirintumapp.fileprovider", arquivo)
                intent.putExtra(Intent.EXTRA_TEXT, "Registrei esse arquivo no aplicativo Labirintum.")
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.setDataAndType(uri, "text/csv")

                if (arquivo.exists() && intent.resolveActivity(packageManager) != null)
                    startActivity(Intent.createChooser(intent, "Compartilhar usando"))

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class ViewPagerAdapter(manager: FragmentManager) : FragmentPagerAdapter(manager){
        private val mFragmentList = arrayListOf<Fragment>()
        private val mFragmentTitleList = arrayListOf<String>()

        override fun getCount() = mFragmentList.size
        override fun getItem(pos: Int) = mFragmentList[pos] 
        override fun getPageTitle(pos: Int) = mFragmentTitleList[pos]
 
        fun adicionaFragmento(fragmento: Fragment, nome: String){
            mFragmentList.add(fragmento)
            mFragmentTitleList.add(nome)
        }
    }

    inner class TaskCarregaArquivo : AsyncTask<Void, Void?, ViewPagerAdapter>(){
        override fun onPreExecute(){
            if (builder == null){
                builder = AlertDialog.Builder(this@MenuRegistrosAnteriores)
                builder!!.setTitle("Carregando arquivo...")
                builder!!.setCancelable(false)
                
                val progressBar = ProgressBar(this@MenuRegistrosAnteriores)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT)
                progressBar.layoutParams = layoutParams

                builder!!.setView(progressBar)
                progressDialog = builder!!.create()
            }

            progressDialog.show()
        }

        override fun doInBackground(vararg params: Void?): ViewPagerAdapter {
            viewPager = findViewById(R.id.viewpager)
            val adapter = ViewPagerAdapter(supportFragmentManager)
            
            val fragGraf = FragmentoGraficos()
            fragGraf.defineDiretorio(diretorioArquivo)

            val fragText = FragmentoTexto()
            fragText.inicializaFragmento(diretorioArquivo, this@MenuRegistrosAnteriores)

            adapter.adicionaFragmento(fragGraf, "GRÁFICOS")
            adapter.adicionaFragmento(fragText, "TEXTO")

            return adapter
        }

        override fun onPostExecute(result: ViewPagerAdapter){
            viewPager.adapter = result
 
            tabLayout = findViewById(R.id.tabs)
            tabLayout.setupWithViewPager(viewPager)

            progressDialog.dismiss()
        }
    }
}