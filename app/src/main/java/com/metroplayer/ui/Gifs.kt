package com.metroplayer.ui

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder

/**
 * Один общий загрузчик гифок на всё приложение.
 * Раньше каждая гифка создавала свой ImageLoader (свой кэш и своя сеть) — это съедало память и тормозило.
 */
object GifLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader =
        instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                }
                .build()
                .also { instance = it }
        }
}
