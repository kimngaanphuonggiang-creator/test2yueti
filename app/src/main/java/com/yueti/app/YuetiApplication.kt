package com.yueti.app

import android.app.Application
import android.content.Context
import androidx.compose.material3.ComposeMaterial3Flags
import androidx.compose.material3.ExperimentalMaterial3Api
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.yueti.app.music.MusicNetworkStack

class YuetiApplication : Application(), SingletonImageLoader.Factory {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate() {
        super.onCreate()
        ComposeMaterial3Flags.isAnchoredDraggableComponentsInvalidationFixEnabled = true
        ComposeMaterial3Flags.isAnchoredDraggableComponentsStrictOffsetCheckEnabled = false
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = { MusicNetworkStack.client(context) },
                ),
            )
        }
        .crossfade(true)
        .build()
}
