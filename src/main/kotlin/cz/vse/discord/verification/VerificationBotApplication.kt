package cz.vse.discord.verification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class VerificationBotApplication

fun main(args: Array<String>) {
    runApplication<VerificationBotApplication>(*args)
}
