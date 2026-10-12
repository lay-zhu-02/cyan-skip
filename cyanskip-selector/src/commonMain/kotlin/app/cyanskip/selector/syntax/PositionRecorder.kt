package app.cyanskip.selector.syntax

import app.cyanskip.selector.SelectorPosition
import app.cyanskip.selector.SelectorPositionKind
import app.cyanskip.selector.SelectorSourceMap
import app.cyanskip.selector.SourceRange

internal class RecordedPositions(
    val positions: Array<out SelectorPosition>,
    val sourceMap: SelectorSourceMap,
)

internal class PositionRecorder {
    private val positions = mutableListOf<SelectorPosition>()
    private val ranges = mutableMapOf<Any, SourceRange>()

    fun recordPosition(
        kind: SelectorPositionKind,
        start: Int,
        end: Int,
    ) {
        positions.add(SelectorPosition(kind, start, end))
    }

    fun record(
        value: Any,
        kind: SelectorPositionKind,
        start: Int,
        end: Int,
    ) {
        recordPosition(kind, start, end)
        ranges[value] = SourceRange(start, end)
    }

    fun freeze(): RecordedPositions = RecordedPositions(
        positions = freezePositions(),
        sourceMap = SelectorSourceMap(ranges),
    )

    fun freezePositions(): Array<out SelectorPosition> = positions.toTypedArray()
}
