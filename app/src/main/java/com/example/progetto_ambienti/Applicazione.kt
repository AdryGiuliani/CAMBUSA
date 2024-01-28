package com.example.progetto_ambienti

import android.app.Application
import android.content.Context

class Applicazione  : Application() {
    companion object{
        private var instance: Applicazione? = null

        fun getApplicationContext() : Context{
            return instance?.applicationContext?:throw IllegalStateException("istanza nulla")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}