package com.rio.keuanganku.security

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Pengelolaan passcode aplikasi.
 * PIN tidak pernah disimpan mentah — hanya hash SHA-256 dengan salt acak.
 */
object SecurityManager {

    private fun prefs(c: Context) = c.getSharedPreferences("security", Context.MODE_PRIVATE)

    fun isPasscodeSet(c: Context): Boolean = prefs(c).contains("hash")

    fun pinLength(c: Context): Int = prefs(c).getInt("len", 6)

    fun setPasscode(c: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = sha256(salt + pin.toByteArray())
        prefs(c).edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt("len", pin.length)
            .apply()
    }

    fun verify(c: Context, pin: String): Boolean {
        val p = prefs(c)
        val saltB64 = p.getString("salt", null) ?: return false
        val stored = p.getString("hash", null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val hash = Base64.encodeToString(sha256(salt + pin.toByteArray()), Base64.NO_WRAP)
        return hash == stored
    }

    fun clearPasscode(c: Context) {
        prefs(c).edit()
            .remove("salt").remove("hash").remove("len")
            .putBoolean("bio", false)
            .apply()
    }

    fun isBiometricEnabled(c: Context): Boolean = prefs(c).getBoolean("bio", false)

    fun setBiometricEnabled(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean("bio", enabled).apply()
    }

    fun canUseBiometric(c: Context): Boolean =
        BiometricManager.from(c)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun sha256(b: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(b)
}
