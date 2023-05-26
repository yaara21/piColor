package com.picolor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

//    private static final int PERMISSION_REQUEST_CODE = 200;
//    private static final int PICK_IMAGE_REQUEST = 100;
//    private static final int REQUEST_IMAGE_CAPTURE = 101;

    private Button mBtnPickImage;
    private Button mBtnAlbum;
    private Button mBtnOthers;
    private ImageView mImageView;

    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnPickImage = findViewById(R.id.upbutton);
        mBtnAlbum = findViewById(R.id.mBtnAlbum);
        mBtnOthers = findViewById(R.id.mBtnOthers);
        mImageView = findViewById(R.id.imageView);

        //sends user to 'published' activity
        mBtnOthers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, publishedActivity.class));
            }
        });

        //sends user to 'my album' activity
        mBtnAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, MyAlbumActivity.class));
            }
        });

        mBtnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open gallery for choosing photo
            Intent intent1 = new Intent();
            intent1.setAction(intent1.ACTION_GET_CONTENT);
            intent1.setType("image/*");
            startActivityForResult(intent1, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //checks chosen data
        if (requestCode == 1 && data!=null){
            mImageUri = data.getData();
            mImageView.setImageURI(mImageUri);
            //sends user to 'upload' activity, where coloring function is, and sends the selected photo
            Intent intent2 = new Intent(MainActivity.this, UploadActivity.class);
            intent2.putExtra("sentImage", mImageUri);
            startActivity(intent2);

        }

    }
}