import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.quocnguyen.smartbuildview"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // JUnit 5 for testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
}

intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ IDEA Community Edition
    
    // Core plugin dependencies
    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.kotlin"
    ))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    
    withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }

    test {
        useJUnitPlatform()
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("") // No upper limit - compatible with all future versions
    }

    // Configure Plugin Verifier to test against specific IDE versions
    runPluginVerifier {
        ideVersions.set(listOf(
            "2024.1",    // Minimum supported version
            "2024.2",    // Test with newer version
            "2024.3"     // Test with latest stable
        ))
        // Fail build if compatibility issues are found
        failureLevel.set(
            listOf(
                RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
                RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN
            )
        )
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
