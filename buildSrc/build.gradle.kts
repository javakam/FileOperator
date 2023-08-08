plugins {
    `kotlin-dsl`
}
repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.kgp)
    implementation(libs.agp)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

kotlin {
    //jvmToolchain(17)
//    jvmToolchain {
//        // 配置要使用的编译器和运行时
//        jdk("17", "/path/to/jdk-17")
//        // 可以添加更多的 jdk() 配置以支持不同版本的 JDK
//    }
}