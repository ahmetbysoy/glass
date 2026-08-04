package com.glasspro.tracker.core.di

import android.content.Context

object AppModule {
    fun provideAppContext(context: Context): Context = context.applicationContext
}
