package com.example.colorlinkgame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val dotPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    // Game state
    private var grid = mutableListOf<Dot>()
    private var paths = mutableMapOf<Int, Path>()
    private var currentColor: Int? = null
    private var currentPath: Path? = null
    private var currentStart: Dot? = null
    private var currentEnd: Dot? = null
    private var levelComplete = false
    private var currentLevel = 1
    private var levelCompleteListener: (() -> Unit)? = null

    // Available colors for the dots
    private val gameColors = listOf(
        Color.RED,
        Color.BLUE,
        Color.YELLOW,
        Color.GREEN,
        Color.MAGENTA,
        Color.CYAN,
        Color.parseColor("#FFA500")  // Orange
    )
    // Store touch points for smooth path
    private val touchPoints = mutableListOf<PointF>()
    private var hasMoved = false

    // Data classes for level configuration
    data class LevelConfig(
        val gridSize: Int,
        val dotConnections: List<DotConnection>
    )

    data class DotConnection(
        val dot1Position: Position,
        val dot2Position: Position,
        val colorIndex: Int
    )

    data class Position(val x: Int, val y: Int)

    data class Dot(
        val x: Float,
        val y: Float,
        val color: Int,
        var connected: Boolean = false
    )
    // Modified level configurations to support 40 levels
    private fun generateLevelConfig(level: Int): LevelConfig {
        val random = Random(level) // Use level as seed for consistent generation
        val gridSize = 6 // Fixed grid size
        val numDots = random.nextInt(6, 9) // Random number between 6 and 8 dots (pairs)
        val dotConnections = mutableListOf<DotConnection>()

        val usedPositions = mutableSetOf<Position>()

        for (i in 0 until numDots/2) {
            var dot1Pos: Position
            var dot2Pos: Position

            // Generate unique positions
            do {
                dot1Pos = Position(
                    random.nextInt(1, gridSize + 1),
                    random.nextInt(1, gridSize + 1)
                )
            } while (dot1Pos in usedPositions)

            do {
                dot2Pos = Position(
                    random.nextInt(1, gridSize + 1),
                    random.nextInt(1, gridSize + 1)
                )
            } while (dot2Pos in usedPositions || dot2Pos == dot1Pos)

            usedPositions.add(dot1Pos)
            usedPositions.add(dot2Pos)

            dotConnections.add(DotConnection(dot1Pos, dot2Pos, i % gameColors.size))
        }

        return LevelConfig(gridSize, dotConnections)
    }

    // Level configurations
    private val levelConfigurations = mutableMapOf(
        1 to LevelConfig(
            gridSize = 3,
            dotConnections = listOf(
                DotConnection(Position(1, 1), Position(2, 2), 0),
                DotConnection(Position(1, 2), Position(2, 1), 1),
                DotConnection(Position(3, 1), Position(3, 2), 2)
            )
        ),
        2 to LevelConfig(
            gridSize = 4,
            dotConnections = listOf(
                DotConnection(Position(1, 1), Position(3, 3), 0),
                DotConnection(Position(2, 1), Position(2, 3), 1),
                DotConnection(Position(3, 1), Position(1, 3), 2),
                DotConnection(Position(4, 1), Position(4, 4), 3)
            )
        ),
        3 to LevelConfig(
            gridSize = 5,
            dotConnections = listOf(
                DotConnection(Position(1, 1), Position(4, 4), 0),
                DotConnection(Position(2, 2), Position(3, 3), 1),
                DotConnection(Position(4, 1), Position(1, 4), 2),
                DotConnection(Position(5, 2), Position(3, 5), 3),
                DotConnection(Position(5, 5), Position(2, 3), 4)
            )
        )
    )


init {
    for (i in 1..100) {
        levelConfigurations[i] = generateLevelConfig(i)
    }
    setupLevel(1)
}

    fun setLevelCompleteListener(listener: () -> Unit) {
        levelCompleteListener = listener
    }

    fun setupLevel(level: Int) {
        currentLevel = level
        // Clear existing state
        grid.clear()
        paths.clear()
        currentColor = null
        currentPath = null
        currentStart = null
        currentEnd = null
        levelComplete = false

        // Get level configuration and create dots
        val config = levelConfigurations[level] ?: levelConfigurations[1]
        createDotsFromConfig(config!!)
        invalidate()
    }

    private fun createDotsFromConfig(config: LevelConfig) {
        val cellWidth = width / (config.gridSize + 1f)
        val cellHeight = height / (config.gridSize + 1f)

        config.dotConnections.forEach { connection ->
            grid.add(Dot(
                connection.dot1Position.x * cellWidth,
                connection.dot1Position.y * cellHeight,
                gameColors[connection.colorIndex]
            ))
            grid.add(Dot(
                connection.dot2Position.x * cellWidth,
                connection.dot2Position.y * cellHeight,
                gameColors[connection.colorIndex]
            ))
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupLevel(currentLevel)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw level number
        canvas.drawText("Level $currentLevel", width / 2f, 100f, textPaint)

        // Draw completed paths
        for ((color, path) in paths) {
            paint.color = color
            canvas.drawPath(path, paint)
        }

        // Draw current path
        currentPath?.let {
            paint.color = currentColor ?: Color.BLACK
            canvas.drawPath(it, paint)
        }

        // Draw dots
        for (dot in grid) {
            dotPaint.color = dot.color
            canvas.drawCircle(dot.x, dot.y, 40f, dotPaint)

            if (dot.connected) {
                dotPaint.color = Color.WHITE
                canvas.drawCircle(dot.x, dot.y, 20f, dotPaint)
            }
        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (levelComplete) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> handleTouchStart(event.x, event.y)
            MotionEvent.ACTION_MOVE -> handleTouchMove(event.x, event.y)
            MotionEvent.ACTION_UP -> handleTouchEnd(event.x, event.y)
        }
        return true
    }


private fun handleTouchStart(x: Float, y: Float) {
    val startDot = findNearestDot(x, y) ?: return

    if (!startDot.connected) {
        currentStart = startDot
        currentColor = startDot.color
        currentPath = Path().apply {
            moveTo(startDot.x, startDot.y)
        }
        hasMoved = false
        touchPoints.clear()
        invalidate()
    }
}

    private fun handleTouchMove(x: Float, y: Float) {
        currentPath?.let { path ->
            if (abs(x - (currentStart?.x ?: 0f)) > 10f || abs(y - (currentStart?.y ?: 0f)) > 10f) {
                hasMoved = true
            }

            touchPoints.add(PointF(x, y))
            path.reset()
            path.moveTo(currentStart?.x ?: 0f, currentStart?.y ?: 0f)

            for (point in touchPoints) {
                path.lineTo(point.x, point.y)
            }

            invalidate()
        }
    }

    private fun handleTouchEnd(x: Float, y: Float) {
        val endDot = findNearestDot(x, y)
        currentStart?.let { start ->
            if (!hasMoved) {
                resetCurrentPath()
                return
            }

            if (endDot != null && endDot.color == start.color && !endDot.connected) {
                if (!doesPathIntersect(touchPoints) && !doesPathPassThroughDot(touchPoints)) {
                    val finalPath = Path().apply {
                        moveTo(start.x, start.y)
                        for (point in touchPoints) {
                            lineTo(point.x, point.y)
                        }
                        lineTo(endDot.x, endDot.y)
                    }
                    paths[start.color] = finalPath
                    start.connected = true      // تعيين حالة الاتصال للنقطة الأولى
                    endDot.connected = true     // تعيين حالة الاتصال للنقطة الثانية
                    checkLevelComplete()
                }
            }
        }
        resetCurrentPath()
    }
    private fun doesPathPassThroughDot(points: List<PointF>): Boolean {
        val currentEndDot = findNearestDot(points.last().x, points.last().y)

        for (dot in grid) {
            if (dot == currentStart || dot == currentEndDot || dot.connected) continue

            val dotRadius = 45f
            for (i in 1 until points.size - 1) {
                val point = points[i]

                val distance = kotlin.math.sqrt(
                    (dot.x - point.x) * (dot.x - point.x) +
                            (dot.y - point.y) * (dot.y - point.y)
                )

                if (distance < dotRadius) {
                    return true
                }
            }
        }
        return false
    }

    private fun pointToLineDistance(x: Float, y: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val numerator = abs((y2 - y1) * x - (x2 - x1) * y + x2 * y1 - y2 * x1)
        val denominator = sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1))
        return if (denominator != 0f) numerator / denominator else 0f
    }


    private fun findNearestDot(x: Float, y: Float): Dot? {
        val touchRadius = 100f
        return grid.firstOrNull { dot ->
            abs(dot.x - x) < touchRadius && abs(dot.y - y) < touchRadius
        }
    }

private fun doesPathIntersect(points: List<PointF>): Boolean {
    // Convert the current path to line segments
    val newSegments = mutableListOf<LineSegment>()
    if (points.size > 1) {
        for (i in 0 until points.size - 1) {
            newSegments.add(
                LineSegment(
                    points[i].x,
                    points[i].y,
                    points[i + 1].x,
                    points[i + 1].y
                )
            )
        }
    }

    // Check each existing path for intersections
    for ((_, path) in paths) {
        val pathSegments = getPathSegments(path)
        for (newSeg in newSegments) {
            for (existingSeg in pathSegments) {
                if (doLinesIntersect(newSeg, existingSeg)) {
                    return true
                }
            }
        }
    }
    return false
}
    private fun getPathSegments(path: Path): List<LineSegment> {
        val segments = mutableListOf<LineSegment>()
        val pm = PathMeasure(path, false)
        val length = pm.length
        val numSegments = 20 // Number of segments to approximate the path

        val pos1 = FloatArray(2)
        val pos2 = FloatArray(2)

        for (i in 0 until numSegments - 1) {
            pm.getPosTan(i * length / numSegments, pos1, null)
            pm.getPosTan((i + 1) * length / numSegments, pos2, null)
            segments.add(LineSegment(pos1[0], pos1[1], pos2[0], pos2[1]))
        }

        return segments
    }

    private data class LineSegment(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float
    )

    private fun doLinesIntersect(line1: LineSegment, line2: LineSegment): Boolean {
        fun orientation(p: PointF, q: PointF, r: PointF): Int {
            val value = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y)
            return when {
                value == 0f -> 0
                value > 0 -> 1
                else -> 2
            }
        }

        fun onSegment(p: PointF, q: PointF, r: PointF): Boolean {
            return q.x <= max(p.x, r.x) && q.x >= min(p.x, r.x) &&
                    q.y <= max(p.y, r.y) && q.y >= min(p.y, r.y)
        }

        val p1 = PointF(line1.x1, line1.y1)
        val q1 = PointF(line1.x2, line1.y2)
        val p2 = PointF(line2.x1, line2.y1)
        val q2 = PointF(line2.x2, line2.y2)

        val o1 = orientation(p1, q1, p2)
        val o2 = orientation(p1, q1, q2)
        val o3 = orientation(p2, q2, p1)
        val o4 = orientation(p2, q2, q1)

        if (o1 != o2 && o3 != o4) return true

        if (o1 == 0 && onSegment(p1, p2, q1)) return true
        if (o2 == 0 && onSegment(p1, q2, q1)) return true
        if (o3 == 0 && onSegment(p2, p1, q2)) return true
        if (o4 == 0 && onSegment(p2, q1, q2)) return true

        return false
    }

private fun checkLevelComplete() {
    levelComplete = grid.all { it.connected }
    if (levelComplete) {
        if (currentLevel == 100) {
            AlertDialog.Builder(context)
                .setTitle("Congratulations!")
                .setMessage("You've completed all levels! Do you want to start over?")
                .setPositiveButton("Yes") { _, _ ->
                    setupLevel(1)
                }
                .setNegativeButton("No", null)
                .setCancelable(false)
                .show()
        } else {
            levelCompleteListener?.invoke()
        }
    }
}

    private fun resetCurrentPath() {
        touchPoints.clear()
        currentPath = null
        currentStart = null
        currentColor = null
        hasMoved = false
        invalidate()
    }
    fun resetLevel() {
        setupLevel(currentLevel)
    }

    fun isLevelComplete(): Boolean = levelComplete

    fun getCurrentLevel(): Int = currentLevel
}