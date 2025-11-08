package com.example.tv_app.mobile_ui

import android.annotation.SuppressLint
import android.text.Spanned
import android.text.SpannedString
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.egeniq.androidtvprogramguide.ProgramGuideFragment
import com.egeniq.androidtvprogramguide.R as ProgramGuideR
import com.egeniq.androidtvprogramguide.entity.ProgramGuideChannel
import com.egeniq.androidtvprogramguide.entity.ProgramGuideSchedule
import com.egeniq.androidtvprogramguide.util.FilterOption
import com.example.tv_app.model.Channel
import com.example.tv_app.model.Playlist
import com.example.tv_app.model.TvProgram
import com.example.tv_app.repository.PlaylistService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class IptvEpgFragment(
    private val playlist: Playlist,
    private val playlistService: PlaylistService,
    private val onChannelSelected: (Channel) -> Unit = {}
) : ProgramGuideFragment<IptvEpgFragment.IptvProgram>() {

    private var selectedCategoryId: Long? = null

    override val CAN_FOCUS_CHANNEL = true
    override val SCROLL_SYNCING = true

    companion object {
        private val TAG = IptvEpgFragment::class.java.name
    }

    data class IptvChannel(
        override val id: String,
        override val name: Spanned?,
        override val imageUrl: String?,
        val channel: Channel
    ) : ProgramGuideChannel

    data class IptvProgram(
        val id: String,
        val title: String,
        val description: String,
        val startTime: Date?,
        val endTime: Date?,
        val channel: Channel
    )

    override fun onScheduleClicked(programGuideSchedule: ProgramGuideSchedule<IptvProgram>) {
        val innerSchedule = programGuideSchedule.program
        if (innerSchedule == null) {
            Log.w(TAG, "Unable to open schedule!")
            return
        }
        if (programGuideSchedule.isCurrentProgram) {
            Toast.makeText(context, "Play ${innerSchedule.channel.name}", Toast.LENGTH_LONG).show()
            onChannelSelected(innerSchedule.channel)
        } else {
            Toast.makeText(context, "Program: ${innerSchedule.title}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onScheduleSelected(programGuideSchedule: ProgramGuideSchedule<IptvProgram>?) {
        val titleView = view?.findViewById<TextView>(ProgramGuideR.id.programguide_detail_title)
        titleView?.text = programGuideSchedule?.displayTitle ?: ""
        
        val metadataView = view?.findViewById<TextView>(ProgramGuideR.id.programguide_detail_metadata)
        val program = programGuideSchedule?.program
        if (program?.startTime != null) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            metadataView?.text = "Starts at ${timeFormat.format(program.startTime)}"
        } else {
            metadataView?.text = ""
        }
        
        val descriptionView = view?.findViewById<TextView>(ProgramGuideR.id.programguide_detail_description)
        descriptionView?.text = program?.description ?: ""
        
        val imageView = view?.findViewById<ImageView>(ProgramGuideR.id.programguide_detail_image) ?: return
        if (programGuideSchedule != null && program != null) {
            val logoUrl = program.channel.logoUrl
            if (!logoUrl.isNullOrEmpty()) {
                Glide.with(imageView)
                    .load(logoUrl)
                    .centerCrop()
                    .error(ProgramGuideR.drawable.programguide_icon_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(withCrossFade())
                    .into(imageView)
            } else {
                Glide.with(imageView).clear(imageView)
                imageView.setImageResource(ProgramGuideR.drawable.programguide_icon_placeholder)
            }
        } else {
            Glide.with(imageView).clear(imageView)
        }
    }

    override fun onChannelSelected(channel: ProgramGuideChannel) {
        val titleView = view?.findViewById<TextView>(ProgramGuideR.id.programguide_detail_title)
        titleView?.text = channel.name
        val metadataView = view?.findViewById<TextView>(ProgramGuideR.id.programguide_detail_metadata)
        metadataView?.text = null
        val descriptionView = view?.findViewById<TextView>(ProgramGuideR.id.programguide_detail_description)
        descriptionView?.text = "Select a program to see details"
        val imageView = view?.findViewById<ImageView>(ProgramGuideR.id.programguide_detail_image) ?: return
        
        if (channel is IptvChannel && !channel.channel.logoUrl.isNullOrEmpty()) {
            Glide.with(imageView)
                .load(channel.channel.logoUrl)
                .centerCrop()
                .error(ProgramGuideR.drawable.programguide_icon_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(withCrossFade())
                .into(imageView)
        } else {
            Glide.with(imageView).clear(imageView)
            imageView.setImageResource(ProgramGuideR.drawable.programguide_icon_placeholder)
        }
    }

    override fun onChannelClicked(channel: ProgramGuideChannel) {
        if (channel is IptvChannel) {
            Toast.makeText(context, "Channel clicked: ${channel.name}", Toast.LENGTH_LONG).show()
            onChannelSelected(channel.channel)
        }
    }

    override fun isTopMenuVisible(): Boolean {
        return false
    }

    @SuppressLint("CheckResult")
    override fun requestingProgramGuideFor(localDate: LocalDate) {
        setState(State.Loading)

        Single.fromCallable {
            runBlocking {
                try {
                    val channels = if (selectedCategoryId != null) {
                        playlistService.getChannelsForCategory(selectedCategoryId!!)
                    } else {
                        playlistService.getLiveTVChannelsForPlaylist(playlist)
                    }
                    Log.d(TAG, "Found ${channels.size} channels for playlist ${playlist.name}" +
                          if (selectedCategoryId != null) " in category $selectedCategoryId" else "")
                    
                    val iptvChannels = channels.map { channel ->
                        IptvChannel(
                            id = channel.id.toString(),
                            name = SpannedString(channel.name),
                            imageUrl = channel.logoUrl,
                            channel = channel
                        )
                    }

                    val channelMap = mutableMapOf<String, List<ProgramGuideSchedule<IptvProgram>>>()

                    for (channel in channels) {
                        try {
                            val programs = playlistService.getEpgProgramsForChannel(channel)
                            Log.d(TAG, "Found ${programs.size} programs for channel ${channel.name}")
                            
                            val scheduleList = mutableListOf<ProgramGuideSchedule<IptvProgram>>()
                            
                            // Filter programs for the selected date
                            val dayStart = localDate.atStartOfDay().atZone(DISPLAY_TIMEZONE)
                            val dayEnd = dayStart.plusDays(1)
                            
                            val dayPrograms = programs.filter { program ->
                                program.startTime != null && program.stopTime != null &&
                                        Instant.ofEpochMilli(program.startTime!!.time).atZone(DISPLAY_TIMEZONE).toLocalDate() == localDate
                            }

                            if (dayPrograms.isNotEmpty()) {
                                for (program in dayPrograms) {
                                    val schedule = createScheduleFromTvProgram(program, channel)
                                    scheduleList.add(schedule)
                                }
                            } else {
                                // Create a placeholder program for the whole day if no EPG data
                                val startTime = dayStart.toInstant()
                                val endTime = dayEnd.toInstant()
                                val placeholder = createPlaceholderSchedule(channel, startTime, endTime)
                                scheduleList.add(placeholder)
                            }
                            
                            channelMap[channel.id.toString()] = scheduleList
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading programs for channel ${channel.name}", e)
                            // Create placeholder for channels with errors
                            val dayStart = localDate.atStartOfDay().atZone(DISPLAY_TIMEZONE)
                            val dayEnd = dayStart.plusDays(1)
                            val placeholder = createPlaceholderSchedule(channel, dayStart.toInstant(), dayEnd.toInstant())
                            channelMap[channel.id.toString()] = listOf(placeholder)
                        }
                    }

                    return@runBlocking Pair(iptvChannels, channelMap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading EPG data", e)
                    throw e
                }
            }
        }.delay(500, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                setData(it.first, it.second, localDate)
                if (it.first.isEmpty()) {
                    setState(State.Error("No channels available for this playlist."))
                } else {
                    setState(State.Content)
                }
            }, { error ->
                Log.e(TAG, "Unable to load EPG data!", error)
                setState(State.Error("Failed to load EPG data: ${error.message}"))
            })
    }

    private fun createScheduleFromTvProgram(
        program: TvProgram,
        channel: Channel
    ): ProgramGuideSchedule<IptvProgram> {
        val startTime = if (program.startTime != null) {
            Instant.ofEpochMilli(program.startTime!!.time)
        } else {
            Instant.now()
        }
        val endTime = if (program.stopTime != null) {
            Instant.ofEpochMilli(program.stopTime!!.time)
        } else {
            startTime.plus(30, ChronoUnit.MINUTES)
        }
        
        val iptvProgram = IptvProgram(
            id = program.id.toString(),
            title = program.title,
            description = program.description ?: "No description available",
            startTime = program.startTime,
            endTime = program.stopTime,
            channel = channel
        )

        return ProgramGuideSchedule.createScheduleWithProgram(
            id = program.id,
            startsAt = startTime,
            endsAt = endTime,
            isClickable = true,
            displayTitle = program.title,
            program = iptvProgram
        )
    }

    private fun createPlaceholderSchedule(
        channel: Channel,
        startTime: Instant,
        endTime: Instant
    ): ProgramGuideSchedule<IptvProgram> {
        val startTimeJava = java.time.Instant.ofEpochMilli(startTime.toEpochMilli())
        val endTimeJava = java.time.Instant.ofEpochMilli(endTime.toEpochMilli())
        
        val iptvProgram = IptvProgram(
            id = "placeholder_${channel.id}",
            title = "No Program Information",
            description = "EPG data not available for this channel",
            startTime = Date.from(startTimeJava),
            endTime = Date.from(endTimeJava),
            channel = channel
        )

        return ProgramGuideSchedule.createScheduleWithProgram(
            id = -1L,
            startsAt = startTime,
            endsAt = endTime,
            isClickable = true,
            displayTitle = "No Program Information",
            program = iptvProgram
        )
    }

    override fun requestRefresh() {
        requestingProgramGuideFor(currentDate)
    }

    override fun getAvailableCategories(): List<FilterOption> {
        return runBlocking {
            try {
                val categories = playlistService.getCategoriesForPlaylist(playlist.id)
                val filterOptions = mutableListOf<FilterOption>()
                
                // Add "All Channels" option
                filterOptions.add(FilterOption("All Channels", "all", true))
                
                // Add category options
                categories.forEach { category ->
                    filterOptions.add(FilterOption(category.name, category.id.toString(), false))
                }
                
                filterOptions
            } catch (e: Exception) {
                Log.e(TAG, "Error getting categories", e)
                emptyList()
            }
        }
    }

    override fun onCategorySelected(category: FilterOption) {
        selectedCategoryId = if (category.value == "all") {
            null
        } else {
            category.value.toLongOrNull()
        }
        requestingProgramGuideFor(currentDate)
    }
}
