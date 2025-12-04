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

data class Valuation(
    val condition: Boolean,
    val inverseCondition: Boolean,
    val realValue: Boolean?
) {
  val isUnknown: Boolean
    get() = !condition && !inverseCondition

  val isTrue: Boolean
    get() = condition && !inverseCondition

  val isFalse: Boolean
    get() = !condition && inverseCondition

  override fun toString(): String =
      when {
        isUnknown -> "?"
        isTrue -> "T"
        isFalse -> "F"
        else -> error("Illegal")
      }

  override fun equals(other: Any?): Boolean =
      this === other ||
          (other is Valuation &&
              condition == other.condition &&
              inverseCondition == other.inverseCondition)

  override fun hashCode(): Int = 31 * condition.hashCode() + inverseCondition.hashCode()
}
