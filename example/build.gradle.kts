plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    // Project
    api(project(":krpc:iface"))
    testCompileOnly(project(":krpc:compile"))
    kaptTest(project(":krpc:kapt"))
    testRuntimeOnly(project(":krpc:runtime"))

    // Runtime
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.linecorp.armeria:armeria-grpc")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}
