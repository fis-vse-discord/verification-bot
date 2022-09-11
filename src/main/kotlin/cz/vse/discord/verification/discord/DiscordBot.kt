package cz.vse.discord.verification.discord

import cz.vse.discord.verification.service.VerificationService
import dev.kord.common.Color
import dev.kord.common.entity.*
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.GuildChatInputCommandInteraction
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.channel
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
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
                "complete-verification" -> displayCodeModal(interaction)
                else -> completeMemberVerification(interaction)
            }
        }

        client.on<ModalSubmitInteractionCreateEvent> {
            when (interaction.modalId) {
                "username-prompt" -> handleUsernameModal(interaction, interaction.textInputs["username"]?.value ?: "")
                "code-prompt" -> handleCodeModal(interaction, interaction.textInputs["code"]?.value ?: "")

                else -> logger.error("Unknown modal interaction [${interaction.modalId}]")
            }
        }
    }

    private suspend fun handleUsernameModal(interaction: ModalSubmitInteraction, username: String) {
        val deferred = interaction.deferEphemeralResponse()

        if (!username.matches("[a-z]+[a-z0-9]+".toRegex())) {
            deferred.respond {
                embed {
                    color = Color(0xED4245)
                    title = "Neplatný formát školního emailu"
                    description = "Zadej username ve formátu, kterým se přihlašuješ do insisu, tedy např. `user00`"
                }
            }

            return
        }

        val verification = service.createVerification(username)

        deferred.respond {
            embed {
                color = Color(0x57F287)
                title = "Na školní email ti byl zaslán email s ověřovacím kódem"
                description = """
                    Často se stává, že tento email skončí ve spamu, protože nepochází z domény vse.cz
                    Po obržení kódu klikni na tlačítko **Dokončení verifikaci**, kde můžeš tento kód zadat.
                """.trimIndent()
                footer {
                    text = "${verification.username}@vse.cz"
                }
            }
        }
    }

    private suspend fun handleCodeModal(interaction: ModalSubmitInteraction, code: String) {
        val deferred = interaction.deferEphemeralResponse()
        val username = service.completeVerification(code)

        if (username == null) {
            deferred.respond {
                embed {
                    color = Color(0xED4245)
                    title = "Zadaný kód není platný"
                    description = "Pokud problém přetrvá, zkus si nechat zaslat nový kód, nebo napiš někomu z administrátorů"
                }
            }

            return
        }

        createConfirmationMessage(username, interaction.kord, interaction.user)

        deferred.respond {
            embed {
                color = Color(0x57F287)
                title = "Tvůj VŠE účet byl ověřen"
                description = "Během chvíle ti bude přidělena role, která odemyká plný přístup na server"
            }
        }
    }

    private suspend fun createConfirmationMessage(username: String, client: Kord, user: User) {
        val channel = client.getChannelOf<TextChannel>(Snowflake(configuration.confirmationChannel))

        channel?.createMessage {
            embed {
                title = "Potvrzení verifikace"
                description = "Pro potvrzení a přidělení role uživateli je potřeba potvrdit verifikaci"

                field("Member", false) { user.mention }
                field("VŠE účet", false) { username }
            }

            actionRow {
                interactionButton(ButtonStyle.Success, "confirm:${user.id}") {
                    label = "Legit"
                }
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
                    title = "Tento příkaz je dostupný pouze pro správce serveru"
                }
            }

            return
        }

        channel.createMessage {
            embed {
                color = Color(0x009ee0)
                title = "Verifikace školního účtu VŠE pro zpřístupnění serveru"
                description = """
                    |Pro zahájení verifikace klikni na tlačítko **Ověření školního účtu** pod touto zprávou.
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
                interactionButton(ButtonStyle.Primary, "verification") { label = "\uD83D\uDD11 Ověření školního účtu" }
                interactionButton(ButtonStyle.Secondary, "complete-verification") { label = "Dokončení verifikace" }
            }
        }

        response.respond {
            embed {
                color = Color(0x57F287)
                title = "Verification message created"
            }
        }
    }

    private suspend fun displayVerificationModal(interaction: ButtonInteraction) {
        interaction.modal("Ověření školního účtu VŠE", "username-prompt") {
            actionRow {
                textInput(TextInputStyle.Short, "username", "Školní username") {
                    allowedLength = 6..6
                    placeholder = "Např. user09"
                    required = true
                }
            }
        }
    }

    private suspend fun displayCodeModal(interaction: ButtonInteraction) {
        interaction.modal("Dokončení ověření školního účtu VŠE", "code-prompt") {
            actionRow {
                textInput(TextInputStyle.Short, "code", "Zaslaný verifikační kód") {
                    allowedLength = 32..32
                    placeholder = "Např. lpUYpbobZlz81EqFB6ljLPPG6MJbg5RH"
                    required = true
                }
            }
        }
    }

    private suspend fun completeMemberVerification(interaction: ButtonInteraction) {
        val deferred = interaction.deferEphemeralResponse()
        val original = interaction.message

        val (_, id) = interaction.componentId.split(":")

        val guild = interaction.kord.getGuild(Snowflake(configuration.guild)) ?: return
        val member = guild.getMember(Snowflake(id)).asMember()


        member.addRole(Snowflake(configuration.verifiedRole))

        original.edit {
            actionRow {
                interactionButton(ButtonStyle.Success, "confirmed") {
                    label = "Potvrzeno od " + interaction.user.username
                    disabled = true
                }
            }
        }

        deferred.respond {
            embed {
                color = Color(0x57F287)
                title = "Role byla přidělena"
            }
        }
    }
}