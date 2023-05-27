package com.picolor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.lang.ref.Reference;

public class MyAlbumActivity extends AppCompatActivity {


    private Button aBtnNext;
    private Button aBtnDownload;
    private ImageView aImageView;
    private final static FirebaseAuth aAuth = FirebaseAuth.getInstance();

    private final static DatabaseReference aRef = FirebaseDatabase.getInstance().getReference().child("users");
    private final static StorageReference aStorageRef = FirebaseStorage.getInstance().getReference();
    private StorageReference aImageRef;
    private String aId;
    private Integer user_num;
    private Integer currentNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_album);

        aImageView = findViewById(R.id.albumImage);
        aBtnNext = findViewById(R.id.aBtnNext);
        aBtnDownload = findViewById(R.id.aBtnDownload);
        aId = aAuth.getCurrentUser().getUid();
        currentNum = 0;




        aBtnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getNumber(currentNum);
                        currentNum = currentNum + 1;
                        //Toast.makeText(MyAlbumActivity.this, Integer.toString(user_num), Toast.LENGTH_SHORT).show();
                        //changePhoto();
                    }
                });
            }
        });
        aBtnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // checks permission for downloading
                checkPer();
                if (ContextCompat.checkSelfPermission(MyAlbumActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    // Get the bitmap from the imageView
                    Bitmap bitmap1 = ((BitmapDrawable) aImageView.getDrawable()).getBitmap();
                    // Save the bitmap to the user's gallery
                    String photoName = aId.substring(0, 10) + currentNum;
                    String filePath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap1, photoName, "coloredImage");
                    // Notify the user that the image has been saved
                    Toast.makeText(MyAlbumActivity.this, "image has been saved to gallery!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private void getNumber(int currentNum)
    {
        aRef.child(aId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String sNum = (String) snapshot.getValue();
                user_num = Integer.parseInt(sNum);
                //Toast.makeText(MyAlbumActivity.this, Integer.toString(user_num), Toast.LENGTH_SHORT).show();
                changePhoto(user_num, currentNum);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyAlbumActivity.this, "EROR, please try again", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void changePhoto(int user_num, int currentNum)
    {
        //if user has no photos
        if (user_num == 0)
        {
            Toast.makeText(this, "no photos yet, go ahead and color!", Toast.LENGTH_LONG).show();
        }
        else
        {
            //if user watched all photos. it tell him and start again
            if (user_num <= currentNum) {
                currentNum = 0;
                Toast.makeText(this, "go ahead and color more photos!", Toast.LENGTH_LONG).show();
            }
            //showing the next photo
            String photoPath = aId + UploadActivity.addZeros(Integer.toString(currentNum)) + ".jpg";
            showImage(photoPath);
        }

    }
    //retrieve image from firebase in requested path, and shows in imageView
    private void showImage(String photoPath) {
        aImageRef = aStorageRef.child(photoPath);
        aImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getApplicationContext()).load(uri).into(aImageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MyAlbumActivity.this, "EROR occurred. try again", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //2 next functions: permission to write to the phone storage - necessary for downloading
    private void checkPer()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // if user denied
        if (requestCode == 1) {
            Toast.makeText(this, "Permission is necessary for downloading", Toast.LENGTH_SHORT).show();
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        }

    }
}