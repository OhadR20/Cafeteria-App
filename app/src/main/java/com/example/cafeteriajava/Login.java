package com.example.cafeteriajava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Login extends AppCompatActivity {
    // Declare UI components and Firebase authentication
    Button btnLogin;
    TextView registerNow;
    TextInputEditText etEmail;
    TextInputEditText etPassword;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){ // If a user is signed in, proceed to the main activity directly
            Intent intent = new Intent(getApplicationContext(), MainActivity.class); // Create intent for MainActivity
            startActivity(intent); // Start the MainActivity
            finish(); // Close the Login activity so the user can't go back to it
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Set the layout for the Login activity
        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Authentication instance
        // Bind UI elements from the layout using their IDs
        btnLogin = findViewById(R.id.btnLogin);
        registerNow = findViewById(R.id.registerNow);
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
        // Set click listener for the "Register Now" TextView
        registerNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // When clicked, create an intent to navigate to the Register activity
                Intent intent = new Intent(getApplicationContext(), Register.class);
                startActivity(intent); // Start the Register activity
                finish(); // Close the Login activity
            }
        });
        // Set click listener for the Login button
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email, password; // Variables to store the email and password from input fields
                progressBar.setVisibility(View.VISIBLE); // Show the progress bar while logging in
                // Retrieve the text entered in the email and password fields
                email = String.valueOf(etEmail.getText());
                password = String.valueOf(etPassword.getText());
                // Check if the email field is empty; if yes, show a Toast and stop further execution
                if (TextUtils.isEmpty(email)){
                    Toast.makeText(Login.this, "Enter email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Check if the password field is empty; if yes, show a Toast and stop further execution
                if (TextUtils.isEmpty(password)){
                    Toast.makeText(Login.this, "Enter password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Use Firebase Authentication to sign in with the provided email and password
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener( new OnCompleteListener<AuthResult>() {

                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE); // Hide the progress bar after the task is complete
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Toast.makeText(Login.this, "Login successful", Toast.LENGTH_SHORT);
                                    Log.d("Login", "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w("Login", "signInWithEmail:failure", task.getException());
                                    Toast.makeText(Login.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


            }
        });
    }
}