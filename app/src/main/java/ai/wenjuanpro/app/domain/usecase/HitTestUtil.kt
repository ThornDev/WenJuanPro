package ai.wenjuanpro.app.domain.usecase

import kotlin.math.sqrt

object HitTestUtil {
    /**
     * Finds the nearest blue dot to the touch point within hit radius.
     * Returns the grid index of the hit dot, or null if no hit.
     *
     * @param touchX touch X in pixels relative to grid origin
     * @param touchY touch Y in pixels relative to grid origin
     * @param dotsPositions list of 10 blue dot grid indices (0-63)
     * @param cellSizePx cell size in pixels
     * @param selectedIndices set of already-selected grid indices to exclude
     * @return grid index of hit dot, or null
     */
    fun hitTest(
        touchX: Float,
        touchY: Float,
        dotsPositions: List<Int>,
        cellSizePx: Float,
        selectedIndices: Set<Int>,
    ): Int? {
        val hitRadius = cellSizePx * 0.35f
        var bestIndex: Int? = null
        var bestDistance = Float.MAX_VALUE

        for (gridIndex in dotsPositions) {
            if (gridIndex in selectedIndices) continue
            val row = gridIndex / 8
            val col = gridIndex % 8
            val cx = (col + 0.5f) * cellSizePx
            val cy = (row + 0.5f) * cellSizePx
            val dx = touchX - cx
            val dy = touchY - cy
            val distance = sqrt(dx * dx + dy * dy)
            if (distance <= hitRadius && distance < bestDistance) {
                bestDistance = distance
                bestIndex = gridIndex
            }
        }
        return bestIndex
    }
}
