import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.0"
    id("fabric-loom") version "1.10.1"
    id("maven-publish")
//    id("com.github.johnrengelman.shadow") version "8.1.1"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

val ktorVersion = project.property("ktor_version") as String
val nettyVersion = project.property("netty_version") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("zixamc-rust") {
            sourceSet("main")
            sourceSet("client")
        }
    }
}


repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
    mavenLocal()
    maven("https://maven.kotlinlang.org")
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modApi("ru.kochkaev:zixamc.api:${project.property("zixamc_api_version")}")

    // Ktor
    val ktorModules = listOf(
        "ktor-client-core-jvm",
        "ktor-server-core-jvm",
        "ktor-server-host-common-jvm",
        "ktor-server-netty-jvm",
        "ktor-server-auth-jvm",
        "ktor-server-sessions-jvm",
        "ktor-server-content-negotiation-jvm",
        "ktor-serialization-gson-jvm",
        "ktor-serialization-jvm",
        "ktor-websocket-serialization-jvm",
        "ktor-websockets-jvm",
        "ktor-events-jvm",
        "ktor-http-cio-jvm",
        "ktor-http-jvm",
        "ktor-io-jvm",
        "ktor-network-jvm",
        "ktor-utils-jvm",
    )
    ktorModules.forEach { module ->
        include(api("io.ktor:$module:$ktorVersion")!!)
    }
    // Netty
    val nettyModules = listOf(
        "netty-buffer",
        "netty-codec",
        "netty-codec-http2",
        "netty-codec-http",
        "netty-common",
        "netty-handler",
        "netty-resolver",
        "netty-transport",
        "netty-transport-classes-epoll",
        "netty-transport-classes-kqueue",
        "netty-transport-native-kqueue",
        "netty-transport-native-unix-common",
    )
    nettyModules.forEach { module ->
        include(api("io.netty:$module:$nettyVersion")!!)
    }
    // OkHttp
    api("com.squareup.okhttp3:okhttp:${project.property("okhttp_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version"),
            "kotlin_loader_version" to project.property("kotlin_loader_version")
        )
    }
}

//tasks.shadowJar {
//    setProperty("zip64", true)
//
//    from(sourceSets.main.get().output)
//
//    configurations = listOf(project.configurations.runtimeClasspath.get())
//    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//
//    relocate("io.ktor", "zixamc.rest.shaded.ktor")
//    // relocate("com.google.gson", "zixamc.rest.shaded.gson")
//    // relocate("kotlinx.coroutines", "zixamc.rest.shaded.coroutines")
//    // relocate("io.netty", "zixamc.rest.shaded.netty")
//
//    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
//    exclude("net/fabricmc/language/kotlin/**")
//    exclude("META-INF/services/net.fabricmc.loader.api.LanguageAdapter")
//
//    archiveClassifier.set("")
//}

//tasks.jar {
//    enabled = false
//}

//tasks.remapJar {
//    inputFile.set(tasks.shadowJar.get().archiveFile)
//    dependsOn(tasks.shadowJar)
//}

//tasks.build {
//    dependsOn(tasks.remapJar)
//}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
