plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    id("org.eclipse.keyple")
    id("com.diffplug.spotless")
    id("maven-publish")
}

kotlin {
    jvmToolchain(17)

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "keyplelessreaderlib"
            isStatic = false
        }
    }

    androidTarget {
        publishLibraryVariants("release", "debug")
    }

    jvm {
        kotlin {
            jvmToolchain(17)
        }
    }

    sourceSets {
        commonMain.dependencies {

            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization)

            implementation(libs.okio)

            api(libs.napier)
        }

        androidMain.dependencies {
        }

        iosMain.dependencies {
        }

        jvmMain.dependencies {
        }
    }
}

android {
    namespace = "org.eclipse.keyple.keyplelessreaderlib"
    compileSdk = 34

    defaultConfig {
        minSdk = 23
        targetSdk = 34
    }
}

group = "org.eclipse.keyple"
version = "0.1.2"

publishing {
    repositories {
        mavenLocal {
            //...
        }
    }
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        kotlin {
            target("**/*.kt")
            ktfmt()
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
        }
    }
}