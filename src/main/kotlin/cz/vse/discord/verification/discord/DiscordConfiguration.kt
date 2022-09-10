package cz.vse.discord.verification.discord

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "discord")
data class DiscordConfiguration(
    val token: String,
    val confirmationChannel: Long
)