package com.example.labirintumapp

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MenuSettings : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_settings)

        val spnRecDelay = findViewById<Spinner>(R.id.delaySpinner)
        val rdgFileType = findViewById<RadioGroup>(R.id.cgpFormatArq)
        val edtMaxLines = findViewById<EditText>(R.id.edtNumMaxLinhas)
        val swtMaxLines = findViewById<Switch>(R.id.switchStopRecording)
        val txtMaxLines = findViewById<TextView>(R.id.txtSwitchStopRecording)
        val chkGraficoAcc = findViewById<CheckBox>(R.id.checkboxAcc)
        val chkGraficoGir = findViewById<CheckBox>(R.id.checkboxGir)
        val btnSalvar = findViewById<TextView>(R.id.btnSalvar)
        val btnCancelar = findViewById<TextView>(R.id.btnCancelar)

        val intentRecebido = intent
        val filetype = intentRecebido.getStringExtra("KEY_EXTENSAO_ARQUIVO")
        val maxlines = intentRecebido.getStringExtra("KEY_NUM_MAX_LINHAS")
        val recdelay = intentRecebido.getStringExtra("KEY_DELAY_GRAVACAO")
        val graphvis = intentRecebido.getStringExtra("KEY_GRAFICOS_VISIVEIS")
        
        var newFiletype = filetype
        var newMaxlines = maxlines
        var newRecdelay = recdelay
        var newGraphvis = graphvis

        val arrayDelays = arrayOf<String>(
            "Delay normal (200 milissegundos)",
            "Delay acima do normal (100 milissegundos)",
            "Delay rápido (60 milissegundos)",
            "Delay ultrarrápido (20 milissegundos)")

        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayDelays);  
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
        spnRecDelay.setAdapter(aa);

        when (recdelay){
            "200" -> spnRecDelay.setSelection(0)
            "100" -> spnRecDelay.setSelection(1)
            "60" -> spnRecDelay.setSelection(2)
            "20" -> spnRecDelay.setSelection(3)
        }

        spnRecDelay.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(av: AdapterView<*>?, v: View, pos: Int, id: Long){
                newRecdelay = when (pos){
                    0 -> "200"
                    1 -> "100"
                    2 -> "60"
                    3 -> "20"
                    else -> "0"
                }
            }

            override fun onNothingSelected(av: AdapterView<*>?){

            }
        })

        when (filetype){
            "csv" -> rdgFileType.check(R.id.chkCSV)
            "txt" -> rdgFileType.check(R.id.chkTXT)
        }
        rdgFileType.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
                val rb = findViewById<RadioButton>(checkedId)
                when (rb.text.toString()) {
                    "Formato .csv" -> newFiletype = "csv"
                    "Formato .txt" -> newFiletype = "txt"
                }
            }
        })

        edtMaxLines.setText(maxlines)
        if (maxlines == "0"){
            swtMaxLines.isChecked = false
            txtMaxLines.text = "Recurso desativado"
            edtMaxLines.isEnabled = false

        } else {
            swtMaxLines.isChecked = true
            txtMaxLines.text = "Recurso ativado"
            edtMaxLines.isEnabled = true
            edtMaxLines.setText(maxlines)
        }
        swtMaxLines.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                if (isChecked){
                    txtMaxLines.text = "Recurso ativado"
                    edtMaxLines.isEnabled = true
                }
                else {
                    txtMaxLines.text = "Recurso desativado"
                    edtMaxLines.isEnabled = false
                }
            }
        })

        when (graphvis){
            "0" -> {
                chkGraficoAcc.setChecked(false)
                chkGraficoGir.setChecked(false)
            }

            "1" -> {
                chkGraficoAcc.setChecked(true)
                chkGraficoGir.setChecked(false)
            }

            "2" -> {
                chkGraficoAcc.setChecked(false)
                chkGraficoGir.setChecked(true)
            }

            "3" -> {
                chkGraficoAcc.setChecked(true)
                chkGraficoGir.setChecked(true)
            }
        }

        btnSalvar.setOnClickListener {
            val intentSender = Intent(applicationContext, MainActivity::class.java)
            newMaxlines = if (edtMaxLines.isEnabled) edtMaxLines.text.toString() else "0"
            val g1 = if (chkGraficoAcc.isChecked) 1 else 0
            val g2 = if (chkGraficoGir.isChecked) 2 else 0
            newGraphvis = (g1 + g2).toString()
            intentSender.putExtra("KEY_NOME_ACTIVITY", "MenuSettings")
            intentSender.putExtra("KEY_ACAO_USUARIO", "salvar")
            intentSender.putExtra("KEY_EXTENSAO_ARQUIVO", newFiletype)
            intentSender.putExtra("KEY_NUM_MAX_LINHAS", newMaxlines)
            intentSender.putExtra("KEY_DELAY_GRAVACAO", newRecdelay)
            intentSender.putExtra("KEY_GRAFICOS_VISIVEIS", newGraphvis)
            startActivity(intentSender)
            Toast.makeText(this, "As alterações foram salvas com sucesso!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnCancelar.setOnClickListener {
            newMaxlines = if (edtMaxLines.isEnabled) edtMaxLines.text.toString() else "0"
            val g1 = if (chkGraficoAcc.isChecked) 1 else 0
            val g2 = if (chkGraficoGir.isChecked) 2 else 0
            newGraphvis = (g1 + g2).toString()
            if (newFiletype != filetype || newRecdelay != recdelay ||
            newMaxlines != maxlines || newGraphvis != graphvis){
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Deseja cancelar?")
                builder.setMessage("As alterações feitas não serão salvas.")

                builder.setPositiveButton("Continuar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        val intentSender = Intent(applicationContext, MainActivity::class.java)
                        intentSender.putExtra("KEY_NOME_ACTIVITY", "MenuSettings")
                        intentSender.putExtra("KEY_ACAO_USUARIO", "cancelar")
                        startActivity(intentSender)
                        dialog.dismiss()
                        finish()
                    }
                })

                builder.setNegativeButton("Voltar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        dialog.dismiss()
                    }
                })
                
                builder.create().show()
            
            } else {
                val intentSender = Intent(applicationContext, MainActivity::class.java)
                intentSender.putExtra("KEY_NOME_ACTIVITY", "MenuSettings")
                intentSender.putExtra("KEY_ACAO_USUARIO", "cancelar")
                startActivity(intentSender)
                finish()
            }          
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus

            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)

                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0)
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}