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
    // Declare member variables for UI components and Firebase
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
        if (currentUser != null) { // If user exists, bypass registration and go directly to main screen
            Intent intent = new Intent(getApplicationContext(), MainActivity.class); // Create intent to start MainActivity
            startActivity(intent); // Start MainActivity
            finish(); // Close the Register activity
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call the superclass implementation
        setContentView(R.layout.activity_register); // Set the layout for the registration screen

        // Bind UI elements
        btnReg = findViewById(R.id.btnRegister);
        tvloginNow = findViewById(R.id.LoginNow);
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        etFirstName = findViewById(R.id.firstname);
        etLastName = findViewById(R.id.lastname);
        switchAdmin = findViewById(R.id.switchAdmin);
        progressBar = findViewById(R.id.progressBar);
        // Initialize Firebase Authentication instance
        mAuth = FirebaseAuth.getInstance();

        // Set click listener for the "Login Now" TextView to redirect to the Login activity
        tvloginNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class); // Create intent for Login activity
                startActivity(intent); // Start the Login activity
                finish(); // Close the Register activity
            }
        });

        // Set click listener for the registration button to process user registration
        btnReg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Declare variables to hold user input data
                String email, password, firstname, lastName;
                progressBar.setVisibility(View.VISIBLE); // Show progress bar to indicate the registration process has started

                // Retrieve text from input fields and convert to String
                email = String.valueOf(etEmail.getText());
                password = String.valueOf(etPassword.getText());
                firstname = String.valueOf(etFirstName.getText());
                lastName = String.valueOf(etLastName.getText());

                // Validate that email is not empty; if empty, show a Toast and stop the process
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(Register.this, "Enter email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Validate that password is not empty; if empty, show a Toast and stop the process
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Register.this, "Enter password", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Validate that first name is provided; if empty, show a Toast and stop the process
                if (TextUtils.isEmpty(firstname)) {
                    Toast.makeText(Register.this, "Enter first name", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }
                // Validate that last name is provided; if empty, show a Toast and stop the process
                if (TextUtils.isEmpty(lastName)) {
                    Toast.makeText(Register.this, "Enter last name", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                // Create a new user in Firebase Authentication using the provided email and password
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() { // Listen for completion of the registration task
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE); // Hide the progress bar
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser(); // Get the newly created user
                                    // Save additional user data (first name, last name, admin status) to Firebase Realtime Database
                                    saveUserData(user, firstname, lastName);
                                    Toast.makeText(Register.this, "User created successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class); // Create intent for MainActivity
                                    startActivity(intent); // Start MainActivity
                                    finish(); // Close the Register activity
                                } else {
                                    // If registration fails, show an error message using a Toast
                                    Toast.makeText(Register.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

    }

    // Helper method to save additional user data to Firebase Realtime Database
    private void saveUserData(FirebaseUser user, String firstName, String lastName) {
        if (user != null) { // Ensure the user object is not null before saving data
            // Get a reference to the "users" node in the database
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users");
            // Create a map to hold the user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("email", user.getEmail());
            userData.put("admin", switchAdmin.isChecked());
            userData.put("firstname", firstName);
            userData.put("lastname", lastName);

            // Save the user data under the user's unique ID in the database
            databaseReference.child(user.getUid()).setValue(userData);
        }
    }
}
