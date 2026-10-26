package com.example.model

import java.util.UUID

data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Shooter",
    val isDefault: Boolean = false,
    val nodes: List<MappingNode> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
