package com.example.progetto_ambienti

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.progetto_ambienti.ui.gallery.toRicetta
import com.example.progetto_ambienti.ui.home.toProdotto
import com.example.progetto_ambienti.ui.slideshow.toPosizione
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "databaseProdotti2.db",null, 1) {

    val tabelle = hashMapOf<String,List<String>>(
        "prodotto" to listOf<String>("nome","scadenza","preferibilmente"),
        "difficolta" to listOf<String>("diffValue"),
        "ricetta" to listOf<String>("titolo","diffValue","durata","testo"),
        "posizione" to listOf<String>("nome","lat","long","ind")
    )

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE difficolta (diffValue VARCHAR(50) PRIMARY KEY)")
        db?.execSQL("CREATE TABLE posizione (nome TEXT PRIMARY KEY, lat DOUBLE, long DOUBLE,ind TEXT)")
        db?.execSQL("CREATE TABLE prodotto ( nome TEXT PRIMARY KEY, scadenza SMALLDATETIME, preferibilmente BOOLEAN)")
        db?.execSQL("CREATE TABLE ricetta ( titolo TEXT PRIMARY KEY, diffValue INTEGER,durata TEXT, testo TEXT, FOREIGN KEY(diffValue) REFERENCES difficolta(diffValue))")
        db?.insert("difficolta",null,contentDiff("facile"))
        db?.insert("difficolta",null,contentDiff("media"))
        db?.insert("difficolta",null,contentDiff("difficile"))

        //la chiave esterna poteva essere evitata, ho preferito utilizzarla per esercizio personale
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    fun insertProdotto(nome: String, scadenza: String, preferibilmente : Boolean): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        val colProdotti = tabelle.get("prodotto")
        cv.put(colProdotti!![0], nome)
        cv.put(colProdotti[1], scadenza)
        cv.put(colProdotti[2], preferibilmente)
        val result = db.insert("prodotto", null, cv)
        return result != -1L
    }
    fun contentDiff(diffValue: String): ContentValues{
        val cv = ContentValues()
        val colDiff = tabelle.get("difficolta")
        cv.put(colDiff!![0], diffValue)
        return cv
    }

    fun insertRicetta(titolo: String, difficolta : String,durata : String, testo : String): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        val colProdotti = tabelle.get("ricetta")
        cv.put(colProdotti!![0], titolo)
        cv.put(colProdotti[1], difficolta )
        cv.put(colProdotti[2], durata)
        cv.put(colProdotti[3], testo)
        val result = db.insert("ricetta", null, cv)
        return result != -1L
    }

    fun getProdottiCur(): Cursor? {
        val db = this.readableDatabase
        val query = "SELECT * FROM prodotto"
        return db.rawQuery(query, null)
    }

    fun getProdottiWarn() : Collection<Prodotto> {
        val list = mutableListOf<Prodotto>()
        val curProdotti = getProdottiCur()
        val curDate = System.currentTimeMillis()
        while (curProdotti!!.moveToNext()) {
            var prodotto: Prodotto
            try {
                prodotto = toProdotto(curProdotti)
                val scad = SimpleDateFormat("dd/MM/yyyy").parse(prodotto.scadenza).time
                if (scad - curDate < TimeUnit.DAYS.toMillis(SOGLIA_SCAD))
                    list.add(prodotto)
            } catch (e: IllegalStateException) {
                Log.d("prodotto", "Nessun Prodotto")

            }
        }
        return list
    }


    fun rimuoviProdotti(prodotti: MutableCollection<Prodotto>){
        val db = this.writableDatabase
        for (p in prodotti){
            val nome  = p.nome
            db.execSQL("DELETE FROM prodotto WHERE nome LIKE '%$nome%';")
        }
    }

    fun rimuoviRicette(ricette: MutableCollection<Ricetta>){
        val db = this.writableDatabase
        for (r in ricette){
            val titolo  = r.titolo
            db.execSQL("DELETE FROM ricetta WHERE titolo LIKE '%$titolo%';")
        }
    }

    fun rimuoviPosizioni(posizioni: Collection<Posizione>){
        val db = this.writableDatabase
        for (p in posizioni){
            val nome  = p.nome
            db.execSQL("DELETE FROM posizione WHERE nome LIKE '%$nome%';")
        }
    }
    fun insertPosizione(nome: String, lat: Double, long : Double): Boolean {
        val db = this.writableDatabase
        val cv = ContentValues()
        val colPosizioni = tabelle.get("posizione")
        cv.put(colPosizioni!![0], nome)
        cv.put(colPosizioni[1], lat)
        cv.put(colPosizioni[2], long)
        val result = db.insert("posizione", null, cv)
        return result != -1L
    }

    fun insertPosizioni(posizioni : Collection<Posizione>): Boolean {
        val db = this.writableDatabase
        var result = 1L
        for (p in posizioni) {
            val cv = ContentValues()
            val colPosizioni = tabelle.get("posizione")
            cv.put(colPosizioni!![0], p.nome)
            cv.put(colPosizioni[1], p.lat)
            cv.put(colPosizioni[2], p.long)
            cv.put(colPosizioni[3], p.indirizzo)
            if (db.insert("posizione", null, cv) < 0)
                result=-1L
        }
        return result != -1L
    }

    fun getPosizioni(): Collection<Posizione> {
        val db = this.readableDatabase
        val query = "SELECT * FROM posizione"
        val cur= db.rawQuery(query, null)
        val array= ArrayList<Posizione>()
        while (cur!!.moveToNext()) {
            var posizione = Posizione()
            try {
                posizione = toPosizione(cur)
            } catch (e: IllegalStateException) {
                Log.d("posizione", "Nessuna Posizione")

            }
            array.add(posizione)
        }
        return array
    }

    fun getProdotti(): Collection<Prodotto> {
        val db = this.readableDatabase
        val query = "SELECT * FROM prodotto"
        val cur= db.rawQuery(query, null)
        val array= ArrayList<Prodotto>()
        while (cur!!.moveToNext()) {
            try {
               val prod = toProdotto(cur)
                array.add(prod)
            } catch (e: IllegalStateException) {
                Log.d("posizione", "Nessuna Posizione")

            }

        }
        return array
    }

    fun getRicette(): Collection<Ricetta> {
        val db = this.readableDatabase
        val query = "SELECT * FROM ricetta"
        val cur= db.rawQuery(query, null)
        val array= ArrayList<Ricetta>()
        while (cur!!.moveToNext()) {
            try {
                val r = toRicetta(cur)
                array.add(r)
            } catch (e: IllegalStateException) {
                Log.d("posizione", "Nessuna Posizione")
            }
        }
        return array
    }


}