package com.picolor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CreateAcActivity extends AppCompatActivity {


    private EditText cMail;
    private EditText cPassword;
    private Button cBtnCreate;
    private static final FirebaseAuth cAuth = FirebaseAuth.getInstance();
    private static final DatabaseReference cRef = FirebaseDatabase.getInstance().getReference().child("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_ac);

        cMail = findViewById(R.id.cEnterMail);
        cPassword = findViewById(R.id.cEnterPassword);
        cBtnCreate = findViewById(R.id.cBtnCreate);
        cBtnCreate.setOnClickListener(view -> {
            createAccount();
        });
    }

    private void createAccount(){
        String mail = cMail.getText().toString();
        String password = cPassword.getText().toString();
        //checks that the text is not empty
        if (TextUtils.isEmpty(mail)){
            Toast.makeText(this, "please enter email", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)){
            Toast.makeText(this, "please enter password", Toast.LENGTH_SHORT).show();
        } else{
            //create the user
            cAuth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){
                        Toast.makeText(CreateAcActivity.this, "account created successfully! (: ", Toast.LENGTH_SHORT).show();
                        addToDict();
                        startActivity(new Intent(CreateAcActivity.this, LogginActivity.class));
                    } else{
                        Toast.makeText(CreateAcActivity.this, "Creation Eror: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }


    //in firebase create for the new user number of photos - 0
    private void addToDict() {
        String uid = cAuth.getCurrentUser().getUid();
        cRef.child(uid).setValue("0");


    }


}