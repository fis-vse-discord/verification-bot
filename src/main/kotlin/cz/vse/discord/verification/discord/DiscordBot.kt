package cz.vse.discord.verification.discord

import cz.vse.discord.verification.service.VerificationService
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

@Service
class DiscordBot(
    private val configuration: DiscordConfiguration,
    private val service: VerificationService
) : CommandLineRunner {

    override fun run(vararg args: String?): Unit = runBlocking {
        val client = Kord(configuration.token)

        client.on<ReadyEvent> {
            client.editPresence {
                watching("VŠE students getting verified")
            }
        }

        client.login()
    }

}