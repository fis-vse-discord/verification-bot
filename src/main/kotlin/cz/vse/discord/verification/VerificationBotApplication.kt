package cz.vse.discord.verification

import cz.vse.discord.verification.discord.DiscordConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(DiscordConfiguration::class)
class VerificationBotApplication

fun main(args: Array<String>) {
    runApplication<VerificationBotApplication>(*args)
}
