plugins {
    `kotlin-dsl`
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
}

allprojects {
    version = "0.0.0-SNAPSHOT"

    apply {
        plugin("io.spring.dependency-management")
        plugin("org.gradle.maven-publish") // A.k.a. `maven-publish`
        plugin("org.gradle.kotlin.kotlin-dsl") // A.k.a. `kotlin-dsl`
    }

    repositories {
        mavenCentral()
    }

    kotlinDslPluginOptions {
        experimentalWarning.set(false)
    }
}
