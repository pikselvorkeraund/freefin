package com.finlite.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finlite.app.util.Categories
import com.finlite.app.util.Periods
import com.finlite.app.vm.FinanceViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun HomeScreen(vm: FinanceViewModel) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    val income by vm.income.collectAsStateWithLifecycle()
    val expense by vm.expense.collectAsStateWithLifecycle()
    var addType by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PeriodSelector(
            vm.periodType.value,
            Periods.range(vm.periodType.value, vm.periodOffset.intValue).label,
            onType = vm::setType, onShift = vm::shiftPeriod, onReset = vm::resetPeriod,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { addType = 0 }, modifier = Modifier.weight(1f).height(84.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))) {
                Text("− Расход", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { addType = 1 }, modifier = Modifier.weight(1f).height(84.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                Text("+ Доход", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Доходы", income, Color(0xFF2E7D32), Modifier.weight(1f))
            SummaryCard("Расходы", expense, Color(0xFFC62828), Modifier.weight(1f))
            SummaryCard("Баланс", income - expense,
                if (income - expense >= 0) Color(0xFF2E7D32) else Color(0xFFC62828), Modifier.weight(1f))
        }

        Text("Операции", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(txs, key = { it.id }) { tx ->
                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Categories.emojiFor(tx.category), fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tx.category, fontWeight = FontWeight.Medium)
                            Text(Periods.formatDateTime(tx.ts), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline)
                            if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall)
                        }
                        MoneyText(tx.amountMinor, tx.type == 1)
                    }
                }
            }
            if (txs.isEmpty()) {
                item {
                    Text("Пока пусто. Добавьте операцию кнопкой выше.",
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }

    addType?.let { t ->
        AddSheet(type = t, onDismiss = { addType = null },
            onSave = { amt, cat, note, ts -> vm.add(t, amt, cat, note, ts); addType = null })
    }
}

@Composable
fun SummaryCard(title: String, minor: Long, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline)
            Text(Periods.formatMoney(minor), color = color, fontWeight = FontWeight.Bold,
                fontSize = 15.sp, maxLines = 1)
        }
    }
}

internal fun parseMinor(s: String): Long = try {
    if (s.isBlank() || s == ".") 0
    else BigDecimal(s).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
} catch (e: Exception) { 0 }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSheet(type: Int, onDismiss: () -> Unit, onSave: (Long, String, String, Long) -> Unit) {
    val isExp = type == 0
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf((if (isExp) Categories.expense else Categories.income).first().name) }
    var note by remember { mutableStateOf("") }
    var ts by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDate by remember { mutableStateOf(false) }
    val cats = if (isExp) Categories.expense else Categories.income

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isExp) "Новый расход" else "Новый доход", style = MaterialTheme.typography.titleLarge)
            Text(Periods.formatMoney(parseMinor(amount)), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExp) Color(0xFFC62828) else Color(0xFF2E7D32))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cats.forEach { c ->
                    FilterChip(selected = category == c.name, onClick = { category = c.name },
                        label = { Text("${c.emoji} ${c.name}") })
                }
            }
            OutlinedTextField(note, { note = it }, label = { Text("Заметка") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            TextButton(onClick = { showDate = true }) { Text("📅 " + Periods.formatDate(ts)) }
            Keypad(amount) { amount = it }
            Button(onClick = { val m = parseMinor(amount); if (m > 0) onSave(m, category, note, ts) },
                enabled = parseMinor(amount) > 0, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Сохранить", fontSize = 18.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = ts)
        DatePickerDialog(onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { ts = it }; showDate = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Отмена") } }) {
            DatePicker(state)
        }
    }
}

@Composable
fun Keypad(amount: String, onChange: (String) -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"), listOf("4", "5", "6"),
        listOf("7", "8", "9"), listOf(".", "0", "⌫"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    OutlinedButton(onClick = { onChange(pressKey(amount, k)) },
                        modifier = Modifier.weight(1f).height(56.dp)) { Text(k, fontSize = 22.sp) }
                }
            }
        }
    }
}

private fun pressKey(cur: String, k: String): String = when (k) {
    "⌫" -> cur.dropLast(1)
    "." -> if (cur.contains('.')) cur else cur + "."
    else -> if (cur.substringAfter('.', "").length >= 2) cur else cur + k
}
