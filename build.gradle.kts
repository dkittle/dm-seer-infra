plugins {
    kotlin("jvm") version "1.7.10"
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
    implementation("com.pulumi:pulumi:(,1.0]")
    implementation("org.virtuslab:pulumi-aws-kotlin:5.29.1.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

application {
    mainClass.set(
        project.findProperty("mainClass") as? String ?: "${group}.MainKt"
    )
}
