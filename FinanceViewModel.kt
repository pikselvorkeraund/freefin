package com.finlite.app.vm

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finlite.app.data.AppDb
import com.finlite.app.data.TxEntity
import com.finlite.app.util.PeriodRange
import com.finlite.app.util.Periods
import com.finlite.app.util.PeriodType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDb.get(app).txDao()

    val periodType = mutableStateOf(PeriodType.MONTH)
    val periodOffset = mutableIntStateOf(0)

    /** Поток выбранного периода (реагирует на смену типа и смещения) */
    private val rangeFlow: kotlinx.coroutines.flow.Flow<PeriodRange> =
        kotlinx.coroutines.flow.combine(
            androidx.compose.runtime.snapshotFlow { periodType.value },
            androidx.compose.runtime.snapshotFlow { periodOffset.intValue },
        ) { t, o -> Periods.range(t, o) }

    /** Транзакции за выбранный период */
    val transactions: StateFlow<List<TxEntity>> =
        rangeFlow.flatMapLatest { r -> dao.observeRange(r.from, r.to) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Доходы за период */
    val income: StateFlow<Long> =
        rangeFlow.flatMapLatest { r -> dao.observeIncome(r.from, r.to) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Расходы за период */
    val expense: StateFlow<Long> =
        rangeFlow.flatMapLatest { r -> dao.observeExpense(r.from, r.to) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setType(t: PeriodType) {
        periodType.value = t
        periodOffset.intValue = 0
    }

    fun shiftPeriod(dir: Int) {
        periodOffset.intValue += dir
    }

    fun resetPeriod() {
        periodOffset.intValue = 0
    }

    fun add(type: Int, amountMinor: Long, category: String, note: String, ts: Long) {
        viewModelScope.launch {
            dao.insert(TxEntity(type = type, amountMinor = amountMinor, category = category, note = note, ts = ts))
        }
    }

    fun update(tx: TxEntity) {
        viewModelScope.launch { dao.update(tx) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }
}
