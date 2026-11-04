package com.goodwy.messages.common.util

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object AppUpdateManager {

    private const val GITHUB_REPO = "abolfazl81079-glitch/4Messages-Apple-LiquidGlass"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    const val CURRENT_TAG = "v2.7.0-complete"

    fun checkForUpdate(activity: Activity, showUpToDateToast: Boolean = true) {
        val handler = Handler(Looper.getMainLooper())

        val checkingDialog = AlertDialog.Builder(activity)
            .setTitle("بررسی به‌روزرسانی")
            .setMessage("در حال اتصال به سرور جهت دریافت وضعیت آخرین نسخه...")
            .setCancelable(false)
            .create()
        checkingDialog.show()

        thread {
            try {
                val url = URL(LATEST_RELEASE_API)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "AB-SMS-Android-Updater")
                conn.connectTimeout = 12000
                conn.readTimeout = 12000

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val tagName = json.optString("tag_name", "")
                    val releaseName = json.optString("name", tagName)
                    val releaseBody = json.optString("body", "بهبود عملکرد، پایداری و رفع اشکالات")
                    val assets = json.optJSONArray("assets")

                    var downloadUrl: String? = null
                    var apkFileName = "AB-SMS-$tagName.apk"
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk")) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                apkFileName = name
                                break
                            }
                        }
                    }

                    handler.post {
                        try { checkingDialog.dismiss() } catch (e: Exception) {}

                        if (downloadUrl.isNullOrEmpty()) {
                            Toast.makeText(activity, "فایل نصبی (APK) در آخرین نسخه گیتهاب یافت نشد.", Toast.LENGTH_LONG).show()
                            return@post
                        }

                        // Compare release tag with current tag
                        val isNewer = tagName.isNotEmpty() && tagName != CURRENT_TAG
                        if (isNewer) {
                            showUpdatePromptDialog(activity, tagName, releaseName, releaseBody, downloadUrl, apkFileName)
                        } else {
                            if (showUpToDateToast) {
                                AlertDialog.Builder(activity)
                                    .setTitle("برنامه به‌روز است")
                                    .setMessage("شما از آخرین نسخه برنامه استفاده می‌کنید.\nنسخه فعلی: $CURRENT_TAG")
                                    .setPositiveButton("تأیید", null)
                                    .setNeutralButton("دانلود مجدد فایل نصبی") { _, _ ->
                                        downloadAndInstallApk(activity, downloadUrl, apkFileName)
                                    }
                                    .show()
                            }
                        }
                    }
                } else {
                    handler.post {
                        try { checkingDialog.dismiss() } catch (e: Exception) {}
                        Toast.makeText(activity, "پاسخ از سرور دریافت نشد (کد خطا: $responseCode)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    try { checkingDialog.dismiss() } catch (e: Exception) {}
                    Toast.makeText(activity, "خطا در بررسی به‌روزرسانی: لطفاً اینترنت خود را چک کنید.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showUpdatePromptDialog(
        activity: Activity,
        tagName: String,
        name: String,
        body: String,
        downloadUrl: String,
        apkFileName: String
    ) {
        AlertDialog.Builder(activity)
            .setTitle("نسخه جدید AB-SMS در دسترس است! 🎉")
            .setMessage("نسخه: $tagName\nعنوان: $name\n\nتغییرات:\n$body\n\nآیا مایل به دریافت و نصب خودکار هستید؟")
            .setPositiveButton("دانلود و نصب خودکار") { _, _ ->
                downloadAndInstallApk(activity, downloadUrl, apkFileName)
            }
            .setNegativeButton("بعداً", null)
            .show()
    }

    private fun downloadAndInstallApk(activity: Activity, downloadUrl: String, apkFileName: String) {
        val handler = Handler(Looper.getMainLooper())
        val progressDialog = ProgressDialog(activity).apply {
            setTitle("در حال دانلود نسخه جدید AB-SMS")
            setMessage("لطفاً شکیبا باشید...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            max = 100
            progress = 0
            setCancelable(false)
            show()
        }

        thread {
            try {
                var currentUrl = downloadUrl
                var conn = URL(currentUrl).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "AB-SMS-Android-Updater")

                var redirect = true
                var redirectsCount = 0
                while (redirect && redirectsCount < 6) {
                    val status = conn.responseCode
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER ||
                        status == 307 || status == 308
                    ) {
                        currentUrl = conn.getHeaderField("Location")
                        conn.disconnect()
                        conn = URL(currentUrl).openConnection() as HttpURLConnection
                        conn.instanceFollowRedirects = true
                        conn.setRequestProperty("User-Agent", "AB-SMS-Android-Updater")
                        redirectsCount++
                    } else {
                        redirect = false
                    }
                }

                val contentLength = conn.contentLength
                val cacheDir = File(activity.cacheDir, "updates").apply { if (!exists()) mkdirs() }
                val apkFile = File(cacheDir, apkFileName)

                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                val progress = (totalRead * 100 / contentLength).toInt()
                                handler.post { progressDialog.progress = progress }
                            }
                        }
                    }
                }

                handler.post {
                    try { progressDialog.dismiss() } catch (e: Exception) {}
                    promptInstallApk(activity, apkFile)
                }
            } catch (e: Exception) {
                handler.post {
                    try { progressDialog.dismiss() } catch (e: Exception) {}
                    Toast.makeText(activity, "خطا در دانلود فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun promptInstallApk(activity: Activity, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(activity, "فایل دانلود شده یافت نشد!", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                AlertDialog.Builder(activity)
                    .setTitle("نیاز به اجازه نصب")
                    .setMessage("برای نصب نسخه جدید، لطفاً اجازه نصب از منابع ناشناخته را برای AB-SMS فعال کنید.")
                    .setPositiveButton("ورود به تنظیمات") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                        activity.startActivity(intent)
                    }
                    .setNegativeButton("انصراف", null)
                    .show()
                return
            }
        }

        try {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "خطا در اجرای بسته نصبی: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
