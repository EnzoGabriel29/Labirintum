package com.example.labirintumapp

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

fun isNumerico(str: String): Boolean {
    try {
        str.toInt()
        return true
    } catch (e: NumberFormatException) {
        return false
    }
}

const val TAG = "LABIRINTUMAPP"

class MainActivity : AppCompatActivity(){
    private lateinit var btnRegistros: Button
    private lateinit var btnIniciar: LinearLayout
    private lateinit var btnConfigs: LinearLayout
    private lateinit var mainLayout: RelativeLayout 

    private var numLinhasMaximo = 0
    private var intervaloGravacao = 0
    private var graficosVisiveis = 0
    private var extensaoArquivo = ""

    companion object {
        private val MY_PERMISSIONS_REQUEST_WRITE = 1;
    }
    
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)

        if (!isPermissaoEscrita())
            verificarPermissaoEscrita()
        
        btnIniciar = findViewById(R.id.btnIniciar)
        btnConfigs = findViewById(R.id.btnConfigs)
        btnRegistros = findViewById(R.id.btnRegistros)
        mainLayout = findViewById(R.id.main_layout)

        btnIniciar.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                val bDefineNomeArquivo = AlertDialog.Builder(this@MainActivity)
                bDefineNomeArquivo.setTitle("Qual é o nome do arquivo?")

                val input = EditText(this@MainActivity)
                val lp2 = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
                input.layoutParams = lp2
                input.hint = "Insira o nome do arquivo"
                bDefineNomeArquivo.setView(input, 20, 0, 20, 0)

                bDefineNomeArquivo.setPositiveButton("Iniciar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, id: Int) {
                        var nomeArquivo = input.text.toString()
                        val diretorioGravacao = this@MainActivity.geraDiretorioGravacao(nomeArquivo)
                        this@MainActivity.iniciarGravacao(diretorioGravacao, MenuGravacao.MODO_PADRAO)
                    }
                })
                bDefineNomeArquivo.setNegativeButton("Cancelar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        dialog.cancel()
                    }
                })
                bDefineNomeArquivo.show()
            }
        })

        btnConfigs.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                defineLayoutConfiguracoes()
            }
        })

        btnRegistros.setOnClickListener(object : View.OnClickListener {
            override public fun onClick(v: View) {
                val intent = Intent(applicationContext, MenuRegistros::class.java)
                startActivity(intent)
            }
        })

        atualizaPadroes()
    }

    private fun atualizaPadroes(){
        val intentRecebido = intent
        if (intentRecebido == null) return

        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        val prefEditor = pref.edit()

        if (pref.getBoolean("KEY_PRIMEIRA_EXECUCAO", true)) {
            prefEditor.putBoolean("KEY_PRIMEIRA_EXECUCAO", false)
            prefEditor.putBoolean("KEY_ORDENAR_REGISTROS_NOME", true)
            prefEditor.putBoolean("KEY_ORDENAR_REGISTROS_CRESC", true)
            prefEditor.putInt("KEY_NUM_MAX_LINHAS", 120)
            prefEditor.putInt("KEY_DELAY_GRAVACAO", 200)
            prefEditor.putString("KEY_EXTENSAO_ARQUIVO", "csv")
            prefEditor.putInt("KEY_GRAFICOS_VISIVEIS", 3)
            prefEditor.commit()
            return
        }

        if (intentRecebido.getStringExtra("KEY_NOME_ACTIVITY") == "MenuSettings"
        && intentRecebido.getStringExtra("KEY_ACAO_USUARIO") == "salvar"){
            prefEditor.putInt("KEY_NUM_MAX_LINHAS", intentRecebido.getStringExtra("KEY_NUM_MAX_LINHAS").toInt())
            prefEditor.putInt("KEY_DELAY_GRAVACAO", intentRecebido.getStringExtra("KEY_DELAY_GRAVACAO").toInt())
            prefEditor.putString("KEY_EXTENSAO_ARQUIVO", intentRecebido.getStringExtra("KEY_EXTENSAO_ARQUIVO"))
            prefEditor.putInt("KEY_GRAFICOS_VISIVEIS", intentRecebido.getStringExtra("KEY_GRAFICOS_VISIVEIS").toInt())
            prefEditor.commit()
        }

        numLinhasMaximo = pref.getInt("KEY_NUM_MAX_LINHAS", 120)
        intervaloGravacao = pref.getInt("KEY_DELAY_GRAVACAO", 200)
        graficosVisiveis = pref.getInt("KEY_GRAFICOS_VISIVEIS", 3)
        extensaoArquivo = pref.getString("KEY_EXTENSAO_ARQUIVO", "csv") ?: "csv"
    }

    override fun onResume(){
        super.onResume()
        this.atualizaPadroes()
    }

    private fun geraDiretorioGravacao(nomeArq: String): String {
        var nomeArquivo = nomeArq
        val diretorioPai = "${Environment.getExternalStorageDirectory().path}/LabirintumDados"
        var intCont = 0
        var strCont = ""

        if (nomeArquivo.endsWith(extensaoArquivo))
            nomeArquivo.dropLast(extensaoArquivo.length)

        val pasta = File(diretorioPai)
        if (!pasta.exists()){
            pasta.mkdirs()
        }

        var diretorioArquivo: String
        while (true) {
            diretorioArquivo = "${diretorioPai}/${nomeArquivo}${strCont}.${extensaoArquivo}"
            var arquivo = File(diretorioArquivo)

            if (arquivo.isFile()){
                intCont += 1
                strCont = " (%02d)".format(intCont)
            } else return diretorioArquivo
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_remote -> defineLayoutRemoto()
        }

        return true
    }

    private fun iniciarGravacao(diretorioArquivo: String, modoGravacao: Int){
        if (isPermissaoEscrita()){
            val intent = Intent(applicationContext, MenuGravacao::class.java)
            intent.putExtra("KEY_NOME_ACTIVITY", "MainActivity")
            intent.putExtra("KEY_DIRETORIO_ARQUIVO", diretorioArquivo)
            intent.putExtra("KEY_MODO_GRAVACAO", modoGravacao)
            intent.putExtra("KEY_EXTENSAO_ARQUIVO", this.extensaoArquivo)
            intent.putExtra("KEY_NUM_MAX_LINHAS", this.numLinhasMaximo)
            intent.putExtra("KEY_DELAY_GRAVACAO", this.intervaloGravacao)
            intent.putExtra("KEY_GRAFICOS_VISIVEIS", this.graficosVisiveis)
            startActivity(intent)
            finish()

        } else {
            Toast.makeText(this, "Não foi possível começar a gravação: " +
                "o aplicativo não tem permissões suficientes.", Toast.LENGTH_LONG).show()
        }
    }

    private fun defineLayoutRemoto(){
        Toast.makeText(this, "O modo remoto foi ativado!", Toast.LENGTH_SHORT).show()
        mainLayout.removeView(btnIniciar)
        val diretorioGravacao = geraDiretorioGravacao("bluetoothRec")
        iniciarGravacao(diretorioGravacao, MenuGravacao.MODO_REMOTO)
    }

    private fun defineLayoutConfiguracoes(){
        val intent = Intent(applicationContext, MenuSettings::class.java)
        intent.putExtra("KEY_NOME_ACTIVITY", "MainActivity")
        intent.putExtra("KEY_EXTENSAO_ARQUIVO", this.extensaoArquivo.toString())
        intent.putExtra("KEY_NUM_MAX_LINHAS", this.numLinhasMaximo.toString())
        intent.putExtra("KEY_DELAY_GRAVACAO", this.intervaloGravacao.toString())
        intent.putExtra("KEY_GRAFICOS_VISIVEIS", this.graficosVisiveis.toString())
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE -> {
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Não foi possível ativar a " + 
                        "funcionalidade de escrita.", Toast.LENGTH_LONG).show()
                    return
                }
            }

            else -> { }       
        }
    }

    private fun isPermissaoEscrita(): Boolean {
        return (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == 
            PackageManager.PERMISSION_GRANTED)
    }

    private fun verificarPermissaoEscrita(){
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
            MY_PERMISSIONS_REQUEST_WRITE)
    }
}