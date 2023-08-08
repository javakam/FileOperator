import com.android.build.gradle.LibraryExtension
import org.gradle.api.tasks.bundling.Jar
import java.io.FileReader
import java.util.Properties

/**
 * https://docs.gradle.org/current/userguide/publishing_maven.html
 *
 * Precompiled script plugin from:
 * https://github.com/cortinico/kotlin-android-template/blob/master/buildSrc/src/main/kotlin/publish.gradle.kts
 *
 * The following plugin tasks care of setting up:
 * - Publishing to Maven Central and Sonatype Snapshots
 * - GPG Signing with in memory PGP Keys
 * - Javadoc/SourceJar are attached via AGP
 *
 * To use it just apply:
 *
 * plugins {
 *     publish
 * }
 *
 * To your build.gradle.kts.
 *
 * If you copy over this file in your project, make sure to copy it inside: buildSrc/src/main/kotlin/publish.gradle.kts.
 * Make sure to copy over also buildSrc/build.gradle.kts otherwise this plugin will fail to compile due to missing dependencies.
 */
plugins {
    id("maven-publish")
    id("signing")
}

//查看Sonatype信息 https://issues.sonatype.org/issues/?filter=-2
//val globalVariable: String by rootProject.extra
val PUBLISH_GROUP_ID = "com.github.javakam"
val PUBLISH_VERSION = rootProject.extra["versionName"]

//extra["signing.keyId"] = ''
//extra["signing.password"] = ''
//extra["signing.secretKeyRingFile"] = ''
//extra["ossrhUsername"] = ''
//extra["ossrhPassword"] = ''
val localProperties = Properties()
FileReader("local.properties").use { reader ->
    localProperties.load(reader)
}
localProperties.forEach { (key, value) ->
    extra[key.toString()] = value.toString()
}

publishing {
    repositories {
        maven {
            name = "nexus"
            // https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = extra["ossrhUsername"] as String
                password = extra["ossrhPassword"] as String
            }
        }
    }

    publications {
        create<MavenPublication>("release") {
            // The coordinates of the library, being set from variables that we'll set up in a moment
            groupId = extra["PUBLISH_GROUP_ID"] as String
            artifactId = extra["PUBLISH_ARTIFACT_ID"] as String
            version = extra["PUBLISH_VERSION"] as String

            afterEvaluate {
                if (plugins.hasPlugin("com.android.library")) {
                    from(components["release"])
                } else {
                    from(components["java"])
                }
            }

            pom {
                version = extra["PUBLISH_VERSION"] as String
                description.set("Android File Operator Library.")
                url.set("https://github.com/javakam/${rootProject.name}")

                licenses {
                    license {
                        //协议类型，一般默认Apache License2.0的话不用改
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("javakam")
                        name.set("javakam")
                        email.set("jooybao@foxmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:github.com/javakam/${rootProject.name}.git")
                    developerConnection.set("scm:git:ssh://github.com:javakam/${rootProject.name}.git")
                    url.set("https://github.com/javakam/${rootProject.name}/tree/master")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/javakam/${rootProject.name}/issues")
                }
            }
        }
    }

    signing {
        //useInMemoryPgpKeys(signingKey, signingPwd)
        sign(publishing.publications["release"])
    }
}


//val String.byProperty: String? get() = findProperty(this) as? String