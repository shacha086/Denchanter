val javaVersion = 17
val kspigotVersion = "1.19.0"
plugins {
    kotlin("jvm") version "1.7.10"
    id("io.papermc.paperweight.userdev") version "1.3.8"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "com.shacha"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // PaperMC Dependency
    paperDevBundle("1.19-R0.1-SNAPSHOT")

    // KSpigot dependency
    implementation("net.axay", "kspigot", kspigotVersion)
}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf(
                "-Xjdk-release=$javaVersion",
            )
            jvmTarget = "$javaVersion"
        }
    }
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(javaVersion)
    }
    assemble {
        dependsOn(reobfJar)
    }
}

bukkit {
    name = "Denchanter"
    apiVersion = "1.19"
    prefix = name
//    depend = listOf(
//        "kotlin-stdlib"
//    )
    authors = listOf(
        "shacha",
    )
    main = "$group.denchanter.Denchanter"
    version = getVersion().toString()
    libraries = listOf(
        "net.axay:kspigot:$kspigotVersion",
    )
    description = "De-enchant-er!"
    website = "github.com/shacha086"
}