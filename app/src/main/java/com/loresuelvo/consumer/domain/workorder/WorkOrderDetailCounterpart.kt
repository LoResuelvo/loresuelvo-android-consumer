package com.loresuelvo.consumer.domain.workorder

data class WorkOrderDetailCounterpart(
    val id: String,
    val name: String,
    val surname: String,
    val categoryName: String,
    val profilePhotoUrl: String?,
    val identityVerified: Boolean = false,
)
