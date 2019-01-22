package com.rtu.itlab.responses.event

import com.google.gson.JsonObject
import com.rtu.itlab.database.DBClient
import com.rtu.itlab.emailsender.*
import com.rtu.itlab.responses.ResponseHandler
import com.rtu.itlab.responses.event.models.*
import com.rtu.itlab.utils.Config
import com.typesafe.config.ConfigException
import org.slf4j.LoggerFactory

/**
 * Class of sending a message to the user who was invited to the event.
 * @param eventView - Json info about event
 * @param db - Database with persons info
 */
class EventInvite(private val eventView: EventView? = null, db: DBClient? = null) : ResponseHandler(db) {
    private val logger = LoggerFactory.getLogger("com.rtu.itlab.responses.event.EventInvite")

    override fun sendEmail() {}

    fun sendEmail(invitedUsers: List<String>, event: Event) {
        val html = HtmlEmail()
        val emailNotify = event.invite().getForEmailNotice()
        if (emailNotify.contains("description"))
            html.changeDescription(emailNotify["description"]!!)
        else
            html.changeDescription("")

        if (emailNotify.contains("title"))
            html.changeTitle(emailNotify["title"]!!)
        else
            html.changeTitle("")

        if (emailNotify.contains("url"))
            html.changeUrl(emailNotify["url"]!!.removePrefix("Ссылка на событие: "))
        else when (val response = Config().checkPath("frontend.host")) {
            null -> html.changeUrl(config.getString("null"))
            else -> html.changeUrl(response)
        }

        try {
            if (html.getHtmlString().isNotBlank()) {
                sendMail(
                    UserMail(config.getString("mail.email"), config.getString("mail.password")),
                    MailMessage("RTUITLAB NOTIFICATION", html.getHtmlString()),
                    HostMail(config.getString("mail.port"), config.getString("mail.host")),
                    invitedUsers.toMutableSet()
                )
            } else {
                logger.error("Html is empty or blank. Can't send message to users!")
            }
        } catch (ex: ConfigException) {
            logger.error(ex.message + " (CONFIG)")
        }

    }

    override fun send(): JsonObject {
        return JsonObject()
    }

    fun send(invitedUsers: List<Int>, event: Event) {
        vk.messages()
            .send(actor, invitedUsers)
            .message(
                event.invite().concatenate()
            ).execute()

        logger.info("Invite messages sent to users VK")
    }
}