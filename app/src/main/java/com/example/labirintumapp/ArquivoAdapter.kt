package com.example.labirintumapp

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import android.widget.Toast
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.text.SimpleDateFormat
import android.util.Log
import android.widget.PopupMenu
import android.view.MenuItem
import java.io.File
import androidx.cardview.widget.CardView

class InfoArquivo (nome: String, modif: String, 
        dir: String, dados: Array<Double>){
    val nomeArquivo = nome
    val dataModificado = modif
    val diretorioArquivo = dir
    val dadosArquivo = dados
}

public class ArquivoAdapter(arquivos: MutableList<InfoArquivo>,
    act: MenuRegistros, list: RecyclerViewClickListener
    ) : RecyclerView.Adapter<ArquivoAdapter.ArquivoViewHolder>() {
    
    public inner class ArquivoViewHolder : RecyclerView.ViewHolder {
        val txtNomeArquivo: TextView
        val txtDataModificado: TextView
        val graphDadosArquivo: GraphView
        val btnMostraOpcoes: AppCompatImageButton
        val cardViewArquivo: CardView

        constructor (v: View) : super(v) {
            txtNomeArquivo = v.findViewById(R.id.txtNomeArquivo)
            txtDataModificado = v.findViewById(R.id.txtDataModificado)
            graphDadosArquivo = v.findViewById(R.id.graphDadosArquivo)
            btnMostraOpcoes = v.findViewById(R.id.btnMostraOpcoes)
            cardViewArquivo = v.findViewById(R.id.cardViewArquivo)      

            btnMostraOpcoes.setOnClickListener {
                val popup = PopupMenu(activity, btnMostraOpcoes)  
                popup.menuInflater.inflate(R.menu.popup_registros, popup.menu)  

                popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {  
                    override public fun onMenuItemClick(item: MenuItem): Boolean {
                        listener.onItemClicked(item.title.toString(), adapterPosition)
                        return true
                    }  
                })

                popup.show()
            }

            cardViewArquivo.setOnClickListener {
                listener.onCardClicked(adapterPosition)
            }
        }
    }

    private val listaArquivos = arquivos
    private val activity = act
    private val listener = list

    override public fun getItemCount(): Int {
        return listaArquivos.size
    }

    override public fun onBindViewHolder(avh: ArquivoViewHolder, i: Int){
        val a = this.listaArquivos[i]
        val nomeArquivo = a.nomeArquivo
        val dataModificado = String.format("Modificado em %s", a.dataModificado)

        avh.txtNomeArquivo.setText(nomeArquivo)
        avh.txtDataModificado.setText(dataModificado)

        avh.graphDadosArquivo.getViewport().setXAxisBoundsManual(true)
        avh.graphDadosArquivo.getViewport().setMinX(0.0)
        avh.graphDadosArquivo.getViewport().setMaxX(10.0)
        avh.graphDadosArquivo.getViewport().setYAxisBoundsManual(true)
        avh.graphDadosArquivo.getViewport().setMinY(0.0)
        avh.graphDadosArquivo.getViewport().setMaxY(10.0)
        var pontosArquivo = emptyArray<DataPoint>()
        for (x in 0 until a.dadosArquivo.size)
            pontosArquivo += DataPoint(x.toDouble(), a.dadosArquivo[x])
        val seriesArquivo = LineGraphSeries<DataPoint>(pontosArquivo)
        avh.graphDadosArquivo.addSeries(seriesArquivo)

        avh.btnMostraOpcoes
    }

    override public fun onCreateViewHolder(vg: ViewGroup, i: Int): ArquivoViewHolder {
        val itemView = LayoutInflater.from(vg.context).inflate(
            R.layout.campo_registros_anteriores, vg, false)

        return ArquivoViewHolder(itemView)
    }

    override public fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
    }
}