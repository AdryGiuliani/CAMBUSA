package com.example.progetto_ambienti

import android.os.Parcel
import android.os.Parcelable
import java.text.SimpleDateFormat

open class Prodotto(nome :String="", scadenza : String="", preferibilmente :Boolean=false) : Parcelable, Comparable<Prodotto>{

    var nome = nome
    var scadenza = scadenza
    var preferibilmente = preferibilmente

    fun getNome(){ nome}
    fun getScadenza(){ scadenza}
    fun getPreferibilmente(){ preferibilmente}

    constructor(parcel: Parcel) : this() {
        nome = parcel.readString()!!
        scadenza = parcel.readString()!!
        preferibilmente = parcel.readByte() != 0.toByte()
    }

    override fun toString(): String {
        return "Prodotto( nome='$nome', scadenza='$scadenza', preferibilmente=$preferibilmente)"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(nome)
        parcel.writeString(scadenza)
        parcel.writeByte(if (preferibilmente) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun compareTo(other: Prodotto): Int {
        val d1 = SimpleDateFormat("dd/MM/yyyy").parse(this.scadenza).time
        val d2 = SimpleDateFormat("dd/MM/yyyy").parse(other.scadenza).time
        if (d1==d2) return 0
        else if (d1 >d2) return 1
        return -1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Prodotto

        return nome == other.nome
    }

    override fun hashCode(): Int {
        return nome.hashCode()
    }


    companion object CREATOR : Parcelable.Creator<Prodotto> {
        override fun createFromParcel(parcel: Parcel): Prodotto {
            return Prodotto(parcel)
        }
        override fun newArray(size: Int): Array<Prodotto?> {
            return arrayOfNulls(size)
        }
    }

}

class Ricetta( titolo :String="", durata : String= "", difficolta : String="", contenuto : String="") {
    var titolo = titolo
    var durata = durata
    var difficolta = difficolta
    var contenuto = contenuto
    override fun toString(): String {
        return "Ricetta( titolo='$titolo', foto=$durata, difficolta='$difficolta', contenuto = '$contenuto')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Ricetta
        return titolo == other.titolo
    }

    override fun hashCode(): Int {
        var result = titolo.hashCode()
        result = 31 * result + durata.hashCode()
        result = 31 * result + difficolta.hashCode()
        result = 31 * result + contenuto.hashCode()
        return result
    }


}

class Posizione(nome: String="",ind: String="", lat: Double=0.0, long:Double=0.0) : Parcelable{
    var nome=nome
    var lat = lat
    var long= long
    var indirizzo= ind

    constructor(parcel: Parcel) : this() {
        nome = parcel.readString()!!
        lat = parcel.readDouble()
        long = parcel.readDouble()
        indirizzo= parcel.readString()!!
    }

    override fun toString(): String {
        return "Posizione(nome='$nome', lat=$lat, long=$long, ind=$indirizzo)"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(nome)
        parcel.writeDouble(lat)
        parcel.writeDouble(long)
        parcel.writeString(indirizzo)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Posizione
        return nome == other.nome
    }
    
    companion object CREATOR : Parcelable.Creator<Posizione> {
        override fun createFromParcel(parcel: Parcel): Posizione {
            return Posizione(parcel)
        }

        override fun newArray(size: Int): Array<Posizione?> {
            return arrayOfNulls(size)
        }
    }

}