package ai.framex.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

class SecureCredentialStore(context: Context) {
  private val prefs = context.getSharedPreferences("frame_x_secure", Context.MODE_PRIVATE)
  private val keyAlias = "frame_x_ai_credentials"
  private val transformation = "AES/GCM/NoPadding"

  private fun key(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        keyAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setUserAuthenticationRequired(false)
        .build()
    )
    return generator.generateKey()
  }

  fun put(id: String, secret: String) {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.ENCRYPT_MODE, key())
    val ciphertext = cipher.doFinal(secret.toByteArray(StandardCharsets.UTF_8))
    val encodedIv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    val encodedCiphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    prefs.edit()
      .putString("${id}_iv", encodedIv)
      .putString("${id}_value", encodedCiphertext)
      .apply()
  }

  fun get(id: String): String? {
    val iv = prefs.getString("${id}_iv", null) ?: return null
    val ciphertext = prefs.getString("${id}_value", null) ?: return null

    val cipher = Cipher.getInstance(transformation)
    cipher.init(
      Cipher.DECRYPT_MODE,
      key(),
      GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
    )
    return String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), StandardCharsets.UTF_8)
  }

  fun remove(id: String) {
    prefs.edit()
      .remove("${id}_iv")
      .remove("${id}_value")
      .apply()
  }
}
