package cz.vse.discord.verification.repository

import cz.vse.discord.verification.domain.PendingVerification
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingVerificationRepository : CoroutineCrudRepository<PendingVerification, String> {

    suspend fun deleteAllByUsername(username: String)

    suspend fun findByCode(code: String): PendingVerification?

}