package io.pleo.antaeus.core.utils

import java.time.*


class TimeService(timeZone:String) {

    val timeZone = timeZone

    fun isFirst(): Boolean{
        //return true to fake first
        return LocalDate.now(ZoneId.of(this.timeZone)).dayOfMonth == 1
    }

    fun getDistanceToFirstOfNextMonth(): Long {

        val nextMonth = LocalDateTime.now(ZoneId.of(timeZone)).plusMonths(1)

        val midNight = LocalTime.of(0,0)

        val firstOfNextMonth = LocalDateTime.of(LocalDate.of(nextMonth.year, nextMonth.month, 1),midNight)

        //return 1000 to see how delay works
        return Duration.between(LocalDateTime.now(ZoneId.of(timeZone)), firstOfNextMonth).toMillis()
    }

}

