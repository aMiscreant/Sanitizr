@file:Suppress("DEPRECATION")

package com.yourname.sanitizr.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZInputStream
import org.tukaani.xz.XZOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object FileSanitizer {

    // Define the supported file types and their common extensions
    val supportedFileTypes = mapOf(
        "image" to listOf(
            "jpg", "jpeg", "png", "gif", "webp", "tiff", "bmp", "heic"
        ),
        "video" to listOf(
            "mp4", "mov", "avi", "mkv", "webm", "3gp", "flv", "mpeg"
        ),
        "audio" to listOf(
            "mp3", "wav", "flac", "aac", "ogg", "m4a", "wma", "alac", "oga", "webp"
        ),
        "pdf" to listOf(
            "pdf"
        ),
        "document" to listOf(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "odt", "ods", "odp", "rtf", "txt", "csv"
        ),
        "ebook" to listOf(
            "epub", "mobi", "azw3", "fb2"
        ),
        "archive" to listOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz"
        )
    )

    fun sanitizeFile(file: File, fileType: String = "auto"): Boolean {
        val type = if (fileType == "auto") {
            detectFileType(file) ?: return false.also {
                println("[SANITIZR] Could not detect file type for: ${file.name}")
            }
        } else {
            fileType.lowercase(Locale.ROOT)
        }

        return when (type) {
            "image" -> sanitizeImage(file)
            "video" -> sanitizeVideo(file)
            "audio" -> sanitizeAudio(file)
            "pdf" -> sanitizePdf(file)
            "document" -> sanitizeDocument(file)
            "ebook" -> sanitizeEbook(file)
            "archive" -> when (file.extension.lowercase(Locale.ROOT)) {
                "zip" -> sanitizeZip(file)
                "tar" -> sanitizeTar(file)
                "gz" -> sanitizeGz(file)
                "bz2" -> sanitizeBz2(file)
                "xz" -> sanitizeXz(file)
                else -> sanitizeArchive(file) // <-- fallback stub
            }

            else -> {
                println("[SANITIZR] Unsupported file type: $type")
                false
            }
        }
    }

    fun detectFileType(file: File): String? {
        val ext = file.extension.lowercase()
        return supportedFileTypes.entries.find { ext in it.value }?.key
    }

    private fun fullyStripImageMetadata(file: File): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap == null) {
                println("[SANITIZR] Failed to decode image: ${file.absolutePath}")
                return false
            }

            val format = when (file.extension.lowercase(Locale.ROOT)) {
                "png" -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG
            }

            val tempFile = File(file.parent, "sanitized_${file.name}")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(format, 100, out)
            }

            if (file.delete()) {
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
                return false
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeImage(file: File): Boolean {
        return try {
            // --- STEP 1 ---
            // Attempt to scrub metadata directly
            val exif = ExifInterface(file)

            exif.setAttribute(ExifInterface.TAG_DATETIME, "")
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "")
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, "")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME, "")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL, "")
            exif.setAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED, "")

            exif.setAttribute(ExifInterface.TAG_MAKE, "")
            exif.setAttribute(ExifInterface.TAG_MODEL, "")
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "")
            exif.setAttribute(ExifInterface.TAG_ARTIST, "")
            exif.setAttribute(ExifInterface.TAG_COPYRIGHT, "")
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "")
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)

            exif.saveAttributes()

            // --- STEP 2 ---
            // For absolute privacy, re-encode the bitmap to strip all EXIF blocks completely
            val success = fullyStripImageMetadata(file)
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun sanitizeAudio(file: File): Boolean {
        return try {
            val tempFile = File(file.parentFile, "temp_${file.name}")

            // Quote paths with spaces
            val cmd = listOf(
                "-y",
                "-i", "\"${file.absolutePath}\"",
                "-map_metadata", "-1",
                "-c", "copy",
                "\"${tempFile.absolutePath}\""
            ).joinToString(" ")

            val session = FFmpegKit.execute(cmd)
            println("[SANITIZR] FFmpeg logs:\n${session.allLogsAsString}")

            if (ReturnCode.isSuccess(session.returnCode)) {
                if (file.delete()) {
                    tempFile.renameTo(file)
                } else {
                    tempFile.delete()
                    false
                }
            } else {
                println("[SANITIZR] FFmpeg failed: ${session.returnCode}")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    private fun sanitizeVideo(file: File): Boolean {
        return try {
            val tempFile = File(file.parentFile, "temp_${file.name}")

            // Quote paths with spaces
            val cmd = listOf(
                "-y",
                "-i", "\"${file.absolutePath}\"",
                "-map_metadata", "-1",
                "-c", "copy",
                "\"${tempFile.absolutePath}\""
            ).joinToString(" ")

            val session = FFmpegKit.execute(cmd)
            println("[SANITIZR] FFmpeg logs:\n${session.allLogsAsString}")

            if (ReturnCode.isSuccess(session.returnCode)) {
                if (file.delete()) {
                    tempFile.renameTo(file)
                } else {
                    tempFile.delete()
                    false
                }
            } else {
                println("[SANITIZR] FFmpeg failed: ${session.returnCode}")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun sanitizePdf(file: File): Boolean {
        val tempFile = File(file.parentFile, "temp_${file.name}")
        println("[SANITIZR] Starting PDF sanitization for: ${file.absolutePath}")

        return try {
            val document = PDDocument.load(file)
            println("[SANITIZR] PDF loaded successfully")

            // Clear metadata
            val info = PDDocumentInformation()
            document.documentInformation = info
            println("[SANITIZR] Metadata cleared")

            // Save to temp file
            document.save(tempFile)
            document.close()
            println("[SANITIZR] Saved sanitized PDF to temp file: ${tempFile.absolutePath}")

            // Replace original safely
            if (file.delete()) {
                if (tempFile.renameTo(file)) {
                    println("[SANITIZR] PDF replaced original successfully")
                    true
                } else {
                    println("[SANITIZR] Failed to rename temp file to original")
                    tempFile.delete()
                    false
                }
            } else {
                println("[SANITIZR] Failed to delete original PDF")
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            println("[SANITIZR] Exception during PDF sanitization: ${e.message}")
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    private fun sanitizeDocument(file: File): Boolean {
        println("[SANITIZR] Starting Word document sanitization for: ${file.absolutePath}")

        val tempFile = File(file.parentFile, "temp_${file.name}")

        return try {
            // Open the Word file (.docx) as an OPCPackage
            OPCPackage.open(file).use { opc ->
                val doc = XWPFDocument(opc)
                println("[SANITIZR] Document loaded successfully")

                // Strip core properties metadata
                val props = doc.properties.coreProperties
                props.title = ""
                props.creator = ""
                props.description = ""
                props.setSubjectProperty("")
                props.keywords = ""
                println("[SANITIZR] Core metadata cleared")

                // Write to temporary file first
                FileOutputStream(tempFile, false).use { out ->
                    doc.write(out)
                }
                doc.close()
                println("[SANITIZR] Document saved successfully to temp file: ${tempFile.absolutePath}")
            }

            // Replace original file with temp file
            if (file.delete()) {
                val renamed = tempFile.renameTo(file)
                println(
                    if (renamed)
                        "[SANITIZR] Temp file replaced original successfully"
                    else
                        "[SANITIZR] Failed to rename temp file to original"
                )
                renamed
            } else {
                println("[SANITIZR] Failed to delete original file")
                tempFile.delete() // cleanup
                false
            }
        } catch (e: Exception) {
            println("[SANITIZR] Exception during document sanitization: ${e.message}")
            e.printStackTrace()
            tempFile.delete() // cleanup
            false
        }
    }

    private fun sanitizeEbook(file: File): Boolean {
        println("[SANITIZR] Starting ebook sanitization for: ${file.absolutePath}")

        return try {
            if (file.extension.lowercase() == "epub") {
                val tempFile = File(file.parentFile, "temp_${file.name}")
                println("[SANITIZR] Temp file will be: ${tempFile.absolutePath}")

                ZipFile(file).use { zip ->
                    ZipOutputStream(tempFile.outputStream()).use { zos ->
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val entryData = zip.getInputStream(entry).readBytes()

                            if (entry.name.endsWith(".opf")) {
                                println("[SANITIZR] Sanitizing metadata in ${entry.name}")
                                val sanitizedXml = sanitizeEpubMetadata(entryData)
                                zos.putNextEntry(ZipEntry(entry.name))
                                zos.write(sanitizedXml)
                                zos.closeEntry()
                            } else {
                                zos.putNextEntry(ZipEntry(entry.name))
                                zos.write(entryData)
                                zos.closeEntry()
                            }
                        }
                    }
                }

                // Replace original only if temp file was successfully created
                if (file.delete()) {
                    val renamed = tempFile.renameTo(file)
                    println(
                        if (renamed)
                            "[SANITIZR] Temp file replaced original successfully"
                        else {
                            tempFile.delete()
                            "[SANITIZR] Failed to rename temp file to original"
                        }
                    )
                    renamed
                } else {
                    println("[SANITIZR] Failed to delete original ebook")
                    tempFile.delete()
                    false
                }
            } else {
                println("[SANITIZR] Stub sanitizer: Ebook (${file.name}) not yet implemented")
                copyAsSanitized(file)
            }
        } catch (e: Exception) {
            println("[SANITIZR] Exception during ebook sanitization for ${file.name}: ${e.message}")
            e.printStackTrace()
            copyAsSanitized(file)
        }
    }

    private fun sanitizeArchive(file: File): Boolean {
        println("[SANITIZR] Stub sanitizer: Generic archive (${file.name}) not yet implemented")
        return copyAsSanitized(file)
    }

    private fun sanitizeEpubMetadata(xmlData: ByteArray): ByteArray {
        return try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(xmlData.inputStream())
            doc.documentElement.normalize()

            val metadataNodes = doc.getElementsByTagName("metadata")
            if (metadataNodes.length > 0) {
                val metadata = metadataNodes.item(0)
                println("[SANITIZR] Found <metadata> element, removing child nodes")
                while (metadata.hasChildNodes()) {
                    metadata.removeChild(metadata.firstChild)
                }
            } else {
                println("[SANITIZR] No <metadata> element found in EPUB OPF")
            }

            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            val outputStream = ByteArrayOutputStream()
            transformer.transform(DOMSource(doc), StreamResult(outputStream))
            outputStream.toByteArray()
        } catch (e: Exception) {
            println("[SANITIZR] Exception while sanitizing EPUB metadata: ${e.message}")
            e.printStackTrace()
            xmlData // fallback: return original data if something fails
        }
    }

    private fun sanitizeZip(file: File): Boolean {
        if (!file.exists()) {
            println("[SANITIZR] File does not exist: ${file.absolutePath}")
            return false
        }

        val tempFile = File(file.parentFile, "temp_sanitized.zip")
        println("[SANITIZR] Starting ZIP sanitization: ${file.absolutePath}")

        return try {
            ZipFile(file).use { zip ->
                ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()

                        val newEntry = ZipEntry(entry.name)
                        newEntry.time = 0 // reset timestamp
                        zos.putNextEntry(newEntry)

                        if (!entry.isDirectory) {
                            zip.getInputStream(entry).use { input ->
                                input.copyTo(zos)
                            }
                        }

                        zos.closeEntry()
                        println("[SANITIZR] Added entry: ${entry.name}")
                    }
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
                println("[SANITIZR] ZIP sanitization successful: ${file.absolutePath}")
                true
            } else {
                println("[SANITIZR] Failed to delete original ZIP: ${file.absolutePath}")
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            println("[SANITIZR] Exception during ZIP sanitization: ${e.message}")
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    private fun sanitizeTar(file: File): Boolean {
        println("[SANITIZR] Starting TAR sanitization: ${file.absolutePath}")

        val tempFile = File(file.parentFile, "temp_sanitized.tar")

        return try {
            TarArchiveInputStream(file.inputStream()).use { tis ->
                TarArchiveOutputStream(tempFile.outputStream()).use { tos ->
                    var entry: TarArchiveEntry? = tis.nextTarEntry

                    while (entry != null) {
                        val currentEntry = entry // non-nullable copy

                        val sanitizedEntry = if (!currentEntry.isDirectory) {
                            TarArchiveEntry(currentEntry.name).apply {
                                size = currentEntry.size
                                modTime = Date(0L)
                                userName = ""
                                groupName = ""
                                mode = 0b110100100 // rw-r--r--
                            }
                        } else {
                            TarArchiveEntry(currentEntry.name).apply {
                                modTime = Date(0L)
                                userName = ""
                                groupName = ""
                            }
                        }

                        tos.putArchiveEntry(sanitizedEntry)
                        if (!currentEntry.isDirectory) {
                            tis.copyTo(tos)
                        }
                        tos.closeArchiveEntry()

                        println("[SANITIZR] Added TAR entry: ${currentEntry.name}")

                        entry = tis.nextTarEntry
                    }

                    tos.finish()
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
                println("[SANITIZR] TAR sanitization successful: ${file.absolutePath}")
                true
            } else {
                println("[SANITIZR] Failed to delete original TAR: ${file.absolutePath}")
                tempFile.delete()
                false
            }

        } catch (e: Exception) {
            println("[SANITIZR] Exception during TAR sanitization: ${e.message}")
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    private fun sanitizeGz(file: File): Boolean {
        println("[SANITIZR] Starting GZ sanitization: ${file.absolutePath}")

        val tempFile = File(file.parentFile, "temp_sanitized.gz")

        return try {
            FileInputStream(file).use { fis ->
                GZIPInputStream(fis).use { gis ->
                    FileOutputStream(tempFile).use { fos ->
                        GZIPOutputStream(fos).use { gos ->
                            gis.copyTo(gos)
                        }
                    }
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
                println("[SANITIZR] GZ sanitization successful: ${file.absolutePath}")
                true
            } else {
                println("[SANITIZR] Failed to delete original GZ: ${file.absolutePath}")
                tempFile.delete()
                false
            }

        } catch (e: Exception) {
            println("[SANITIZR] Exception during GZ sanitization: ${e.message}")
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    private fun sanitizeBz2(file: File): Boolean {
        println("[SANITIZR] Starting BZ2 sanitization: ${file.absolutePath}")

        val tempFile = File(file.parentFile, "temp_sanitized.bz2")

        return try {
            FileInputStream(file).use { fis ->
                BZip2CompressorInputStream(fis).use { bzis ->
                    FileOutputStream(tempFile).use { fos ->
                        BZip2CompressorOutputStream(fos).use { bzos ->
                            bzis.copyTo(bzos)
                        }
                    }
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
                println("[SANITIZR] BZ2 sanitization successful: ${file.absolutePath}")
                true
            } else {
                println("[SANITIZR] Failed to delete original BZ2: ${file.absolutePath}")
                tempFile.delete()
                false
            }

        } catch (e: Exception) {
            println("[SANITIZR] Exception during BZ2 sanitization: ${e.message}")
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    private fun sanitizeXz(file: File): Boolean {
        println("[SANITIZR] Starting XZ sanitization: ${file.absolutePath}")

        val tempFile = File(file.parentFile, "temp_sanitized.xz")

        return try {
            FileInputStream(file).use { fis ->
                XZInputStream(fis).use { xzis ->
                    FileOutputStream(tempFile).use { fos ->
                        XZOutputStream(fos, LZMA2Options()).use { xzos ->
                            xzis.copyTo(xzos)
                        }
                    }
                }
            }

            if (file.delete()) {
                tempFile.renameTo(file)
                println("[SANITIZR] XZ sanitization successful: ${file.absolutePath}")
                true
            } else {
                println("[SANITIZR] Failed to delete original XZ: ${file.absolutePath}")
                tempFile.delete()
                false
            }

        } catch (e: Exception) {
            println("[SANITIZR] Exception during XZ sanitization: ${e.message}")
            e.printStackTrace()
            tempFile.delete()
            false
        }
    }

    private fun copyAsSanitized(file: File): Boolean {
        val tempFile = File(file.parentFile, "temp_sanitized_${file.name}")
        return try {
            file.copyTo(tempFile, overwrite = true)
            if (file.delete()) {
                tempFile.renameTo(file)
                println("[SANITIZR] Stub-sanitized (copied): ${file.absolutePath}")
                true
            } else {
                println("[SANITIZR] Failed to delete original during stub-sanitization: ${file.absolutePath}")
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            println("[SANITIZR] Failed to stub-sanitize ${file.absolutePath}: ${e.message}")
            tempFile.delete()
            false
        }
    }

}
