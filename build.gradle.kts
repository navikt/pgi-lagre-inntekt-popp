import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kafkaVersion = "2.8.2"
val confluentVersion = "5.5.1"
val ktorVersion = "1.3.2-1.4.0-rc"
val msal4jVersion = "1.8.1"

val ktorSupportVersion = "0.0.24"
val pgiSchemaVersion = "0.0.7"

val micrometerVersion = "1.11.4"
val logbackClassicVersion = "1.4.11"
val logstashLogbackEncoder = "5.2" // todo: fails if upgraded
val slf4jVersion = "2.0.9"
val log4jVersion = "2.20.0"

val junitJupiterVersion = "5.10.0"
val wiremockVersion = "2.27.2"
val kafkaEmbeddedEnvVersion = "2.5.0"

group = "no.nav.pgi"

plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("com.github.ben-manes.versions") version "0.48.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    jcenter()
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
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("no.nav.pensjonsamhandling:pensjon-samhandling-ktor-support:$ktorSupportVersion")

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    implementation("no.nav.pgi:pgi-schema:$pgiSchemaVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")

    implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoder")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    implementation("com.microsoft.azure:msal4j:$msal4jVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("com.github.tomakehurst:wiremock:$wiremockVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion") {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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

