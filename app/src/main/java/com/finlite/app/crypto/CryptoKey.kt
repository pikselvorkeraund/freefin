package com.finlite.app.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Провайдер пароля для SQLCipher на базе Android Keystore.
 *
 * Схема:
 *  - При первом запуске генерируется случайный 256-битный пароль БД
 *    (SecureRandom) и 256-битный AES-ключ в Android Keystore (alias fl_wrap).
 *  - Пароль БД шифруется этим ключом (AES/GCM) и сохраняется в приватном
 *    SharedPreferences только в зашифрованном виде (IV + ciphertext).
 *  - Материал Keystore-ключа за пределы TEE/StrongBox не экспортируется:
 *    декомпиляция кода даёт лишь alias и схему, но не сам ключ —
 *    для расшифровки БД нужен доступ к Keystore конкретного устройства.
 *  - Keystore-ключ привязан к приложению: при очистке данных/удалении
 *    приложения он исчезает. Тогда пароль невозможно восстановить —
 *    старая БД удаляется и создаётся новая (см. Db.kt), что соответствует
 *    спецификации "ключ генерируется при первой установке или при исчезновении".
 */
object CryptoKey {

    private const val PREFS_NAME = "fl_cfg_v2"
    private const val PREFS_WRAPPED = "w"

    private const val KS_PROVIDER = "AndroidKeyStore"
    private const val KS_ALIAS = "fl_wrap_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_LEN = 12

    /** Возвращает сырой 256-битный пароль БД (создаёт при первом вызове). */
    fun passphrase(context: Context): ByteArray {
        val prefs = prefs(context)
        val wrapped = prefs.getString(PREFS_WRAPPED, null)
        return if (wrapped != null) {
            unwrap(prefs, wrapped)
        } else {
            val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
            wrap(prefs, raw)
            raw
        }
    }

    /** true, если Keystore-ключ на месте и в prefs лежит валидная обёртка. */
    fun hasUsableKey(context: Context): Boolean {
        val prefs = prefs(context)
        if (!prefs.contains(PREFS_WRAPPED)) return false
        return try {
            loadKey() != null
        } catch (e: Exception) {
            false
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun wrap(prefs: SharedPreferences, raw: ByteArray) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val blob = cipher.iv + cipher.doFinal(raw)
        prefs.edit().putString(PREFS_WRAPPED, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }

    private fun unwrap(prefs: SharedPreferences, wrapped: String): ByteArray {
        val blob = Base64.decode(wrapped, Base64.NO_WRAP)
        val iv = blob.copyOfRange(0, GCM_IV_LEN)
        val ct = blob.copyOfRange(GCM_IV_LEN, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, loadKey()!!, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun getOrCreateKey(): SecretKey {
        loadKey()?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS_PROVIDER)
        kg.init(
            KeyGenParameterSpec.Builder(
                KS_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return kg.generateKey()
    }

    private fun loadKey(): SecretKey? {
        val ks = KeyStore.getInstance(KS_PROVIDER).apply { load(null) }
        return (ks.getEntry(KS_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }
}
