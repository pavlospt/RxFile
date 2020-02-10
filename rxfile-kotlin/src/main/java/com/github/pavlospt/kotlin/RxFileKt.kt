package com.github.pavlospt.kotlin

import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.github.pavlospt.kotlin.logger.NoOpRxFileLogger
import com.github.pavlospt.kotlin.logger.RxFileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.URLConnection
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.util.*

class RxFileKt(
    private val context: Context,
    private val logger: RxFileLogger = NoOpRxFileLogger
) {

    private val contentResolver: ContentResolver by lazy {
        context.contentResolver
    }

    enum class MimeMap {
        MIME_TYPE_MAP,
        URL_CONNECTION
    }

    suspend fun createFilesFromClipData(clipData: ClipData): List<File?>? =
        createFilesFromClipData(clipData, MimeMap.URL_CONNECTION)

    suspend fun createFileFromUri(data: Uri): File? = withContext(Dispatchers.IO) {
        createFileFromUri(data, MimeMap.URL_CONNECTION)
    }

    suspend fun getThumbnail(uri: Uri, size: Size): Bitmap? = withContext(Dispatchers.IO) {
        val isPorAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val isGooglePhotosUri = uri.isGooglePhotosUri

        if (!isPorAbove || !isGooglePhotosUri) {
            return@withContext fallbackThumbnailLoad(uri = uri)
        }

        val gPhotosCPRegex =
            "content://com\\.google\\.android\\.apps\\.photos\\.contentprovider/-*\\d/-*\\d/(.*)"
                .toRegex()

        val gPhotosRegexMatch = gPhotosCPRegex.find(uri.toString())
            ?: return@withContext fallbackThumbnailLoad(uri = uri)

        val (actualUri: String) = gPhotosRegexMatch.destructured

        val decodedUri = Uri.decode(actualUri)

        val mimeTypeIdRegex = "content://media/external/(video|images)/media/(\\d+)"
            .toRegex()

        val mimeTypeIDRegex = mimeTypeIdRegex.find(decodedUri)
            ?: return@withContext fallbackThumbnailLoad(uri = uri)

        val (type: String, id: String) = mimeTypeIDRegex.destructured

        val externalContentUri = when (type) {
            Constants.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            Constants.IMAGES -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else -> {
                return@withContext fallbackThumbnailLoad(uri = uri)
            }
        }

        return@withContext contentResolver.loadThumbnail(
            ContentUris.withAppendedId(
                externalContentUri,
                id.toLong()
            ), size, null
        )
    }

    private suspend fun fallbackThumbnailLoad(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }

    /**
     * Create a copy of the files found under the ClipData item passed from MultiSelection,
     * in the Library's cache folder.
     *
     * You can supply mime type mapping parameter to choose which map will be used.
     *
     * MIME_TYPE_MAP - The mime type of the resource will be
     *                 determined by MimeTypeMap.getMimeTypeFromExtension() method.
     * URL_CONNECTION_MAP - The mime type of the resource will be
     *                      determined by URLConnection.guessContentTypeFromName() method.
     *
     * */
    private suspend fun createFilesFromClipData(
        clipData: ClipData,
        mimeTypeMap: MimeMap
    ): List<File> = withContext(Dispatchers.IO) {
        (0 until clipData.itemCount)
            .mapNotNull {
                val uri: Uri = clipData.getItemAt(it).uri ?: return@mapNotNull null
                fileFromUri(uri, mimeTypeMap)
            }
    }

    /**
     * Get image thumbnail from an image file, by path.
     * */
    private suspend fun getThumbnailFromPath(filePath: String?): Bitmap? =
        withContext(Dispatchers.IO) {
            val sourceBitmap = BitmapFactory.decodeFile(filePath)
            return@withContext ThumbnailUtils.extractThumbnail(
                sourceBitmap,
                sourceBitmap.width,
                sourceBitmap.height
            )
        }

    /**
     * Deprecation of used methods here, suggests using a newer API available from 26+ and we do not
     * want to drop support for smaller APIs yet.
     * */
    @Suppress("DEPRECATION")
    private suspend fun getVideoThumbnail(
        filePath: String?,
        thumbnailKind: Int = MediaStore.Images.Thumbnails.MINI_KIND
    ): Bitmap? = withContext(Dispatchers.IO) {
        filePath ?: return@withContext null
        return@withContext ThumbnailUtils.createVideoThumbnail(
            filePath,
            thumbnailKind
        )
    }

    /**
     * Get a thumbnail from the provided Image or Video Uri.
     *
     * Deprecation of used methods here, suggests using a newer API available from 26+ and we do not
     * want to drop support for smaller APIs yet.
     * */
    @Suppress("DEPRECATION")
    private suspend fun getImageThumbnail(
        data: Uri,
        thumbnailKind: Int = MediaStore.Images.Thumbnails.MINI_KIND
    ): Bitmap? = getThumbnailFromUriWithSizeAndKind(data, 0, 0, thumbnailKind)

    /**
     * Get a thumbnail from the provided Image or Video Uri in the specified size.
     *
     * Deprecation of used methods here, suggests using a newer API available from 26+ and we do not
     * want to drop support for smaller APIs yet.
     * */
    @Suppress("DEPRECATION")
    private suspend fun getThumbnailFromUriWithSize(
        data: Uri,
        requiredWidth: Int,
        requiredHeight: Int,
        thumbnailKind: Int = MediaStore.Images.Thumbnails.MINI_KIND
    ): Bitmap? = getThumbnailFromUriWithSizeAndKind(
        data,
        requiredWidth,
        requiredHeight,
        thumbnailKind
    )

    /**
     * Get a thumbnail from the provided Image or Video Uri in the specified size and kind.
     * Kind is a value of MediaStore.Images.Thumbnails.MICRO_KIND or MediaStore.Images.Thumbnails.MINI_KIND
     *
     * Deprecation of used methods here, suggests using a newer API available from 26+ and we do not
     * want to drop support for smaller APIs yet.
     * */
    @Suppress("DEPRECATION")
    private suspend fun getThumbnailFromUriWithSizeAndKind(
        data: Uri,
        requiredWidth: Int,
        requiredHeight: Int,
        kind: Int
    ): Bitmap? = withContext(Dispatchers.IO) {

        val options = BitmapFactory.Options().apply {
            if (requiredWidth > 0 && requiredHeight > 0) {
                inJustDecodeBounds = true
                inSampleSize = calculateInSampleSize(
                    options = this,
                    reqWidth = requiredWidth,
                    reqHeight = requiredHeight
                )
                inJustDecodeBounds = false
            }
        }

        fun decodeBitmapFromPFD(): Bitmap? {
            val parcelFileDescriptor: ParcelFileDescriptor? = context.contentResolver
                .openFileDescriptor(data, Constants.READ_MODE)

            return parcelFileDescriptor?.run {
                val fileDescriptor = parcelFileDescriptor.fileDescriptor

                return BitmapFactory.decodeFileDescriptor(
                    fileDescriptor,
                    null,
                    options
                ).also { parcelFileDescriptor.close() }
            }
        }

        if (!data.isMediaUri) {

            logger.log("Not a media uri:$data")

            if (data.isGoogleDriveDocument) {

                logger.log("Google Drive Uri:$data")

                val file = DocumentFile.fromSingleUri(context, data)
                val fileType = file?.type ?: return@withContext null
                val isImageOrVideo: Boolean = fileType.startsWith(Constants.IMAGE_TYPE) ||
                        fileType.startsWith(Constants.VIDEO_TYPE)

                if (isImageOrVideo) {
                    logger.log("Google Drive Uri:$data (Video or Image)")
                    return@withContext decodeBitmapFromPFD()
                }
            } else {
                logger.log("Dropbox or other DocumentsProvider Uri:$data")
                return@withContext decodeBitmapFromPFD()
            }
        } else {
            logger.log("Uri for thumbnail:$data")

            val parts: List<String?> = data.lastPathSegment?.split(":").orEmpty()

            if (parts.size < 2) {
                return@withContext null
            }

            val (_, fileId: String?) = parts

            if (fileId == null) return@withContext null

            return@withContext when {
                data.toString().contains(Constants.VIDEO) -> {
                    MediaStore.Video.Thumbnails.getThumbnail(
                        context.contentResolver,
                        fileId.toLong(),
                        kind,
                        options
                    )
                }
                data.toString().contains(Constants.IMAGE) -> {
                    MediaStore.Images.Thumbnails.getThumbnail(
                        context.contentResolver,
                        fileId.toLong(),
                        kind,
                        options
                    )
                }
                else -> null
            }
        }

        return@withContext null
    }

    private fun getFileType(filePath: String?): String? {
        logger.log("Filepath in getFileType: $filePath")

        val parts = filePath?.split("/").orEmpty()

        return if (parts.isNotEmpty()) {
            URLConnection.guessContentTypeFromName(parts[0])
        } else {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth

        logger.log("Height: $height Width: $width")

        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private suspend fun createFileFromUri(data: Uri, mimeTypeMap: MimeMap): File? =
        withContext(Dispatchers.IO) {
            fileFromUri(data, mimeTypeMap)
        }

    @Throws(Exception::class)
    private suspend fun fileFromUri(data: Uri, mimeTypeMap: MimeMap): File? =
        withContext(Dispatchers.IO) {
            val file = DocumentFile.fromSingleUri(context, data) ?: return@withContext null
            val fileType = file.type
            val fileName: String = file.name ?: "${UUID.randomUUID()}"

            val filePathBuilder = StringBuilder().apply {
                append(context.externalCacheDir.toString())
                append(Constants.FOLDER_SEPARATOR)
                append(fileName)
            }

            val mimeType: String? = if (mimeTypeMap == MimeMap.MIME_TYPE_MAP) {
                getTypeWithMimeTypeMap(fileName)
            } else {
                getTypeWithURLConnection(fileName)
            }

            if (fileType == Constants.APPLICATION_PDF && mimeType == null) {
                filePathBuilder.append(".").append(Constants.PDF_EXTENSION)
            }

            val filePath = filePathBuilder.toString()

            if (!filePath.createFile()) {
                return@withContext File(filePath)
            }

            val parcelFileDescriptor =
                context.contentResolver.openFileDescriptor(data, Constants.READ_MODE)
                    ?: return@withContext null

            val inputStream: InputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)

            val from = Channels.newChannel(inputStream)
            val to = Channels.newChannel(FileOutputStream(filePath))

            fastChannelCopy(from, to)

            from.close()
            to.close()
            parcelFileDescriptor.close()

            return@withContext File(filePath)
        }

    @Throws(IOException::class)
    private suspend fun fastChannelCopy(
        src: ReadableByteChannel,
        dest: WritableByteChannel
    ) = withContext(Dispatchers.IO) {
        val buffer = ByteBuffer.allocateDirect(16 * 1024)
        while (src.read(buffer) != -1) {
            buffer.flip()
            dest.write(buffer)
            buffer.compact()
        }
        buffer.flip()
        while (buffer.hasRemaining()) {
            dest.write(buffer)
        }
    }

    @Throws(IOException::class)
    private suspend fun String.createFile(): Boolean = withContext(Dispatchers.IO) {
        if (!existsAsFile) {
            val temp = File(this@createFile)
            if (!temp.createNewFile()) {
                logger.log("Something went wrong while creating file: $this")
            } else {
                logger.log("File: $this created.")
            }
        } else {
            logger.log("File: $this already exists.")
            return@withContext false
        }
        return@withContext true
    }

    private fun getTypeWithMimeTypeMap(fileName: String): String? {
        val fileExtension = fileName.split(".").last()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
    }

    private fun getTypeWithURLConnection(fileName: String?): String? =
        URLConnection.guessContentTypeFromName(fileName)

    private val Uri.isGoogleDriveDocument: Boolean
        get() = Constants.GOOGLE_DRIVE_DOCUMENT_AUTHORITY == authority

    private val String.existsAsFile: Boolean
        get() = File(this).exists()

    private val Uri.isMediaUri: Boolean
        get() {
            return if (authority == Constants.MEDIA_DOCUMENTS_AUTHORITY) {
                val lastPathSegment: String = lastPathSegment ?: ""
                lastPathSegment.contains(Constants.IMAGE) || lastPathSegment.contains(Constants.VIDEO)
            } else {
                Constants.MEDIA_AUTHORITY == authority
            }
        }

    private val Uri.isGooglePhotosUri: Boolean
        get() = this.authority == Constants.GOOGLE_PHOTOS_MEDIA_AUTHORITY
}
