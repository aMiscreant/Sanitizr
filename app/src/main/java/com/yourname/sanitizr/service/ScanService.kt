package com.yourname.sanitizr.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ScanService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
