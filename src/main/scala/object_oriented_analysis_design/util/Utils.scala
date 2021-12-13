package object_oriented_analysis_design.util

import object_oriented_analysis_design.{MSA, MSS}

import java.text.{DateFormatSymbols, DecimalFormat, DecimalFormatSymbols, SimpleDateFormat}
import java.time.temporal.ChronoUnit
import java.time.{Instant, ZoneId}
import java.util.{Calendar, Locale, TimeZone}

object Utils {
    implicit val formats = org.json4s.DefaultFormats.withLong.withDouble.withStrictOptionParsing

    // -------------------------------------------------------------------------------------------------------------------
    // Методы работы с датой и временем
    // -------------------------------------------------------------------------------------------------------------------
    val MINUTE = 60 * 1000L
    val HOUR = MINUTE * 60
    val DAY = HOUR * 24

    val defaultTimeZoneStr = "GMT+3"
    val defaultDateFormat = "yyyy-MM-dd"
    val defaultTimeFormat = "yyyy-MM-dd HH:mm:ss"
    val defaultTimeZone = TimeZone.getTimeZone(defaultTimeZoneStr)

    def getId: String = org.bson.types.ObjectId.get().toString

    implicit class MapItemGetter(msa: MSA) {
        def getOrError(key: String): Any =
            msa.getOrElse(key, throw new Exception(s"MapItem '$key' is not defined... Map: $msa"))
    }

    def getDayStartInMillis(millis: Long, timeZone: String = defaultTimeZoneStr): Long = {
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(timeZone))
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant.getEpochSecond * 1000L
    }

    def getDayEndInMillis(millis: Long, timeZone: String = defaultTimeZoneStr): Long = {
        getDayStartInMillis(millis, timeZone) + DAY - 1
    }

    def getFirstDayInMonth(millis: Long, timeZone: String = defaultTimeZoneStr): Long = {
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(timeZone))
            .truncatedTo(ChronoUnit.DAYS)
            .withDayOfMonth(1)
            .toInstant.getEpochSecond * 1000L
    }

    def addMonth(millis: Long, months: Int, timeZone: String = defaultTimeZoneStr): Long = {
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(timeZone))
            .plusMonths(months)
            .toInstant.getEpochSecond * 1000L
    }

    def addYear(millis: Long, years: Int, timeZone: String = defaultTimeZoneStr): Long = {
        Instant.ofEpochMilli(millis).atZone(ZoneId.of(timeZone))
            .plusYears(years)
            .toInstant.getEpochSecond * 1000L
    }

    def getAllPartsOfDateAsMap(millis: Long, prefix: String, timeZone: String = defaultTimeZoneStr): MSS = {
        //в случае неправильного формата - возбудится исключение
        val cal = Calendar.getInstance()
        cal.setTimeInMillis(millis)
        cal.setTimeZone(TimeZone.getTimeZone(timeZone))

        def getMonthName(locale: String, caseFlag: Boolean = false) =
            if (caseFlag) {
                DateFormatSymbols.getInstance(new Locale(locale)).getMonths()(cal.get(Calendar.MONTH))
            } else {
                Utils.millisToFormattedDate(millis, "MMMMM", locale = locale).toLowerCase
            }

        Map(
            prefix + ".year" -> Utils.numberFormatter("00", cal.get(Calendar.YEAR)),
            prefix + ".shortYear" -> Utils.numberFormatter("00", cal.get(Calendar.YEAR) - 2000),
            prefix + ".month" -> Utils.numberFormatter("00", cal.get(Calendar.MONTH) + 1),
            prefix + ".day" -> Utils.numberFormatter("00", cal.get(Calendar.DAY_OF_MONTH)),
            prefix + ".hour" -> Utils.numberFormatter("00", cal.get(Calendar.HOUR_OF_DAY)),
            prefix + ".minute" -> Utils.numberFormatter("00", cal.get(Calendar.MINUTE)),
            prefix + ".second" -> Utils.numberFormatter("00", cal.get(Calendar.SECOND)),
            prefix + ".dayOfYear" -> Utils.numberFormatter("000", cal.get(Calendar.DAY_OF_YEAR)),
            prefix + ".month.name.ru" -> getMonthName("ru"),
            prefix + ".month.name.ru.firstUpper" -> getMonthName("ru").capitalize,
            prefix + ".month.name.ru.case" -> getMonthName("ru", caseFlag = true),
            prefix + ".month.name.en" -> getMonthName("en"),
            prefix + ".month.name.en.firstUpper" -> getMonthName("en").capitalize
        )
    }

    def millisToFormattedDate(
                                 t: Long = System.currentTimeMillis,
                                 format: String = defaultTimeFormat,
                                 timeZone: String = defaultTimeZoneStr,
                                 locale: String = "ru",
                                 extraMonth: Int = 0,
                                 extraYear: Int = 0
                             ): String = {
        val sdf = new SimpleDateFormat(format, new Locale(locale))
        val addedMonths = addMonth(t, extraMonth, timeZone)
        val addedYears = addYear(addedMonths, extraYear, timeZone)
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone))
        sdf.format(new java.util.Date(addedYears))
    }

    def formattedDateToMillis(date: String, format: String = defaultDateFormat, timeZone: String = defaultTimeZoneStr): Long = {
        val sdf = new SimpleDateFormat(format)
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone))
        sdf.parse(date).getTime
    }

    def numberFormatter(pattern: String, number: BigDecimal): String = {
        val symbols = DecimalFormatSymbols.getInstance
        symbols.setGroupingSeparator(' ')
        symbols.setDecimalSeparator(',')
        new DecimalFormat(pattern, symbols).format(number)
    }



    def json2map(json: String): MSA = org.json4s.native.Serialization.read[MSA](json)

    def map2json(map: MSA, isPretty: Boolean = false): String = isPretty match {
        case true => org.json4s.native.Serialization.writePretty(map)
        case false => org.json4s.native.Serialization.write(map)
    }
}
