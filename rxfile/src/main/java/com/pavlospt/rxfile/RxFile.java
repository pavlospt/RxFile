package com.pavlospt.rxfile;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Copyright 2015 Pavlos-Petros Tournaris
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class RxFile {

    private static final String TAG = RxFile.class.getSimpleName();

    public static Observable<File> createFileFromGoogleDrive(final Context context, final Uri data, final String fileName) {
        return Observable.defer(new Func0<Observable<File>>() {
            @Override
            public Observable<File> call() {
                DocumentFile file = DocumentFile.fromSingleUri(context,data);
                String fileType = file.getType();
                File fileCreated = null;
                Log.e(TAG,"Permission:" + checkWriteExternalPermission(context));
                try {
                    if (checkWriteExternalPermission(context)) {
                        ParcelFileDescriptor parcelFileDescriptor =
                                context.getContentResolver().openFileDescriptor(data, Constants.READ_MODE);
                        InputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                        String filePath = Environment.getExternalStorageDirectory().getPath()
                                + Constants.DEFAULT_CACHE_DIRECTORY_NAME
                                + fileName;
                        String fileExtension = fileName.substring((fileName.lastIndexOf('.')) + 1);
                        String dirPath = Environment.getExternalStorageDirectory().getPath()
                                + Constants.DEFAULT_CACHE_DIRECTORY_NAME;
                        Log.e(TAG, "Cache dir path: " + dirPath);
                        String mimeType = getMimeType(fileName);

                        Log.e(TAG, "From Drive guessed type: " + getMimeType(fileName));

                        Log.e(TAG, "Extension: " + fileExtension);

                        if (fileType.equals(Constants.APPLICATION_PDF)
                                && mimeType == null) {
                            filePath += "." + Constants.PDF_EXTENSION;
                        }

                        createDirectory(dirPath);
                        createFile(filePath);

                        ReadableByteChannel from = Channels.newChannel(inputStream);
                        WritableByteChannel to = Channels.newChannel(new FileOutputStream(filePath));
                        fastChannelCopy(from, to);
                        from.close();
                        to.close();
                        fileCreated = new File(filePath);
                        Log.e(TAG, "Path for made file: " + fileCreated.getAbsolutePath());
                    } else {
                        Log.e(TAG, "You must supply "
                                + Constants.WRITE_EXTERNAL_PERMISSION
                                + " in your Manifest.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                    e.printStackTrace();
                    return Observable.error(e);
                }

                return Observable.just(fileCreated);
            }
        });
    }

    public static Observable<Bitmap> getThumbnail(Context context, Uri uri) {
        return getThumbnailFromUri(context, uri);
    }

    public static Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth, int requiredHeight) {
        return getThumbnailFromUriWithSize(context, uri, requiredWidth, requiredHeight);
    }

    public static Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth, int requiredHeight, int kind) {
        return getThumbnailFromUriWithSizeAndKind(context, uri, requiredWidth, requiredHeight, kind);
    }

    public static Observable<Bitmap> getThumbnailFromUri(final Context context, final Uri data) {
        return Observable.fromCallable(new Func0<Bitmap>() {
            @Override
            public Bitmap call() {
                Bitmap bitmap = null;
                if (!isMediaUri(data)) {
                    Log.e(TAG, "Not a media uri");
                    if (isGoogleDriveDocument(data)){
                        Log.e(TAG, "Google Drive Uri");
                        DocumentFile file = DocumentFile.fromSingleUri(context,data);
                        if(file.getType().startsWith(Constants.IMAGE_TYPE) ||
                                file.getType().startsWith(Constants.VIDEO_TYPE)){
                            Log.e(TAG, "Google Drive Uri");
                            ParcelFileDescriptor parcelFileDescriptor;
                            try {
                                parcelFileDescriptor = context.getContentResolver().
                                        openFileDescriptor(data, Constants.READ_MODE);
                                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                                parcelFileDescriptor.close();
                                return bitmap;
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(TAG,"Google Drive uri error:" + e.getMessage());
                            }
                        }
                    } else if(data.getScheme().equals(Constants.FILE)){
                        Log.e(TAG,"Dropbox or other content provider");
                        ParcelFileDescriptor parcelFileDescriptor;
                        try {
                            parcelFileDescriptor = context.getContentResolver().
                                    openFileDescriptor(data, Constants.READ_MODE);
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                            parcelFileDescriptor.close();
                            return bitmap;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        ParcelFileDescriptor parcelFileDescriptor;
                        try {
                            parcelFileDescriptor = context.getContentResolver().
                                    openFileDescriptor(data, Constants.READ_MODE);
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                            parcelFileDescriptor.close();
                            return bitmap;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    Log.e(TAG, "Uri for thumbnail: " + data.toString());
                    Log.e(TAG, "Uri for thumbnail: " + data);
                    String[] parts = data.getLastPathSegment().split(":");
                    String fileId = parts[1];
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(data, null, null, null, null);
                        if (cursor != null) {
                            Log.e(TAG, "Cursor size: " + cursor.getCount());
                            if (cursor.moveToFirst()) {
                                if (data.toString().contains(Constants.VIDEO)) {
                                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                                            context.getContentResolver(),
                                            Long.parseLong(fileId),
                                            MediaStore.Video.Thumbnails.MINI_KIND,
                                            null);
                                } else if (data.toString().contains(Constants.IMAGE)) {
                                    Log.e(TAG, "Image Uri");
                                    bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                                            context.getContentResolver(),
                                            Long.parseLong(fileId),
                                            MediaStore.Images.Thumbnails.MINI_KIND,
                                            null);
                                }
                                Log.e(TAG, bitmap == null ? "null" : "not null");
                            }
                        }
                        return bitmap;
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while getting thumbnail:" + e.getMessage());
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                }
                return bitmap;
            }
        });
    }

    public static Observable<Bitmap> getThumbnailFromUriWithSize(
            final Context context,
            final Uri data,
            final int requiredWidth,
            final int requiredHeight
    ) {
        return Observable.fromCallable(new Func0<Bitmap>() {
            @Override
            public Bitmap call() {
                Bitmap bitmap = null;
                ParcelFileDescriptor parcelFileDescriptor;
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);
                options.inJustDecodeBounds = false;
                if (!isMediaUri(data)) {
                    Log.e(TAG, "Not a media uri");
                    if (isGoogleDriveDocument(data)){
                        Log.e(TAG, "Google Drive Uri");
                        DocumentFile file = DocumentFile.fromSingleUri(context,data);
                        try {
                            parcelFileDescriptor = context.getContentResolver().
                                    openFileDescriptor(data, Constants.READ_MODE);
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                            parcelFileDescriptor.close();
                            return bitmap;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    Log.e(TAG, "Uri for thumbnail: " + data.toString());
                    Log.e(TAG, "Uri for thumbnail: " + data);
                    String[] parts = data.getLastPathSegment().split(":");
                    String fileId = parts[1];
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(data, null, null, null, null);
                        if (cursor != null) {
                            Log.e(TAG, "Cursor size: " + cursor.getCount());
                            if (cursor.moveToFirst()) {
                                if (data.toString().contains(Constants.VIDEO)) {
                                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                                            context.getContentResolver(),
                                            Long.parseLong(fileId),
                                            MediaStore.Video.Thumbnails.MINI_KIND,
                                            options);
                                } else if (data.toString().contains(Constants.IMAGE)) {
                                    Log.e(TAG, "Image Uri");
                                    bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                                            context.getContentResolver(),
                                            Long.parseLong(fileId),
                                            MediaStore.Images.Thumbnails.MINI_KIND,
                                            options);
                                }
                                Log.e(TAG, bitmap == null ? "null" : "not null");
                            }
                        }
                        return bitmap;
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while getting thumbnail:" + e.getMessage());
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                }
                return bitmap;
            }
        });
    }

    public static Observable<Bitmap> getThumbnailFromUriWithSizeAndKind(
            final Context context,
            final Uri data,
            final int requiredWidth,
            final int requiredHeight,
            final int kind
    ) {
        return Observable.fromCallable(new Func0<Bitmap>() {
            @Override
            public Bitmap call() {
                Bitmap bitmap = null;
                ParcelFileDescriptor parcelFileDescriptor;
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);
                options.inJustDecodeBounds = false;
                if (!isMediaUri(data)) {
                    Log.e(TAG, "Not a media uri");
                    if (isGoogleDriveDocument(data)){
                        Log.e(TAG, "Google Drive Uri");
                        DocumentFile file = DocumentFile.fromSingleUri(context,data);
                        try {
                            parcelFileDescriptor = context.getContentResolver().
                                    openFileDescriptor(data, Constants.READ_MODE);
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                            parcelFileDescriptor.close();
                            return bitmap;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    Log.e(TAG, "Uri for thumbnail: " + data.toString());
                    Log.e(TAG, "Uri for thumbnail: " + data);
                    String[] parts = data.getLastPathSegment().split(":");
                    String fileId = parts[1];
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(data, null, null, null, null);
                        if (cursor != null) {
                            Log.e(TAG, "Cursor size: " + cursor.getCount());
                            if (cursor.moveToFirst()) {
                                if (data.toString().contains(Constants.VIDEO)) {
                                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                                            context.getContentResolver(),
                                            Long.parseLong(fileId),
                                            kind,
                                            options);
                                } else if (data.toString().contains(Constants.IMAGE)) {
                                    Log.e(TAG, "Image Uri");
                                    bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                                            context.getContentResolver(),
                                            Long.parseLong(fileId),
                                            kind,
                                            options);
                                }
                                Log.e(TAG, bitmap == null ? "null" : "not null");
                            }
                        }
                        return bitmap;
                    } catch (Exception e) {
                        Log.e(TAG, "Exception while getting thumbnail:" + e.getMessage());
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                }
                return bitmap;
            }
        });
    }

    public static Observable<Boolean> clearCachingDirectory(final Context context) {
        return Observable.fromCallable(new Func0<Boolean>() {
            @Override
            public Boolean call() {
                ArrayList<File> filesInDir = null;
                String dirPath = Environment.getExternalStorageDirectory().getPath()
                        + Constants.DEFAULT_CACHE_DIRECTORY_NAME;
                File tempDirFile = new File(dirPath);
                if (tempDirFile.exists()) {
                    if (tempDirFile.isDirectory()) {
                        if (tempDirFile.listFiles().length > 0)
                            filesInDir = new ArrayList<>(Arrays.asList(tempDirFile.listFiles()));
                    }
                }
                if (filesInDir != null) {
                    for (File f : filesInDir) {
                        if (f.exists() && f.isFile()) {
                            if (!f.delete()) {
                                Log.e(TAG, "Something went wrong " +
                                        "while trying to delete file: " + f.getName());
                            }
                        }
                    }
                } else return false;
                return true;
            }
        });
    }

    public static Observable<String> getFileExtension(final String fileName) {
        return Observable.fromCallable(new Func0<String>() {
            @Override
            public String call() {
                return fileName.substring((fileName.lastIndexOf('.')) + 1);
            }
        });
    }

    public static Observable<Boolean> ifExists(final String path) {
        return Observable.fromCallable(new Func0<Boolean>() {
            @Override
            public Boolean call() {
                return new File(path).exists();
            }
        });
    }

    public static Observable<Bitmap> getThumbnail(String filePath) {
        return getFileType(filePath)
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s != null;
                    }
                })
                .flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String s) {
                        if(s.equalsIgnoreCase(Constants.VIDEO))
                            return getVideoThumbnail(s);
                        else if(s.equalsIgnoreCase(Constants.IMAGE))
                            return getThumbnailFromPath(s);
                        return null;
                    }
                });
    }

    public static Observable<Bitmap> getVideoThumbnail(final String filePath) {
        return Observable.fromCallable(new Func0<Bitmap>() {
            @Override
            public Bitmap call() {
                return ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);
            }
        });
    }

    public static Observable<Bitmap> getVideoThumbnailFromPath(final String path, final int kind) {
        return Observable.defer(new Func0<Observable<Bitmap>>() {
            @Override
            public Observable<Bitmap> call() {
                return Observable.just(ThumbnailUtils.createVideoThumbnail(path, kind));
            }
        });
    }

    public static Observable<Bitmap> getThumbnailFromPath(String filePath) {
        final Bitmap sourceBitmap = BitmapFactory.decodeFile(filePath);
        return Observable.fromCallable(new Func0<Bitmap>() {
            @Override
            public Bitmap call() {
                return ThumbnailUtils.
                        extractThumbnail(sourceBitmap, sourceBitmap.getWidth(), sourceBitmap.getHeight());
            }
        });
    }

    public static Observable<String> getFileType(String filePath) {
        Log.e(TAG, "Filepath in getFileType: " + filePath);
        final String[] parts = filePath.split("/");
        return Observable.fromCallable(new Func0<String>() {
            @Override
            public String call() {
                return parts.length > 0 ? URLConnection.guessContentTypeFromName(parts[0]) : null;
            }
        });
    }

    public static Observable<String> getPathFromUriForFileDocument(final Context context, final Uri contentUri) {
        return Observable.fromCallable(new Func0<String>() {
            @Override
            public String call() {
                String pathFound = null;
                Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
                if(cursor != null){
                    if (cursor.moveToFirst())
                        pathFound = cursor.getString(
                                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                    cursor.close();
                    Log.e(TAG, "PathFound: " + pathFound);
                }
                return pathFound;
            }
        });
    }

    public static Observable<String> getPathFromUriForMediaDocument(final Context context,
                                                                     final Uri mediaUri,
                                                                     final String mediaDocumentId) {
        return Observable.fromCallable(new Func0<String>() {
            @Override
            public String call() {
                String pathFound = null;
                Cursor cursor = context.getContentResolver().query(mediaUri, null, Constants.ID_COLUMN_VALUE + " =?"
                        , new String[]{mediaDocumentId}, null);
                if(cursor != null){
                    if (cursor.moveToFirst())
                        pathFound = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                    cursor.close();
                    Log.e(TAG, "PathFound: " + pathFound);
                }
                return pathFound;
            }
        });
    }

    public static Observable<String> getPathFromUriForImageDocument(final Context context,
                                                                    Uri mediaUri,
                                                                    final String mediaDocumentId) {
        return Observable.fromCallable(new Func0<String>() {
            @Override
            public String call() {
                String pathFound = null;
                Cursor cursor = context.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, Constants.ID_COLUMN_VALUE + " =?"
                        , new String[]{mediaDocumentId}, null);
                if(cursor != null){
                    if (cursor.moveToFirst())
                        pathFound = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                    cursor.close();
                    Log.e(TAG, "PathFound: " + pathFound);
                }
                return pathFound;
            }
        });
    }

    private static Observable<String> getPathFromUriForVideoDocument(final Context context,
                                                                     Uri mediaUri,
                                                                     final String mediaDocumentId) {
        return Observable.fromCallable(new Func0<String>() {
            @Override
            public String call() {
                String pathFound = null;
                Cursor cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, Constants.ID_COLUMN_VALUE + " =?"
                        , new String[]{mediaDocumentId}, null);
                if(cursor != null) {
                    if (cursor.moveToFirst())
                        pathFound = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
                    cursor.close();
                    Log.e(TAG, "PathFound: " + pathFound);
                }
                return pathFound;
            }
        });
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return Constants.EXTERNAL_STORAGE_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return Constants.DOWNLOADS_DIRECTORY_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return Constants.MEDIA_DOCUMENTS_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isGoogleDriveDocument(Uri uri) {
        return Constants.GOOGLE_DRIVE_DOCUMENT_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean isMediaUri(Uri uri) {
        if (uri.getAuthority().equals(Constants.MEDIA_DOCUMENTS_AUTHORITY)) {
            return uri.getLastPathSegment().contains(Constants.IMAGE) ||
                    uri.getLastPathSegment().contains(Constants.VIDEO);
        }
        return Constants.MEDIA_AUTHORITY.equals(uri.getAuthority());
    }

    private static boolean checkWriteExternalPermission(Context context) {
        int res = context.checkCallingOrSelfPermission(Constants.WRITE_EXTERNAL_PERMISSION);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static String getMimeType(String fileName) {
        return URLConnection.guessContentTypeFromName(fileName);
    }

    private static void createDirectory(String path) {
        if (!checkExistence(path)) {
            File temp = new File(path);
            if (!temp.mkdir()) {
                Log.e(TAG, "Something went wrong while creating directory: " + path);
            } else {
                Log.e(TAG, "Directory: " + path + " created.");
            }
        } else {
            Log.e(TAG, "Directory: " + path + " already exists.");
        }
    }

    private static void createFile(String path) throws IOException {
        if (!checkExistence(path)) {
            File temp = new File(path);
            if (!temp.createNewFile()) {
                Log.e(TAG, "Something went wrong while creating directory: " + path);
            } else {
                Log.e(TAG, "File: " + path + " created.");
            }
        } else {
            Log.e(TAG, "File: " + path + " already exists.");
        }
    }

    private static boolean checkExistence(String path) {
        Log.e(TAG, "Check path: " + path);
        File temp = new File(path);
        return temp.exists();
    }

    public static void fastChannelCopy(final ReadableByteChannel src,
                                       final WritableByteChannel dest) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (src.read(buffer) != -1) {
            buffer.flip();
            dest.write(buffer);
            buffer.compact();
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        Log.e(TAG, "Height: " + height + " Width: " + width);
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
