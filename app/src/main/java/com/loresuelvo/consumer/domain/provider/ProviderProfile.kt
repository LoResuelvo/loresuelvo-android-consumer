package com.loresuelvo.consumer.domain.provider

data class ProviderProfile(
    val id: Int,
    val name: String,
    val surname: String,
    val profilePhotoUrl: String?,
    val category: ProviderCategory,
    val ratingAverage: Double,
    val ratingCount: Int,
    val identityVerified: Boolean,
    val workOrders: List<ProviderWorkOrder>,
)
