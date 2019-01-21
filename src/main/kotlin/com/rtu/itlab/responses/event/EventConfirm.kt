package com.rtu.itlab.responses.event

import com.google.gson.JsonObject
import com.rtu.itlab.database.DBClient
import com.rtu.itlab.database.DBUser
import com.rtu.itlab.emailsender.*
import com.rtu.itlab.responses.ResponseHandler
import com.rtu.itlab.responses.event.models.EventView
import org.slf4j.LoggerFactory

class EventConfirm(eventView: EventView, db: DBClient, val id: String) : ResponseHandler(db) {
    override fun sendEmail(){}

    private fun sendEmail(user: DBUser) {
        val html = HtmlEmail()
        val emailNotify = notify.getForEmailNotice()

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
        else
            html.changeUrl(config.getString("frontend.host"))

        sendMail(
            UserMail(config.getString("mail.email"), config.getString("mail.password")),
            MailMessage("RTUITLAB NOTIFICATION", html.getHtmlString()),
            HostMail(config.getString("mail.port"), config.getString("mail.host")),
            setOf(user.email!!)
        )

        logger.info("Info messages about event confirm sent to user ${user.id} Email ${user.emailNotice}")
    }

    private val logger = LoggerFactory.getLogger("com.rtu.itlab.responses.event.EventConfirm")

    private val notify = NotifyMessages().event().eventConfirm(eventView.title).addUrl(eventView)

    override fun send(): JsonObject {
        val userInfo = db!!.getDbUserByKey(id)

        if (userInfo != null) {

            sendEmail(userInfo)

            logger.info("User vkId: ${userInfo.vkId}")
            if (userInfo.vkNotice) {
                vk.messages()
                    .send(actor, userInfo.vkId!!.toInt())
                    .message(
                        notify.concatenate()
                    ).execute()
            }

            resultJson.addProperty("statusCode", 1)
            logger.info("Info messages about event confirm sent to user ${userInfo.id} VK ${userInfo.vkNotice}")
        } else {
            resultJson.addProperty("statusCode", 30)
            logger.error("Can't send messages to user, can't find him in db!")
        }
        return resultJson
    }

}