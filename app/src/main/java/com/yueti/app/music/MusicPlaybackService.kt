package com.yueti.app.music

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class MusicPlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        val playbackClient = MusicNetworkStack.client(this).newBuilder()
            .addInterceptor { chain ->
                val cookie = MusicCredentialStore.load(this@MusicPlaybackService)
                val request = if (cookie.isBlank()) chain.request() else {
                    chain.request().newBuilder().header("Cookie", cookie).build()
                }
                chain.proceed(request)
            }
            .build()
        val httpDataSource = OkHttpDataSource.Factory(playbackClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android) Yueti/0.11.5")
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://music.163.com/",
                    "Origin" to "https://music.163.com",
                ),
            )
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSource))
            .build()
            .apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
            volume = 1f
        }
        session = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
