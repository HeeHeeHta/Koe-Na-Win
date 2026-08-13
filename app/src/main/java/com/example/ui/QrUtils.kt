package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

object QrUtils {
    fun generateQrCode(text: String, size: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveQrToGallery(context: Context, bitmap: Bitmap, filename: String = "kbzpay_qr"): Boolean {
        var success = false
        val resolver = context.contentResolver
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KoeNaWin")
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    resolver.openOutputStream(imageUri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        success = true
                    }
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val myDir = File(imagesDir, "KoeNaWin")
                if (!myDir.exists()) {
                    myDir.mkdirs()
                }
                val file = File(myDir, "$filename.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    success = true
                    // Update gallery scanner
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        if (success) {
            Toast.makeText(context, "QR ပုံရိပ်အား Gallery ထဲသို့ သိမ်းဆည်းပြီးပါပြီ 🖼️", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "သိမ်းဆည်းမှု မအောင်မြင်ပါ", Toast.LENGTH_SHORT).show()
        }
        return success
    }

    fun shareQrImage(context: Context, bitmap: Bitmap, filename: String = "kbzpay_qr") {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "$filename.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.koenawin.chntr.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "QR မျှဝေရန်"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "မျှဝေမှု မအောင်မြင်ပါ", Toast.LENGTH_SHORT).show()
        }
    }
}
