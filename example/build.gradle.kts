plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    compileOnly(project(":krpc:compile"))
    compileOnly(project(":krpc:iface"))
    kapt(project(":krpc:kapt"))
    runtimeOnly(project(":krpc:runtime"))

    implementation("com.linecorp.armeria:armeria-grpc")
}
