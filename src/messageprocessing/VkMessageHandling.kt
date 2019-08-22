package messageprocessing

import bot.keyboard.getKeyboardForCurrentPerson
import bot.keyboard.keyboard
import com.google.gson.JsonObject
import database.HibernateUtil
import database.schema.NotificationsEntity
import emailsender.*

import org.slf4j.LoggerFactory
import utils.Config
import workwithapi.RequestsToServerApi

class VKMessageHandling : Handler() {

    private val logger = LoggerFactory.getLogger(this.javaClass.name)

    private var email: String? = null

    private var id: String? = null

    private var keyboard = "{\"buttons\":[],\"one_time\":true}"

    private val requestsToServerApi = RequestsToServerApi()

    override fun sendEmail() {
        val html = HtmlEmail()

        when (val apiUrl = Config().loadPath("apiserver.host")) {
            null -> logger.error("There is no path 'apiserver.host' in the config file")
            else -> html.run {
                changeDescription(
                    "<p>Ваш аккаунт сайта $apiUrl был привязан к системе уведомлений</p>" +
                            "<p>Управлять(отключать или включать) уведомлениями вы можете путем общения с ботом RTU IT Lab</p>"
                )
                changeUrl(apiUrl)
            }
        }

        html.changeTitle("Уведомление об успешном подключении системы уведомлений")

        if (html.getHtmlString().isNotBlank()) {

            sendMail(
                UserMail(Config().loadPath("mail.email"), Config().loadPath("mail.password")),
                MailMessage(
                    "RTUITLAB NOTIFICATION",
                    html.getHtmlString().replace("Перейти к событию", "Перейти на сайт")
                ),
                HostMail(Config().loadPath("mail.port"), Config().loadPath("mail.host")),
                setOf(email!!)
            )

        } else {
            logger.error("Html is empty or blank. Can't send message to users!")
        }
    }

    private fun sendMessage(message: String, vkId: String) {
        if (message.isNotBlank()) {
            vk.messages()
                .send(actor)
                .userId(vkId.toInt())
                .message(message)
                .keyboard(keyboard)
                .execute()

        }
    }

    private fun getKeyboardJson(vkId: String, databaseConnection: HibernateUtil): String {
        var result = ""

        val keyboardClass = getKeyboardForCurrentPerson(requestsToServerApi, vkId, databaseConnection)

        if (keyboardClass.lines.size > 0)
            result = keyboardClass.getKeyboardJson().toString()

        return if (result.isNotBlank())
            result
        else
            "{\"buttons\":[],\"one_time\":true}"
    }

    override fun process(inputJson: JsonObject, databaseConnection: HibernateUtil) {

        val vkId = inputJson.getAsJsonObject("object").get("from_id").asString
        val messageText = inputJson.getAsJsonObject("object").get("text").asString

        var userModel = requestsToServerApi.getUserModelByVkId(vkId)

        val message = if (userModel != null) {
            id = userModel.id
            email = userModel.email

            if (databaseConnection.isUserInDatabase(id!!)) {
                keyboard = getKeyboardJson(vkId, databaseConnection)

                if (messageText.startsWith("L:"))
                    "По моему вы прислали код для авторизации " +
                            "в данном сервисе. Мне не хотелось бы вас огорчать" +
                            ", но к большому сожалению придется. Возможно вы даже " +
                            "и не подозревали, что я скажу это, но я не могу вас авторизовать " +
                            "так как по моим данным вы уже были авторизованы в нашем сервисе."
                else
                    "Дорогой подписчик ! К большому для нас сожалению, и , может быть, для вас" +
                            ", но я вас не понимаю. Пожалуйста, изучите мой лексикон написав комманду \"/help\" " +
                            "и тогда в следующий раз я скорее всего смогу вас понять"

            } else {
                val res = if (messageText.startsWith("L:")) {
                    userModel = requestsToServerApi.sendTokenToServerForAccess(messageText, vkId)
                    if (userModel != null) {
                        databaseConnection.addEntity(
                            NotificationsEntity(
                                userModel.id,
                                vkNotification = true,
                                emailNotication = true
                            )
                        )
                    } else {
                        false
                    }
                } else {
                    false
                }

                if (res) {
                    keyboard = getKeyboardJson(vkId, databaseConnection)
                    "Спасибо, за авторизацию в центре уведомлений" +
                            " RTUITLAB, теперь у меня +1 человек, чтобы заваливать" +
                            " его возможно важной для него информацией"
                } else {
                    keyboard = "{\"buttons\":[],\"one_time\":true}"
                    if (messageText.startsWith("L:"))
                        "По непредвиденным для нас обстоятельствам" +
                                " во время вашей авторизации что-то пошло не" +
                                " так (Возможно не верный код авторизации), " +
                                "если вы взволнованы произошедшим, то сообщите" +
                                " кому-нибудь, кто возмжно знает решение проблемы"
                    else
                        "По непредвиденным для нас обстоятельствам" +
                                " во время вашей авторизации что-то пошло не" +
                                " так (Вы прислали не код авторизации), " +
                                "если вы взволнованы произошедшим, то сообщите" +
                                " кому-нибудь, кто возмжно знает решение проблемы"
                }
            }
        } else {
            if (messageText.startsWith("L:")) {
                userModel = requestsToServerApi.sendTokenToServerForAccess(messageText, vkId)

                if (userModel != null) {
                    id = userModel.id
                    email = userModel.email
                }

                if (id != null && email != null) {
                    val res = databaseConnection.addEntity(
                        NotificationsEntity(
                            id,
                            vkNotification = true,
                            emailNotication = true
                        )
                    )
                    if (res) {
                        keyboard = getKeyboardJson(vkId, databaseConnection)
                        "Спасибо, за авторизацию в центре уведомлений" +
                                " RTUITLAB, теперь у меня +1 человек, чтобы заваливать" +
                                " его возможно важной для него информацией"
                    } else {
                        keyboard = "{\"buttons\":[],\"one_time\":true}"
                        "По непредвиденным для нас обстоятельствам" +
                                " во время вашей авторизации что-то пошло не" +
                                " так с добавлением вас в базу данных," +
                                " если вы взволнованы произошедшим, то сообщите" +
                                " кому-нибудь, кто возмжно знает решение проблемы"
                    }
                } else {
                    keyboard = "{\"buttons\":[],\"one_time\":true}"
                    "По непредвиденным для нас обстоятельствам" +
                            " во время вашей авторизации что-то пошло не" +
                            " так, если вы взволнованы произошедшим, то сообщите" +
                            " кому-нибудь, кто возмжно знает решение проблемы"
                }
            } else {
                keyboard = "{\"buttons\":[],\"one_time\":true}"
                "Ранее вы не были авторизованы в данном сервисе, " +
                        "а я не понимаю, что мне пишут незнакомцы."
            }
        }
        sendMessage(message, vkId)
    }

}