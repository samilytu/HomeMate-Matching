package com.samilozturk.homemate_matching.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.provider.OpenableColumns
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

@ColorInt
fun Context.themeColor(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}

fun Activity.snackbar(message: String, isError: Boolean = false) {
    val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
    if (isError) {
        snackbar.setBackgroundTint(themeColor(androidx.appcompat.R.attr.colorError))
    }
    snackbar.show()
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

@SuppressLint("Range")
fun Uri.getFileName(contentResolver: ContentResolver): String {
    val cursor = contentResolver.query(this, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayName = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            return displayName!!
        }
    }
    return this.pathSegments.last()
}

fun Bitmap.toFile(file: File): File {
    val os = BufferedOutputStream(FileOutputStream(file))
    this.compress(Bitmap.CompressFormat.JPEG, 100, os)
    os.close()
    return file
}

fun Bitmap.toUri(context: Context): Uri {
    val tempFile = File(context.cacheDir, "Bitmap${System.currentTimeMillis()}")
    return this.toFile(tempFile).toUri()
}

fun MarkerOptions.icon(context: Context, resId: Int): MarkerOptions = apply {
    val vectorDrawable = AppCompatResources.getDrawable(context, resId)!!
    vectorDrawable.setBounds(
        0,
        0,
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight
    )
    val bitmap = Bitmap.createBitmap(
        vectorDrawable.intrinsicWidth,
        vectorDrawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    vectorDrawable.draw(canvas)
    icon(BitmapDescriptorFactory.fromBitmap(bitmap))
}

fun Context.openEmail(email: String) {
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "text/plain"
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
    startActivity(Intent.createChooser(intent, "E-posta gönder"))
}

fun Context.openWhatsapp(phone: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    val phone = phone.replace(" ", "").replace("+", "")
    intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phone")
    startActivity(intent)
}