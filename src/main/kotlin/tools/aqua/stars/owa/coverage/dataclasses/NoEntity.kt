package tools.aqua.stars.owa.coverage.dataclasses

import tools.aqua.stars.core.types.EntityType
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds

class NoEntity(override val tickData: UnknownTickData) : EntityType<NoEntity, UnknownTickData, SingleTickSegment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
  override val id: Int = -1
}