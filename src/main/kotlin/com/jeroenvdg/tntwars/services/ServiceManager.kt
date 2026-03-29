package com.jeroenvdg.tntwars.services

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.services.achievements.HikariPersistentAchievementService
import com.jeroenvdg.tntwars.services.achievements.IAchievementsService
import com.jeroenvdg.tntwars.services.achievements.SimpleAchievementService
import com.jeroenvdg.tntwars.services.boosterService.HikariBoosterService
import com.jeroenvdg.tntwars.services.boosterService.IBoosterService
import com.jeroenvdg.tntwars.services.boosterService.SimpleBoosterService
import com.jeroenvdg.tntwars.services.playerSettings.HikariPersistentSettingsService
import com.jeroenvdg.tntwars.services.playerSettings.IPlayerSettingsService
import com.jeroenvdg.tntwars.services.playerSettings.SimplePlayerSettingsService
import com.jeroenvdg.tntwars.services.playerStats.HikariPersistentPlayerStatsService
import com.jeroenvdg.tntwars.services.playerStats.IPlayerStatsService
import com.jeroenvdg.tntwars.services.playerStats.SimpleStatsService
import com.jeroenvdg.tntwars.services.userIdentifier.IUserIdentifierService
import com.jeroenvdg.tntwars.services.userIdentifier.SimpleIdService
import com.jeroenvdg.tntwars.services.vanishService.IPlayerVanishService
import com.jeroenvdg.tntwars.services.vanishService.SimplePlayerVanishService
import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.SQLUtils
import com.jeroenvdg.tntwars.services.webhookService.DiscordWebhookService
import com.jeroenvdg.tntwars.services.webhookService.IWebhookService
import com.zaxxer.hikari.HikariDataSource


class ServiceManager {

    private val services = HashMap<Class<*>, IService>()
    private val plugin = TNTWars.instance

    fun initServices() {

        setService(SimpleIdService(), IUserIdentifierService::class.java)
        setService(SimplePlayerVanishService(), IPlayerVanishService::class.java)
        setService(DiscordWebhookService(), IWebhookService::class.java);

        if (plugin.config.mySQLConfig.enabled) {
            val mySQLConfig = plugin.config.mySQLConfig
            SQLUtils.createDatabaseIfMissing(mySQLConfig.host, mySQLConfig.port, mySQLConfig.username, mySQLConfig.password)
            val hikari = HikariDataSource()
            hikari.dataSourceClassName = "com.mysql.cj.jdbc.MysqlDataSource"
            hikari.addDataSourceProperty("serverName", mySQLConfig.host)
            hikari.addDataSourceProperty("port", mySQLConfig.port)
            hikari.addDataSourceProperty("databaseName", mySQLConfig.database)
            hikari.addDataSourceProperty("user", mySQLConfig.username)
            hikari.addDataSourceProperty("password", mySQLConfig.password)
            hikari.addDataSourceProperty("allowPublicKeyRetrieval", true)
            hikari.addDataSourceProperty("useSSL", false)

            hikari.poolName = "TNTWars"
            hikari.maximumPoolSize = 6
            hikari.minimumIdle = 2
            hikari.connectionTimeout = 2000
            hikari.leakDetectionThreshold = 4000

            setService(HikariPersistentSettingsService(hikari), IPlayerSettingsService::class.java)
            setService(HikariPersistentPlayerStatsService(hikari), IPlayerStatsService::class.java)
            setService(HikariPersistentAchievementService(hikari, plugin.config.serverId), IAchievementsService::class.java)
            setService(HikariBoosterService(hikari, "tntwars"), IBoosterService::class.java)
        } else {
            setService(SimplePlayerSettingsService(), IPlayerSettingsService::class.java)
            setService(SimpleStatsService(), IPlayerStatsService::class.java)
            setService(SimpleAchievementService(), IAchievementsService::class.java)
            setService(SimpleBoosterService(), IBoosterService::class.java)
        }
    }

    fun <T : IService> hasService(serviceType: Class<T>): Boolean {
        return services.containsKey(serviceType)
    }

    fun <T : IService> setService(service: T, serviceType: Class<T>) {
        if (services.containsKey(serviceType)) {
            Debug.warn("Overriding service ${serviceType.name}")
            disposeService(serviceType)
        }
        service.init()
        services[serviceType] = service
    }

    fun <T : IService> getService(serviceType: Class<T>) : T {
        val service = services[serviceType] ?: throw Exception("Could not find service ${serviceType.name}")
        return service as T
    }

    fun <T> getServiceOptional(serviceType: Class<T>): T? {
        val service = services[serviceType]
        return service as T?
    }

    fun dispose() {
        for (key in services.keys.toList()) {
            disposeService(key)
        }
    }

    private fun disposeService(serviceType: Class<*>) {
        val service = services[serviceType] ?: return
        service.dispose()
    }
}