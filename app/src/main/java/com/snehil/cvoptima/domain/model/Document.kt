package com.snehil.cvoptima.domain.model

data class Document(
    val id: String,
    val userId: Long,
    val jobTitle: String,
    val companyName: String,
    val jdText: String,
    val resumeMd: String,
    val status: String
)
