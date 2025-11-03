package ru.kochkaev.zixamc.rest.openAPI

data class OpenAPIConfig(
    val enabled: Boolean = true,
    val title: String = "ZixaMC Rest API",
    val description: String = "This Swagger UI provides a live, interactive view of the server API so you can explore, test, and understand available endpoints and their inputs/outputs.\n" +
            "\n" +
            "- Purpose: browse endpoints grouped by path, inspect request/response schemas, and execute requests directly from the UI.  \n" +
            "- Base path: use the API root and paths shown in the UI; requests are sent to the server and return real responses.  \n" +
            "- Authentication: use the Authorize button to set a Bearer token (Authorization: Bearer <token>). The UI will include your token in requests and refresh the OpenAPI spec based on your permissions.  \n" +
            "- Permissions: some endpoints may be hidden or restricted depending on the token’s permissions; the documentation will reflect what the token can access.  \n" +
            "- Content types: JSON bodies use application/json; file uploads use application/octet-stream with binary format; file downloads return application/octet-stream.  \n" +
            "- Notes: response examples, status codes, and default values are provided where available. Use the Try it out feature to validate request parameters and see actual responses.",
    val version: String = "1.0",
    val urlPrefix: String = "/api",
    val openApiAddress: String = "/docs/openapi",
    val hideWithoutPermsByDefault: Boolean = false,
    val swagger: SwaggerConfig = SwaggerConfig(),
)
