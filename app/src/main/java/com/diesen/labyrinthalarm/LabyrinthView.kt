package com.diesen.labyrinthalarm

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.media.MediaPlayer
import android.os.Handler
import android.view.View

class LabyrinthView(context: Context?) : View(context), SensorEventListener {
    //ゲームの状態
    val NULL_STATE = -1
    val GAME_INIT = 0
    val GAME_RUNNING = 1
    val GAME_OVER = 2
    val GAME_COMPLETE = 3
    var mGameState = NULL_STATE

    //表示用テキスト
    var array = context?.resources?.getStringArray(R.array.game_strings)
    private val TXT_GAME_INIT: String? = array?.get(0)
    private val TXT_GAME_OVER: String? = array?.get(1)
    private val TXT_GAME_COMPLETE: String? = array?.get(2)
    private val TXT_TOTAL_TIME: String? = array?.get(3)
    private val TXT_RESTART: String? = array?.get(4)

    //アラーム音再生用Player
    private var alarmPlayer: MediaPlayer? = null

    //テキスト用Paint
    private val mBgPaint = Paint().apply {
        color = Color.rgb(255,190,30)
    }
    private val mTxtPaintOverlay = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 40.0f
    }
    private val mTxtPaintResult = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 80.0f
    }
    //時間
    private var mStartTime: Long = 0
    private var mTotalTime: Long = 0

    //壁
    private val WALL = Labyrinth.WALL_TILE
    //反発係数
    private val REBOUND = 0.3f

    private var mLabyrinth = Labyrinth()
    private var mWidth = 0
    private var mHeight = 0
    private var mBall = Ball()

    //ローパスフィルタ係数
    private val FILTER_FACTOR = 0.2f
    //端末の加速度
    private var mAccelX = 0.0f
    private var mAccelY = 0.0f
    //ボールの加速度
    private var mVectorX = 0.0f
    private var mVectorY = 0.0f

    //ゲームループ用
    private var mHandler = Handler()
    private var mDrawThread = Runnable {gameLoop()}


    //ゲームの初期化
    fun initGame() {
        mGameState = GAME_INIT
        mHandler.removeCallbacks(mDrawThread)
        mTotalTime = 0
        mBall.setPosition(mBall.mRadius * 6, mBall.mRadius * 6)
        invalidate()

        if(alarmPlayer == null) {
            alarmPlayer = MediaPlayer.create(context, R.raw.piano).apply { isLooping = true }
            alarmPlayer?.start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = w
        mHeight = h
        mLabyrinth.setSize(w, h)
        mBall.mRadius = w / (2 * Labyrinth.MAZE_COLS)
        initGame()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null) return

        //ゲーム中
        if (mGameState == GAME_RUNNING) {
            mLabyrinth.draw(canvas)
            mBall.draw(canvas)
            return
        }

        //ゲーム中以外
        when (mGameState) {
            GAME_INIT -> {
                mLabyrinth.draw(canvas)
                mBall.draw(canvas)
                canvas.drawText(TXT_GAME_INIT, (mWidth / 2).toFloat(), (mHeight / 2).toFloat(), mTxtPaintOverlay)
            }
            GAME_OVER -> {
                mLabyrinth.draw(canvas)
                canvas.drawText(TXT_GAME_OVER, (mWidth / 2).toFloat(), (mHeight / 2).toFloat(), mTxtPaintOverlay)
            }
            GAME_COMPLETE -> {
                canvas.drawRect(0.0f, 0.0f, mWidth.toFloat(), mHeight.toFloat(), mBgPaint)
                canvas.drawText(TXT_GAME_COMPLETE, (mWidth / 2).toFloat(), (mHeight / 2).toFloat(), mTxtPaintResult)
                canvas.drawText("$TXT_TOTAL_TIME: $mTotalTime ms", (mWidth / 2).toFloat(), mHeight / 2 + mTxtPaintResult.fontSpacing, mTxtPaintResult)
                canvas.drawText(TXT_RESTART, (mWidth / 2).toFloat(), mHeight - mTxtPaintResult.fontSpacing * 3, mTxtPaintResult)
            }
        }
    }

    fun setLabyrinthData(data:Array<IntArray>){
        mLabyrinth.mData = data
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //何もしない
    }

    override fun onSensorChanged(event: SensorEvent?) {
        synchronized(this){
            if (event != null){
                mAccelX = (mAccelX * FILTER_FACTOR) + (event.values[0] * 0.5f * (1.0f - FILTER_FACTOR))
                mAccelY = (mAccelY * FILTER_FACTOR) + (event.values[1] * 0.5f * (1.0f - FILTER_FACTOR))
            }
        }
    }

    //ゲーム実行中のループ
    private fun gameLoop() {
        mVectorX -= mAccelX
        mVectorY += mAccelY
        val nextX = (mBall.mX + mVectorX).toInt()
        val nextY = (mBall.mY + mVectorY).toInt()
        val radius = mBall.mRadius

        if (nextX - radius < 0) mVectorX *= -REBOUND
        if (nextX + radius > mWidth) mVectorX *= -REBOUND
        if (nextY - radius < 0) mVectorY *= -REBOUND
        if (nextY + radius > mHeight) mVectorY *= -REBOUND

        if (nextY + radius > mHeight) mVectorY *= -REBOUND
        if (radius < nextX && nextX < mWidth - radius && radius < nextY && nextY < mHeight - radius) {
            //壁の当たり判定
            val ul = mLabyrinth.getCellType(nextX - radius, nextY - radius)
            val ur = mLabyrinth.getCellType(nextX + radius, nextY - radius)
            val dl = mLabyrinth.getCellType(nextX - radius, nextY + radius)
            val dr = mLabyrinth.getCellType(nextX + radius, nextY + radius)
            if (ul != WALL && ur != WALL && dl != WALL && dr != WALL) {
            } else if (ul != WALL && ur == WALL && dl != WALL && dr == WALL) {
                mVectorX *= -REBOUND
            } else if (ul == WALL && ur != WALL && dl == WALL && dr != WALL) {
                mVectorX *= -REBOUND
            } else if (ul == WALL && ur == WALL && dl != WALL && dr != WALL) {
                mVectorY *= -REBOUND
            } else if (ul != WALL && ur != WALL && dl == WALL && dr == WALL) {
                mVectorY *= -REBOUND
            } else if (ul == WALL && ur != WALL && dl != WALL && dr != WALL) {
                if (mVectorX < 0.0f && mVectorY > 0.0f) {
                    mVectorX *= -REBOUND
                } else if (mVectorX > 0.0f && mVectorY < 0.0f) {
                    mVectorY *= -REBOUND
                } else {
                    mVectorX *= -REBOUND
                    mVectorY *= -REBOUND
                }
            } else if (ul != WALL && ur == WALL && dl != WALL && dr != WALL) {
                if (mVectorX > 0.0f && mVectorY > 0.0f) {
                    mVectorX *= -REBOUND
                } else if (mVectorX < 0.0f && mVectorY < 0.0f) {
                    mVectorY *= -REBOUND
                } else {
                    mVectorX *= -REBOUND
                    mVectorY *= -REBOUND
                }
            } else if (ul != WALL && ur != WALL && dl == WALL && dr != WALL) {
                if (mVectorX > 0.0f && mVectorY > 0.0f) {
                    mVectorY *= -REBOUND
                } else if (mVectorX < 0.0f && mVectorY < 0.0f) {
                    mVectorX *= -REBOUND
                } else {
                    mVectorX *= -REBOUND
                    mVectorY *= -REBOUND
                }
            } else if (ul != WALL && ur != WALL && dl != WALL && dr == WALL) {
                if (mVectorX < 0.0f && mVectorY > 0.0f) {
                    mVectorY *= -REBOUND
                } else if (mVectorX > 0.0f && mVectorY < 0.0f) {
                    mVectorX *= -REBOUND
                } else {
                    mVectorX *= -REBOUND
                    mVectorY *= -REBOUND
                }
            } else {
                mVectorX *= -REBOUND
                mVectorY *= -REBOUND
            }
            //穴の判定
            if (mLabyrinth.getCellType(nextX, nextY) == Labyrinth.VOID_TILE) {
                stopGame()
            }
            //ゴール判定
            if (mLabyrinth.getCellType(nextX, nextY) == Labyrinth.EXIT_TILE) {
                completeGame()
            }
        }

        mBall.move(mVectorX.toInt(),mVectorY.toInt())
        invalidate()
        if (mGameState == GAME_RUNNING){
            mHandler.removeCallbacks(mDrawThread)
            mHandler.postDelayed(mDrawThread, 30)
        }
    }

    //ゲームを開始
    fun startGame() {
        mGameState = GAME_RUNNING
        mHandler.post(mDrawThread)
        mBall.setPosition(mBall.mRadius * 6, mBall.mRadius * 6)
        mVectorX = 0.0f
        mVectorY = 0.0f
        mStartTime = System.currentTimeMillis()
    }

    //ゲームの中断(穴に落下時)
    fun stopGame() {
        mGameState = GAME_OVER
        mHandler.removeCallbacks(mDrawThread)
        mTotalTime += System.currentTimeMillis() - mStartTime
    }

    //ゲームの完遂(ゴールに到達時)
    fun completeGame() {
        alarmPlayer?.stop()
        mGameState = GAME_COMPLETE

        val data:SharedPreferences = context.getSharedPreferences("DataSave", Context.MODE_PRIVATE)
        val editor = data.edit()
        editor.putString("State", "stop")
        editor.apply()

        mHandler.removeCallbacks(mDrawThread)
        mTotalTime += System.currentTimeMillis() - mStartTime
    }
}