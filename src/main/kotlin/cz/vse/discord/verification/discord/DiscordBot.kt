package cz.vse.discord.verification.discord

import cz.vse.discord.verification.service.VerificationService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

@Service
class DiscordBot(private val service: VerificationService) : CommandLineRunner {

    override fun run(vararg args: String?): Unit = runBlocking {
    }

}