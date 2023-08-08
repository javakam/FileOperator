pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    //repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven ( url="https://jitpack.io" )
        maven ( url= "https://s01.oss.sonatype.org/content/groups/public" )
        maven ( url= "https://maven.aliyun.com/nexus/content/groups/public/" )
        maven ( url ="https://repo.spring.io/release" )
        maven ( url= "https://repository.jboss.org/maven2" )
//        mavenLocal()
        jcenter() // Warning: this repository is going to shut down soon
    }
}

rootProject.name = ("FileOperator")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(
    "app",
    "library_core",
    "library_selector",
    "library_compressor"
)




