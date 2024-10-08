import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kafkaVersion = "3.7.1"
val msal4jVersion = "1.8.1"

val pgiDomainVersion = "0.0.5"

val micrometerVersion = "1.13.3"
val logbackClassicVersion = "1.4.14"
val logstashLogbackEncoder = "5.2" // todo: fails if upgraded
val slf4jVersion = "2.0.9"
val log4jVersion = "2.20.0"

val junitJupiterVersion = "5.10.3" // kan ikke være 5.11 pga problem med spring-boot-plugin'en
val wiremockVersion = "3.9.1"
val kafkaEmbeddedEnvVersion = "3.2.4"

val jacksonVersion = "2.17.2"
val guavaVersion = "33.3.0-jre"
val httpClientVersion = "4.5.14"
val gsonVersion = "2.11.0"
val commonsCompressVersion = "1.24.0"
val commonsCodecVersion = "1.16.0"
val commonsIoVersion = "2.14.0"
val jsonVersion = "20240303"
val snappyJavaVersion = "1.1.10.7"
val jakartaWsRsVersion = "3.1.0"
val jakartaBindVersion = "3.0.1"

val assertJVersion = "3.26.3"

val springBootVersion = "3.3.3"
val jerseyVersion = "3.1.8"
val kotlinxVersion = "1.9.0"

group = "no.nav.pgi"

plugins {
    val kotlinVersion = "2.0.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("org.springframework.boot") version "3.3.3"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("com.github.ben-manes.versions") version "0.51.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
    maven("https://maven.pkg.github.com/navikt/pensjon-samhandling-ktor-support") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven("https://maven.pkg.github.com/navikt/pgi-domain") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

//    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1") // TODO: versjonsvariabel

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("no.nav.pgi:pgi-domain:$pgiDomainVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoder")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("com.microsoft.azure:msal4j:$msal4jVersion")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:$jakartaWsRsVersion")
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.json:json:$jsonVersion")
    implementation("org.xerial.snappy:snappy-java:$snappyJavaVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")

    testImplementation("org.assertj:assertj-core:$assertJVersion")

    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("org.wiremock:wiremock-jetty12:$wiremockVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation(("org.glassfish.jersey.core:jersey-server:$jerseyVersion"))
    testImplementation(("org.glassfish.jersey.core:jersey-common:$jerseyVersion"))
    testImplementation(("org.glassfish.jersey.core:jersey-client:$jerseyVersion"))
    testImplementation(("org.glassfish.jersey.inject:jersey-hk2:$jerseyVersion"))
    testImplementation("jakarta.xml.bind:jakarta.xml.bind-api:$jakartaBindVersion")

}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("app")

    manifest {
        attributes["Main-Class"] = "no.nav.pgi.popp.lagreinntekt.ApplicationKt"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations.runtimeClasspath.get().forEach {
            val buildDir = layout.buildDirectory.get()
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = FULL
    }
}