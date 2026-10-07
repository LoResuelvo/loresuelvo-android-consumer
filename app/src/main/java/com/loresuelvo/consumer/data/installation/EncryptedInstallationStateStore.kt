package com.loresuelvo.consumer.data.installation

import android.content.SharedPreferences
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.installation.InstallationIdentity
import com.loresuelvo.consumer.domain.installation.InstallationStateStore
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named

@Singleton
class EncryptedInstallationStateStore @Inject constructor(
    @Named("installationPrefs") private val preferences: SharedPreferences,
) : InstallationStateStore {
    @Synchronized
    override fun prepare(userId: Int, attemptId: String): InstallationBinding {
        val identity = readIdentity() ?: InstallationIdentity(uuid(), uuid())
        val existing = readBinding(identity)
        if (existing?.userId == userId && existing.attemptId == attemptId) return existing
        val next = InstallationBinding(identity, uuid(), existing?.id, userId, attemptId)
        save(next)
        return next
    }

    @Synchronized
    override fun confirm(binding: InstallationBinding) {
        if (readBinding(binding.identity)?.id != binding.id) throw IOException("Installation binding changed")
        save(binding.copy(confirmed = true))
    }

    private fun readIdentity(): InstallationIdentity? {
        val id = preferences.getString("installation_id", null)
        val secret = preferences.getString("installation_secret", null)
        if (id == null && secret == null) return null
        return InstallationIdentity(canonicalUuid(id), canonicalUuid(secret))
    }

    private fun readBinding(identity: InstallationIdentity): InstallationBinding? {
        val id = preferences.getString("binding_id", null) ?: return null
        return InstallationBinding(
            identity = identity,
            id = canonicalUuid(id),
            previousId = preferences.getString("previous_binding_id", null)?.let(::canonicalUuid),
            userId = preferences.getInt("user_id", 0),
            attemptId = preferences.getString("attempt_id", "").orEmpty(),
            confirmed = preferences.getBoolean("confirmed", false),
        )
    }

    private fun save(binding: InstallationBinding) {
        val saved = preferences.edit()
            .putString("installation_id", binding.identity.id)
            .putString("installation_secret", binding.identity.secret)
            .putString("binding_id", binding.id)
            .putString("previous_binding_id", binding.previousId)
            .putInt("user_id", binding.userId)
            .putString("attempt_id", binding.attemptId)
            .putBoolean("confirmed", binding.confirmed)
            .commit()
        if (!saved) throw IOException("Installation state could not be persisted")
    }

    private fun canonicalUuid(value: String?): String {
        if (value == null) throw IOException("Incomplete stored installation identity")
        val parsed = try { UUID.fromString(value) } catch (error: IllegalArgumentException) {
            throw IOException("Invalid stored installation identity", error)
        }
        if (parsed.version() != 4 || parsed.toString() != value) throw IOException("Invalid stored installation identity")
        return parsed.toString()
    }

    private fun uuid() = UUID.randomUUID().toString()
}
