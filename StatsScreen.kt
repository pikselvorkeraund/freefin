package com.finlite.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finlite.app.data.TxEntity
import com.finlite.app.util.Categories
import com.finlite.app.util.Periods
import com.finlite.app.vm.FinanceViewModel

@Composable
fun StatsScreen(vm: FinanceViewModel) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val income by vm.income.collectAsStateWithLifecycle()
    val expense by vm.expense.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PeriodSelector(
            vm.periodType.value,
            Periods.range(vm.periodType.value, vm.periodOffset.intValue).label,
            onType = vm::setType, onShift = vm::shiftPeriod, onReset = vm::resetPeriod,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Доходы", income, Color(0xFF2E7D32), Modifier.weight(1f))
            SummaryCard("Расходы", expense, Color(0xFFC62828), Modifier.weight(1f))
            SummaryCard("Баланс", income - expense,
                if (income - expense >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), Modifier.weight(1f))
        }

        CategoryBars("Расходы по категориям", txs.filter { it.type == 0 }, false)
        CategoryBars("Доходы по категориям", txs.filter { it.type == 1 }, true)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun CategoryBars(title: String, list: List<TxEntity>, isIncome: Boolean) {
    val byCat = list.groupBy { it.category }
        .map { Triple(Categories.emojiFor(it.key), it.key, it.value.sumOf { t -> t.amountMinor }) }
        .sortedByDescending { it.third }
    val max = byCat.maxOfOrNull { it.third } ?: 1

    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    if (byCat.isEmpty()) {
        Text("Нет данных", color = MaterialTheme.colorScheme.outline)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            byCat.forEach { (emoji, name, sum) ->
                Column {
                    Row(Modifier.fillMaxWidth()) {
                        Text("$emoji $name", Modifier.weight(1f))
                        MoneyText(sum, isIncome)
                    }
                    LinearProgressIndicator(
                        progress = { sum.toFloat() / max },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }
    }
}
