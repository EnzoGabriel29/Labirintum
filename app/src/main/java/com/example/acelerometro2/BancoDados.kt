package com.example.acelerometro2

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Acelerometro {
    var horario: String? = null
    var valorX: Double = 0.0
    var valorY: Double = 0.0
    var valorZ: Double = 0.0

    constructor(){}

    constructor (Horario: String, ValorX: Double, ValorY: Double, ValorZ: Double){
        this.horario = Horario
        this.valorX = ValorX
        this.valorY = ValorY
        this.valorZ = ValorZ
    }
}

class BancoDados(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VER){
    companion object {
        private val DATABASE_VER = 1
        private val DATABASE_NAME = "EDMTDB.db"

        private val NOME_TABELA = "Acelerometro"
        private val COL_HORARIO = "Horario"
        private val COL_VALORX = "ValorX"
        private val COL_VALORY = "ValorY"
        private val COL_VALORZ = "ValorZ"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE_QUERY = ("CREATE TABLE $NOME_TABELA ($COL_HORARIO TEXT,$COL_VALORX REAL,$COL_VALORY REAL,$COL_VALORZ REAL)")
        db!!.execSQL(CREATE_TABLE_QUERY)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $NOME_TABELA")
        onCreate(db!!)
    }

    val getAcc: Array<Acelerometro>
        get(){
            var arrayAcc = emptyArray<Acelerometro>()
            val selectQuery = "SELECT * FROM $NOME_TABELA"
            val db: SQLiteDatabase = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)

            if(cursor.moveToFirst()){
                do{
                    val acc = Acelerometro()
                    acc.horario = cursor.getString(cursor.getColumnIndex(COL_HORARIO))
                    acc.valorX = cursor.getDouble(cursor.getColumnIndex(COL_VALORX))
                    acc.valorY = cursor.getDouble(cursor.getColumnIndex(COL_VALORY))
                    acc.valorZ = cursor.getDouble(cursor.getColumnIndex(COL_VALORZ))
                    arrayAcc += acc
                } while(cursor.moveToNext())
            }
            db.close()
            return arrayAcc
        }

    fun addAcc(acc: Acelerometro){
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_HORARIO, acc.horario)
        values.put(COL_VALORX, acc.valorX)
        values.put(COL_VALORY, acc.valorY)
        values.put(COL_VALORZ, acc.valorZ)

        db.insert(NOME_TABELA, null, values)
        db.close()
    }

    fun updateAcc(acc: Acelerometro): Int{
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_HORARIO, acc.horario)
        values.put(COL_VALORX, acc.valorX)
        values.put(COL_VALORY, acc.valorY)
        values.put(COL_VALORZ, acc.valorZ)

        return db.update(NOME_TABELA, values, "$COL_HORARIO=?", arrayOf(acc.horario))
    }

    fun deleteAcc(acc: Acelerometro){
        val db = this.writableDatabase
        db.delete(NOME_TABELA,"$COL_HORARIO=?", arrayOf(acc.horario))
        db.close()
    }
}