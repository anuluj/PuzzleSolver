package com.example.mat.puzzlesolver;

import org.opencv.core.Mat;
import org.opencv.imgproc.*;
import org.opencv.android.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InitialActivity extends Activity {

    Button btPhotoOfImage, btPhotoOfPuzzles, btDemo, btContinue;
    int toContinue = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    UriHelper mApp;
    int photoType;
    Context context;
    Uri photoURI;
    ImageView mImageView,mImageView2;
    String mCurrentPhotoPath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initial);
        btPhotoOfImage = (Button) findViewById(R.id.btPhotoOfImage);
        btPhotoOfPuzzles = (Button) findViewById(R.id.btPhotoOfPuzzles);
        btContinue = (Button) findViewById(R.id.btContinue);
        btDemo = (Button) findViewById(R.id.btDemo);
        //btContinue.setEnabled(false);

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView2 = (ImageView) findViewById(R.id.imageView2);

        mApp = ((UriHelper) getApplicationContext());
        context = getApplicationContext();

        btPhotoOfImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent(UriHelper.FULL_PHOTO);
                photoType=UriHelper.FULL_PHOTO;
            }
        });
        btPhotoOfPuzzles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent(UriHelper.PUZZLE_PHOTO);
                photoType=UriHelper.PUZZLE_PHOTO;

            }
        });
        btContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: activity do przetwarzania obrazu
                Toast.makeText(context,"activity do przetwarzania obrazu", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(context, ExtractingPiecesActivity.class);
                i.putExtra("isDemo", false);
                startActivity(i);

            }
        });
        btDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, ExtractingPiecesActivity.class);
                i.putExtra("isDemo", true);
                startActivity(i);
            }
        });
    }
    public void isContinue(){
        if(toContinue==2){
            btContinue.setEnabled(true);
            btContinue.setTextColor(Color.GREEN);
            Toast.makeText(context,"Mozesz przejsc do kolejnego etapu", Toast.LENGTH_SHORT).show();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(context, ""+photoURI.toString(),Toast.LENGTH_LONG).show();
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),photoURI);
                if((bitmap.getWidth() > 3000 )|| (bitmap.getHeight() >3000) ){
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/ 2, bitmap.getHeight()/2, false);
                    Log.d("BITMAP", "TOO BIG:" + photoURI.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(photoType == UriHelper.FULL_PHOTO)
                mImageView.setImageBitmap(bitmap);
            else
                mImageView2.setImageBitmap(bitmap);
        }
    }
    private void dispatchTakePictureIntent(int photoType) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d("Error: ", ex.getMessage());
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                Log.d("file Name: ", photoURI.toString());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                if(photoType == UriHelper.FULL_PHOTO) {
                    mApp.setFullPhotoUri(photoURI.toString());
                }
                else {
                    mApp.setPuzzlesPhotoUri(photoURI.toString());
                }
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d("file Name: ", imageFileName);
        return image;
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("file_uri", photoURI);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        photoURI = savedInstanceState.getParcelable("file_uri");
    }
}
