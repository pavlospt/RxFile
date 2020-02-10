package com.pavlospt.rxfile;

import android.content.ClipData;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.documentfile.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;
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
import java.util.List;
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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class RxFile {

  private static final String TAG = RxFile.class.getSimpleName();

  private static boolean LOGGING_ENABLED = false;

  public enum MimeMap {
    MimeTypeMap,
    UrlConnection
  }

  /*
   * Create a copy of the file found under the provided Uri, in the Library's cache folder.
   *
   * The mime type of the resource will be determined by URLConnection.guessContentTypeFromName() method.
   * */
  public static Observable<File> createFileFromUri(final Context context, final Uri data) {
    return createFileFromUri(context, data, MimeMap.UrlConnection);
  }

  /*
   * Create a copy of the file found under the provided Uri, in the Library's cache folder.
   *
   * You can supply mime type mapping parameter to choose which map will be used.
   *
   * MIME_TYPE_MAP - The mime type of the resource will be determined by MimeTypeMap.getMimeTypeFromExtension() method.
   * URL_CONNECTION_MAP - The mime type of the resource will be determined by URLConnection.guessContentTypeFromName() method.
   * */
  private static Observable<File> createFileFromUri(final Context context, final Uri data,
      final MimeMap mimeTypeMap) {
    return Observable.defer(new Func0<Observable<File>>() {
      @Override
      public Observable<File> call() {
        try {
          return Observable.just(fileFromUri(context, data, mimeTypeMap));
        } catch (Exception e) {
          logError(e);
          return Observable.error(e);
        }
      }
    });
  }

  /*
   * Create a copy of the files found under the provided ArrayList of Uris, in the Library's cache folder.
   *
   * The mime type of the resource will be determined by URLConnection.guessContentTypeFromName() method.
   * */
  public static Observable<List<File>> createFileFromUri(final Context context,
      final ArrayList<Uri> uris) {
    return createFileFromUri(context, uris, MimeMap.UrlConnection);
  }

  /*
   * Create a copy of the files found under the provided ArrayList of Uris, in the Library's cache folder.
   *
   * You can supply mime type mapping parameter to choose which map will be used.
   *
   * MIME_TYPE_MAP - The mime type of the resource will be determined by MimeTypeMap.getMimeTypeFromExtension() method.
   * URL_CONNECTION_MAP - The mime type of the resource will be determined by URLConnection.guessContentTypeFromName() method.
   * */
  private static Observable<List<File>> createFileFromUri(final Context context,
      final ArrayList<Uri> uris, final MimeMap mimeTypeMap) {
    return Observable.defer(new Func0<Observable<List<File>>>() {
      @Override
      public Observable<List<File>> call() {

        List<File> filesRetrieved = new ArrayList<>(uris.size());

        for (Uri data : uris) {
          try {
            filesRetrieved.add(fileFromUri(context, data, mimeTypeMap));
          } catch (Exception e) {
            logError(e);
            return Observable.error(e);
          }
        }

        return Observable.just(filesRetrieved);
      }
    });
  }

  /*
   * Create a copy of the files found under the ClipData item passed from MultiSelection, in the Library's cache folder.
   *
   * The mime type of the resource will be determined by URLConnection.guessContentTypeFromName() method.
   * */
  public static Observable<List<File>> createFilesFromClipData(final Context context,
      final ClipData clipData) {

    return createFilesFromClipData(context, clipData, MimeMap.UrlConnection);
  }

  /*
   * Create a copy of the files found under the ClipData item passed from MultiSelection, in the Library's cache folder.
   *
   * You can supply mime type mapping parameter to choose which map will be used.
   *
   * MIME_TYPE_MAP - The mime type of the resource will be determined by MimeTypeMap.getMimeTypeFromExtension() method.
   * URL_CONNECTION_MAP - The mime type of the resource will be determined by URLConnection.guessContentTypeFromName() method.
   *
   * */
  private static Observable<List<File>> createFilesFromClipData(final Context context,
      final ClipData clipData, final MimeMap mimeTypeMap) {
    return Observable.defer(new Func0<Observable<List<File>>>() {
      @Override
      public Observable<List<File>> call() {
        int numOfUris = clipData.getItemCount();
        List<File> filesRetrieved = new ArrayList<>(numOfUris);

        for (int i = 0; i < numOfUris; i++) {
          Uri data = clipData.getItemAt(i).getUri();
          if (data != null) {
            try {
              filesRetrieved.add(fileFromUri(context, data, mimeTypeMap));
            } catch (Exception e) {
              logError(e);
              return Observable.error(e);
            }
          }
        }
        return Observable.just(filesRetrieved);
      }
    });
  }

  /*
   * Get a thumbnail from the provided Image or Video Uri.
   * */
  public static Observable<Bitmap> getThumbnail(Context context, Uri uri) {
    return getThumbnailFromUri(context, uri);
  }

  /*
   * Get a thumbnail from the provided Image or Video Uri in the specified size.
   * */
  public static Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth,
      int requiredHeight) {
    return getThumbnailFromUriWithSize(context, uri, requiredWidth, requiredHeight);
  }

  /*
   * Get a thumbnail from the provided Image or Video Uri in the specified size and kind.
   * Kind is a value of MediaStore.Images.Thumbnails.MICRO_KIND or MediaStore.Images.Thumbnails.MINI_KIND
   * */
  public static Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth,
      int requiredHeight, int kind) {
    return getThumbnailFromUriWithSizeAndKind(context, uri, requiredWidth, requiredHeight, kind);
  }

  /*
   * Get a thumbnail from the provided Image or Video Uri.
   * */
  private static Observable<Bitmap> getThumbnailFromUri(final Context context, final Uri data) {
    return getThumbnailFromUriWithSizeAndKind(context, data, 0, 0,
        MediaStore.Images.Thumbnails.MINI_KIND);
  }

  /*
   * Get a thumbnail from the provided Image or Video Uri in the specified size.
   * */
  private static Observable<Bitmap> getThumbnailFromUriWithSize(final Context context,
      final Uri data, final int requiredWidth, final int requiredHeight) {
    return getThumbnailFromUriWithSizeAndKind(context, data, requiredWidth, requiredHeight,
        MediaStore.Images.Thumbnails.MINI_KIND);
  }

  /*
   * Get a thumbnail from the provided Image or Video Uri in the specified size and kind.
   * Kind is a value of MediaStore.Images.Thumbnails.MICRO_KIND or MediaStore.Images.Thumbnails.MINI_KIND
   * */
  private static Observable<Bitmap> getThumbnailFromUriWithSizeAndKind(final Context context,
      final Uri data, final int requiredWidth, final int requiredHeight, final int kind) {
    return Observable.fromCallable(new Func0<Bitmap>() {
      @Override
      public Bitmap call() {
        Bitmap bitmap = null;
        ParcelFileDescriptor parcelFileDescriptor;
        final BitmapFactory.Options options = new BitmapFactory.Options();
        if (requiredWidth > 0 && requiredHeight > 0) {
          options.inJustDecodeBounds = true;
          options.inSampleSize = calculateInSampleSize(options, requiredWidth, requiredHeight);
          options.inJustDecodeBounds = false;
        }
        if (!isMediaUri(data)) {
          logDebug("Not a media uri:" + data);
          if (isGoogleDriveDocument(data)) {
            logDebug("Google Drive Uri:" + data);
            DocumentFile file = DocumentFile.fromSingleUri(context, data);
            if (file.getType().startsWith(Constants.IMAGE_TYPE) || file.getType()
                .startsWith(Constants.VIDEO_TYPE)) {
              logDebug("Google Drive Uri:" + data + " (Video or Image)");
              try {
                parcelFileDescriptor = context.getContentResolver().
                    openFileDescriptor(data, Constants.READ_MODE);
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
                parcelFileDescriptor.close();
                return bitmap;
              } catch (IOException e) {
                logError(e);
              }
            }
          } else if (data.getScheme().equals(Constants.FILE)) {
            logDebug("Dropbox or other DocumentsProvider Uri:" + data);
            try {
              parcelFileDescriptor = context.getContentResolver().
                  openFileDescriptor(data, Constants.READ_MODE);
              FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
              bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
              parcelFileDescriptor.close();
              return bitmap;
            } catch (IOException e) {
              logError(e);
            }
          } else {
            try {
              parcelFileDescriptor = context.getContentResolver().
                  openFileDescriptor(data, Constants.READ_MODE);
              FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
              bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
              parcelFileDescriptor.close();
              return bitmap;
            } catch (IOException e) {
              logError(e);
            }
          }
        } else {
          logDebug("Uri for thumbnail:" + data);
          String[] parts = data.getLastPathSegment().split(":");
          String fileId = parts[1];
          Cursor cursor = null;
          try {
            cursor = context.getContentResolver().query(data, null, null, null, null);
            if (cursor != null) {
              logDebug("Cursor size:" + cursor.getCount());
              if (cursor.moveToFirst()) {
                if (data.toString().contains(Constants.VIDEO)) {
                  bitmap = MediaStore.Video.Thumbnails.getThumbnail(context.getContentResolver(),
                      Long.parseLong(fileId), kind, options);
                } else if (data.toString().contains(Constants.IMAGE)) {
                  bitmap = MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(),
                      Long.parseLong(fileId), kind, options);
                }
              }
            }
            return bitmap;
          } catch (Exception e) {
            logError(e);
          } finally {
            if (cursor != null) cursor.close();
          }
        }
        return bitmap;
      }
    });
  }

  /*
   * Get a file extension based on the given file name.
   * */
  public static Observable<String> getFileExtension(final String fileName) {
    return Observable.fromCallable(new Func0<String>() {
      @Override
      public String call() {
        return fileName.substring((fileName.lastIndexOf('.')) + 1);
      }
    });
  }

  /*
   * Check if a File exists.
   * */
  public static Observable<Boolean> ifExists(final String path) {
    return Observable.fromCallable(new Func0<Boolean>() {
      @Override
      public Boolean call() {
        return new File(path).exists();
      }
    });
  }

  /*
   * Get thumbnail from a File path.
   * */
  public static Observable<Bitmap> getThumbnail(String filePath) {
    return getFileType(filePath).filter(new Func1<String, Boolean>() {
      @Override
      public Boolean call(String s) {
        return s != null;
      }
    }).flatMap(new Func1<String, Observable<Bitmap>>() {
      @Override
      public Observable<Bitmap> call(String s) {
        if (s.equalsIgnoreCase(Constants.VIDEO)) {
          return getVideoThumbnail(s);
        } else if (s.equalsIgnoreCase(Constants.IMAGE)) return getThumbnailFromPath(s);
        return null;
      }
    });
  }

  /*
   * Get video thumbnail from a video file, by path.
   * */
  public static Observable<Bitmap> getVideoThumbnail(final String filePath) {
    return Observable.fromCallable(new Func0<Bitmap>() {
      @Override
      public Bitmap call() {
        return ThumbnailUtils.createVideoThumbnail(filePath,
            MediaStore.Images.Thumbnails.MINI_KIND);
      }
    });
  }

  /*
   * Get video thumbnail from a video file, by path, with the selected kind.
   * Kind is a value of MediaStore.Images.Thumbnails.MICRO_KIND or MediaStore.Images.Thumbnails.MINI_KIND
   * */
  public static Observable<Bitmap> getVideoThumbnailFromPathWithKind(final String path,
      final int kind) {
    return Observable.defer(new Func0<Observable<Bitmap>>() {
      @Override
      public Observable<Bitmap> call() {
        return Observable.just(ThumbnailUtils.createVideoThumbnail(path, kind));
      }
    });
  }

  /*
   * Get image thumbnail from an image file, by path.
   * */
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
    logDebug("Filepath in getFileType: " + filePath);
    final String[] parts = filePath.split("/");
    return Observable.fromCallable(new Func0<String>() {
      @Override
      public String call() {
        return parts.length > 0 ? URLConnection.guessContentTypeFromName(parts[0]) : null;
      }
    });
  }

  /*
   * Get path from Uri, for a FileDocument.
   * */
  public static Observable<String> getPathFromUriForFileDocument(final Context context,
      final Uri contentUri) {
    return Observable.fromCallable(new Func0<String>() {
      @Override
      public String call() {
        String pathFound = null;
        Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            pathFound =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
          }
          cursor.close();
          logDebug("Path found:" + pathFound);
        }
        return pathFound;
      }
    });
  }

  /*
   * Get path from Uri, for a MediaDocument.
   * */
  public static Observable<String> getPathFromUriForMediaDocument(final Context context,
      final Uri mediaUri, final String mediaDocumentId) {
    return Observable.fromCallable(new Func0<String>() {
      @Override
      public String call() {
        String pathFound = null;
        Cursor cursor = context.getContentResolver()
            .query(mediaUri, null, Constants.ID_COLUMN_VALUE + " =?",
                new String[] { mediaDocumentId }, null);
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            pathFound =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
          }
          cursor.close();
          logDebug("Path found:" + pathFound);
        }
        return pathFound;
      }
    });
  }

  /*
   * Get path from Uri, for an ImageDocument.
   * */
  public static Observable<String> getPathFromUriForImageDocument(final Context context,
      final String mediaDocumentId) {
    return Observable.fromCallable(new Func0<String>() {
      @Override
      public String call() {
        String pathFound = null;
        Cursor cursor = context.getContentResolver()
            .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null,
                Constants.ID_COLUMN_VALUE + " =?", new String[] { mediaDocumentId }, null);
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            pathFound =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
          }
          cursor.close();
          logDebug("Path found:" + pathFound);
        }
        return pathFound;
      }
    });
  }

  /*
   * Get path from Uri, for a VideoDocument.
   * */
  public static Observable<String> getPathFromUriForVideoDocument(final Context context,
      final String mediaDocumentId) {
    return Observable.fromCallable(new Func0<String>() {
      @Override
      public String call() {
        String pathFound = null;
        Cursor cursor = context.getContentResolver()
            .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null,
                Constants.ID_COLUMN_VALUE + " =?", new String[] { mediaDocumentId }, null);
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            pathFound =
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
          }
          cursor.close();
          logDebug("Path found:" + pathFound);
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
      return uri.getLastPathSegment().contains(Constants.IMAGE) || uri.getLastPathSegment()
          .contains(Constants.VIDEO);
    }
    return Constants.MEDIA_AUTHORITY.equals(uri.getAuthority());
  }

  private static boolean checkWriteExternalPermission(Context context) {
    int res = context.checkCallingOrSelfPermission(Constants.WRITE_EXTERNAL_PERMISSION);
    return (res == PackageManager.PERMISSION_GRANTED);
  }

  public static String getTypeWithMimeTypeMap(String fileName) {
    String fileExtension = fileName.substring((fileName.lastIndexOf('.')) + 1);
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
  }

  public static String getTypeWithURLConnection(String fileName) {
    return URLConnection.guessContentTypeFromName(fileName);
  }

  private static boolean createFile(String path) throws IOException {
    if (!checkExistence(path)) {
      File temp = new File(path);
      if (!temp.createNewFile()) {
        logDebug("Something went wrong while creating file: " + path);
      } else {
        logDebug("File: " + path + " created.");
      }
    } else {
      logDebug("File: " + path + " already exists.");
      return false;
    }
    return true;
  }

  private static boolean checkExistence(String path) {
    logDebug("Check path: " + path);
    File temp = new File(path);
    return temp.exists();
  }

  private static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest)
      throws IOException {
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

  private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
      int reqHeight) {
    final int height = options.outHeight;
    final int width = options.outWidth;
    logDebug("Height: " + height + " Width: " + width);
    int inSampleSize = 1;

    if (height > reqHeight || width > reqWidth) {

      final int halfHeight = height / 2;
      final int halfWidth = width / 2;

      while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
        inSampleSize *= 2;
      }
    }

    return inSampleSize;
  }

  private static File fileFromUri(Context context, Uri data, MimeMap mimeTypeMap) throws Exception {
    DocumentFile file = DocumentFile.fromSingleUri(context, data);
    String fileType = file.getType();
    String fileName = file.getName();
    File fileCreated;
    ParcelFileDescriptor parcelFileDescriptor =
        context.getContentResolver().openFileDescriptor(data, Constants.READ_MODE);
    InputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
    logDebug("External cache dir:" + context.getExternalCacheDir());
    String filePath = context.getExternalCacheDir() + Constants.FOLDER_SEPARATOR + fileName;
    String fileExtension = fileName.substring((fileName.lastIndexOf('.')) + 1);
    String mimeType = mimeTypeMap == MimeMap.MimeTypeMap ? getTypeWithMimeTypeMap(fileName)
        : getTypeWithURLConnection(fileName);

    logDebug("From Google Drive guessed type: " + mimeType);
    logDebug("Extension: " + fileExtension);

    if (fileType.equals(Constants.APPLICATION_PDF) && mimeType == null) {
      filePath += "." + Constants.PDF_EXTENSION;
    }

    if (!createFile(filePath)) {
      return new File(filePath);
    }

    ReadableByteChannel from = Channels.newChannel(inputStream);
    WritableByteChannel to = Channels.newChannel(new FileOutputStream(filePath));
    fastChannelCopy(from, to);
    from.close();
    to.close();
    fileCreated = new File(filePath);
    logDebug("Path for made file: " + fileCreated.getAbsolutePath());
    return fileCreated;
  }

  public static void setLoggingEnabled(boolean loggingEnabled) {
    LOGGING_ENABLED = loggingEnabled;
  }

  private static void logDebug(String message) {
    if (LOGGING_ENABLED) Log.d(TAG, message);
  }

  private static void logError(Throwable throwable) {
    if (LOGGING_ENABLED) Log.e(TAG, "", throwable);
  }
}
