package com.example.cafeteriajava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    Button btnReg;
    TextInputEditText etEmail, etPassword, etFirstName, etLastName;;
    TextView tvloginNow;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    Switch switchAdmin;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnReg = findViewById(R.id.btnRegister);
        tvloginNow = findViewById(R.id.LoginNow);
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        etFirstName = findViewById(R.id.firstname);
        etLastName = findViewById(R.id.lastname);
        switchAdmin = findViewById(R.id.switchAdmin);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();

        tvloginNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        btnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email, password, firstname, lastName;
                progressBar.setVisibility(View.VISIBLE);
                email = String.valueOf(etEmail.getText());
                password = String.valueOf(etPassword.getText());
                firstname = String.valueOf(etFirstName.getText());
                lastName = String.valueOf(etLastName.getText());

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(Register.this, "Enter email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Register.this, "Enter password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(firstname)) {
                    Toast.makeText(Register.this, "Enter first name", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                if (TextUtils.isEmpty(lastName)) {
                    Toast.makeText(Register.this, "Enter last name", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    saveUserData(user, firstname, lastName);
                                    Toast.makeText(Register.this, "User created successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(Register.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

    }

    private void saveUserData(FirebaseUser user, String firstName, String lastName) {
        if (user != null) {
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("admin", switchAdmin.isChecked());
            userData.put("firstname", firstName);
            userData.put("lastname", lastName);
            databaseReference.child(user.getUid()).setValue(userData);
        }
    }
}
