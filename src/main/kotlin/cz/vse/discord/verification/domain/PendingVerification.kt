package cz.vse.discord.verification.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("pending_verifications")
data class PendingVerification(
    @Id
    @Column("id")
    val id: Int = 0,

    @Column("username")
    val username: String,

    @Column("code")
    val code: String
)