package com.diesen.labyrinthalarm

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.properties.Delegates



class Labyrinth {
    companion object {
        const val MAZE_ROWS = 33
        const val MAZE_COLS = 20
        const val PATH_TILE = 0
        const val WALL_TILE = 1
        const val EXIT_TILE = 2
        const val VOID_TILE = 3
    }

    var mData = Array(MAZE_ROWS) {IntArray(MAZE_COLS,{0})}

    private var mTileWidth: Int by Delegates.notNull()
    private var mTileHeight: Int by Delegates.notNull()

    private var mPathPaint = Paint().apply { color = Color.rgb(155, 220, 255) }
    private var mWallPaint = Paint().apply { color = Color.rgb(102, 54, 255) }
    private var mExitPaint = Paint().apply { color = Color.rgb(255,190,30) }
    private var mVoidPaint = Paint().apply { color = Color.BLACK }

    fun setSize(w: Int, h: Int) {
        mTileWidth = w / MAZE_COLS
        mTileHeight = h / MAZE_ROWS
    }

    fun draw(canvas: Canvas) {
        for (row in 0 until MAZE_ROWS) {
            for (col in 0 until MAZE_COLS) {
                when (mData[row][col]) {
                    PATH_TILE,null -> canvas.drawRect((col * mTileWidth).toFloat(), (row * mTileHeight).toFloat(),
                            ((col + 1) * mTileWidth).toFloat(), ((row + 1) * mTileHeight).toFloat(), mPathPaint)
                    WALL_TILE -> canvas.drawRect((col * mTileWidth).toFloat(), (row * mTileHeight).toFloat(),
                            ((col + 1) * mTileWidth).toFloat(), ((row + 1) * mTileHeight).toFloat(), mWallPaint)
                    EXIT_TILE -> canvas.drawRect((col * mTileWidth).toFloat(), (row * mTileHeight).toFloat(),
                            ((col + 1) * mTileWidth).toFloat(), ((row + 1) * mTileHeight).toFloat(), mExitPaint)
                    VOID_TILE -> canvas.drawRect((col * mTileWidth).toFloat(), (row * mTileHeight).toFloat(),
                            ((col + 1) * mTileWidth).toFloat(), ((row + 1) * mTileHeight).toFloat(), mVoidPaint)
                }
            }
        }
    }

    fun getCellType(x: Int, y: Int): Int {
        val j = (x / mTileWidth)
        val i = (y / mTileHeight)
        return if (i < MAZE_ROWS && j < MAZE_COLS) mData[i][j] else PATH_TILE
    }
}