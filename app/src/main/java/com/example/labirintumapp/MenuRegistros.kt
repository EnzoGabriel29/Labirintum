package com.example.labirintumapp

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat

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
    private lateinit var menuOrdenar: Menu
    private var ordenarNome = true
    private var ordenarCresc = true 

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

        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        ordenarNome = pref.getBoolean("KEY_ORDENAR_REGISTROS_NOME", true)
        ordenarCresc = pref.getBoolean("KEY_ORDENAR_REGISTROS_CRESC", true)

        arquivoAdapter = ArquivoAdapter(infoArquivos, this, this)
        ordenaLista(ordenarNome, ordenarCresc, true)
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

    private fun ordenaLista(nome: Boolean, cresc: Boolean, primeiraVez: Boolean){
        if ((nome != ordenarNome || cresc != ordenarCresc) || primeiraVez){
            val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
            val prefEditor = pref.edit()

            prefEditor.putBoolean("KEY_ORDENAR_REGISTROS_NOME", nome)
            prefEditor.putBoolean("KEY_ORDENAR_REGISTROS_CRESC", cresc)
            prefEditor.commit() 

            ordenarNome = nome
            ordenarCresc = cresc

            if (nome) infoArquivos.sortWith(
                compareBy<InfoArquivo>({ it.nomeArquivo }))

            else infoArquivos.sortWith(
                compareBy<InfoArquivo>({ SimpleDateFormat(
                    "dd/MM/yyyy, HH:mm:ss").parse(it
                    .dataModificado).time }))

            if (!cresc) infoArquivos.reverse()

            infoArquivosUI = infoArquivos
            arquivoAdapter.atualizaLista(infoArquivos)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_registros, menu)
        
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item1: MenuItem): Boolean {
        when (item1.itemId){
            R.id.item_ordenar -> {
                val itemView = findViewById<View>(R.id.item_ordenar)
                val popup = PopupMenu(this, itemView)  
                popup.menuInflater.inflate(R.menu.popup_ordenar, popup.menu)  

                if (ordenarNome) popup.menu.getItem(0).setChecked(true)
                else popup.menu.getItem(1).setChecked(true)

                popup.setOnMenuItemClickListener {
                    item2: MenuItem ->
                        when (item2.itemId){
                            R.id.action_ordenar_nome -> {
                                popup.menu.getItem(0).setChecked(true)
                                popup.menu.getItem(1).setChecked(false)
                                ordenaLista(true, ordenarCresc, false)
                            }

                            R.id.action_ordenar_data -> {
                                popup.menu.getItem(0).setChecked(false)
                                popup.menu.getItem(1).setChecked(true)
                                ordenaLista(false, ordenarCresc, false)
                            }
                        }

                    true
                }

                popup.show()

                return true
            }

            R.id.item_ordenar_por -> {
                val itemView = findViewById<View>(R.id.item_ordenar_por)
                val popup = PopupMenu(this, itemView)  
                popup.menuInflater.inflate(R.menu.popup_ordenar_por, popup.menu)  

                if (ordenarCresc) popup.menu.getItem(0).setChecked(true)
                else popup.menu.getItem(1).setChecked(true)

                popup.setOnMenuItemClickListener {
                    item2: MenuItem ->
                        when (item2.itemId){
                            R.id.action_ordenar_cresc -> {
                                popup.menu.getItem(0).setChecked(true)
                                popup.menu.getItem(1).setChecked(false)
                                ordenaLista(ordenarNome, true, false)
                            }

                            R.id.action_ordenar_decresc -> {
                                popup.menu.getItem(0).setChecked(false)
                                popup.menu.getItem(1).setChecked(true)
                                ordenaLista(ordenarNome, false, false)
                            }
                        }

                    true
                }

                popup.show()

                return true
            }

            else -> return super.onOptionsItemSelected(item1)
        }
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
                val builderExclusao = AlertDialog.Builder(this)
                builderExclusao.setTitle("Confirmar exclusão")
                builderExclusao.setMessage("Deseja excluir \"" + a.nomeArquivo + "\"?")

                builderExclusao.setPositiveButton("OK"){
                    dialog: DialogInterface, _: Int ->
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

                        dialog.dismiss()
                }

                builderExclusao.setNegativeButton("Cancelar"){
                    dialog: DialogInterface, _: Int ->
                        dialog.dismiss()   
                }
                
                builderExclusao.create().show()
            }

            else -> { }
        } 
    }
}