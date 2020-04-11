import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
val twitter4jStreamVersion = "4.0.7"
val jacksonVersion = "2.10.3"

plugins {
    java
    kotlin("jvm") version "1.3.71"
}

group = "org.sdf.rkm"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.twitter4j:twitter4j-stream:$twitter4jStreamVersion")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.apache.kafka:kafka-clients:2.4.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.github.jillesvangurp:es-kotlin-wrapper-client:1.0-X-Beta-2-7.6.0")
    implementation("org.apache.logging.log4j:log4j-core:2.13.1")
    implementation(kotlin("stdlib-jdk8"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
