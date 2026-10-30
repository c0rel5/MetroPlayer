package com.metroplayer.data

import android.content.Context
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {
    var tracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var artists by mutableStateOf<List<ArtistInfo>>(emptyList())
        private set
    var albums by mutableStateOf<List<AlbumInfo>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    private var byId: Map<Long, Track> = emptyMap()

    fun find(id: Long): Track? = byId[id]

    private class Scan(
        val tracks: List<Track>,
        val artists: List<ArtistInfo>,
        val albums: List<AlbumInfo>
    )

    suspend fun load() {
        val scan = withContext(Dispatchers.IO) { scanDevice() }
        byId = scan.tracks.associateBy { it.id }
        tracks = scan.tracks
        artists = scan.artists
        albums = scan.albums
        loaded = true
    }

    private fun scanDevice(): Scan {
        val list = ArrayList<Track>()
        val a = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"

        try {
            context.contentResolver.query(a, projection, selection, null, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (c.moveToNext()) {
                    val artist = c.getString(iArtist)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "неизвестный исполнитель"
                    val album = c.getString(iAlbum)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "неизвестный альбом"
                    list.add(
                        Track(
                            id = c.getLong(iId),
                            title = c.getString(iTitle) ?: "без названия",
                            artist = artist,
                            album = album,
                            albumId = c.getLong(iAlbumId),
                            durationMs = c.getLong(iDur),
                            trackNo = c.getInt(iTrack) % 1000,
                            format = (c.getString(iName) ?: "").substringAfterLast('.', "").lowercase()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // нет доступа или сбой MediaStore — просто пустая библиотека
        }

        val sorted = list.sortedBy { it.title.lowercase() }
        val artists = sorted.groupBy { it.artist }
            .map { ArtistInfo(it.key, it.value.size) }
            .sortedBy { it.name.lowercase() }
        val albums = sorted.groupBy { it.albumId }
            .map { (id, ts) -> AlbumInfo(id, ts.first().album, ts.first().artist, ts.size) }
            .sortedBy { it.name.lowercase() }
        return Scan(sorted, artists, albums)
    }
}
