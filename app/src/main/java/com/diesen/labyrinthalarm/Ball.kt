package com.diesen.labyrinthalarm

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Ball {
    var mX = 0
    var mY = 0
    var mBallPaint: Paint = Paint().apply {
        color = Color.rgb(125,125,125)
        isAntiAlias = true
    }
    var mRadius = 0

    fun setPosition(x: Int, y: Int) {
        mX = x
        mY = y
    }

    fun draw(canvas: Canvas){
        canvas.drawCircle(mX.toFloat(),mY.toFloat(),mRadius.toFloat(),mBallPaint)
    }

    fun move(dx:Int, dy:Int){
        mX += dx
        mY += dy
    }
}