package com.prostuti.student.domain.graph.space

/**
 * Everything one line draws in 3D, in world units, meshed for a box of half-size [range].
 * [surfaces] take the line's colour and opacity; [solidParts] (tubes, arrows, dots) are always
 * opaque; [lines] are contours or mesh lines; [heightMin]/[heightMax] scale "colour by height".
 */
class SpaceGeometry(
    val lineId: Long,
    val range: Double,
    val surfaces: List<Mesh3> = emptyList(),
    val solidParts: List<Mesh3> = emptyList(),
    val lines: Lines3? = null,
    val heightMin: Float = 0f,
    val heightMax: Float = 0f,
    /** Labelled points (world coordinates) such as a point's own position or a vector's tip. */
    val labels: List<DoubleArray> = emptyList(),
)
