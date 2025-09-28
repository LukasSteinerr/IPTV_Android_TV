package com.example.tv_app.epg.model

import androidx.compose.runtime.Stable
import com.example.tv_app.model.Channel
import com.example.tv_app.model.TvProgram
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Stable
data class EpgChannel(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val originalChannel: Channel
)

@Stable
data class EpgProgram(
    val id: String,
    val title: String,
    val description: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isCurrentProgram: Boolean = false,
    val isClickable: Boolean = true,
    val originalProgram: TvProgram?
) {
    val durationMinutes: Long = java.time.Duration.between(startTime, endTime).toMinutes()
    val widthRatio: Float = durationMinutes / 30f // Base width is 30 minutes
}

@Stable
data class EpgTimeSlot(
    val time: LocalDateTime,
    val displayText: String
) {
    companion object {
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        
        fun generateTimeSlots(
            startTime: LocalDateTime,
            endTime: LocalDateTime,
            intervalMinutes: Int = 30
        ): List<EpgTimeSlot> {
            val slots = mutableListOf<EpgTimeSlot>()
            var current = startTime
            
            while (current.isBefore(endTime)) {
                slots.add(
                    EpgTimeSlot(
                        time = current,
                        displayText = current.format(timeFormatter)
                    )
                )
                current = current.plusMinutes(intervalMinutes.toLong())
            }
            
            return slots
        }
    }
}

@Stable
data class EpgData(
    val channels: List<EpgChannel>,
    val programsByChannel: Map<String, List<EpgProgram>>,
    val timeSlots: List<EpgTimeSlot>,
    val date: LocalDate,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime
) {
    companion object {
        fun createFromChannelsAndPrograms(
            channels: List<Channel>,
            programsByChannel: Map<String, List<TvProgram>>,
            date: LocalDate,
            zoneId: ZoneId = ZoneId.systemDefault()
        ): EpgData {
            val epgChannels = channels.map { channel ->
                EpgChannel(
                    id = channel.id.toString(),
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    originalChannel = channel
                )
            }
            
            val startTime = date.atStartOfDay()
            val endTime = startTime.plusDays(1)
            
            val epgProgramsByChannel = programsByChannel.mapKeys { it.key }
                .mapValues { (channelId, programs) ->
                    programs.map { program ->
                        val progStartTime = program.startTime?.toInstant()?.atZone(zoneId)?.toLocalDateTime() ?: startTime
                        val progEndTime = program.stopTime?.toInstant()?.atZone(zoneId)?.toLocalDateTime() ?: progStartTime.plusMinutes(30)
                        
                        val now = LocalDateTime.now()
                        val isCurrentProgram = now.isAfter(progStartTime) && now.isBefore(progEndTime)
                        
                        EpgProgram(
                            id = program.id.toString(),
                            title = program.title,
                            description = program.description ?: "No description available",
                            startTime = progStartTime,
                            endTime = progEndTime,
                            isCurrentProgram = isCurrentProgram,
                            originalProgram = program
                        )
                    }.sortedBy { it.startTime }
                }
            
            // Fill gaps in programming
            val filledProgramsByChannel = epgProgramsByChannel.mapValues { (_, programs) ->
                fillProgramGaps(programs, startTime, endTime)
            }
            
            val timeSlots = EpgTimeSlot.generateTimeSlots(startTime, endTime)
            
            return EpgData(
                channels = epgChannels,
                programsByChannel = filledProgramsByChannel,
                timeSlots = timeSlots,
                date = date,
                startTime = startTime,
                endTime = endTime
            )
        }
        
        private fun fillProgramGaps(
            programs: List<EpgProgram>,
            dayStart: LocalDateTime,
            dayEnd: LocalDateTime
        ): List<EpgProgram> {
            if (programs.isEmpty()) {
                return listOf(
                    EpgProgram(
                        id = "gap_full_day",
                        title = "No Program Information",
                        description = "EPG data not available",
                        startTime = dayStart,
                        endTime = dayEnd,
                        isClickable = false,
                        originalProgram = null
                    )
                )
            }
            
            val result = mutableListOf<EpgProgram>()
            var currentTime = dayStart
            
            for (program in programs.sortedBy { it.startTime }) {
                // Add gap before program if needed
                if (currentTime.isBefore(program.startTime)) {
                    result.add(
                        EpgProgram(
                            id = "gap_${currentTime.toEpochSecond(java.time.ZoneOffset.UTC)}",
                            title = "No Program Information",
                            description = "EPG data not available",
                            startTime = currentTime,
                            endTime = program.startTime,
                            isClickable = false,
                            originalProgram = null
                        )
                    )
                }
                
                result.add(program)
                currentTime = program.endTime
            }
            
            // Add gap at the end if needed
            if (currentTime.isBefore(dayEnd)) {
                result.add(
                    EpgProgram(
                        id = "gap_end_${currentTime.toEpochSecond(java.time.ZoneOffset.UTC)}",
                        title = "No Program Information",
                        description = "EPG data not available",
                        startTime = currentTime,
                        endTime = dayEnd,
                        isClickable = false,
                        originalProgram = null
                    )
                )
            }
            
            return result
        }
    }
}
