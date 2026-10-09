package com.loresuelvo.consumer.data.installation

import android.content.SharedPreferences
import com.loresuelvo.consumer.domain.notifications.NotificationInstallation
import com.loresuelvo.consumer.domain.notifications.NotificationInstallationReader
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.installation.InstallationIdentity
import com.loresuelvo.consumer.domain.installation.PendingInstallationRemoval
import com.loresuelvo.consumer.domain.installation.InstallationStateStore
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named

@Singleton
class EncryptedInstallationStateStore @Inject constructor(
    @Named("installationPrefs") private val preferences: SharedPreferences,
) : InstallationStateStore, NotificationInstallationReader {
    override fun prepare(userId: Int, attemptId: String): InstallationBinding =
        prepare(userId, attemptId, newAuthentication = false)

    @Synchronized
    override fun prepare(userId: Int, attemptId: String, newAuthentication: Boolean): InstallationBinding {
        val identity = readIdentity() ?: InstallationIdentity(uuid(), uuid())
        val existing = readBinding(identity)
        if (existing?.userId == userId && !newAuthentication) return existing
        val next = InstallationBinding(identity, uuid(), existing?.id, userId, attemptId)
        save(next)
        return next
    }

    @Synchronized
    override fun confirm(binding: InstallationBinding) {
        if (readBinding(binding.identity)?.id != binding.id) throw IOException("Installation binding changed")
        val supersededRemoval = binding.previousId?.takeIf { id -> readRemovals().any { it.bindingId == id } }
        save(binding.copy(confirmed = true), removePendingBindingId = supersededRemoval)
    }

    @Synchronized
    override fun beginRemoval(userId: Int): PendingInstallationRemoval? {
        val existingRemovals = readRemovals()
        val identity = readIdentity() ?: return existingRemovals.firstOrNull { it.userId == userId }
        val binding = readBinding(identity) ?: return existingRemovals.firstOrNull { it.userId == userId }
        if (binding.userId != userId) return existingRemovals.firstOrNull { it.userId == userId }
        val removal = PendingInstallationRemoval(identity, binding.id, userId)
        if (existingRemovals.any { it.bindingId == removal.bindingId }) return removal
        val updated = existingRemovals + removal
        val editor = preferences.edit().putBoolean("confirmed", false)
        writeRemovals(updated, editor)
        val saved = editor.commit()
        if (!saved) throw IOException("Installation removal could not be persisted")
        return removal
    }

    @Synchronized
    override fun pendingRemoval(): PendingInstallationRemoval? = readRemovals().firstOrNull()

    @Synchronized
    override fun pendingRemovals(): List<PendingInstallationRemoval> = readRemovals()

    @Synchronized
    override fun completeRemoval(bindingId: String): Boolean {
        val removals = readRemovals()
        if (removals.none { it.bindingId == bindingId }) return false
        val editor = preferences.edit()
        writeRemovals(removals.filterNot { it.bindingId == bindingId }, editor)
        val current = readIdentity()?.let(::readBinding)
        if (current?.id == bindingId) removeBinding(editor)
        if (!editor.commit()) throw IOException("Installation removal could not be completed")
        return true
    }

    @Synchronized
    override fun confirmedInstallation(): NotificationInstallation? {
        val identity = readIdentity() ?: return null
        val binding = readBinding(identity)?.takeIf { it.confirmed } ?: return null
        return NotificationInstallation(identity.id, binding.id, binding.userId)
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

    private fun save(binding: InstallationBinding, removePendingBindingId: String? = null) {
        val editor = preferences.edit()
            .putString("installation_id", binding.identity.id)
            .putString("installation_secret", binding.identity.secret)
            .putString("binding_id", binding.id)
            .putString("previous_binding_id", binding.previousId)
            .putInt("user_id", binding.userId)
            .putString("attempt_id", binding.attemptId)
            .putBoolean("confirmed", binding.confirmed)
        if (removePendingBindingId != null) {
            val removals = readRemovals().filterNot { it.bindingId == removePendingBindingId }
            writeRemovals(removals, editor)
        }
        if (!editor.commit()) throw IOException("Installation state could not be persisted")
    }

    private fun readRemovals(): List<PendingInstallationRemoval> {
        val count = preferences.getInt("pending_removal_count", 0)
        if (count !in 0..128) throw IOException("Invalid stored installation removals")
        return (0 until count).map { index ->
            val prefix = "pending_removal_${index}_"
            val userId = preferences.getInt("${prefix}user_id", 0)
            if (userId <= 0) throw IOException("Invalid stored installation removal")
            PendingInstallationRemoval(
                identity = InstallationIdentity(
                    canonicalUuid(preferences.getString("${prefix}installation_id", null)),
                    canonicalUuid(preferences.getString("${prefix}installation_secret", null)),
                ),
                bindingId = canonicalUuid(preferences.getString("${prefix}binding_id", null)),
                userId = userId,
            )
        }
    }

    private fun writeRemovals(removals: List<PendingInstallationRemoval>, editor: SharedPreferences.Editor) {
        if (removals.size > 128) throw IOException("Too many pending installation removals")
        val count = preferences.getInt("pending_removal_count", 0).coerceIn(0, 128)
        repeat(count) { index ->
            val prefix = "pending_removal_${index}_"
            editor.remove("${prefix}installation_id")
                .remove("${prefix}installation_secret")
                .remove("${prefix}binding_id")
                .remove("${prefix}user_id")
        }
        editor.putInt("pending_removal_count", removals.size)
        removals.forEachIndexed { index, removal ->
            val prefix = "pending_removal_${index}_"
            editor.putString("${prefix}installation_id", removal.identity.id)
                .putString("${prefix}installation_secret", removal.identity.secret)
                .putString("${prefix}binding_id", removal.bindingId)
                .putInt("${prefix}user_id", removal.userId)
        }
    }

    private fun removeBinding(editor: SharedPreferences.Editor) {
        editor.remove("binding_id")
            .remove("previous_binding_id")
            .remove("user_id")
            .remove("attempt_id")
            .remove("confirmed")
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
