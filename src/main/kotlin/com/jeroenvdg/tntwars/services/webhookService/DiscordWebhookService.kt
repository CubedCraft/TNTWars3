package com.jeroenvdg.tntwars.services.webhookService

import com.google.gson.JsonObject
import com.jeroenvdg.tntwars.TNTWars
import java.net.HttpURLConnection
import java.net.URI

class DiscordWebhookService : IWebhookService {
    companion object {
        val config get() = TNTWars.instance.config
    }

    override fun send(message: String) {
        val url = URI.create(config.discordConfig.tntwarsWebhookURL).toURL();
        val json = JsonObject()
        json.addProperty("content", message)

        val jsonString = json.toString()

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        connection.outputStream.use { it.write(jsonString.toByteArray()) }
    }

    override fun init() {
    }

    override fun dispose() {
    }

}