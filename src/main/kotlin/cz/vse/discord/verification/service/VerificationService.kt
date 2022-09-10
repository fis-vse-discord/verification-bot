package cz.vse.discord.verification.service

import cz.vse.discord.verification.repository.PendingVerificationRepository
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine

@Service
class VerificationService(
    private val repository: PendingVerificationRepository,
    private val templates: TemplateEngine,
    private val mailer: JavaMailSender
) {

}