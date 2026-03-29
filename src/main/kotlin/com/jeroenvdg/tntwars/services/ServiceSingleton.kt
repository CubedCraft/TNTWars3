package com.jeroenvdg.tntwars.services

import com.jeroenvdg.tntwars.TNTWars

open class ServiceSingleton<T : IService>(private val serviceType: Class<T>) {
    fun current() = TNTWars.instance.services.getService(serviceType)
}