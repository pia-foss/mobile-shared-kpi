@file:Suppress("unused")

package com.privateinternetaccess.kpi.internals.utils

/**
 * Port of Javas java.util.concurrent.TimeUnit implementation.
 * see: https://android.googlesource.com/platform/libcore2/+/refs/heads/master/luni/src/main/java/java/util/concurrent/TimeUnit.java
 */
enum class KTimeUnit {
    NANOSECONDS {
        override fun toNanos(duration: Long): Long = duration
        override fun toMicros(duration: Long): Long = duration / (MICROSECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS)
        override fun toMillis(duration: Long): Long = duration / (MILLISECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS)
        override fun toSeconds(duration: Long): Long = duration / (SECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS)
        override fun toMinutes(duration: Long): Long = duration / (MINUTE_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS)
        override fun toHours(duration: Long): Long = duration / (HOUR_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS)
        override fun toDays(duration: Long): Long = duration / (DAY_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS)
        override fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = sourceUnit.toNanos(sourceDuration)
    },
    MICROSECONDS {
        override fun toNanos(duration: Long): Long = scale(duration = duration, magnitude = MICROSECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (MICROSECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS))
        override fun toMicros(duration: Long): Long = duration
        override fun toMillis(duration: Long): Long = duration / (MILLISECOND_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS)
        override fun toSeconds(duration: Long): Long = duration / (SECOND_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS)
        override fun toMinutes(duration: Long): Long = duration / (MINUTE_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS)
        override fun toHours(duration: Long): Long = duration / (HOUR_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS)
        override fun toDays(duration: Long): Long = duration / (DAY_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS)
        override fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = sourceUnit.toMicros(sourceDuration)
    },
    MILLISECONDS {
        override fun toNanos(duration: Long): Long = scale(duration = duration, magnitude = MILLISECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (MILLISECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS))
        override fun toMicros(duration: Long): Long = scale(duration = duration, magnitude = MILLISECOND_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (MILLISECOND_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS))
        override fun toMillis(duration: Long): Long = duration
        override fun toSeconds(duration: Long): Long = duration / (SECOND_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS)
        override fun toMinutes(duration: Long): Long = duration / (MINUTE_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS)
        override fun toHours(duration: Long): Long = duration / (HOUR_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS)
        override fun toDays(duration: Long): Long = duration / (DAY_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS)
        override fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = sourceUnit.toMillis(sourceDuration)
    },
    SECONDS {
        override fun toNanos(duration: Long): Long = scale(duration = duration, magnitude = SECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (SECOND_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS))
        override fun toMicros(duration: Long): Long = scale(duration = duration, magnitude = SECOND_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (SECOND_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS))
        override fun toMillis(duration: Long): Long = scale(duration = duration, magnitude = SECOND_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (SECOND_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS))
        override fun toSeconds(duration: Long): Long = duration
        override fun toMinutes(duration: Long): Long = duration / (MINUTE_IN_NANOSECONDS / SECOND_IN_NANOSECONDS)
        override fun toHours(duration: Long): Long = duration / (HOUR_IN_NANOSECONDS / SECOND_IN_NANOSECONDS)
        override fun toDays(duration: Long): Long = duration / (DAY_IN_NANOSECONDS / SECOND_IN_NANOSECONDS)
        override fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = sourceUnit.toSeconds(sourceDuration)
    },
    MINUTES {
        override fun toNanos(duration: Long): Long = scale(duration = duration, magnitude = MINUTE_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (MINUTE_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS))
        override fun toMicros(duration: Long): Long = scale(duration = duration, magnitude = MINUTE_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (MINUTE_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS))
        override fun toMillis(duration: Long): Long = scale(duration = duration, magnitude = MINUTE_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (MINUTE_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS))
        override fun toSeconds(duration: Long): Long = scale(duration = duration, magnitude = MINUTE_IN_NANOSECONDS / SECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (MINUTE_IN_NANOSECONDS / SECOND_IN_NANOSECONDS))
        override fun toMinutes(duration: Long): Long = duration
        override fun toHours(duration: Long): Long = duration / (HOUR_IN_NANOSECONDS / MINUTE_IN_NANOSECONDS)
        override fun toDays(duration: Long): Long = duration / (DAY_IN_NANOSECONDS / MINUTE_IN_NANOSECONDS)
        override fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = sourceUnit.toMinutes(sourceDuration)
    },
    HOURS {
        override fun toNanos(duration: Long): Long = scale(duration = duration, magnitude = HOUR_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (HOUR_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS))
        override fun toMicros(duration: Long): Long = scale(duration = duration, magnitude = HOUR_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (HOUR_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS))
        override fun toMillis(duration: Long): Long = scale(duration = duration, magnitude = HOUR_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (HOUR_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS))
        override fun toSeconds(duration: Long): Long = scale(duration = duration, magnitude = HOUR_IN_NANOSECONDS / SECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (HOUR_IN_NANOSECONDS / SECOND_IN_NANOSECONDS))
        override fun toMinutes(duration: Long): Long = scale(duration = duration, magnitude = HOUR_IN_NANOSECONDS / MINUTE_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (HOUR_IN_NANOSECONDS / MINUTE_IN_NANOSECONDS))
        override fun toHours(duration: Long): Long = duration
        override fun toDays(duration: Long): Long = duration / (DAY_IN_NANOSECONDS / HOUR_IN_NANOSECONDS)
        override fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = sourceUnit.toHours(sourceDuration)
    },
    DAYS {
        override fun toNanos(duration: Long): Long = scale(duration = duration, magnitude = DAY_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (DAY_IN_NANOSECONDS / NANOSECOND_IN_NANOSECONDS))
        override fun toMicros(duration: Long): Long = scale(duration = duration, magnitude = DAY_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (DAY_IN_NANOSECONDS / MICROSECOND_IN_NANOSECONDS))
        override fun toMillis(duration: Long): Long = scale(duration = duration, magnitude = DAY_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (DAY_IN_NANOSECONDS / MILLISECOND_IN_NANOSECONDS))
        override fun toSeconds(duration: Long): Long = scale(duration = duration, magnitude = DAY_IN_NANOSECONDS / SECOND_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (DAY_IN_NANOSECONDS / SECOND_IN_NANOSECONDS))
        override fun toMinutes(duration: Long): Long = scale(duration = duration, magnitude = DAY_IN_NANOSECONDS / MINUTE_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (DAY_IN_NANOSECONDS / MINUTE_IN_NANOSECONDS))
        override fun toHours(duration: Long): Long = scale(duration = duration, magnitude = DAY_IN_NANOSECONDS / HOUR_IN_NANOSECONDS, overflow = Long.MAX_VALUE / (DAY_IN_NANOSECONDS / HOUR_IN_NANOSECONDS))
        override fun toDays(duration: Long): Long = duration
        override fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = sourceUnit.toDays(sourceDuration)
    }
    ;

    /**
     * Convert the given time duration in the given unit to this unit.
     * Conversions from finer to coarser granularities truncate, so lose precision.
     * For example converting <tt>999</tt> milliseconds to seconds results in <tt>0</tt>.
     * Conversions from coarser to finer granularities with arguments that would numerically overflow saturate to <tt>Long.MIN_VALUE</tt> if negative or <tt>Long.MAX_VALUE</tt> if positive.
     *
     * <p>For example, to convert 10 minutes to milliseconds, use:
     * <tt>TimeUnit.MILLISECONDS.convert(10L, TimeUnit.MINUTES)</tt>
     *
     * @param sourceDuration the time duration in the given <tt>sourceUnit</tt>
     * @param sourceUnit the unit of the <tt>sourceDuration</tt> argument
     * @return the converted duration in this unit, or <tt>Long.MIN_VALUE</tt> if conversion would negatively overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     */
    open fun convert(sourceDuration: Long, sourceUnit: KTimeUnit): Long = throw UnsupportedOperationException()

    /**
     * Equivalent to <tt>NANOSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration, or <tt>Long.MIN_VALUE</tt> if conversion would negatively overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    open fun toNanos(duration: Long): Long = throw UnsupportedOperationException()

    /**
     * Equivalent to <tt>MICROSECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration, or <tt>Long.MIN_VALUE</tt> if conversion would negatively overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    open fun toMicros(duration: Long): Long = throw UnsupportedOperationException()

    /**
     * Equivalent to <tt>MILLISECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration, or <tt>Long.MIN_VALUE</tt> if conversion would negatively overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    open fun toMillis(duration: Long): Long = throw UnsupportedOperationException()

    /**
     * Equivalent to <tt>SECONDS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration, or <tt>Long.MIN_VALUE</tt> if conversion would negatively overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     */
    open fun toSeconds(duration: Long): Long = throw UnsupportedOperationException()

    /**
     * Equivalent to <tt>MINUTES.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration, or <tt>Long.MIN_VALUE</tt> if conversion would negatively overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     * @since 1.6
     */
    open fun toMinutes(duration: Long): Long = throw UnsupportedOperationException()

    /**
     * Equivalent to <tt>HOURS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration, or <tt>Long.MIN_VALUE</tt> if conversion would negatively overflow, or <tt>Long.MAX_VALUE</tt> if it would positively overflow.
     * @see #convert
     * @since 1.6
     */
    open fun toHours(duration: Long): Long = throw UnsupportedOperationException()

    /**
     * Equivalent to <tt>DAYS.convert(duration, this)</tt>.
     * @param duration the duration
     * @return the converted duration
     * @see #convert
     * @since 1.6
     */
    open fun toDays(duration: Long): Long = throw UnsupportedOperationException()

    companion object {
        private const val NANOSECOND_IN_NANOSECONDS: Long = 1L              // = 1L
        private const val MICROSECOND_IN_NANOSECONDS: Long = 1000L          // = NANOSECOND_IN_NANOSECONDS * 1000L
        private const val MILLISECOND_IN_NANOSECONDS: Long = 1_000_000L     // = MICROSECOND_IN_NANOSECONDS * 1000L
        private const val SECOND_IN_NANOSECONDS: Long = 1_000_000_000L      // = MILLISECOND_IN_NANOSECONDS * 1000L
        private const val MINUTE_IN_NANOSECONDS: Long = 60_000_000_000L     // = SECOND_IN_NANOSECONDS * 60L
        private const val HOUR_IN_NANOSECONDS: Long = 3_600_000_000_000L    // = MINUTE_IN_NANOSECONDS * 60L
        private const val DAY_IN_NANOSECONDS: Long = 86_400_000_000_000L    // = HOUR_IN_NANOSECONDS * 24L

        /**
         * Scale duration by magnitude, checking for overflow.
         */
        private fun scale(duration: Long, magnitude: Long, overflow: Long): Long = when {
            duration > overflow -> Long.MAX_VALUE
            duration < -overflow -> Long.MIN_VALUE
            else -> duration * magnitude
        }
    }
}