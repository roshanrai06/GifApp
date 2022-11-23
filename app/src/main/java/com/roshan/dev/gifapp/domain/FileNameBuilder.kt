package com.roshan.dev.gifapp.domain

import java.time.ZoneId
import java.util.Date

/**
 * Utility class for building a file name.
 */
object FileNameBuilder {



    /**
     * Build a string formatted like: YYYY_MM_DD_SS in the default time zone on device.
     */
    fun buildFileName(): String {
        val zonedDateTime = Date().toInstant().atZone(ZoneId.systemDefault())
        return "${zonedDateTime.year}_" +
                "${formatMonth(zonedDateTime.month.value)}_" +
                "${formatDay(zonedDateTime.dayOfMonth)}_" +
                "${zonedDateTime.second}_" +
                "${zonedDateTime.nano}"
    }

    private fun formatDay(day: Int): String {
        return if (day < 10) {
            "0$day"
        } else {
            "$day"
        }
    }

    private fun formatMonth(month: Int): String {
        return if (month < 10) {
            "0$month"
        } else {
            "$month"
        }
    }
}