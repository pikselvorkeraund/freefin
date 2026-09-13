package com.finlite.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finlite.app.data.TxEntity
import com.finlite.app.util.Categories
import com.finlite.app.util.Periods
import com.finlite.app.vm.FinanceViewModel

@Composable
fun HistoryScreen(vm: FinanceViewModel) {
    val txs by vm.transactions.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<TxEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PeriodSelector(
            vm.periodType.value,
            Periods.range(vm.periodType.value, vm.periodOffset.intValue).label,
            onType = vm::setType, onShift = vm::shiftPeriod, onReset = vm::resetPeriod,
        )

        if (txs.isEmpty()) Text("Нет операций за период", color = MaterialTheme.colorScheme.outline)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(txs, key = { it.id }) { tx ->
                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth().clickable { editing = tx }) {
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
        }
    }

    editing?.let { tx ->
        EditDialog(tx,
            onSave = { vm.update(it); editing = null },
            onDelete = { vm.delete(tx.id); editing = null },
            onDismiss = { editing = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditDialog(tx: TxEntity, onSave: (TxEntity) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf(Periods.formatMoney(tx.amountMinor)) }
    var category by remember { mutableStateOf(tx.category) }
    var note by remember { mutableStateOf(tx.note) }
    var ts by remember { mutableStateOf(tx.ts) }
    var showDate by remember { mutableStateOf(false) }
    val cats = if (tx.type == 0) Categories.expense else Categories.income

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cats.forEach { c ->
                        FilterChip(selected = category == c.name, onClick = { category = c.name },
                            label = { Text("${c.emoji} ${c.name}") })
                    }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Заметка") }, singleLine = true)
                TextButton(onClick = { showDate = true }) { Text("📅 " + Periods.formatDate(ts)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val m = parseMinor(amount.replace(" ", "").replace(",", "."))
                if (m > 0) onSave(tx.copy(amountMinor = m, category = category, note = note, ts = ts))
            }) { Text("Сохранить") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )

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
