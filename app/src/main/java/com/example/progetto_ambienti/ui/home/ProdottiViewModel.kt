package com.example.progetto_ambienti.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProdottiViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Inserisci qui i tuoi prodotti!"
    }
    val text: LiveData<String> = _text
}