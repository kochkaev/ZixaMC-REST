package ru.kochkaev.zixamc.rest.openAPI

data class SwaggerConfig(
    val enabled: Boolean = true,
    val tabIcon: String = "./server-icon.png",
    val tabTitle: String = "ZixaMC API | Swagger",
    val address: String = "/docs",
)
