plugins {
    kotlin("jvm") version "2.0.10"
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
//    jvmToolchain {
//        languageVersion.set(JavaLanguageVersion.of(11))
//    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("com.pulumi:pulumi:0.15.0")
    implementation("org.virtuslab:pulumi-kotlin:0.11.0.0")
    implementation("org.virtuslab:pulumi-aws-kotlin:6.49.1.0")
    implementation("com.pulumi:postgresql:3.15.0")
    implementation("org.virtuslab:pulumi-kubernetes-kotlin:4.16.0.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

application {
    mainClass.set(
        project.findProperty("mainClass") as? String ?: "$group.MainKt"
    )
}
