package com.example.smartvocab.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object EmailSender {
    private const val SMTP_HOST = "smtp.gmail.com"
    private const val SMTP_PORT = "587"
    private const val SENDER_EMAIL = "ngocmanhp667@gmail.com"
    private const val SENDER_PASSWORD = "tqehwnbtbfvysgls"

    suspend fun sendOtpEmail(recipientEmail: String, otp: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", SMTP_HOST)
                    put("mail.smtp.port", SMTP_PORT)
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.ssl.trust", SMTP_HOST)
                }
                val session = javax.mail.Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() = PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                })
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, "SmartVocab"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                    subject = "SmartVocab - Mã xác thực OTP"
                    setContent(
                        """<html><body style='font-family:Arial;'>
                        <h2 style='color:#3525CD;'>SmartVocab</h2>
                        <p>Xin chào,</p>
                        <p>Mã xác thực OTP của bạn là:</p>
                        <h1 style='color:#3525CD; letter-spacing:8px; font-size:36px;'>$otp</h1>
                        <p>Mã này có hiệu lực trong <b>5 phút</b>.</p>
                        <p>Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này.</p>
                        <hr/>
                        <p style='color:gray; font-size:12px;'>SmartVocab - Học thông minh hơn, nhớ lâu hơn.</p>
                        </body></html>""".trimIndent(),
                        "text/html; charset=utf-8"
                    )
                }
                Transport.send(message)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
