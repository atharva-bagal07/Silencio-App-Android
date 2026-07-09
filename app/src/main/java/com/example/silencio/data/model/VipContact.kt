package com.example.silencio.data.model

data class VipContact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val avatarUri: String? = null
)