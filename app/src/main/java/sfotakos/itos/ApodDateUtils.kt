package sfotakos.itos

import android.annotation.SuppressLint
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.JUNE

object ApodDateUtils {

    private const val QUERY_DATE_FORMAT = "yyyy-MM-dd"
    private const val YESTERDAY = -1
    private const val TOMORROW = 1

    private var earliestDateCalendar: Calendar? = null
    private val gmtCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))

    fun gmtCalendar(): Calendar {
        return gmtCalendar?.clone() as Calendar
    }

    /**
     * Sets the earliest date the APOD API has a valid response into a Calendar
     *
     * @return A calendar of the date June 16, 1995
     */
    fun earliestApiDateCalendar(): Calendar {
        if (earliestDateCalendar == null) {
            val calendar = gmtCalendar()
            calendar.set(Calendar.YEAR, 1995)
            calendar.set(Calendar.MONTH, JUNE)
            calendar.set(Calendar.DAY_OF_MONTH, 16)
            earliestDateCalendar = calendar
        }

        return earliestDateCalendar?.clone() as Calendar
    }

    /**
     * Decrement the given calendar a day into the past
     *
     * @param calendar any non-null Calendar object
     * @return the same calendar decremented a day
     */
    fun previousDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, YESTERDAY)
        return calendar
    }

    /**
     * Increment the given calendar a day into the future
     *
     * @param calendar any non-null Calendar object
     * @return the same calendar incremented a day
     */
    fun nextDay(calendar: Calendar): Calendar {
        calendar.add(Calendar.DATE, TOMORROW)
        return calendar
    }

    /**
     * Format calendar into the API formatted string
     *
     * @param calendar any non-null Calendar object
     * @return Date string formatted following the API pattern yyyy-MM-dd
     */
    @SuppressLint("SimpleDateFormat")
    fun calendarToString(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat(QUERY_DATE_FORMAT)
        return dateFormat.format(calendar.time)
    }

    /**
     * Formats specified date string into date object
     *
     * @param date date String formatted following the API pattern yyyy-MM-dd
     * @return Date object
     */
    @SuppressLint("SimpleDateFormat")
    fun stringToDate(date: String): Date {
        return SimpleDateFormat(QUERY_DATE_FORMAT, Locale.ENGLISH).parse(date)
    }

    /**
     * Parse specified Date into date String
     *
     * @param date any valid Date
     * @return String formatted following the API pattern yyyy-MM-dd
     */
    @SuppressLint("SimpleDateFormat")
    fun dateToString(date: Date): String {
        return SimpleDateFormat(QUERY_DATE_FORMAT).format(date)
    }

    /**
     * Formats specified date string into a formatted localized date string
     *
     * @param date Date string formatted following the API pattern yyyy-MM-dd
     * @return Date string formatted according to the default locale
     */
    @SuppressLint("SimpleDateFormat")
    fun localizedDateString(date: String): String {
        val apiDateFormat = SimpleDateFormat(QUERY_DATE_FORMAT)
        val formattedDate: Date?
        return try {
            formattedDate = apiDateFormat.parse(date)
            val finalDateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault())
            finalDateFormat.format(formattedDate)
        } catch (e: ParseException) {
            date
        }
    }

}