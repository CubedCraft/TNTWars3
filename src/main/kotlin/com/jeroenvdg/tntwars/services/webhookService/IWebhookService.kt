package com.jeroenvdg.tntwars.services.webhookService

import com.jeroenvdg.tntwars.services.IService
import com.jeroenvdg.tntwars.services.ServiceSingleton

interface IWebhookService : IService {
    companion object : ServiceSingleton<IWebhookService>(IWebhookService::class.java)

    fun send(message: String)
}