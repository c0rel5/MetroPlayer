package com.metroplayer.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNo: Int,
    val format: String = ""
) {
    val uri: Uri
        get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

    val artUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

data class ArtistInfo(val name: String, val count: Int)

data class AlbumInfo(val id: Long, val name: String, val artist: String, val count: Int)

data class Playlist(val id: Long, val name: String, val trackIds: List<Long>)
