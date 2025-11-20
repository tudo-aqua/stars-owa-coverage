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

import tools.aqua.stars.core.types.TickDifference

class IndexTickDifference(val difference: Int) : TickDifference<IndexTickDifference>() {
  override fun plus(other: IndexTickDifference): IndexTickDifference =
      IndexTickDifference(this.difference + other.difference)

  override fun minus(other: IndexTickDifference): IndexTickDifference =
      IndexTickDifference(this.difference - other.difference)

  override fun compareTo(other: IndexTickDifference): Int =
      this.difference.compareTo(other.difference)

  override fun serialize(): String = difference.toString()

  override fun deserialize(str: String): IndexTickDifference = IndexTickDifference(str.toInt())
}
