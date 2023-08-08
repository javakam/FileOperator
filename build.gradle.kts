//apply(from = "${rootProject.projectDir}/buildSrc/src/main/kotlin/publish.gradle.kts")

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    kotlin("android") apply false
//    alias(libs.plugins.versions)
}

allprojects {
//    extra["PUBLISHING_GROUP"] as String
//    println()
//    group = extra["PUBLISHING_GROUP"] as String
}

tasks {
//    withType<DependencyUpdatesTask>().configureEach {
//        rejectVersionIf {
//            candidate.version.isStableVersion().not()
//        }
//    }
}