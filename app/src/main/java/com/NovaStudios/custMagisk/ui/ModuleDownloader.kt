package com.NovaStudios.custMagisk.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object ModuleDownloader {
    fun enqueue(context: Context, url: String): Long {
        val req = DownloadManager.Request(Uri.parse(url))
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CustMagisk.zip")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(req)
    }
}