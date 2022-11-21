@file:Suppress(
  "UnstableApiUsage",
  "unused",
  "DSL_SCOPE_VIOLATION",
)

import ElideSubstrate.elideTarget

plugins {
  `maven-publish`
  distribution
  signing
  idea

  alias(libs.plugins.ktlint)
  alias(libs.plugins.dokka)
  alias(libs.plugins.versionCheck)
  alias(libs.plugins.testLogger)
  id(libs.plugins.detekt.get().pluginId)
  id(libs.plugins.sonar.get().pluginId)
  id(libs.plugins.kover.get().pluginId)
}

val allPlugins = listOf(
  "injekt",
  "interakt",
  "redakt",
  "sekret",
)

group = "dev.elide.tools"
version = if (project.hasProperty("version")) {
  project.properties["version"] as String
} else {
  "1.0-SNAPSHOT"
}

tasks.create("buildPlugins") {
  description = "Build all Kotlin compiler plugins"
  dependsOn(allPlugins.map { ":$it:build" })
}

val libPlugins = libs.plugins
val isCI = project.hasProperty("elide.ci") && project.properties["elide.ci"] == "true"

tasks.named("build").configure {
  dependsOn("buildPlugins")
}

tasks.named("publish").configure {
  dependsOn(allPlugins.map {
    ":$it:publish"
  })
}

koverMerged {
  enable()

  xmlReport {
    onCheck.set(isCI)
  }

  htmlReport {
    onCheck.set(isCI)
  }
}

sonarqube {
  properties {
    property("sonar.dynamicAnalysis", "reuseReports")
    property("sonar.junit.reportsPath", "build/reports/")
    property("sonar.java.coveragePlugin", "jacoco")
    property("sonar.sourceEncoding", "UTF-8")
    property("sonar.coverage.jacoco.xmlReportPaths", "$buildDir/reports/kover/merged/xml/report.xml")
  }
}

subprojects {
  sonarqube {
    properties {
      property("sonar.sources", "src/main/kotlin")
      property("sonar.tests", "src/test/kotlin")
      property("sonar.java.binaries", "$buildDir/classes/kotlin/main")
      property("sonar.coverage.jacoco.xmlReportPaths", listOf(
        "$buildDir/reports/kover/xml/report.xml",
      ))
    }
  }
}

publishing {
  elideTarget(
    project,
    label = "Elide Tools: Substrate",
    group = project.group as String,
    artifact = "elide-substrate",
    summary = "Kotlin compiler plugins and other core project infrastructure.",
    parent = true,
  )
}
