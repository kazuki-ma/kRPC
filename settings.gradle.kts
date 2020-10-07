
/* ktlint-disable max-line-length */
pluginManagement {
    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/gradle/gradle-plugins")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
    }
}


rootProject.name = "kRPC"

include(":example")

include(":krpc:kapt")
include(":krpc:iface")
include(":krpc:compile")
include(":krpc:runtime")
