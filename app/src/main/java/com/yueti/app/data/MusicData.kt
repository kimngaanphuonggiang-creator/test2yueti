package com.yueti.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music_queue")
data class MusicQueueEntity(
    @PrimaryKey val position: Int,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val playlistId: String,
    val playlistName: String,
    val durationMs: Long,
    val playable: Boolean,
)

@Entity(tableName = "music_playback")
data class MusicPlaybackEntity(
    @PrimaryKey val singletonKey: Int = 0,
    val currentIndex: Int = -1,
    val positionMs: Long = 0L,
    val repeatMode: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)
