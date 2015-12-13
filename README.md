[![Circle CI](https://circleci.com/gh/pavlospt/RxFile/tree/master.svg?style=svg)](https://circleci.com/gh/pavlospt/RxFile/tree/master)
[ ![Download](https://api.bintray.com/packages/pavlospt/android-libraries/RxFile/images/download.svg) ](https://bintray.com/pavlospt/android-libraries/RxFile/_latestVersion)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RxFile-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/2901)

# RxFile
Rx methods to get a File and Image or Video thumbnails from any DocumentProvider on Android (Drive, Dropbox, Photos etc)

# How To Install

Available on `jCenter()` and `mavenCentral()`
```
compile 'com.github.pavlospt:rxfile:1.3'
```

# How To Use
It is really easy to use it, depending on your needs it returns the appropriate file or Bitmap. You can fetch a Bitmap when you are selecting an Image or a Video. The fetched Bitmap is a thumbnail of the selected Image or Video. When you choose to select a file from Dropbox or Drive, the file is being downloaded and copied to the Library's cache folder. It then returns the File object and you have complete access over it in case you need to upload it to a server or in a similar use case.

To get a File, use: (need to change the name of the method) 
```
Observable<File> createFileFromUri(final Context context, final Uri data);
Observable<List<File>> createFileFromUri(final Context context, final ArrayList<Uri> uris);
Observable<List<File>> createFilesFromClipData(final Context context, final ArrayList<Uri> uris);
```

To get a thumbnail, use:
```
Observable<Bitmap> getThumbnail(Context context, Uri uri);
Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth, int requiredHeight);
Observable<Bitmap> getThumbnail(Context context, Uri uri, int requiredWidth, int requiredHeight, int kind);
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
