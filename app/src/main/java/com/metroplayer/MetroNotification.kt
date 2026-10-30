package com.metroplayer

import android.content.Context
import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

/**
 * Уведомление плеера: своя иконка ноты в статус-баре и цвет акцента приложения.
 * Оборачивает стандартный провайдер Media3.
 */
class MetroNotificationProvider(
    context: Context,
    private val accent: () -> Int
) : MediaNotification.Provider {

    private val base = DefaultMediaNotificationProvider(context).apply {
        setSmallIcon(R.drawable.ic_stat_note)
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val n = base.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        n.notification.color = accent()
        return n
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean = base.handleCustomCommand(session, action, extras)
}
