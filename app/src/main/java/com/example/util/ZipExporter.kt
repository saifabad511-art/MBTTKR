package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExporter {
    fun exportCodeAsZip(context: Context, code: String, language: String) {
        try {
            val fileExtension = when (language.lowercase().trim()) {
                "kotlin", "kt" -> "kt"
                "java" -> "java"
                "html" -> "html"
                "css" -> "css"
                "javascript", "js" -> "js"
                "typescript", "ts" -> "ts"
                "json" -> "json"
                "python", "py" -> "py"
                "cpp", "c++" -> "cpp"
                "c" -> "c"
                "xml" -> "xml"
                "sql" -> "sql"
                "markdown", "md" -> "md"
                else -> "txt"
            }
            
            val innerFileName = "mubtakir_source_code.$fileExtension"
            val zipFileName = "mubtakir_project_code.zip"
            
            val cacheDir = context.cacheDir
            val zipFile = File(cacheDir, zipFileName)
            if (zipFile.exists()) {
                zipFile.delete()
            }
            
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                val zipEntry = ZipEntry(innerFileName)
                zipOut.putNextEntry(zipEntry)
                zipOut.write(code.toByteArray())
                zipOut.closeEntry()
            }
            
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                zipFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "تصدير كود $language كـ ZIP 🚀")
                putExtra(Intent.EXTRA_TEXT, "تم توليد هذا الكود البرمجي بواسطة تطبيق مبتكر (Mubtakir App)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "تحميل وحفظ ملف الكود ZIP"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل إنشاء وتصدير ملف ZIP: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
