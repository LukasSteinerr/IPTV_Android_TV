package com.example.iptvsonic.cast

import android.content.Context
import androidx.core.net.toUri // Import for toUri extension function
import androidx.media3.cast.CastPlayer
import androidx.media3.common.MediaItem
import com.example.iptvsonic.model.Movie
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.common.images.WebImage

object CastHelper {

    /**
     * Attempts to start casting the movie to a selected device.
     * If no Cast session is active, returns false.
     *
     * @param context The application context.
     * @param movie The Movie object to cast.
     * @return true if casting started, false if no session is active.
     */
    fun startCasting(context: Context, movie: Movie): Boolean {
        return try {
            val castContext = CastContext.getSharedInstance(context)
            if (castContext.sessionManager.currentCastSession == null) {
                return false // No active Cast session
            }
            
            val session = castContext.sessionManager.currentCastSession!!
            val remoteMediaClient = session.remoteMediaClient
            
            // Use movie.streamUrl as the media URL
            val contentUrl = movie.streamUrl
            if (contentUrl.isNullOrBlank()) {
                android.util.Log.e("CastHelper", "Media URL is null or blank for movie: ${movie.name}")
                return false
            }
            android.util.Log.i("CastHelper", "Attempting to cast URL: $contentUrl")

            // Create MediaInfo (Cast Framework object, not Media3 MediaItem)
            val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
            metadata.putString(MediaMetadata.KEY_TITLE, movie.name)
            metadata.putString(MediaMetadata.KEY_SUBTITLE, movie.description ?: "")
            movie.posterUrl?.let {
                metadata.addImage(WebImage(android.net.Uri.parse(it)))
            }

            val mediaInfo = MediaInfo.Builder(contentUrl)
                .setContentType("video/x-matroska") // MKV container MIME type (more accurate)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(metadata)
                .build()

            remoteMediaClient?.load(mediaInfo, true)
            
            // Assume success if the command was successfully issued and the session is active.
            remoteMediaClient?.let {
                android.util.Log.i("CastHelper", "Media load command issued for ${movie.name}")
                return true
            } ?: run {
                android.util.Log.e("CastHelper", "RemoteMediaClient is null or load command failed.")
                return false
            }
        } catch (e: Exception) {
            android.util.Log.e("CastHelper", "Failed to start casting", e)
            false
        }
    }

    /**
     * Checks if a Cast session is currently active.
     */
    fun isCastSessionActive(context: Context): Boolean {
        return try {
            val castContext = CastContext.getSharedInstance(context)
            castContext.sessionManager.currentCastSession != null
        } catch (e: Exception) {
            false
        }
    }
}