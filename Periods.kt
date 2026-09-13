package com.finlite.app.util

import java.text.DecimalFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PeriodType(val label: String) {
    DAY("День"), WEEK("Неделя"), MONTH("Месяц"), QUARTER("Квартал"), YEAR("Год")
}

data class PeriodRange(val from: Long, val to: Long, val label: String)

object Periods {

    fun range(type: PeriodType, offset: Int, zone: ZoneId = ZoneId.systemDefault()): PeriodRange {
        val today = LocalDate.now(zone).plus(offset.toLong(), periodUnit(type))
        val (start, end) = when (type) {
            PeriodType.DAY -> today to today.plusDays(1)
            PeriodType.WEEK -> {
                val monday = today.with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1)
                monday to monday.plusWeeks(1)
            }
            PeriodType.MONTH -> {
                val first = today.withDayOfMonth(1)
                first to first.plusMonths(1)
            }
            PeriodType.QUARTER -> {
                val qFirstMonth = ((today.monthValue - 1) / 3) * 3 + 1
                val first = today.withMonth(qFirstMonth).withDayOfMonth(1)
                first to first.plusMonths(3)
            }
            PeriodType.YEAR -> {
                val first = today.withDayOfYear(1)
                first to first.plusYears(1)
            }
        }
        val fmt = when (type) {
            PeriodType.DAY -> DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
            PeriodType.WEEK -> DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
            PeriodType.MONTH, PeriodType.QUARTER ->
                DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
            PeriodType.YEAR -> DateTimeFormatter.ofPattern("yyyy")
        }
        val base = start.format(fmt)
        val label = when (type) {
            PeriodType.WEEK -> {
                val endLbl = end.minusDays(1).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru")))
                "$base — $endLbl"
            }
            PeriodType.QUARTER -> "Q${((start.monthValue - 1) / 3) + 1} $base"
            else -> base
        }
        return PeriodRange(
            from = start.atStartOfDay(zone).toInstant().toEpochMilli(),
            to = end.atStartOfDay(zone).toInstant().toEpochMilli(),
            label = label.replaceFirstChar { it.uppercase(Locale("ru")) },
        )
    }

    private fun periodUnit(type: PeriodType) = when (type) {
        PeriodType.DAY -> java.time.temporal.ChronoUnit.DAYS
        PeriodType.WEEK -> java.time.temporal.ChronoUnit.WEEKS
        PeriodType.MONTH -> java.time.temporal.ChronoUnit.MONTHS
        PeriodType.QUARTER -> java.time.temporal.ChronoUnit.MONTHS // смещение в 3 месяца ниже
        PeriodType.YEAR -> java.time.temporal.ChronoUnit.YEARS
    }

    fun formatMoney(minor: Long): String {
        val df = DecimalFormat("#,##0.00")
        return df.format(minor / 100.0)
    }

    fun formatDateTime(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))
            .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), zone))

    fun formatDate(ts: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale("ru"))
            .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), zone))
}

data class Category(val emoji: String, val name: String)

object Categories {
    val expense = listOf(
        Category("🍔", "Еда"),
        Category("🚌", "Транспорт"),
        Category("🏠", "Жильё"),
        Category("📱", "Связь"),
        Category("💊", "Здоровье"),
        Category("👕", "Одежда"),
        Category("🎬", "Развлечения"),
        Category("🛒", "Продукты"),
        Category("🐾", "Питомцы"),
        Category("📦", "Прочее"),
    )
    val income = listOf(
        Category("💼", "Зарплата"),
        Category("🛠", "Подработка"),
        Category("🎁", "Подарок"),
        Category("📈", "Инвестиции"),
        Category("📦", "Прочее"),
    )

    fun all(): List<Category> = expense + income

    fun emojiFor(name: String): String = all().firstOrNull { it.name == name }?.emoji ?: "📦"
}
