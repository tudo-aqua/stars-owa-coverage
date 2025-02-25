package tools.aqua.stars.owa.coverage.dataclasses

import tools.aqua.stars.core.types.TickDataType
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds

class UnknownTickData(
  override val currentTick: TickDataUnitSeconds,
  val unknownData: List<Pair<Boolean, Boolean>>
) : TickDataType<NoEntity, UnknownTickData, SingleTickSegment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
  override var entities: List<NoEntity> = emptyList()
  override lateinit var segment: SingleTickSegment

  override fun toString(): String = unknownData.joinToString(prefix = "${currentTick.tickSeconds}: [", separator = ", ", postfix = "]") { (condition, inverseCondition) ->
    when {
      !condition && !inverseCondition -> "?"
      condition && !inverseCondition -> "\u22A4"
      !condition && inverseCondition -> "\u22A5"
      else -> "Illegal"
    }
  }
}