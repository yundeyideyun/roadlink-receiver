// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// RoadLink: keep synthetic identities and Robolectric downloads in ignored build storage.
subprojects {
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        val testHome = rootProject.layout.buildDirectory.dir("test-home").get().asFile
        val testTemp = rootProject.layout.buildDirectory.dir("test-temp").get().asFile
        systemProperty("user.home", testHome.absolutePath)
        systemProperty("java.io.tmpdir", testTemp.absolutePath)
        systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
        listOf("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort").forEach { key ->
            System.getProperty(key)?.let { systemProperty(key, it) }
        }
        doFirst { testHome.mkdirs(); testTemp.mkdirs() }
    }
}
