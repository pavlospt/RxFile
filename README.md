[![Circle CI](https://circleci.com/gh/pavlospt/RxFile/tree/master.svg?style=svg)](https://circleci.com/gh/pavlospt/RxFile/tree/master)
[ ![Download](https://api.bintray.com/packages/pavlospt/android-libraries/RxFile/images/download.svg) ](https://bintray.com/pavlospt/android-libraries/RxFile/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxFile-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2901)
[![Android_Weekly](https://img.shields.io/badge/Android%20Weekly-RxFile-green.svg)](http://androidweekly.net/issues/issue-183)

# RxFile
Rx methods to get a File and Image or Video thumbnails from any DocumentProvider on Android (Drive, Dropbox, Photos etc)

# How To Install

Available on `jCenter()` and `mavenCentral()`
```
compile 'com.github.pavlospt:rxfile:1.7'
```

# How To Use
It is really easy to use it, depending on your needs it returns the appropriate file or Bitmap. You can fetch a Bitmap when you are selecting an Image or a Video. The fetched Bitmap is a thumbnail of the selected Image or Video. When you choose to select a file from Dropbox or Drive, the file is being downloaded and copied to the Library's cache folder. It then returns the File object and you have complete access over it in case you need to upload it to a server or in a similar use case.

To enable logging:
```java
RxFile.setLoggingEnabled(true);
```

To get a File, use: (need to change the name of the method) 
```java
Observable<File> createFileFromUri(final Context context, final Uri data);
Observable<List<File>> createFileFromUri(final Context context, final ArrayList<Uri> uris);
Observable<List<File>> createFilesFromClipData(final Context context, final ClipData clipData);
```

To get a thumbnail, use:
```java
Observable<Bitmap> getThumbnail(Context context, Uri uri);
Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth, int requiredHeight);
Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth, int requiredHeight, int kind);
```

Get files from a ClipData object (applies to multiple file selection through intent):
```java
RxFile.createFilesFromClipData(this,clipData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<File>>() {
                    @Override
                    public void onCompleted() {
                        Timber.e("onCompleted() for Files called");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e("Error on files fetching:" + e.getMessage());
                    }

                    @Override
                    public void onNext(List<File> files) {
                        Timber.e("Files list size:" + files.size());
                        for(File f : files){
                            Timber.e("onNext() file called:" + f.getAbsolutePath());
                        }
                    }
                });
```
Get file from single Uri:
```java
RxFile.createFileFromUri(this,uri)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<File>() {
                    @Override
                    public void onCompleted() {
                        Timber.e("onCompleted() for File called");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e("Error on file fetching:" + e.getMessage());
                    }

                    @Override
                    public void onNext(File file) {
                        Timber.e("onNext() file called:" + file.getAbsolutePath());
                    }
                });
```

Get Thumbnail from Uri (photo or video):
```java
RxFile.getThumbnail(this,data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {
                        Timber.e("onCompleted() called");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e("onError called with: " +  e.getMessage());
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        mBitmap.setImageBitmap(bitmap);
                    }
                });
```

For more, check the [Wiki](https://github.com/pavlospt/RxFile/wiki/Methods) 


Credits
=======
Author : Pavlos-Petros Tournaris (p.tournaris@gmail.com)

Google+ : [+Pavlos-Petros Tournaris](https://plus.google.com/u/0/+PavlosPetrosTournaris/)

Facebook : [Pavlos-Petros Tournaris](https://www.facebook.com/pavlospt)

LinkedIn : [Pavlos-Petros Tournaris](https://www.linkedin.com/pub/pavlos-petros-tournaris/44/abb/218)

(In case you use this in your app let me know to make a list of apps that use it! )

License
=======

    Copyright 2015 Pavlos-Petros Tournaris

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
