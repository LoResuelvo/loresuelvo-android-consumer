package com.loresuelvo.consumer.data.api

import com.loresuelvo.consumer.data.api.dto.InstallationResponseDto
import com.loresuelvo.consumer.data.api.mapper.toDomain
import com.loresuelvo.consumer.data.api.mapper.toRegistrationRequest
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.installation.InstallationRegistrationResult
import com.loresuelvo.consumer.domain.installation.InstallationRepository
import com.loresuelvo.consumer.domain.installation.RegistrationOutcome
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named
import kotlinx.serialization.SerializationException
import retrofit2.Response

@Singleton
class ApiInstallationRepository @Inject constructor(
    @Named("installationApi") private val api: BackendApi,
) : InstallationRepository {
    override suspend fun register(
        binding: InstallationBinding,
        token: String,
        locale: String,
        accessToken: String,
    ): InstallationRegistrationResult = try {
        val response = api.registerInstallation(
            binding.identity.id,
            "Bearer $accessToken",
            binding.toRegistrationRequest(token, locale),
        )
        response.toRegistrationResult()
    } catch (error: IOException) {
        InstallationRegistrationResult.Failed(RegistrationOutcome.NetworkFailure(error))
    } catch (error: SerializationException) {
        InstallationRegistrationResult.Failed(RegistrationOutcome.InvalidConfirmation)
    }

    private fun Response<InstallationResponseDto>.toRegistrationResult(): InstallationRegistrationResult {
        if (!isSuccessful) return InstallationRegistrationResult.Failed(httpFailure(code()))
        if (code() != 200 && code() != 201) {
            return InstallationRegistrationResult.Failed(RegistrationOutcome.InvalidConfirmation)
        }
        val confirmation = body()?.toDomain()
            ?: return InstallationRegistrationResult.Failed(RegistrationOutcome.InvalidConfirmation)
        return InstallationRegistrationResult.Confirmed(confirmation)
    }

    private fun httpFailure(statusCode: Int): RegistrationOutcome = when (statusCode) {
        400 -> RegistrationOutcome.BadRequest
        401 -> RegistrationOutcome.Unauthorized
        403 -> RegistrationOutcome.Forbidden
        409 -> RegistrationOutcome.Conflict
        else -> RegistrationOutcome.ServerFailure(statusCode)
    }
}
