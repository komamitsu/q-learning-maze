package org.komamitsu.qlearningtest

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.properties.Delegates
import kotlin.random.Random

enum class Direction(val diffOfY: Int, val diffOfX: Int) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1)
}

data class Position(val x: Int, val y: Int) {
    fun move(direction: Direction) : Position {
        return Position(x + direction.diffOfX, y + direction.diffOfY)
    }

    fun isWall(map: List<List<Int>>) : Boolean {
        return map[y][x] == 1
    }
}

class QCalc(
    private val map: List<List<Int>>,
    private val goal: Position,
    private val alpha: Double,
    private val discount: Double
) {
    private val state = mutableMapOf<Position, MutableMap<Direction, Double>>()

    fun qValue(position: Position, direction: Direction) : Double {
        return state.computeIfAbsent(position) {
            mutableMapOf()
        }.computeIfAbsent(direction) {
            0.0
        }
    }

    fun update(position: Position, direction: Direction) {
        val originalQ = qValue(position, direction)

        val nextPosition = position.move(direction)

        val value = if (nextPosition == goal) {
            10.0
        }
        else if (nextPosition.isWall(map)) {
            -10.0
        }
        else {
            0.0
        }

        val nextMax = Direction.values().maxOf { dir ->
            qValue(nextPosition, dir)
        }

        state[position]!![direction] = originalQ + alpha * (value + discount * nextMax - originalQ)
//        println("p:$position, q:${state[position]!![direction]}, v:$value")
    }
}

class MyGame : ApplicationAdapter() {
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var batch: SpriteBatch
    private lateinit var camera: OrthographicCamera
    private lateinit var font: BitmapFont
    private var widthOfField by Delegates.notNull<Int>()
    private var heightOfField by Delegates.notNull<Int>()
    private val map = listOf(
        listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        listOf(1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1),
        listOf(1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1),
        listOf(1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 1),
        listOf(1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1),
        listOf(1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 1),
        listOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1),
        listOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    )
    private var mapHeight: Int = 0
    private var mapWidth: Int = 0
    private val menuWidth = 128

    private val start = Position(1, 1)
    private val goal = Position(10, 1)
    private var current = start
    private val qCalc = QCalc(
        map = map,
        goal = goal,
        alpha = 0.4,
        discount = 1.0
    )
    private enum class Mode {
        LEARN, PLAY
    }
    private var mode = Mode.LEARN
    private var playFrame = 0

    override fun create() {
        shapeRenderer = ShapeRenderer()
        shapeRenderer.setAutoShapeType(true)
        batch = SpriteBatch()
        font = BitmapFont()
        font.color = Color.BLACK
        mapHeight = Gdx.graphics.height * 2
        mapWidth = Gdx.graphics.width * 2
        Gdx.graphics.setWindowedMode(mapWidth + menuWidth, mapHeight)
        widthOfField = mapWidth / map.first().size
        heightOfField = mapHeight / map.size
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.x = (Gdx.graphics.width / 2).toFloat()
        camera.position.y = (Gdx.graphics.height / 2).toFloat()
        camera.update()
        shapeRenderer.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined
    }

    private fun renderLearn() {
        repeat(32) {
            val direction = if (Random.nextInt(10) < 0) {
                Direction.values().sortedBy { Random.nextInt(Direction.values().size) }.maxByOrNull { dir ->
                    qCalc.qValue(current, dir)
                }!!
            }
            else {
                Direction.values()[Random.nextInt(Direction.values().size)]
            }
            qCalc.update(current, direction)
            val next = current.move(direction)
            if (!next.isWall(map)) {
                current = next
            }
        }
    }

    private fun renderPlay() {
        if (playFrame < 40) {
            playFrame++
            return
        }

        playFrame = 0

        if (current == goal) {
            return
        }

        val direction = Direction.values().sortedBy { Random.nextInt(Direction.values().size) }.maxByOrNull { dir ->
            qCalc.qValue(current, dir)
        }!!
        current = current.move(direction)
    }

    override fun render() {
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            mode = Mode.PLAY
            current = start
            playFrame = 0
        }

        ScreenUtils.clear(1f, 1f, 1f, 1f)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        map.forEachIndexed { y, row ->
            row.forEachIndexed { x, _ ->
                val pos = Position(x, y)
                shapeRenderer.color = if (pos == current) {
                    Color.ORANGE
                }
                else if (pos == goal) {
                    Color.YELLOW
                }
                else if (pos.isWall(map)) {
                    Color.GRAY
                }
                else{
                    Color.WHITE
                }
                val renderX = (x * widthOfField).toFloat()
                val renderY = ((map.size - y - 1) * heightOfField).toFloat()
                shapeRenderer.rect(renderX, renderY, widthOfField.toFloat(), heightOfField.toFloat())
            }
        }
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.end()

        batch.begin()
        map.forEachIndexed { y, row ->
            row.forEachIndexed { x, _ ->
                val pos = Position(x, y)
                val renderX = (x * widthOfField).toFloat()
                val renderY = ((map.size - y) * heightOfField).toFloat()
                font.draw(batch, "x:$x, y:$y", renderX + 10, renderY)
                Direction.values().forEachIndexed { index, direction ->
                    font.draw(batch,
                        "$direction: %.2f".format(qCalc.qValue(pos, direction)),
                        renderX + 10, renderY - 20 * (index + 1))
                }
            }
        }
        batch.end()

        when (mode) {
            Mode.LEARN -> renderLearn()
            Mode.PLAY -> renderPlay()
        }
    }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        font.dispose()
    }
}