pluginManagement {
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "My Application"

include(":app")

// Control app used to isolate why bank/UPI apps warn about SnapTile's
// accessibility service. Not part of SnapTile; delete this line (and the
// directory) once the investigation is closed.
include(":a11yprobe")