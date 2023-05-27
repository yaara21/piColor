package com.picolor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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


public class LogginActivity extends AppCompatActivity {


    private Button btnCreateAcc;
    private Button btnLog;
    private EditText lMail;
    private EditText lPassword;
    private static final FirebaseAuth lAuth = FirebaseAuth.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loggin);

        lMail = findViewById(R.id.lEnterMail);
        lPassword = findViewById(R.id.lEnterPassword);
        btnCreateAcc = (Button) findViewById(R.id.lBtnCreate);
        btnLog = findViewById(R.id.lBtnLog);

        //sends user to 'create account'
        btnCreateAcc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LogginActivity.this, CreateAcActivity.class));
            }
        });
        btnLog.setOnClickListener(view -> {
            userLoggin();
        });
    }

    //handle loggin request
    private void userLoggin() {
        String mail = lMail.getText().toString();
        String password = lPassword.getText().toString();
        //checks that text is not empty
        if (TextUtils.isEmpty(mail)) {
            Toast.makeText(this, "please enter email", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "please enter password", Toast.LENGTH_SHORT).show();
        } else {
            //sends mail and password to firebase for checking and connecting
            lAuth.signInWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LogginActivity.this, "logged in successfully! (:", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LogginActivity.this, MainActivity.class));
                    } else {
                        Toast.makeText(LogginActivity.this, "loggin Eror: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }
}