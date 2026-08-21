package com.yueti.app

import android.app.Application
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api

class YuetiApplication : Application() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate() {
        super.onCreate()
        ComposeMaterial3Flags.isAnchoredDraggableComponentsInvalidationFixEnabled = true
        ComposeMaterial3Flags.isAnchoredDraggableComponentsStrictOffsetCheckEnabled = false
    }
}
