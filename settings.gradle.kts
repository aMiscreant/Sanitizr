dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://repo.maven.arthenica.com")
        }
    }
}

rootProject.name = "Sanitizr"
include(":app")
