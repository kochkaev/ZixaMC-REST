package ru.kochkaev.zixamc.rest.openAPI

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import ru.kochkaev.zixamc.rest.SQLClient
import java.util.UUID

object SwaggerUI {
    fun route(app: Application) {
        app.routing {
            get("/api/docs/openapi") {
                val tokenRaw = call.request.header("Authorization")?.replace("Bearer ", "")
                val token = try {
                    UUID.fromString(tokenRaw)
                } catch (_: Exception) { null }
                call.respondText(
                    text = OpenAPIGenerator.json(token),
                    contentType = ContentType.Application.Json
                )
            }
            get("/api/docs/myPerms") {
                val tokenRaw = call.request.header("Authorization")?.replace("Bearer ", "")
                val token = try {
                    UUID.fromString(tokenRaw)
                } catch (_: Exception) { null }
                val sql = token?.let { SQLClient.get(it) }
                if (sql != null) {
                    call.respond(mapOf("permissions" to (sql.permissions.get() ?: listOf())))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                }
            }
            get("/api/docs") {
                val html = $$"""
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
                                window.onload = async () => {
                                    let token = '';

                                    const ui = SwaggerUIBundle({
                                        url: '/api/docs/openapi',
                                        dom_id: '#swagger-ui',
                                        deepLinking: true,
                                        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                                        plugins: [SwaggerUIBundle.plugins.DownloadUrl],
                                        layout: "StandaloneLayout",
                                        requestInterceptor: req => {
                                            if (token) {
                                                req.headers['Authorization'] = `Bearer ${token}`;
                                            }
                                            return req;
                                        },
                                        onComplete: () => {
                                            const system = ui.getSystem();
                                            setInterval(() => {
                                                const auth = system.authSelectors.authorized().toJS();
                                                const bearer = auth.bearerAuth;
                                                if (bearer) {
                                                    if (bearer.value && bearer.value.trim() !== token) {
                                                        token = bearer.value.trim();
                                                        updateSpec();
                                                    } 
                                                } else if (token !== '') {
                                                    token = '';
                                                    updateSpec()
                                                }
                                            }, 1000);
                                        }
                                    });
                                    
                                    async function updateSpec() {
                                        try {
                                            const specResp = await fetch('/api/docs/openapi', {
                                                headers: { 'Authorization': `Bearer ${token}` }
                                            });
                                            if (specResp.ok) {
                                                const newSpecJson = await specResp.json();
                                                ui.specActions.updateJsonSpec(newSpecJson);
                                            }
                                        } catch (e) {
                                            console.error('Failed to update spec', e);
                                        }
                                    }
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