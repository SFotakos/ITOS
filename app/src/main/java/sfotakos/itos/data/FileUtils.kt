package sfotakos.itos.data

import java.io.File
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import android.util.Log
import android.graphics.Bitmap
import java.io.FileOutputStream
import java.io.IOException
import android.provider.MediaStore
import android.content.ContentValues
import android.content.ContentResolver
import android.net.Uri


object FileUtils {
    private const val IMAGE_DIR = "ITOS"

    private fun getImagesDirectory() : File {
        val file =
            File(getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString() +
                    File.separator + IMAGE_DIR)
        if (!file.mkdirs() && !file.isDirectory) {
            //TODO log error
            Log.d("mkdir", "Directory not created")
        }
        return file
    }

    fun generateImagePath(title: String, date: String): File {
        return File(getImagesDirectory(), title + "_" + date + ".jpg")
    }

    fun compressAndSaveImage(file: File, bitmap: Bitmap): Boolean {
        var result = false
        try {
            val fos = FileOutputStream(file)
            result = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            if (!result) {
                //TODO log error
                Log.d("image manager", "Compression failed")
            }
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return result
    }

    fun addImageToGallery(cr: ContentResolver, title: String, description: String, date: String, filepath: File): Uri? {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, title)
        values.put(MediaStore.Images.Media.DISPLAY_NAME, title)
        values.put(MediaStore.Images.Media.DESCRIPTION, description)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
        values.put(MediaStore.Images.Media.DATE_TAKEN, date)
        values.put(MediaStore.Images.Media.DATA, filepath.toString())
        return cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }
}