package tools.aqua.stars.owa.coverage.dataclasses

import tools.aqua.stars.core.types.SegmentType
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.TickDataUnitSeconds

class SingleTickSegment(val tick: UnknownTickData) : SegmentType<NoEntity, UnknownTickData, SingleTickSegment, TickDataUnitSeconds, TickDataDifferenceSeconds> {
  override val ticks: Map<TickDataUnitSeconds, UnknownTickData> = mapOf(TickDataUnitSeconds(0.0) to tick)
  override val segmentSource: String = ""
  override val primaryEntityId: Int = -1
}