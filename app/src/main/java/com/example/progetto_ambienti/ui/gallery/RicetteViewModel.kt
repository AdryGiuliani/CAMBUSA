package com.example.progetto_ambienti.ui.gallery

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RicetteViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Vedrai qui le tue ricette"
    }
    val text: LiveData<String> = _text
}