package com.example.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object ChantingEngine {

    val VIRTUE_MAP = mapOf(
        1 to "အရဟံ",
        2 to "သမ္မာသမ္ဗုဒ္ဓေါ",
        3 to "ဝိဇ္ဇာစရဏသမ္ပန္နော",
        4 to "သုဂတော",
        5 to "လောကဝိဒူ",
        6 to "အနုတ္တရော ပုရိသဓမ္မသာရထိ",
        7 to "သတ္ထာ ဒေဝမနုဿာနံ",
        8 to "ဗုဒ္ဓေါ",
        9 to "ဘဂဝါ"
    )

    val STAGE_SEQUENCES = mapOf(
        1 to listOf(2, 9, 4, 7, 5, 3, 6, 1, 8),
        2 to listOf(3, 1, 5, 8, 6, 4, 7, 2, 9),
        3 to listOf(4, 2, 6, 9, 7, 5, 8, 3, 1),
        4 to listOf(5, 3, 7, 1, 8, 6, 9, 4, 2),
        5 to listOf(6, 4, 8, 2, 9, 7, 1, 5, 3),
        6 to listOf(7, 5, 9, 3, 1, 8, 2, 6, 4),
        7 to listOf(8, 6, 1, 4, 2, 9, 3, 7, 5),
        8 to listOf(9, 7, 2, 5, 3, 1, 4, 8, 6),
        9 to listOf(1, 8, 3, 6, 4, 2, 5, 9, 7)
    )

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getTodayDateString(): String {
        return LocalDate.now().format(dateFormatter)
    }

    fun getDaysBetween(startDateStr: String, endDateStr: String): Long {
        return try {
            val start = LocalDate.parse(startDateStr, dateFormatter)
            val end = LocalDate.parse(endDateStr, dateFormatter)
            ChronoUnit.DAYS.between(start, end)
        } catch (e: Exception) {
            0
        }
    }

    fun addDaysToDate(dateStr: String, days: Long): String {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            date.plusDays(days).format(dateFormatter)
        } catch (e: Exception) {
            dateStr
        }
    }

    fun getBurmeseWeekday(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            when (date.dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> "တနင်္လာနေ့"
                java.time.DayOfWeek.TUESDAY -> "အင်္ဂါနေ့"
                java.time.DayOfWeek.WEDNESDAY -> "ဗုဒ္ဓဟူးနေ့"
                java.time.DayOfWeek.THURSDAY -> "ကြာသပတေးနေ့"
                java.time.DayOfWeek.FRIDAY -> "သောကြာနေ့"
                java.time.DayOfWeek.SATURDAY -> "စနေနေ့"
                java.time.DayOfWeek.SUNDAY -> "တနင်္ဂနွေနေ့"
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun getEnglishWeekday(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }

    fun getStageStartWeekday(stage: Int): java.time.DayOfWeek {
        return when (stage) {
            1 -> java.time.DayOfWeek.MONDAY
            2 -> java.time.DayOfWeek.WEDNESDAY
            3 -> java.time.DayOfWeek.FRIDAY
            4 -> java.time.DayOfWeek.SUNDAY
            5 -> java.time.DayOfWeek.TUESDAY
            6 -> java.time.DayOfWeek.THURSDAY
            7 -> java.time.DayOfWeek.SATURDAY
            8 -> java.time.DayOfWeek.MONDAY
            9 -> java.time.DayOfWeek.WEDNESDAY
            else -> java.time.DayOfWeek.MONDAY
        }
    }

    fun getExpectedWeekdayForPosition(stage: Int, position: Int): java.time.DayOfWeek {
        val startDay = getStageStartWeekday(stage)
        val startValue = startDay.value // 1 (Mon) to 7 (Sun)
        val targetValue = (startValue + position - 2) % 7 + 1
        return java.time.DayOfWeek.of(targetValue)
    }

    fun validateCycleConfig(dateStr: String, stage: Int, virtueIndex: Int): Boolean {
        val position = findPositionForVirtueInStage(stage, virtueIndex)
        val expectedDotw = getExpectedWeekdayForPosition(stage, position)
        return try {
            val date = LocalDate.parse(dateStr, dateFormatter)
            date.dayOfWeek == expectedDotw
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Compute the global day, stage, position and virtue details for a specific date given a starting anchor
     */
    fun calculateDayState(
        anchorDateStr: String,
        anchorStage: Int,
        anchorPosition: Int,
        targetDateStr: String
    ): ChantingDayState? {
        // Calculate the anchor global day index from Stage and Position
        val anchorGlobalDayIndex = (anchorStage - 1) * 9 + (anchorPosition - 1) // 0 to 80

        val daysDifference = getDaysBetween(anchorDateStr, targetDateStr)
        val targetGlobalDayIndex = anchorGlobalDayIndex + daysDifference // can be negative, 0..80, or >= 81

        if (targetGlobalDayIndex < 0) {
            return ChantingDayState(
                dateStr = targetDateStr,
                globalDayIndex = targetGlobalDayIndex.toInt(),
                globalDay = targetGlobalDayIndex.toInt() + 1,
                stage = 1,
                position = 1,
                virtueIndex = 1,
                virtueName = "",
                chantCount = 0,
                isNotStarted = true,
                isCompleted = false
            )
        }

        if (targetGlobalDayIndex >= 81) {
            return ChantingDayState(
                dateStr = targetDateStr,
                globalDayIndex = targetGlobalDayIndex.toInt(),
                globalDay = targetGlobalDayIndex.toInt() + 1,
                stage = 9,
                position = 9,
                virtueIndex = 7,
                virtueName = "",
                chantCount = 0,
                isCompleted = true,
                isCycleFinished = true
            )
        }

        // Within 0 to 80
        val stage = (targetGlobalDayIndex / 9).toInt() + 1
        val position = (targetGlobalDayIndex % 9).toInt() + 1
        val virtueIndex = STAGE_SEQUENCES[stage]?.get(position - 1) ?: 1
        val virtueName = VIRTUE_MAP[virtueIndex] ?: ""

        return ChantingDayState(
            dateStr = targetDateStr,
            globalDayIndex = targetGlobalDayIndex.toInt(),
            globalDay = targetGlobalDayIndex.toInt() + 1,
            stage = stage,
            position = position,
            virtueIndex = virtueIndex,
            virtueName = virtueName,
            chantCount = virtueIndex,
            isCompleted = false
        )
    }

    /**
     * Find position index of an inputted virtue in sequence of specified stage (Manual setup helper)
     */
    fun findPositionForVirtueInStage(stage: Int, virtueIndex: Int): Int {
        val sequence = STAGE_SEQUENCES[stage] ?: return 1
        val index = sequence.indexOf(virtueIndex)
        return if (index != -1) index + 1 else 1
    }
}

data class ChantingDayState(
    val dateStr: String,
    val globalDayIndex: Int, // 0..80
    val globalDay: Int, // 1..81
    val stage: Int, // 1..9
    val position: Int, // 1..9
    val virtueIndex: Int, // 1..9
    val virtueName: String,
    val chantCount: Int,
    val isNotStarted: Boolean = false,
    val isCycleFinished: Boolean = false,
    var isCompleted: Boolean = false
)
