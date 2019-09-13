package com.example.labirintumapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import android.view.View
import android.view.LayoutInflater
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.view.MotionEvent
import android.content.Context
import java.io.File
import java.util.Scanner
import java.text.SimpleDateFormat
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Environment
import android.view.ViewGroup
import android.util.Log
import android.os.Handler

interface RecyclerViewClickListener {
    fun onCardClicked(pos: Int)
    fun onItemClicked(nome: String, pos: Int)
}

class MenuRegistros : AppCompatActivity() , RecyclerViewClickListener {
    private var listaArquivos = emptyArray<File>()
    private var infoArquivos = arrayListOf<InfoArquivo>()
    private lateinit var arquivoAdapter: ArquivoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_registros)

        val dirArquivos = "${Environment.getExternalStorageDirectory().path}/LabirintumDados"
        val pastaArquivos = File(dirArquivos).listFiles()

        for (arquivo in pastaArquivos){
            if (arquivo.name.endsWith(".csv") || arquivo.name.endsWith(".txt"))
                listaArquivos += arquivo
        }

        for (arquivo in listaArquivos){
            val nome = arquivo.name
            val modif = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss").format(arquivo.lastModified())
            val dir = arquivo.absolutePath
            val dados = retornaValoresCSV(arquivo, 11)
            infoArquivos.add(InfoArquivo(nome, modif, dir, dados))
        }

        val recyclerList = findViewById<RecyclerView>(R.id.cardList)
        recyclerList.setHasFixedSize(true)
        val llm = LinearLayoutManager(this)
        llm.setOrientation(LinearLayoutManager.VERTICAL)
        recyclerList.setLayoutManager(llm)

        arquivoAdapter = ArquivoAdapter(infoArquivos, this, this)
        recyclerList.setAdapter(arquivoAdapter)
    }

    private fun retornaValoresCSV(arquivo: File, qtd: Int): Array<Double> {
        val scanner = Scanner(arquivo)
        var linhaLida = ""
        var arrayDados = emptyArray<Double>()

        scanner.nextLine()
        for (i in 1..qtd){
            if (!scanner.hasNextLine()) break

            linhaLida = scanner.nextLine()
            arrayDados += linhaLida.split(",")[1].toDouble()
        }

        return arrayDados
    }

    override public fun onCardClicked(pos: Int){
    
    }

    override public fun onItemClicked(nome: String, pos: Int){
        when (nome){
            "Excluir" -> {
                val a = infoArquivos[pos]
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirmar exclusão")
                builder.setMessage("Deseja excluir \"" + a.nomeArquivo + "\"?")

                builder.setPositiveButton("OK", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        val arquivo = File(a.diretorioArquivo)
                        if (arquivo.exists()){
                            arquivo.delete()
                            infoArquivos.removeAt(pos)
                            arquivoAdapter.notifyItemRemoved(pos)
                        } else {
                            Toast.makeText(this@MenuRegistros, "Não foi possível " + 
                                "excluir o arquivo: o arquivo não existe!",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                })

                builder.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        dialog.dismiss()
                    }
                })
                
                builder.create().show()
            }

            else -> { }
        }
        
   }
}