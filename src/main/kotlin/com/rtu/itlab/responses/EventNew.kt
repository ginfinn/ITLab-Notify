package com.rtu.itlab.responses

import com.google.gson.JsonObject
import com.vk.api.sdk.client.actors.GroupActor

class EventNew(tmp: JsonObject?): ResponseHandler() {
    private val userId = tmp?.get("to")?.asInt
    private val eventTitle: String? = tmp?.get("eventTitle")?.asString
    private val address: String? = tmp?.get("address")?.asString
    private val actor = GroupActor(properties.getProperty("group.id").toInt(), properties.getProperty("group.accessToken"))

    override fun send() {
        vk.messages()
                .send(actor)
                .userId(userId)
                .message("Было создано новое событие!\n$eventTitle\nАдрес: $address")
                .execute()
    }
}