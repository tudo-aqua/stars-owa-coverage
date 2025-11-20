/*
 * Copyright 2025 The STARS OWA Coverage Authors
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

package tools.aqua.stars.owa.coverage.dataclasses

import tools.aqua.stars.core.types.TickUnit

class IndexTickUnit(val index: Int) : TickUnit<IndexTickUnit, IndexTickDifference>() {
  override fun plus(other: IndexTickDifference): IndexTickUnit =
      IndexTickUnit(this.index + other.difference)

  override fun minus(other: IndexTickDifference): IndexTickUnit =
      IndexTickUnit(this.index - other.difference)

  override fun minus(other: IndexTickUnit): IndexTickDifference =
      IndexTickDifference(this.index - other.index)

  override fun compareTo(other: IndexTickUnit): Int = this.index.compareTo(other.index)

  override fun serialize(): String = index.toString()

  override fun deserialize(str: String): IndexTickUnit = IndexTickUnit(str.toInt())
}
