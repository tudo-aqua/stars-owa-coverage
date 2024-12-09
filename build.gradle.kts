/*
 * Copyright 2023-2024 The STARS OWA Coverage Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  kotlin("jvm") version "2.0.0"
  application
  id("io.gitlab.arturbosch.detekt") version "1.23.6"
  id("com.diffplug.spotless") version "6.25.0"
}

group = "tools.aqua"

version = "0.5"

repositories { mavenCentral() }

dependencies {
  testImplementation(kotlin("test"))
  implementation(group = "tools.aqua", name = "stars-core")
  implementation(group = "tools.aqua", name = "stars-logic-kcmftbl")
  implementation(group = "tools.aqua", name = "stars-data-av")
  implementation(group = "tools.aqua", name = "stars-importer-carla")
  implementation(group = "com.github.ajalt.clikt", name = "clikt", version = "4.4.0")
  implementation("org.ow2.sat4j:org.ow2.sat4j.maxsat:2.3.6")
  implementation("tools.aqua:z3-turnkey:4.13.0.1")

  detektPlugins(
      group = "io.gitlab.arturbosch.detekt", name = "detekt-rules-libraries", version = "1.23.6")
}

detekt {
  basePath = rootProject.projectDir.absolutePath
  config.setFrom(files(rootProject.file("contrib/detekt-rules.yml")))
}

spotless {
  kotlin {
    licenseHeaderFile(rootProject.file("contrib/license-header.template.kt")).also {
      it.updateYearWithLatest(true)
    }
    ktfmt()
  }
  kotlinGradle {
    licenseHeaderFile(
            rootProject.file("contrib/license-header.template.kt"),
            "(import |@file|plugins |dependencyResolutionManagement|rootProject.name)")
        .also { it.updateYearWithLatest(true) }
    ktfmt()
  }
}

tasks.test { useJUnitPlatform() }

val reproductionTest by
    tasks.registering(JavaExec::class) {
      group = "verification"
      description = "Runs the reproduction test."
      dependsOn(tasks.run.get().taskDependencies)

      mainClass.set("tools.aqua.stars.owa.coverage.Experiment")
      classpath = sourceSets.main.get().runtimeClasspath
      jvmArgs = listOf("-Xmx64g")
      args =
          listOf(
              // Configure input
              "--input",
              "./stars-reproduction-source/stars-experiments-data/simulation_runs",

              // Set minSegmentTicks filter
              "--minSegmentTicks",
              "11",

              // Sort seeds
              "--sorted",

              // Save results
              "--saveResults",

              // Run reproduction mode
              "--reproduction",
              "baseline",
          )
    }

val reproductionTestAll by
    tasks.registering(JavaExec::class) {
      group = "verification"
      description = "Runs the reproduction test."
      dependsOn(tasks.run.get().taskDependencies)

      mainClass.set("tools.aqua.stars.owa.coverage.Experiment")
      classpath = sourceSets.main.get().runtimeClasspath
      jvmArgs = listOf("-Xmx64g")
      args =
          listOf(
              // Configure input
              "--input",
              "./stars-reproduction-source/stars-experiments-data/simulation_runs",

              // Set minSegmentTicks filter
              "--minSegmentTicks",
              "11",

              // Set allEgo
              "--allEgo",

              // Sort seeds
              "--sorted",

              // Save results
              "--saveResults",

              // Run reproduction mode
              "--reproduction",
              "baseline-all",

              // Show memory usage
              "--showMemoryConsumption")
    }

application { mainClass.set("tools.aqua.stars.owa.coverage.Experiment") }

kotlin { jvmToolchain(17) }
