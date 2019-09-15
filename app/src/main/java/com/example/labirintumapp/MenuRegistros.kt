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
import android.text.TextWatcher
import android.text.Editable

interface RecyclerViewClickListener {
    fun onCardClicked(pos: Int)
    fun onItemClicked(nome: String, pos: Int)
}

class MenuRegistros : AppCompatActivity() , RecyclerViewClickListener {
    private var listaArquivos = emptyArray<File>()
    private var infoArquivos = arrayListOf<InfoArquivo>()
    private var infoArquivosUI = arrayListOf<InfoArquivo>()
    private lateinit var arquivoAdapter: ArquivoAdapter
    private lateinit var edtBuscaRegistros: EditText

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
            infoArquivos.add(InfoArquivo(nome, modif, dir))
        }

        val recyclerList = findViewById<RecyclerView>(R.id.cardList)
        recyclerList.setHasFixedSize(true)
        val llm = LinearLayoutManager(this)
        llm.setOrientation(LinearLayoutManager.VERTICAL)
        recyclerList.setLayoutManager(llm)

        arquivoAdapter = ArquivoAdapter(infoArquivos, this, this)
        recyclerList.setAdapter(arquivoAdapter)

        edtBuscaRegistros = findViewById(R.id.edtBuscaRegistros)
        edtBuscaRegistros.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable){
                infoArquivosUI = arrayListOf<InfoArquivo>()
                
                for (arquivo in infoArquivos){
                   if (arquivo.nomeArquivo.startsWith(s.toString()))
                       infoArquivosUI.add(arquivo)
                }
                arquivoAdapter.atualizaLista(infoArquivosUI)
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int){ }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int){ }
        })
    }

    override public fun onCardClicked(pos: Int){
        val a = infoArquivosUI[pos]

        val intent = Intent(applicationContext, MenuRegistrosAnteriores::class.java)
        intent.putExtra("KEY_DIRETORIO_ARQUIVO", a.diretorioArquivo)
        startActivity(intent)
    }

    override public fun onItemClicked(nome: String, pos: Int){
        when (nome){
            "Excluir" -> {
                val a = infoArquivosUI[pos]
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirmar exclusão")
                builder.setMessage("Deseja excluir \"" + a.nomeArquivo + "\"?")

                builder.setPositiveButton("OK", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        val arquivo = File(a.diretorioArquivo)
                        if (arquivo.exists()){
                            arquivo.delete()
                            infoArquivosUI.removeAt(pos)
                            infoArquivos.remove(a)
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