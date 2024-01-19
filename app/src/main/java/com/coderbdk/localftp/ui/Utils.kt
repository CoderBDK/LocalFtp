package com.coderbdk.localftp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import com.coderbdk.localftp.ui.MainActivity


/**
 * Created by MD. ABDULLAH on Fri, Jan 19, 2024.
 */
class Utils {
    fun takePermission(activity: MainActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent12 =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent12.addCategory("android.intent.category.DEFAULT")
                val uri = Uri.fromParts("package", "com.coderbdk.localftp", null)
                intent12.setData(uri)
               startActivityForResult(activity,intent12, 11, null)
            } catch (e: Exception) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(activity,intent, 11, null)
            }
        } else {
            ActivityCompat.requestPermissions(
               activity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE),
                11
            )
        }
    }

    fun alertPermission(mainActivity: MainActivity) {
        Toast.makeText(mainActivity, "Without permission the server can run but cannot write through uploaded files!", Toast.LENGTH_SHORT).show()
    }
}