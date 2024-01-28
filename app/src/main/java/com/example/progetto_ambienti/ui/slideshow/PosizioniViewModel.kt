package com.example.progetto_ambienti.ui.slideshow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PosizioniViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Le posizioni salvate appariranno qui"
    }
    val text: LiveData<String> = _text
}