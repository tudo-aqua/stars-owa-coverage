/*
 * Copyright 2023-2025 The STARS OWA Coverage Authors
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
  kotlin("jvm") version "2.1.0"
  application
  id("com.diffplug.spotless") version "7.0.2"
}

repositories { mavenCentral() }

dependencies {
  testImplementation(kotlin("test"))
  implementation(group = "tools.aqua", name = "stars-core")
  implementation(group = "tools.aqua", name = "stars-logic-kcmftbl")
  implementation(group = "tools.aqua", name = "stars-data-av")
  //  implementation(group = "tools.aqua", name = "stars-importer-carla")
  implementation(group = "org.ow2.sat4j", name = "org.ow2.sat4j.maxsat", version = "2.3.6")
  implementation(group = "tools.aqua", name = "z3-turnkey", version = "4.13.0.1")
  implementation(group = "org.jgrapht", name = "jgrapht-core", version = "1.5.2")
  implementation(
      group = "org.jetbrains.lets-plot", name = "lets-plot-kotlin-jvm", version = "4.10.0")
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

application { mainClass.set("tools.aqua.stars.owa.coverage.ExperimentKt") }

kotlin { jvmToolchain(17) }
