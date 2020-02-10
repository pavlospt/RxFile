package com.github.pavlospt.kotlin

import android.Manifest
import android.provider.MediaStore

/**
 * Copyright 2015 Pavlos-Petros Tournaris
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
object Constants {
    const val FOLDER_SEPARATOR = "/"
    const val IMAGE_TYPE = "image/"
    const val VIDEO_TYPE = "video/"
    const val APPLICATION_PDF = "application/pdf"
    const val DOWNLOADS_CONTENT_URI = "content://downloads/public_downloads"
    const val IMAGES = "images"
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val PDF_EXTENSION = "pdf"
    const val READ_MODE = "r"
    const val MEDIA_AUTHORITY = "media"
    const val MEDIA_DOCUMENTS_AUTHORITY = "com.android.providers.media.documents"
    const val GOOGLE_DRIVE_DOCUMENT_AUTHORITY = "com.google.android.apps.docs.storage"
    const val GOOGLE_PHOTOS_MEDIA_AUTHORITY = "com.google.android.apps.photos.contentprovider"
}
