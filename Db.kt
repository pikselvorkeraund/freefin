package com.finlite.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.finlite.app.crypto.CryptoKey
import kotlinx.coroutines.flow.Flow
import net.sqlcipher.database.SupportFactory

@Entity(tableName = "transactions")
data class TxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: Int,            // 0 = расход, 1 = доход
    val amountMinor: Long,    // сумма в минорных единицах (копейки/центы)
    val category: String,
    val note: String = "",
    val ts: Long,             // epoch millis
)

@Dao
interface TxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TxEntity): Long

    @Update
    suspend fun update(tx: TxEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM transactions WHERE ts >= :from AND ts < :to ORDER BY ts DESC")
    fun observeRange(from: Long, to: Long): Flow<List<TxEntity>>

    @Query("SELECT COALESCE(SUM(amountMinor),0) FROM transactions WHERE ts >= :from AND ts < :to AND type = 1")
    fun observeIncome(from: Long, to: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor),0) FROM transactions WHERE ts >= :from AND ts < :to AND type = 0")
    fun observeExpense(from: Long, to: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int
}

@Database(entities = [TxEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun txDao(): TxDao

    companion object {
        @Volatile
        private var instance: AppDb? = null

        fun get(context: Context): AppDb =
            instance ?: synchronized(this) {
                val appContext = context.applicationContext

                // Если ключ исчез (очистка данных/переустановка), а БД есть —
                // удаляем нечитаемую БД, чтобы Room создал новую с новым ключом.
                val dbFile = appContext.getDatabasePath("finlite.db")
                if (dbFile.exists() && !CryptoKey.hasUsableKey(appContext)) {
                    dbFile.delete()
                }

                Room.databaseBuilder(appContext, AppDb::class.java, "finlite.db")
                    .openHelperFactory(SupportFactory(CryptoKey.passphrase(appContext)))
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
