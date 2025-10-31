package ru.kochkaev.zixamc.rest.openAPI

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

object SwaggerUI {
    fun route(app: Application) {
        app.routing {
            get("/api/docs/openapi") {
                call.respondText(
                    text = OpenAPIGenerator.json,
                    contentType = ContentType.Application.Json
                )
            }
            get("/api/docs") {
                val html = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <title>ZixaMC API Docs</title>
                            <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
                            <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js" charset="UTF-8"></script>
                            <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js" charset="UTF-8"></script>
                            <style>
                                html { box-sizing: border-box; overflow: -moz-scrollbars-horizontal; overflow-y: scroll; }
                                *, *:before, *:after { box-sizing: inherit; }
                                body { margin:0; background: #fafafa; }
                            </style>
                        </head>
                        <body>
                            <div id="swagger-ui"></div>
                            <script>
                                window.onload = function() {
                                    const ui = SwaggerUIBundle({
                                        url: "/api/docs/openapi",
                                        dom_id: '#swagger-ui',
                                        deepLinking: true,
                                        presets: [
                                            SwaggerUIBundle.presets.apis,
                                            SwaggerUIStandalonePreset
                                        ],
                                        plugins: [SwaggerUIBundle.plugins.DownloadUrl],
                                        layout: "StandaloneLayout"
                                    });
                                    window.ui = ui;
                                };
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                call.respondText(html, ContentType.Text.Html)
            }
        }
    }
}