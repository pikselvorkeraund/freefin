package com.finlite.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finlite.app.util.PeriodType
import com.finlite.app.util.Periods

/** Селектор периода: тип (День…Год), стрелки назад/вперёд, метка диапазона */
@Composable
fun PeriodSelector(
    type: PeriodType,
    label: String,
    onType: (PeriodType) -> Unit,
    onShift: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PeriodType.entries.forEach { t ->
                    val selected = t == type
                    if (selected) {
                        Button(
                            onClick = { onType(t) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = ButtonDefaults.ContentPadding,
                        ) { Text(t.label, fontSize = 11.sp, maxLines = 1) }
                    } else {
                        OutlinedButton(
                            onClick = { onType(t) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = ButtonDefaults.ContentPadding,
                        ) { Text(t.label, fontSize = 11.sp, maxLines = 1) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onShift(-1) }) {
                    Icon(Icons.Filled.KeyboardArrowLeft, "Назад")
                }
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { onShift(1) }) {
                    Icon(Icons.Filled.KeyboardArrowRight, "Вперёд")
                }
            }
            Text(
                "Сегодня",
                modifier = Modifier.fillMaxWidth().clickable { onReset() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun MoneyText(minor: Long, isIncome: Boolean? = null, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium) {
    val color = when (isIncome) {
        true -> Color(0xFF2E7D32)
        false -> Color(0xFFC62828)
        null -> MaterialTheme.colorScheme.onSurface
    }
    val sign = when (isIncome) {
        true -> "+"
        false -> "−"
        null -> ""
    }
    Text(
        "$sign${Periods.formatMoney(minor)}",
        style = style,
        color = color,
        fontWeight = FontWeight.Bold,
    )
}
