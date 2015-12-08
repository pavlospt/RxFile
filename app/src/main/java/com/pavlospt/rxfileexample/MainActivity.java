package com.pavlospt.rxfileexample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.pavlospt.rxfile.RxFile;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private ImageView mBitmap;

    private static String TAG = MainActivity.class.getSimpleName();

    private static int REQUEST_FOR_IMAGES_VIDEOS = 1;
    private static int REQUEST_FOR_FILES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBitmap = (ImageView) findViewById(R.id.iv_bitmap);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startIntentForFilePick();
            }
        });

    }

    private void startIntentForFilePick() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Pick files"), REQUEST_FOR_FILES);
    }

    private void startIntentForPick(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Pick files"), REQUEST_FOR_IMAGES_VIDEOS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_FOR_IMAGES_VIDEOS){
                if(data.getData() != null){
                    Log.e(TAG,"Going for service.");
                    DocumentFile file = DocumentFile.fromSingleUri(this,data.getData());
                    Log.e(TAG,"FileName: " + file.getName() + " FileType: " + file.getType());
                    Log.e(TAG,"Document uri: " + file.getUri());
                    fetchThumbnail(file.getUri());
                }else{
                    Log.e(TAG,"Single selection");
                    DocumentFile file = DocumentFile.fromSingleUri(this,data.getClipData().getItemAt(0).getUri());
                    Log.e(TAG,"FileName: " + file.getName() + " FileType: " + file.getType());
                    Log.e(TAG,"Document uri: " + file.getUri());
                    try {
                        ParcelFileDescriptor descriptor = getContentResolver().openFileDescriptor(file.getUri(),"r");
                        FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
                        Log.e(TAG,"File Descriptor: " + fileDescriptor.toString());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            }else if(requestCode == REQUEST_FOR_FILES){
                if(data.getData() != null) {
                    Log.e(TAG,"Going for file.");
                    DocumentFile file = DocumentFile.fromSingleUri(this,data.getData());
                    Log.e(TAG,"FileName: " + file.getName() + " FileType: " + file.getType());
                    Log.e(TAG,"Document uri: " + file.getUri());
                    cacheFileFromDrive(file.getUri(), file.getName());
                }
            }
        }
    }

    private void cacheFileFromDrive(Uri uri, String fileName) {
        RxFile.createFileFromGoogleDrive(this,uri, fileName)
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

    }

    private void fetchThumbnail(Uri data) {
        RxFile.getThumbnailFromUri(this,data)
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
