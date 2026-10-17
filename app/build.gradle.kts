plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

tasks.register("conanInstall") {
    val conanExecutable = "conan"
    val conanFile = layout.projectDirectory.file("src/main/cpp/conanfile.txt")
    val conanHome = System.getenv("CONAN_HOME")
        ?: "${System.getProperty("user.home")}/.conan2"
    val conanProfile = file("$conanHome/profiles/android-studio")

    val buildTypes = listOf("Debug", "Release")
    val architectures = listOf("armv7", "armv8", "x86", "x86_64")

    // These generated files are consumed by CMake. Declaring them lets Gradle
    // skip Conan entirely when the recipe, profile, and generated files are unchanged.
    inputs.file(conanFile)
    inputs.file(conanProfile)
    inputs.property("conanBuildTypes", buildTypes.joinToString(","))
    inputs.property("conanArchitectures", architectures.joinToString(","))
    outputs.files(architectures.flatMap { arch ->
        buildTypes.flatMap { buildType ->
            val generators = file("src/main/cpp/build/$arch/$buildType/generators")
            listOf(
                file("${generators.path}/conan_toolchain.cmake"),
                file("${generators.path}/libjpeg-turbo-config.cmake"),
                file("${generators.path}/CMakePresets.json")
            )
        }
    })

    doLast {
        buildTypes.forEach { buildType ->
            architectures.forEach { arch ->
                val cmd = listOf(
                    conanExecutable,
                    "install", conanFile.asFile.absolutePath,
                    "--profile", conanProfile.absolutePath,
                    "-s", "build_type=$buildType",
                    "-s", "arch=$arch",
                    "--build", "missing",
                    "-c", "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']"
                )

                println(">> ${cmd.joinToString(" ")}")

                val proc = ProcessBuilder(cmd)
                    .directory(projectDir)
                    .redirectErrorStream(true)
                    .start()

                proc.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line -> println(line) }
                }

                val exitCode = proc.waitFor()

                if (exitCode != 0) {
                    throw Exception("Conan install failed with exit code $exitCode")
                }
            }
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("conanInstall")
}

android {
    namespace = "com.pixelshrink.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pixelshrink.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-Wall", "-Wextra")
                arguments("-DCMAKE_TOOLCHAIN_FILE=conan_android_toolchain.cmake")
            }
        }
    }

    buildFeatures {
        compose = true
        prefab = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation("io.coil-kt:coil-compose:2.6.0")
}
