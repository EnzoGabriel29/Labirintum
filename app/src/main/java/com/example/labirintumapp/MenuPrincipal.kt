package com.example.labirintumapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

const val TAG = "LABIRINTUMAPP"

class MenuPrincipal : AppCompatActivity(){
    private lateinit var btnRegistros: Button
    private lateinit var btnIniciar: LinearLayout
    private lateinit var btnConfigs: LinearLayout
    private lateinit var mainLayout: RelativeLayout 

    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE = 1
    }
    
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)

        if (!isPermissaoEscrita())
            verificarPermissaoEscrita()
        
        btnIniciar = findViewById(R.id.btnIniciar)
        btnConfigs = findViewById(R.id.btnConfigs)
        btnRegistros = findViewById(R.id.btnRegistros)
        mainLayout = findViewById(R.id.main_layout)

        btnIniciar.setOnClickListener {
            val builderNomeArq = AlertDialog.Builder(this)
            builderNomeArq.setTitle("Qual é o nome do arquivo?")

            val input = layoutInflater.inflate(R.layout.campo_nome_arquivo, null, false)
            val edtNomeArq = input.findViewById<EditText>(R.id.edtNomeArq)
            builderNomeArq.setView(input)

            builderNomeArq.setPositiveButton("Iniciar"){
                dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                    val nomeArquivo = edtNomeArq.text.toString()
                    val diretorioGravacao = geraDiretorioGravacao(nomeArquivo)
                    iniciarGravacao(diretorioGravacao, MenuGravacao.MODO_PADRAO)
            }

            builderNomeArq.setNegativeButton("Cancelar"){
                dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
            }

            builderNomeArq.show()
        }

        btnConfigs.setOnClickListener {
            defineLayoutConfiguracoes()
        }

        btnRegistros.setOnClickListener {
            val intentRegs = Intent(applicationContext, MenuRegistros::class.java)
            startActivity(intentRegs)
        }

        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        val prefEditor = pref.edit()

        if (pref.getBoolean("KEY_PRIMEIRA_EXECUCAO", true)) {
            prefEditor.putBoolean("KEY_PRIMEIRA_EXECUCAO", false)
            prefEditor.putBoolean("KEY_ORDENAR_REGISTROS_NOME", true)
            prefEditor.putBoolean("KEY_ORDENAR_REGISTROS_CRESC", true)
            prefEditor.putInt("KEY_NUM_MAX_LINHAS", 120)
            prefEditor.putBoolean("KEY_IS_NUM_MAX_LINHAS", true)
            prefEditor.putInt("KEY_DELAY_GRAVACAO", 200)
            prefEditor.putString("KEY_EXTENSAO_ARQUIVO", "csv")
            prefEditor.putInt("KEY_GRAFICOS_VISIVEIS", 3)
            prefEditor.putString("KEY_MODO_CALCULO", "var")
            prefEditor.apply()
        }
    }

    private fun geraDiretorioGravacao(nomeArq: String): String {
        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        val extensaoArquivo = pref.getString("KEY_EXTENSAO_ARQUIVO", "csv") ?: "csv"

        val diretorioPai = "${getExternalFilesDir(null)!!.path}/LabirintumDados"
        var intCont = 0
        var strCont = ""

        if (nomeArq.endsWith(extensaoArquivo))
            nomeArq.dropLast(extensaoArquivo.length)

        val pasta = File(diretorioPai)
        if (!pasta.exists()){
            pasta.mkdirs()
        }

        var diretorioArquivo: String
        while (true) {
            diretorioArquivo = "$diretorioPai/$nomeArq$strCont.$extensaoArquivo"
            val arquivo = File(diretorioArquivo)

            if (arquivo.isFile){
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
            val intentGravacao = Intent(applicationContext, MenuGravacao::class.java)
            intentGravacao.putExtra("KEY_DIRETORIO_ARQUIVO", diretorioArquivo)
            intentGravacao.putExtra("KEY_MODO_GRAVACAO", modoGravacao)
            startActivity(intentGravacao)
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
        val intentConfigs = Intent(applicationContext, MenuOpcoes::class.java)
        startActivity(intentConfigs)
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