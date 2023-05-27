package com.picolor;

import static java.lang.Thread.sleep;

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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.sql.Time;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import okio.Timeout;


public class UploadActivity extends AppCompatActivity {

    private ImageView uImageView;
    private Button btnChange;
    private Button btnColoring;
    private Button btnOnline;
    private Button btnSave;
    FirebaseAuth uAuth;
    DatabaseReference uRef;
    DatabaseReference uOnlineRef;
    StorageReference uStorageRef;
    StorageReference uImageRef;
    FirebaseStorage uStorage;
    private String uid;
    private String stringBytesphoto;
    public static final int SERVER_PORT = 2106;
    public static final String SERVER_IP = "192.168.1.248";
    //String urlPost = "http://10.0.2.2:2106/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        uImageView = findViewById(R.id.uImageView);
        btnChange = findViewById(R.id.uBtnChange);
        btnColoring = findViewById(R.id.uBtnColor);
        btnOnline = findViewById(R.id.uBtnOnline);
        btnSave = findViewById(R.id.uBtnSave);
        uAuth = FirebaseAuth.getInstance();
        uRef = FirebaseDatabase.getInstance().getReference().child("users");
        uOnlineRef = FirebaseDatabase.getInstance().getReference().child("online");
        uid = uAuth.getCurrentUser().getUid();

        uStorage = FirebaseStorage.getInstance();
        uStorageRef = uStorage.getReference();


        Random rnd = new Random();
        Intent recIntent = getIntent();
        //gets the user's chosen photo from 'main activity'
        Uri recUri = (Uri) recIntent.getParcelableExtra("sentImage");
        //show photo in imageview
        uImageView.setImageURI(recUri);

        //create bytes array for the image
        byte[] photoArray;
        InputStream iStream = null;
        try {
            iStream = getContentResolver().openInputStream(recUri);
            photoArray = getBytes(iStream);
            stringBytesphoto = android.util.Base64.encodeToString(photoArray, Base64.NO_WRAP);

            //new String(photoArray, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // if user wants to change photo, sends him to 'main activity'
        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(UploadActivity.this, MainActivity.class));
            }
        });

        btnColoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent inte1 = new Intent(Intent.ACTION_VIEW, Uri.parse(urlPost));
                inte1.putExtra("sentImage", mImageUri);
                startActivity(inte1);*/
                //MediaType mediaType = MediaType.parse("text/plain; charset=utf-8");
                //RequestBody postBody = RequestBody.create(mediaType, stringBytesphoto+ "!!!"+ uid);
                //sendRequest(urlPost, postBody);
                String message = "color!!!" + stringBytesphoto + "!!!" + uid;
                System.out.println(message);
                //calls socket creation for coloring the photo
                socketColor(message, "color");

            }
        });

        btnOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //to retrieve user's number of photos and upload new photo name to firebase
                increacePhotoNum("online", "online");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    System.out.println("sleep fell");
                }
                //calls socket creation for announcing photo upload
                socketColor("upload", "upload");
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // checks permission for downloading
                checkPer();
                if (ContextCompat.checkSelfPermission(UploadActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                    // Get the bitmap from the imageView
                    Bitmap bitmap1 = ((BitmapDrawable) uImageView.getDrawable()).getBitmap();
                    // Save the bitmap to the user's gallery
                    String photoName = uid.substring(0, 10) + (100 + rnd.nextInt(899));
                    String filePath = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap1, photoName, "coloredImage");
                    // Notify the user that the image has been saved
                    Toast.makeText(UploadActivity.this, "image has been saved to gallery!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    // makes photo into bytes array for the sending
    private byte[] getBytes(InputStream inputStream) throws IOException {

        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1){
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }



    //for coloring - increasing by one the user number-of-photos and shows it
    // for uploading - get the photo number
    private void increacePhotoNum(String childName, String purpose) {

        uRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String snum = (String) snapshot.getValue();
                System.out.println(snum);
                Integer num = Integer.parseInt(snum);
                if (purpose.equals("color")) {
                    num = num + 1;
                    snum = String.valueOf(num);
                    uRef.child(childName).setValue(snum);
                    String photoPath = uid + addZeros(Integer.toString(num - 1)) + ".jpg";
                    showImage(photoPath);
                    btnSave.setVisibility(View.VISIBLE);
                }
                if (purpose.equals("online")) {
                    addToOnline(Integer.toString(num -1));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UploadActivity.this, "EROR. please try again", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //create socket connection, send message and get response
    private void socketColor(String message, String meaning) {
        System.out.println("before thread");
        new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println("started new thread");
                        // create a socket connection to the server
                        Socket client_socket = new Socket(SERVER_IP, SERVER_PORT);
                        System.out.println("connected");
                        // create an output stream to send data to the server
                        OutputStream outputStream = client_socket.getOutputStream();
                        PrintWriter writer = new PrintWriter(outputStream, true);
                        //encrypt message
                        String toSend = crypto(message);
                        if (toSend.equals("eror"))
                            throw new Exception("encrypting failed");
                        // send message to the server
                        writer.println(toSend);
                        InputStream inputStream = client_socket.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        // read message from server
                        String response = reader.readLine();
                        System.out.println(response);
                        //if color was successful:
                        if (response.equals("good") && (meaning.equals("color"))) {
                            increacePhotoNum(uid, "color");
                            //also calls showImage
                            btnOnline.setVisibility(View.VISIBLE);
                            //btnSave.setVisibility(View.VISIBLE);
                        }
                        System.out.println("after receiving");
                        //close connection
                        reader.close();
                        writer.close();
                        client_socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("upload", "socket failed");
                    }
                }
            }).start();
    }

    //retrieve image from firebase in requested path, and shows in imageView
    private void showImage(String photoPath){
        uImageRef = uStorageRef.child(photoPath);
        uImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getApplicationContext()).load(uri).into(uImageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UploadActivity.this, "EROR occurred. try again", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //2 next functions: permission to write to the phone storage - necessary for downloading
    private void checkPer() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
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

    //uploading photo name to firebase - when client asks to publish it
    private void addToOnline(String num){
        String path1 = uid + addZeros(num); //+ ".jpg";
        uOnlineRef.child(path1).setValue("");
        Toast.makeText(this, "upload succeeded", Toast.LENGTH_SHORT).show();

    }

    //encrypting messages to server
    public static String crypto(String message){
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            //fake key:
            byte[] keyBytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
            String algorithm  = "RawBytes";
            SecretKeySpec key = new SecretKeySpec(keyBytes, algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] byMessage = message.getBytes("UTF-8");
            byte[] b = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
            ByteArrayOutputStream helpst = new ByteArrayOutputStream();
            helpst.write(b);
            helpst.write(byMessage);
            byte[] together = helpst.toByteArray();
            byte[] hidMessage = cipher.doFinal(together);
            //end
            String stringHid = android.util.Base64.encodeToString(hidMessage, Base64.NO_WRAP);
            return stringHid;
        }catch (Exception e){
            Log.d("encreypting", "crypto failed");
            return "eror";
        }
    }
    //formatting photo number - 19 to 0000000019
    public static String addZeros(String num){
        while (num.length() < 10)
            num = "0" + num;
        System.out.println(num);
        return num;
    }
}