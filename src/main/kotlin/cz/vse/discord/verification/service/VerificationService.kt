package cz.vse.discord.verification.service

import cz.vse.discord.verification.domain.PendingVerification
import cz.vse.discord.verification.repository.PendingVerificationRepository
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.mail.Message
import javax.mail.internet.InternetAddress
import kotlin.random.Random

@Service
class VerificationService(
    private val repository: PendingVerificationRepository,
    private val templates: TemplateEngine,
    private val mailer: JavaMailSender
) {

    suspend fun createVerification(username: String): PendingVerification {
        val email = "$username@vse.cz"
        val code = generateVerificationCode()

        val verification = PendingVerification(
            username = username,
            code = code
        )

        repository.deleteAllByUsername(username)
        repository.save(verification)

        sendEmailWithCode(email, code)

        return verification
    }

    @Suppress("UsePropertyAccessSyntax")
    fun sendEmailWithCode(email: String, code: String) {
        val template = "email/code"
        val context = Context().apply {
            setVariable("email", email)
            setVariable("code", code)
        }

        val html = templates.process(template, context)
        val message = mailer.createMimeMessage()

        message.setFrom(InternetAddress("noreply@vse-verification.vrba.dev", "FIS VŠE Discord"))
        message.setRecipient(Message.RecipientType.TO, InternetAddress(email))
        message.setSubject("Verifikace školního účtu VŠE")
        message.setContent(html, "text/html; charset=UTF-8")

        mailer.send(message)
    }

    private fun generateVerificationCode(): String {
        val seed = ByteBuffer.wrap(SecureRandom.getSeed(Long.SIZE_BYTES)).long
        val random = Random(seed)
        val charset = ('0'..'9') + ('a'..'z') + ('A'..'Z')

        return generateSequence { charset.random(random) }
            .take(32)
            .joinToString("")
    }
}