package com.loresuelvo.consumer.data.api.mapper

import com.loresuelvo.consumer.data.api.dto.InstallationResponseDto
import com.loresuelvo.consumer.data.api.dto.RegisterInstallationRequestDto
import com.loresuelvo.consumer.domain.installation.InstallationBinding
import com.loresuelvo.consumer.domain.installation.InstallationConfirmation

internal fun InstallationResponseDto.toDomain() = InstallationConfirmation(
    installationId = installationId,
    bindingId = bindingId,
    app = app,
    locale = locale,
    enabled = enabled,
)

internal fun InstallationBinding.toRegistrationRequest(token: String, locale: String) = RegisterInstallationRequestDto(
    installationSecret = identity.secret,
    app = "consumer",
    fcmToken = token,
    locale = locale,
    bindingId = id,
    previousBindingId = previousId,
)
