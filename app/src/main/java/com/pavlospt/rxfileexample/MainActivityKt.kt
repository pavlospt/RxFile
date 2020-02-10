package com.pavlospt.rxfileexample

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.github.pavlospt.kotlin.RxFileKt
import com.github.pavlospt.kotlin.logger.AndroidRxFileLogger
import com.pavlospt.rxfileexample.databinding.ActivityMainBinding
import timber.log.Timber
import java.io.File

class MainActivityKt : AppCompatActivity() {

    private companion object {
        private const val REQUEST_FOR_IMAGES_VIDEOS = 1
        private const val REQUEST_FOR_FILES = 2
        private const val REQUEST_PERMISSIONS = 1234
    }

    private lateinit var binding: ActivityMainBinding

    private val coThumb: RxFileKt by lazy {
        RxFileKt(context = this@MainActivityKt, logger = AndroidRxFileLogger)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.pickImageOrVideo.setOnClickListener {
            checkPermissionBeforeAction {
                startIntentForSingleImageVideoPick()
            }
        }

        binding.pickMultipleFiles.setOnClickListener {
            checkPermissionBeforeAction {
                startIntentForMultipleFilePick()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        data ?: return

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_FOR_IMAGES_VIDEOS -> loadImagesVideos(data)
            REQUEST_FOR_FILES -> loadFiles(data)
        }
    }

    private fun checkPermissionBeforeAction(doIfGranted: () -> Unit) {
        val hasExternalStoragePermission: Boolean =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED

        if (hasExternalStoragePermission) {
            doIfGranted()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSIONS
            )
        }
    }

    private fun startIntentForSingleImageVideoPick() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "*/*"
        intent.putExtra(
            Intent.EXTRA_MIME_TYPES,
            arrayOf("image/*", "video/*")
        )
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(
            Intent.createChooser(intent, "Pick files"),
            REQUEST_FOR_IMAGES_VIDEOS
        )
    }

    private fun startIntentForMultipleFilePick() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(intent, "Pick files"),
            REQUEST_FOR_FILES
        )
    }

    private fun loadFiles(data: Intent) {
        val fileUri: Uri? = data.data

        if (fileUri != null) {
            Timber.d("Single file selected")

            val file = DocumentFile.fromSingleUri(this, fileUri) ?: return

            file.logInfo()

            coCacheFileFromDrive(uri = file.uri)
        } else {
            fetchMultipleFiles(clipData = data.clipData)
        }
    }

    private fun loadImagesVideos(data: Intent) {
        val fileUri: Uri? = data.data

        if (fileUri != null) {
            Timber.d("Single file selected")

            val file = DocumentFile.fromSingleUri(this, fileUri) ?: return

            file.logInfo()

            coFetchThumbnail(file = file)
        } else {
            fetchMultipleFiles(clipData = data.clipData)
        }
    }

    private fun fetchMultipleFiles(clipData: ClipData?) {
        Timber.d("Multiple files selected!")

        coFetchFiles(clipData)
    }

    private fun coCacheFileFromDrive(uri: Uri) {
        lifecycleScope.launchWhenCreated {
            val fileFromDrive: File? = coThumb.createFileFromUri(uri)

            fileFromDrive?.let {
                with(binding.pickedFileNames) {
                    visibility = View.VISIBLE
                    text = fileFromDrive.name
                }.also {
                    binding.thumbnailPreview.visibility = View.INVISIBLE
                }

                Timber.d("Cached file with filename: ${fileFromDrive.name}")
            }
        }
    }

    private fun coFetchThumbnail(file: DocumentFile) {
        lifecycleScope.launchWhenCreated {
            val bitmap: Bitmap? = coThumb.getThumbnail(uri = file.uri, size = Size(200, 200))
            bitmap?.let {
                with(binding.thumbnailPreview) {
                    visibility = View.VISIBLE
                    setImageBitmap(it)
                }.also {
                    binding.pickedFileNames.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun coFetchFiles(clipData: ClipData?) {
        clipData ?: return
        lifecycleScope.launchWhenCreated {
            val files: List<File?> = coThumb
                .createFilesFromClipData(clipData = clipData)
                .orEmpty()

            val filePaths = files.mapNotNull { it?.absolutePath }.joinToString("\n\n")

            with(binding.pickedFileNames) {
                visibility = View.VISIBLE
                text = filePaths
            }.also {
                binding.thumbnailPreview.visibility = View.INVISIBLE
            }
        }
    }

    private fun DocumentFile.logInfo() {
        Timber.d("FileName: $name FileType: $type")
        Timber.d("Document uri: $uri")
    }
}
