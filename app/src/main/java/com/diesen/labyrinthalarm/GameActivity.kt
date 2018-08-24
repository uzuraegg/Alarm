package com.diesen.labyrinthalarm

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import net.nend.android.NendAdInterstitial
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Random
import java.util.StringTokenizer


class GameActivity : AppCompatActivity() {
    private lateinit var mLabyrinthView: LabyrinthView
    private lateinit var mSensorManager: SensorManager

    // 端末音量制御用AudioManager
    private lateinit var am: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data: SharedPreferences = this.getSharedPreferences("DataSave", Context.MODE_PRIVATE)
        val editor = data.edit()
        editor.putString("State", "run")
        editor.apply()

        NendAdInterstitial.loadAd(applicationContext, "8c278673ac6f676dae60a1f56d16dad122e23516", 213206)

        hideNavi()

        mLabyrinthView = LabyrinthView(this)
        setContentView(mLabyrinthView)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        am = applicationContext?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        //ゲームレベルをセット
        mLabyrinthView.setLabyrinthData(loadLabyrinth(Random().nextInt(3)+1))
    }

    override fun onPause() {
        super.onPause()
        //センサーリスナーの解除
        mSensorManager.unregisterListener(mLabyrinthView)
    }

    override fun onResume() {
        super.onResume()

        //ゲームを初期化
        mLabyrinthView.initGame()

        //センサーリスナーの登録
        val accele = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mSensorManager.registerListener(mLabyrinthView, accele, SensorManager.SENSOR_DELAY_GAME)

        hideNavi()

        //音量制御
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 10, 0)

        // スリープ無効化
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    //ナビゲーションバー非表示&フルスクリーン
    private fun hideNavi(){
        val view = window.decorView
        view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onBackPressed() {
        if (mLabyrinthView.mGameState == mLabyrinthView.GAME_COMPLETE) {
            super.onBackPressed()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (mLabyrinthView.mGameState) {
                mLabyrinthView.GAME_INIT -> mLabyrinthView.startGame()
                mLabyrinthView.GAME_OVER -> mLabyrinthView.startGame()
                mLabyrinthView.GAME_COMPLETE -> {
                    NendAdInterstitial.showAd(this)
                    finish()
                }
            }
        }
        return true
    }

    private fun loadLabyrinth(level: Int): Array<IntArray> {
        val fileName = "stage$level.txt"
        val mazeRows = Labyrinth.MAZE_ROWS
        val mazeCols = Labyrinth.MAZE_COLS
        val data = Array(mazeRows) { IntArray(mazeCols) }
        var `is`: InputStream? = null
        try {
            `is` = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(`is`))
            var line: String
            var i = 0
            while (i < mazeRows) {
                line = reader.readLine()
                if (line == null)
                    break
                val st = StringTokenizer(line, ",")
                var j = 0
                while (st.hasMoreTokens() && j < mazeCols) {
                    //空白があれば除去
                    val s = st.nextToken().trim()
                    data[i][j] = Integer.parseInt(s)
                    j++
                }
                i++
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                }
            }
        }
        return data
    }
}
