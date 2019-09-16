package com.example.labirintumapp

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.RecyclerView

class InfoArquivo (nome: String, modif: String, dir: String){
    val nomeArquivo = nome
    val dataModificado = modif
    val diretorioArquivo = dir
}

public class ArquivoAdapter(arquivos: MutableList<InfoArquivo>,
    act: MenuRegistros, list: RecyclerViewClickListener
    ) : RecyclerView.Adapter<ArquivoAdapter.ArquivoViewHolder>() {
    
    public inner class ArquivoViewHolder : RecyclerView.ViewHolder {
        val txtNomeArquivo: TextView
        val txtDataModificado: TextView
        val btnMostraOpcoes: AppCompatImageButton
        val cardViewArquivo: LinearLayout

        constructor (v: View) : super(v) {
            txtNomeArquivo = v.findViewById(R.id.txtNomeArquivo)
            txtDataModificado = v.findViewById(R.id.txtDataModificado)
            btnMostraOpcoes = v.findViewById(R.id.btnMostraOpcoes)
            cardViewArquivo = v.findViewById(R.id.cardViewLayout)      

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

    private var listaArquivos = arquivos
    private val activity = act
    private val listener = list

    override fun getItemCount() = listaArquivos.size

    override fun onBindViewHolder(avh: ArquivoViewHolder, i: Int){
        val a = this.listaArquivos[i]
        val nomeArquivo = a.nomeArquivo
        val dataModificado = String.format("Modificado em %s", a.dataModificado)

        avh.txtNomeArquivo.setText(nomeArquivo)
        avh.txtDataModificado.setText(dataModificado)
    }

    override fun onCreateViewHolder(vg: ViewGroup, i: Int): ArquivoViewHolder {
        val itemView = LayoutInflater.from(vg.context).inflate(
            R.layout.campo_registros_anteriores, vg, false)

        return ArquivoViewHolder(itemView)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
    }

    public fun atualizaLista(novaLista: MutableList<InfoArquivo>){
        this.listaArquivos = novaLista
        notifyDataSetChanged()
    }
}