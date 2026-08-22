plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nexomc.com/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly("com.nexomc:nexo:1.26.0")
    implementation("com.github.GriefPrevention:GriefPrevention:16.18.2")
    implementation("org.yaml:snakeyaml:2.2")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.register<JavaExec>("generateRecipeGraphics") {
    group = "tools"
    mainClass.set("com.hishacorp.dataMachines.tools.RecipeGraphicsGenerator")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
