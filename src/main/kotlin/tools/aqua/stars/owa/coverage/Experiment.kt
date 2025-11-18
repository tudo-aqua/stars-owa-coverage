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

import tools.aqua.stars.core.utils.ApplicationConstantsHolder

const val ARGS_ERROR =
    "Expected arguments: \"mtx\" OR \"sim\" OR \"rnd\" [numTags] [maxTicks] [probability] (seed)"

fun main(args: Array<String>) {
  when (args[0]) {
    "mtx" -> runMatrixExperiment()
    "rnd" -> runRandomExperiment(args.drop(1).toTypedArray())
    "sim" -> runSimulationExperiment()
    else -> {
      ApplicationConstantsHolder.logFolder = "/tmp/data/"
      runMatrixExperiment()
    }
  }
}
