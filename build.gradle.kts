plugins {
    kotlin("jvm") version "1.8.10"
    application
}

group = "ca.kittle"
version = "0.0.1-SNAPSHOT"
description = "DM Seer and Buddies app infrastructure"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation("com.pulumi:pulumi:0.9.4")
    implementation("org.virtuslab:pulumi-aws-kotlin:5.42.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

application {
    mainClass.set(
        project.findProperty("mainClass") as? String ?: "$group.MainKt"
    )
}
