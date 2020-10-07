plugins {
    kotlin("jvm")
    kotlin("kapt")
}

dependencies {
    api(project(":krpc:iface"))

    // Other
    compileOnly("com.google.auto.service:auto-service-annotations:1.0-rc7")
    implementation("com.squareup:kotlinpoet:1.5.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    kapt("com.google.auto.service:auto-service:1.0-rc7")

    // Testing
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kapt {
    includeCompileClasspath = false
}

afterEvaluate {
    tasks.withType(JavaCompile::class.java) {
        // kapt plugin を利用すると lombok など java compile の時の annotation processor が効かなくなる
        options.annotationProcessorPath =
            configurations.getByName("kapt") + configurations.getByName("annotationProcessor")
        options.compilerArgs.removeAll { it.equals("-proc:none") }
    }
}
