package com.example.tiketin.model

data class UserModel(
    val user: User,
    val token: String
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
)
