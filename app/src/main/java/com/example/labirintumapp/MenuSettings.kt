package com.example.labirintumapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import android.widget.CompoundButton
import android.widget.Button
import android.widget.Toast
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.view.MotionEvent
import android.content.Context
import android.graphics.Rect
import android.view.inputmethod.InputMethodManager

class MenuSettings : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_settings)

        val edtRecDelay = findViewById<EditText>(R.id.edtIntervalo)
        val rdgFileType = findViewById<RadioGroup>(R.id.cgpFormatArq)
        val edtMaxLines = findViewById<EditText>(R.id.edtNumMaxLinhas)
        val swtMaxLines = findViewById<Switch>(R.id.switchStopRecording)
        val txtMaxLines = findViewById<TextView>(R.id.txtSwitchStopRecording)
        val btnSalvar = findViewById<Button>(R.id.btnSalvar)
        val btnCancelar = findViewById<Button>(R.id.btnCancelar)

        val intentReceiver = intent
        var filetype = intentReceiver.getStringExtra("file_type")
        var maxlines = intentReceiver.getStringExtra("max_lines")
        var recdelay = intentReceiver.getStringExtra("rec_delay")

        edtRecDelay.setText(recdelay)
        edtMaxLines.setText(maxlines)

        when (filetype){
            "csv" -> rdgFileType.check(R.id.chkCSV)
            "txt" -> rdgFileType.check(R.id.chkTXT)
        }
        rdgFileType.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup, checkedId: Int) {
                val rb = findViewById<RadioButton>(checkedId)
                when (rb.text.toString()) {
                    "Formato .csv" -> filetype = "csv"
                    "Formato .txt" -> filetype = "txt"
                }
            }
        })

        if (maxlines == "0"){
            swtMaxLines.isChecked = false
            txtMaxLines.text = "Ativar recurso"
            edtMaxLines.isEnabled = false
        } else {
            swtMaxLines.isChecked = true
            txtMaxLines.text = "Desativar recurso"
            edtMaxLines.isEnabled = true
            edtMaxLines.setText(maxlines)
        }
        swtMaxLines.setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                if (isChecked){
                    txtMaxLines.text = "Desativar recurso"
                    edtMaxLines.isEnabled = true
                }
                else {
                    txtMaxLines.text = "Ativar recurso"
                    edtMaxLines.isEnabled = false
                }
            }
        })

        btnSalvar.setOnClickListener {
            val intentSender = Intent(applicationContext, MainActivity::class.java)
            maxlines = if (edtMaxLines.isEnabled) edtMaxLines.text.toString() else "0"
            recdelay = edtRecDelay.text.toString()
            intentSender.putExtra("act_name", "MenuSettings")
            intentSender.putExtra("user_action", "salvar")
            intentSender.putExtra("file_type", filetype)
            intentSender.putExtra("max_lines", maxlines)
            intentSender.putExtra("rec_delay", recdelay)
            startActivity(intentSender)
            Toast.makeText(this, "As alterações foram salvas com sucesso!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnCancelar.setOnClickListener { 
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Deseja cancelar?")
            builder.setMessage("As alterações feitas não serão salvas.")

            builder.setPositiveButton("Continuar", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface, which: Int) {
                    val intentSender = Intent(applicationContext, MainActivity::class.java)
                    intentSender.putExtra("act_name", "MenuSettings")
                    intentSender.putExtra("user_action", "cancelar")
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
        }
    }

    // clicar fora de um EditText remove o foco sobre ele
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