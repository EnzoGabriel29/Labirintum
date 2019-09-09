package com.example.labirintumapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.widget.*
import java.util.*
import android.os.Build
import android.preference.PreferenceManager
import android.content.Context
import java.io.File
import android.os.Environment
import android.view.View
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat

fun isNumerico(str: String): Boolean {
    try {
        str.toInt()
        return true
    } catch (e: NumberFormatException) {
        return false
    }
}


class MainActivity : AppCompatActivity() {    
    private lateinit var btnIniciar: Button
    private lateinit var btnConfigs: Button
    private lateinit var mainLayout: RelativeLayout 

    private var numLinhasMaximo = 0
    private var intervaloGravacao = 0
    private var extensaoArquivo = ""

    companion object {
        private val MY_PERMISSIONS_REQUEST_WRITE = 1;
    }
    
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_principal)

        if (!isPermissaoEscrita()){
            verificarPermissaoEscrita()
        }

        btnIniciar = findViewById(R.id.btnIniciar)
        btnConfigs = findViewById(R.id.btnConfigs)
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

        atualizaPadroes()
    }

    private fun atualizaPadroes(){
        val intentRecebido = intent
        if (intentRecebido == null) return

        val pref = applicationContext.getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        val prefEditor = pref.edit()

        if (pref.getBoolean("first_run", true)) {
            prefEditor.putBoolean("first_run", false)
            prefEditor.putInt("max_lines", 120)
            prefEditor.putInt("rec_delay", 200)
            prefEditor.putString("file_type", "csv")
            prefEditor.commit()
            return
        }

        if (intentRecebido.getStringExtra("act_name") == "MenuSettings"){
            if (intentRecebido.getStringExtra("user_action") == "salvar"){
                prefEditor.putInt("max_lines", intentRecebido.getStringExtra("max_lines").toInt())
                prefEditor.putInt("rec_delay", intentRecebido.getStringExtra("rec_delay").toInt())
                prefEditor.putString("file_type", intentRecebido.getStringExtra("file_type"))
                prefEditor.commit()
            }
        }

        numLinhasMaximo = pref.getInt("max_lines", 120)
        intervaloGravacao = pref.getInt("rec_delay", 200)
        extensaoArquivo = pref.getString("file_type", "csv") ?: return
    }

    override fun onResume(){
        super.onResume()
        this.atualizaPadroes()
    }

    private fun geraDiretorioGravacao(nomeArq: String): String {
        var nomeArquivo = nomeArq
        val diretorioPai = "${Environment.getExternalStorageDirectory().path}/AccelerometerSaveData"
        var intCont = 0
        var strCont = ""
        val sufixo = nomeArquivo.takeLast(2)

        if (nomeArquivo.endsWith(extensaoArquivo))
            nomeArquivo.dropLast(extensaoArquivo.length)

        if (isNumerico(sufixo)) {
            intCont = sufixo.toInt()
            strCont = "-%02d".format(intCont)
            nomeArquivo = nomeArquivo.dropLastWhile{ it.isDigit() }
            nomeArquivo = nomeArquivo.dropLast(1)
        }

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
                strCont = "-%02d".format(intCont)
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
            intent.putExtra("act_name", "MainActivity")
            intent.putExtra("file_path", diretorioArquivo)
            intent.putExtra("rec_mode", modoGravacao)
            intent.putExtra("file_type", this.extensaoArquivo)
            intent.putExtra("max_lines", this.numLinhasMaximo)
            intent.putExtra("rec_delay", this.intervaloGravacao)
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
        intent.putExtra("act_name", "MainActivity")
        intent.putExtra("file_type", this.extensaoArquivo.toString())
        intent.putExtra("max_lines", this.numLinhasMaximo.toString())
        intent.putExtra("rec_delay", this.intervaloGravacao.toString())
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
                Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE)
    }
}