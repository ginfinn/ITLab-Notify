@file:Suppress("IMPLICIT_CAST_TO_ANY")

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.vk.api.sdk.client.VkApiClient
import database.HibernateUtil
import database.schema.NotificationsEntity
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveStream
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import messageprocessing.VKMessageHandling
import org.slf4j.LoggerFactory
import utils.Config
import java.io.InputStreamReader

@Suppress("requestHandler")
@kotlin.jvm.JvmOverloads
fun Application.module() {
    Config("resources/secureInfo.conf")

    val databaseConnection = HibernateUtil().setUpSession()

    val logger = LoggerFactory.getLogger("HttpRequestHandler")

    val vkMessageHandling = VKMessageHandling()

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        }
    }

    routing {
        get("/") { call.respond("Server is online") }

        post("/bot") {
            try {
                val inputJson = Gson().fromJson(
                    InputStreamReader(
                        call.receiveStream(),
                        "UTF-8"
                    ), JsonObject::class.java
                )

                val secretFromConfigForVkApi = Config().loadPath("group.secret")
                val secretFromConfigForBackApi = Config().loadPath("apiserver.secret")
                val secretFromRequest = inputJson.get("secret").asString

                //If request from vk api
                if (!secretFromConfigForVkApi.isNullOrBlank() &&
                    !secretFromRequest.isNullOrBlank() &&
                    secretFromConfigForVkApi == secretFromRequest
                ) {

                    when (inputJson.get("type").asString) {

                        "confirmation" -> {
                            val responseForConnectServerToVk = Config().loadPath("group.responseForConnect")
                            if (responseForConnectServerToVk != null)
                                call.respond(responseForConnectServerToVk)
                            else {
                                logger.error("group.responseForConnect is null.")
                            }
                        }

                        "message_new" -> {

                            if(!inputJson.isJsonNull)
                                vkMessageHandling.process(inputJson,databaseConnection)
                            else
                                logger.error("Json from vk api is null")

                            call.respond("OK")
                        }

                        "message_reply" -> {
                            call.respond("OK")
                        }
                    }

                } else if (!secretFromConfigForBackApi.isNullOrBlank() &&
                    !secretFromRequest.isNullOrBlank() &&
                    secretFromConfigForBackApi == secretFromRequest
                ) {


                }

            } catch (jsonSyntaxEx: JsonSyntaxException) {
                logger.error("JsonSyntaxException ${jsonSyntaxEx.message}")
                call.response.status(HttpStatusCode.BadRequest)
            }
        }
    }

}




