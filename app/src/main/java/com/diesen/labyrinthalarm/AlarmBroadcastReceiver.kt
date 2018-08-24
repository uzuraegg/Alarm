package com.diesen.labyrinthalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlarmBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Toast.makeText(context, "アラームを受信しました", Toast.LENGTH_SHORT).show()
        val settingIntent = Intent(context, SettingActivity::class.java)
                .putExtra("onReceive", true)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(settingIntent)
    }
}