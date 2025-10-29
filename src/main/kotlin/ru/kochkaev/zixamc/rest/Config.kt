package ru.kochkaev.zixamc.rest

import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.api.config.ConfigFile
import java.io.File

data class Config(
    val port: Int = 42548,
    val sqlClientsTableName: String = "rest_clients",
) {
    companion object: ConfigFile<Config>(
        file = File(FabricLoader.getInstance().configDir.toFile(), "ZixaMC-REST.json"),
        model = Config::class.java,
        supplier = ::Config
    )
}