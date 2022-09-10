package cz.vse.discord.verification.discord

import cz.vse.discord.verification.service.VerificationService
import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service

@Service
class DiscordBot(
    private val configuration: DiscordConfiguration,
    private val service: VerificationService
) : CommandLineRunner {

    private val logger: Logger = LoggerFactory.getLogger(this::class.qualifiedName)

    override fun run(vararg args: String?): Unit = runBlocking {
        val client = Kord(configuration.token)

        registerSlashCommandListener(client)
        registerInteractionsListener(client)

        client.on<ReadyEvent> {
            client.editPresence { watching("VŠE students getting verified") }
            client.createGlobalChatInputCommand("create-verification-message", "Create verification message") {
                channel("channel", "A text channel to which the verification message should be sent") {
                    channelTypes = listOf(ChannelType.GuildText)
                    required = true
                }
            }
        }

        client.login()
    }

    private suspend fun registerSlashCommandListener(client: Kord) {
        client.on<GuildChatInputCommandInteractionCreateEvent> {
            when (interaction.invokedCommandName) {
                "create-verification-message" -> createVerificationMessage(interaction)
                else -> logger.error("Unknown slash command [/${interaction.invokedCommandName}]")
            }
        }
    }

    private suspend fun registerInteractionsListener(client: Kord) {
        client.on<ButtonInteractionCreateEvent> {
            when (interaction.componentId) {
                "verification" -> displayVerificationModal(interaction)
                else -> logger.error("Unknown button interaction [${interaction.componentId}]")
            }
        }
    }

    private suspend fun createVerificationMessage(interaction: GuildChatInputCommandInteraction) {
        val channel = interaction.command.channels["channel"]?.asChannelOf<TextChannel>() ?: return
        val member = interaction.user.asMember()
        val permissions = member.getPermissions()

        val response = interaction.deferEphemeralResponse()

        if (!permissions.contains(Permission.ManageGuild)) {
            response.respond {
                embed {
                    color = Color(0xED4245)
                    title = "This command is available to admins only"
                    description = "For more info check 1984 by George Orwell"
                }
            }

            return
        }

        channel.createMessage {
            embed {
                color = Color(0x009ee0)
                title = "Verifikace školního účtu VŠE pro zpřístupnění serveru"
                description = """
                    |Pro zahájení verifikace klikni na tlačítko pod touto zprávou.
                    |
                    |Po zadání školního emailu ti bude zaslán email s ověřovacím kódem
                    |
                    |Po úspěšné verifikaci ti pak bude přidělena role <@&799326721530003546>, která odemyká přístup na server.
                    |
                    |V případě jakýchkoliv problémů, můžeš napsat komukoliv z adminů (ideálně <@238728915647070209>).""".trimMargin()
                thumbnail {
                    url = "https://i.imgur.com/XrqdhmR.png"
                }
            }

            actionRow {
                interactionButton(ButtonStyle.Primary, "verification") {
                    label = "\uD83D\uDD11 Ověření školního účtu"
                }
            }
        }

        response.respond {
            embed {
                color = Color(0x49a72c)
                title = "Verification message created"
            }
        }
    }

    private suspend fun displayVerificationModal(interaction: ButtonInteraction) {
        interaction.modal("Ověření školního účtu VŠE", "verification-prompt") {
            actionRow {
                textInput(TextInputStyle.Short, "username", "Školní username") {
                    allowedLength = 6..6
                    placeholder = "Např. user09"
                    required = true
                }
            }
        }
    }
}