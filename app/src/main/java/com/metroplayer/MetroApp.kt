package com.metroplayer

import android.app.Application
import com.metroplayer.data.MusicRepository
import com.metroplayer.data.Store
import com.metroplayer.player.PlayerController

class MetroApp : Application() {
    lateinit var repo: MusicRepository
    lateinit var store: Store
    lateinit var player: PlayerController

    override fun onCreate() {
        super.onCreate()
        repo = MusicRepository(this)
        store = Store(this)
        player = PlayerController(this, repo, store)
    }
}
