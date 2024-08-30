import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kafkaVersion = "3.6.0"
val confluentVersion = "5.5.1"
val ktorVersion = "1.6.8"
val msal4jVersion = "1.8.1"

val ktorSupportVersion = "0.0.30"
val pgiDomainVersion = "0.0.5"

val micrometerVersion = "1.11.5"
val logbackClassicVersion = "1.4.11"
val logstashLogbackEncoder = "5.2" // todo: fails if upgraded
val slf4jVersion = "2.0.9"
val log4jVersion = "2.20.0"

val junitJupiterVersion = "5.11.0"
val wiremockVersion = "2.27.2"
val kafkaEmbeddedEnvVersion = "2.5.0"

val jacksonVersion = "2.17.2"
val guavaVersion = "32.1.3-jre"
val httpClientVersion = "4.5.14"
val gsonVersion = "2.10.1"
val commonsCompressVersion = "1.24.0"
val commonsCodecVersion = "1.16.0"
val commonsIoVersion = "2.14.0"
val jsonVersion = "20231013"
val snakeYamlVersion = "1.33" // har sikkerhetshull, men kan ikke oppdatere videre til 2.x pga spring
val snappyJavaVersion = "1.1.10.6"

val assertJVersion = "3.26.3"

group = "no.nav.pgi"

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.github.ben-manes.versions") version "0.51.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

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
    maven("https://maven.pkg.github.com/navikt/pgi-schema") {
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("javax.ws.rs:javax.ws.rs-api:2.1.1") // TODO: versjonsvariabel

    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("no.nav.pensjonsamhandling:pensjon-samhandling-ktor-support:$ktorSupportVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("no.nav.pgi:pgi-domain:$pgiDomainVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
//    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")

    implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoder")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    implementation("com.microsoft.azure:msal4j:$msal4jVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    // Overstyrer noen transitive avhengigheter (mest pga sikkerhet)
    // implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion") -- breaks kafka
    implementation("com.google.guava:guava:$guavaVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")
    implementation("commons-codec:commons-codec:$commonsCodecVersion")
    implementation("commons-io:commons-io:$commonsIoVersion")
    implementation("org.json:json:$jsonVersion")
    implementation("org.xerial.snappy:snappy-java:$snappyJavaVersion")
    implementation("org.yaml:snakeyaml:$snakeYamlVersion")
    testImplementation("org.apache.httpcomponents:httpclient:$httpClientVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")

    testImplementation("org.assertj:assertj-core:$assertJVersion")

    testImplementation("com.github.tomakehurst:wiremock:$wiremockVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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