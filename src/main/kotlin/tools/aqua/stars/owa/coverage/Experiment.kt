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

package tools.aqua.stars.owa.coverage

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.io.path.name
import tools.aqua.stars.core.evaluation.TSCEvaluation
import tools.aqua.stars.core.metric.metrics.evaluation.*
import tools.aqua.stars.core.metric.metrics.postEvaluation.*
import tools.aqua.stars.data.av.dataclasses.*
import tools.aqua.stars.importer.carla.CarlaSimulationRunsWrapper
import tools.aqua.stars.importer.carla.loadSegments
import tools.aqua.stars.owa.coverage.metrics.UncertainValidTSCInstancesPerTSCMetric

fun main() {
  downloadAndUnzipExperimentsData()

  println("Loading simulation runs...")
  val simulationRunsWrappers = getSimulationRuns()

  println("Loading segments...")
  val segments =
      loadSegments(
          useEveryVehicleAsEgo = false,
          minSegmentTickCount = 11,
          orderFilesBySeed = true,
          simulationRunsWrappers = simulationRunsWrappers,
      )

  val validTSCInstancesPerProjectionMetric =
      ValidTSCInstancesPerTSCMetric<
          Actor, TickData, Segment, TickDataUnitSeconds, TickDataDifferenceSeconds>()

  val uncertainties =
      mapOf(
          "Must Yield" to 0.5,
          "Oncoming traffic" to 0.5,
          "Overtaking" to 0.5,
          "Pedestrian Crossed" to 0.5,
          "Has Stop Sign" to 0.5,
          "Has Yield Sign" to 0.5,
          "Has Red Light" to 0.5)

  println("Creating TSC...")
  TSCEvaluation(
          tscList = tsc().buildProjections(),
          writePlots = false,
          writePlotDataCSV = false,
          writeSerializedResults = false)
      .apply {
        registerMetricProviders(
            validTSCInstancesPerProjectionMetric,
            MissedPredicateCombinationsPerTSCMetric(validTSCInstancesPerProjectionMetric),
            UncertainValidTSCInstancesPerTSCMetric(
                validTSCInstancesPerProjectionMetric, uncertainties))
        println("Run Evaluation")
        runEvaluation(segments = segments)
      }
}

/**
 * Checks if the experiments data is available. Otherwise, it is downloaded and extracted to the
 * correct folder.
 */
private fun downloadAndUnzipExperimentsData() {
  val reproductionSourceFolderName = "stars-reproduction-source"
  val reproductionSourceZipFile = "$reproductionSourceFolderName.zip"

  if (File(reproductionSourceFolderName).exists()) {
    println("The 'stars-reproduction-source' already exists")
    return
  }

  if (!File(reproductionSourceZipFile).exists()) {
    println("Start with downloading the experiments data. This may take a while.")
    URL("https://zenodo.org/record/8131947/files/stars-reproduction-source.zip?download=1")
        .openStream()
        .use { Files.copy(it, Paths.get(reproductionSourceZipFile)) }
  }

  check(File(reproductionSourceZipFile).exists()) {
    "After downloading the file '$reproductionSourceZipFile' does not exist."
  }

  println("Extracting experiments data from zip file.")
  extractZipFile(zipFile = File(reproductionSourceZipFile), outputDir = File("."))

  check(File(reproductionSourceFolderName).exists()) { "Error unzipping simulation data." }
  check(File("./$reproductionSourceFolderName").totalSpace > 0) {
    "There was an error while downloading/extracting the simulation data. The test zip file is missing."
  }
}

private fun getSimulationRuns(): List<CarlaSimulationRunsWrapper> =
    File("./stars-reproduction-source/stars-experiments-data/simulation_runs").let { file ->
      file
          .walk()
          .filter { it.isDirectory && it != file }
          .toList()
          .mapNotNull { mapFolder ->
            var staticFile: Path? = null
            val dynamicFiles = mutableListOf<Path>()
            mapFolder.walk().forEach { mapFile ->
              if (mapFile.nameWithoutExtension.contains("static_data")) {
                staticFile = mapFile.toPath()
              }
              if (mapFile.nameWithoutExtension.contains("dynamic_data")) {
                dynamicFiles.add(mapFile.toPath())
              }
            }

            if (staticFile == null || dynamicFiles.isEmpty()) {
              return@mapNotNull null
            }

            dynamicFiles.sortBy {
              "_seed([0-9]{1,4})".toRegex().find(it.fileName.name)?.groups?.get(1)?.value?.toInt()
                  ?: 0
            }
            return@mapNotNull CarlaSimulationRunsWrapper(staticFile, dynamicFiles)
          }
    }

/**
 * Extract a zip file into any directory.
 *
 * @param zipFile src zip file
 * @param outputDir directory to extract into. There will be new folder with the zip's name inside
 *   [outputDir] directory.
 * @return the extracted directory i.e.
 */
private fun extractZipFile(zipFile: File, outputDir: File): File? {
  ZipFile(zipFile).use { zip ->
    zip.entries().asSequence().forEach { entry ->
      zip.getInputStream(entry).use { input ->
        if (entry.isDirectory) File(outputDir, entry.name).also { it.mkdirs() }
        else
            File(outputDir, entry.name)
                .also { it.parentFile.mkdirs() }
                .outputStream()
                .use { output -> input.copyTo(output) }
      }
    }
  }
  return outputDir
}
