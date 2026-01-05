package com.example.tv_app.repository

import android.util.Log
import com.example.tv_app.model.ObjectBox
import com.example.tv_app.model.WatchProgress
import com.example.tv_app.model.WatchProgress_
import io.objectbox.Box
import io.objectbox.kotlin.flow
import kotlinx.coroutines.flow.Flow
import io.objectbox.query.QueryBuilder

class WatchProgressRepository {

    private val progressBox: Box<WatchProgress> = ObjectBox.boxStore.boxFor(WatchProgress::class.java)
    
    // Thresholds for deciding if content should be listed in "Continue Watching"
    private val PROGRESS_SAVE_THRESHOLD_MS = 5000L // Only save progress after 5 seconds of viewing
    private val PROGRESS_COMPLETED_THRESHOLD_PERCENT = 0.9 // If position > 90% of duration, mark as completed (remove)
    private val PROGRESS_MIN_PERCENT_TO_DISPLAY = 0.05 // Min 5% watched to display in CW

    /**
     * Saves the watch progress for a given media item.
     * If position is close to completion, it deletes the progress.
     */
    fun saveProgress(mediaId: String, mediaType: String, positionMillis: Long, durationMillis: Long) {
        Log.d("WatchProgressRepo", "Attempting save: mediaId=$mediaId, type=$mediaType, pos=$positionMillis, dur=$durationMillis")

        if (durationMillis <= 0) {
            Log.w("WatchProgressRepo", "Saving cancelled: Duration is 0 or less.")
            return
        }
        
        val TAG = "WatchProgressRepo"

        val progressPercent = positionMillis.toDouble() / durationMillis.toDouble()
        
        // 1. Check if playback is near completion (e.g., > 90%)
        if (progressPercent >= PROGRESS_COMPLETED_THRESHOLD_PERCENT) {
            deleteProgress(mediaId)
            return
        }
        
        // 2. Check if enough time has passed to save progress (e.g., > 5 seconds)
        if (positionMillis < PROGRESS_SAVE_THRESHOLD_MS) {
            // Also delete if user barely started watching
            deleteProgress(mediaId)
            return
        }

        // 3. Save or update progress
        val existing = progressBox.query()
            .equal(WatchProgress_.mediaId, mediaId, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .findFirst()

        val progress = existing ?: WatchProgress(mediaId = mediaId, mediaType = mediaType)
        
        progress.apply {
            this.positionMillis = positionMillis
            this.durationMillis = durationMillis
            this.mediaType = mediaType // Update type in case it's a migration/new entry
            this.lastWatched = System.currentTimeMillis()
        }
        
        Log.d(TAG, "Saving valid progress for $mediaId at $positionMillis/$durationMillis")
        progressBox.put(progress)
    }

    /**
     * Deletes the watch progress entry for a media item.
     */
    fun deleteProgress(mediaId: String) {
        progressBox.query()
            .equal(WatchProgress_.mediaId, mediaId, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .findFirst()
            ?.let {
                Log.d("WatchProgressRepo", "Deleting progress entry for $mediaId")
                progressBox.remove(it)
            }
    }

    /**
     * Gets the saved position for a media item, or 0 if none found.
     */
    fun getSavedPosition(mediaId: String): Long {
        return progressBox.query()
            .equal(WatchProgress_.mediaId, mediaId, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .findFirst()
            ?.positionMillis ?: 0L
    }

    /**
     * Returns a flow of WatchProgress list for the "Continue Watching" row, ordered by last watched time.
     */
    fun getContinueWatchingFlow(): Flow<List<WatchProgress>> {
        // Return a Flow of raw WatchProgress data, ordered by last watched.
        // Filtering based on min/max duration percentage will be done in the ViewModel.
        return progressBox.query()
            .order(WatchProgress_.lastWatched, QueryBuilder.DESCENDING)
            .build()
            .flow()
    }

    /**
     * Clears all watch progress entries for movies (mediaType == "movie").
     */
    fun clearMovieProgress() {
        val query = progressBox.query().equal(WatchProgress_.mediaType, "movie", QueryBuilder.StringOrder.CASE_SENSITIVE).build()
        val removedCount = progressBox.remove(query.find())
        Log.d("WatchProgressRepo", "Cleared $removedCount movie watch progress entries.")
    }

    /**
     * Clears all watch progress entries for TV series/episodes (mediaType == "episode").
     */
    fun clearSeriesProgress() {
        val query = progressBox.query().equal(WatchProgress_.mediaType, "episode", QueryBuilder.StringOrder.CASE_SENSITIVE).build()
        val removedCount = progressBox.remove(query.find())
        Log.d("WatchProgressRepo", "Cleared $removedCount series watch progress entries.")
    }
}