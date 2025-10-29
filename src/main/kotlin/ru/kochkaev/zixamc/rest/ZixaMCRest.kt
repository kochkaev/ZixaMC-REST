package ru.kochkaev.zixamc.rest

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.api.ZixaMC
import ru.kochkaev.zixamc.rest.std.DownloadFile
import ru.kochkaev.zixamc.rest.std.GetMe
import ru.kochkaev.zixamc.rest.std.UploadFile
import ru.kochkaev.zixamc.rest.std.user.GetUser

class ZixaMCRest : ModInitializer {

    override fun onInitialize() {
//        val client = RestClient("a87ff17c-b441-11f0-abdb-0242ac120002")
//        val me = client.send(GetMe)
//        ZixaMC.logger.info("ZixaMC REST Client initialized. Me: $me")
//        ServerLifecycleEvents.SERVER_STARTED.register { _ ->
//            val user = client.send(GetUser, GetUser.Request(1381684202))
//            ZixaMC.logger.info("Fetched user via REST: $user")
//            ZixaMC.logger.info("User data: ${user.data?.entries}")
//            client.send(UploadFile, FabricLoader.getInstance().gameDir.resolve("./ZixaMC-Rest-Uploads/kleverdi.png").toFile(), mapOf("filePath" to "./fabrictailor_uploads/uploaded.png"))
//        }
    }
}
