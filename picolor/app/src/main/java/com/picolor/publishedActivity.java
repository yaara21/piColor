package com.picolor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class publishedActivity extends AppCompatActivity {

    private LinearLayout myLayout;
    private StorageReference pImageRef;
    private StorageReference pStorageRef;
    private FirebaseStorage pStorage;

//    private int SERVER_PORT;
//    private String SERVER_IP;
    private DatabaseReference pRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_published);

        myLayout = (LinearLayout)findViewById(R.id.pLayout);
//        SERVER_PORT = 2106;
//        SERVER_IP = "192.168.1.248";
        pStorage = FirebaseStorage.getInstance();
        pStorageRef = pStorage.getReference();
        pRef = FirebaseDatabase.getInstance().getReference().child("online");
        //when class starts - loads all published photos
        getNames("");
        //when class starts - sends to server that user is listening for updates
        socketListening("listening!!!h\n", "listen");

    }

    //create socket connection, send message and get responses when new photos uploaded
    private void socketListening(String message, String meaning){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // create a socket connection to the server
                    Socket client_socket = new Socket(UploadActivity.SERVER_IP, UploadActivity.SERVER_PORT);
                    // create an output stream to send data to the server
                    OutputStream outputStream = client_socket.getOutputStream();
                    PrintWriter writer = new PrintWriter(outputStream, true);
                    //encrypting message
                    String toSend = UploadActivity.crypto(message);
                    if (toSend.equals("eror"))
                        throw new Exception("encrypting failed");
                    // Send message to the server
                    writer.println(toSend);
                    InputStream inputStream = client_socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    // read message from server
                    String response = "";
                    while (true) {
                        response = reader.readLine();
                        System.out.println(response);
                        if (response.equals("uploaded") && (meaning.equals("listen"))) {
                            //get names will load and add the new photo to the screen
                            getNames("new");
                        }
                    }
                    //socket connection never closed. it keeps listening in the loop
//                    reader.close();
//                    writer.close();
//                    client_socket.close();
                }
                catch (Exception e){
                    e.printStackTrace();
                    Log.d("published", "socket failed");
                }
            }
        }).start();
    }

    private void getNames(String command){
        pRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> photosNames = new ArrayList<>();
                //retrieve all photos names from firebase into an array
                for (DataSnapshot ds : snapshot.getChildren())
                {
                    String name = ds.getKey() + ".jpg";
                    photosNames.add(name);
                }
                //"new" means client got a message that one new photo has been uploaded.
                // so needs to show only last image
                if (command.equals("new")){
                    List<String> newPhoto = new ArrayList<>();
                    Toast.makeText(publishedActivity.this, "new photo has been published", Toast.LENGTH_SHORT).show();
                    String lastphoto = photosNames.get(photosNames.size() - 1);
                    newPhoto.add(lastphoto);
                    handleNext(newPhoto);
                }
                //if not "new", need to show all photos
                else {
                    handleNext(photosNames);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    //retrieve image from firebase in requested path, and shows in imageView
    private void showImage(String photoPath, ImageView cImageView) {
        pImageRef = pStorageRef.child(photoPath);
        pImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Glide.with(getApplicationContext()).load(uri).into(cImageView);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(publishedActivity.this,"EROR occurred in showing. try again", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //for each photo, create a new ImageView, and sends photoname and new imageview to show image
    private void handleNext(List<String> photosNames){
        for (String photo : photosNames){
            ImageView cImageView = new ImageView(publishedActivity.this);
            LinearLayout.LayoutParams par = new LinearLayout.LayoutParams(765, 765);
            par.setMargins(5, 10, 5, 10);
            cImageView.setLayoutParams(par);
            //add the new photo up:
            myLayout.addView(cImageView, 0);
            showImage(photo, cImageView);

        }

    }


}