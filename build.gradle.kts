import org.gradle.api.tasks.testing.logging.TestExceptionFormat

/* ktlint-disable max-line-length */
plugins {
    idea
    java
    `maven-publish`
    `project-report`
    kotlin("jvm") version Versions.KOTLIN
    kotlin("plugin.spring") version Versions.KOTLIN
    kotlin("plugin.noarg") version Versions.KOTLIN
    kotlin("kapt") version Versions.KOTLIN apply false
    id("com.gorylenko.gradle-git-properties") version "2.2.2" apply false
    id("com.google.osdetector") version "1.6.2"
    id("com.google.protobuf") version "0.8.13" apply false
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("me.champeau.gradle.jmh") version "0.5.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.0"
    id("org.springframework.boot") version Versions.SPRING_BOOT apply false
}

allprojects {
    group = "la.serendpity.krpc"
    version = "0.0.0-SNAPSHOT"
    apply(plugin = "project-report")

    repositories {
        mavenCentral()
    }
}

fun Project.hasContents(): Boolean {
    return this.file("src").isDirectory || this.file("build.gradle.kts").isFile
}

allprojects {
    if (project != rootProject && !project.hasContents()) return@allprojects

    repositories {
        mavenCentral()
    }

    apply {
        plugin("org.jlleitschuh.gradle.ktlint")
    }

    ktlint {
        version.set("0.38.1")
        android.set(true)
        verbose.set(true)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
        }
    }
}

// subprojects全体の設定
// compileやformattingなど、kakyoin全体で使える設定はこちらにいれる
subprojects {
    if (!project.hasContents()) return@subprojects

    apply {
        plugin<JavaLibraryPlugin>()
        plugin("maven-publish")
        plugin("checkstyle")
        plugin("io.spring.dependency-management")
    }

    tasks.test {
        maxHeapSize = "1G"
        testLogging.showStandardStreams = true
        testLogging.exceptionFormat = TestExceptionFormat.FULL
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()

        options.encoding = "UTF-8"
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:all", "-Xlint:-processing", "-Xlint:-classfile",
                "-Xlint:-serial", "-Xdiags:verbose", // For lint.
                "-parameters", // For Jackson, MyBatis etc.
                "-Werror"
            )
        )

        sourceSets {
            main { java { setSrcDirs(srcDirs + file("src/main/kotlin/")) } }
            test { java { setSrcDirs(srcDirs + file("src/test/kotlin/")) } }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }
    }

    dependencyManagement {
        setApplyMavenExclusions(false) // For fast re-import.

        imports {
            // https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-dependency-versions.html#dependency-versions-properties
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES) {
                extra["elasticsearch.version"] = Versions.ELASTICSEARCH
                extra["jackson-bom.version"] = Versions.JACKSON
                extra["kotlin-coroutines.version"] = Versions.KOTLIN_COROUTINES
                extra["kotlin.version"] = Versions.KOTLIN
                extra["micrometer.version"] = Versions.MICROMETER
            }
            mavenBom("com.linecorp.armeria:armeria-bom:${Versions.ARMERIA}")
            mavenBom("org.testcontainers:testcontainers-bom:${Versions.TEST_CONTAINERS}")
            mavenBom("io.zipkin.brave:brave-bom:5.12.6")
        }

        dependencies {
            // mysql
            dependency("org.mybatis.spring.boot:mybatis-spring-boot-starter:2.1.1")
            dependency("org.mybatis:mybatis:3.5.3")

            // http client
            dependencySet("com.squareup.okhttp3:3.14.7") {
                entry("okhttp")
                entry("logging-interceptor")
                entry("mockwebserver")
            }
            dependencySet("com.squareup.retrofit2:2.7.2") {
                entry("retrofit")
                entry("converter-jackson")
                entry("retrofit-mock")
            }

            dependency("com.google.guava:guava:${Versions.GUAVA}")
            dependency("com.github.tomakehurst:wiremock:2.25.1")
            dependency("io.github.microutils:kotlin-logging:1.7.8")
            dependency("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.1.RELEASE")

            // protobuf
            dependencySet("com.google.protobuf:${Versions.PROTOCOL_BUFFER}") {
                entry("protobuf-java")
                entry("protobuf-java-util")
            }

            dependencySet("io.grpc:${Versions.GRPC}") {
                entry("grpc-protobuf")
                entry("grpc-stub")
                entry("grpc-api")
                entry("grpc-core")
            }

            // Kotlin
            dependency("io.github.microutils:kotlin-logging:1.7.8")
            dependency("io.projectreactor.kotlin:reactor-kotlin-extensions:1.0.1.RELEASE")
            dependency("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
        }
    }

    configurations.all {
        // Due to we choose logback. commons & log4j logs should be pass to -> Slf4J -> Logback.
        // Remove other log implementation and bridges.
        exclude(group = "log4j") // = Log4j implementation (Old). Replaced by log4j-over-slf4j.
        exclude(module = "slf4j-log4j12") // = SLF4J > Log4J Implementation by SLF4J.
        exclude(module = "slf4j-jdk14") // = SLF4J > JDK14 Binding.
        exclude(module = "slf4j-jcl") // = SLF4J > Commons Logging.
        exclude(module = "commons-logging") // Because bridged by jcl-over-slf4j.
        exclude(module = "commons-logging-api") // Replaced by jcl-over-slf4j.

        // Other old & replaced by other module dependencies.
        exclude(module = "servlet-api") // Old pre-3.0 servlet API artifact

        resolutionStrategy {
            // Disable Gradle caches the contents and artifacts of changing modules.
            // By default, these cached values are kept for 24 hours, after which the cached entry is expired and the module is resolved again.
            cacheChangingModulesFor(0, "seconds")
        }
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-logging")
    }

    configurations.compileClasspath {
        // Libraries we don't use our codebase (suppress autocomplete) but needed runtime.
        exclude(group = "commons-lang", module = "commons-lang")
        exclude(group = "org.codehaus.jackson")
        exclude(group = "org.apache.logging.log4j")
        exclude(group = "javax.inject", module = "javax.inject")
    }

    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper> {
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

            // kotlin
            testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin")
        }

        tasks {
            listOf(compileKotlin, compileTestKotlin).forEach { kotlinCompile ->
                kotlinCompile {
                    kotlinOptions {
                        jvmTarget = "11"
                        allWarningsAsErrors = true
                        javaParameters = true
                        freeCompilerArgs = listOf(
                            "-Xemit-jvm-type-annotations",
                            "-Xinline-classes",
                            "-Xjsr305=strict",
                            "-Xopt-in=kotlin.ExperimentalStdlibApi",
                            "-Xopt-in=kotlin.RequiresOptIn"
                        )
                    }
                }
            }
        }
    }

    plugins.withType<me.champeau.gradle.JMHPlugin> {
        dependencies {
            jmhCompileOnly("org.openjdk.jmh:jmh-generator-annprocess:1.22")
            jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.22")
        }

        configure<me.champeau.gradle.JMHPluginExtension> {
            // Default value for casual testing.
            fork = 1
            warmupIterations = 3
            isZip64 = true
            duplicateClassesStrategy = DuplicatesStrategy.WARN
        }
    }

    tasks {
        // https://docs.spring.io/spring-boot/docs/current/reference/html/appendix-configuration-metadata.html#configuration-metadata-annotation-processor
        compileJava {
            dependsOn(processResources)
        }
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
