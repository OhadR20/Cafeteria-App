package com.example.cafeteriajava;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class ProfileFragment extends Fragment {

    TextView tvDetails, tvAdminStatus;
    Button btnLogout, btnAdmin;
    FirebaseAuth mAuth; // Firebase Authentication instance to handle user authentication
    FirebaseUser user;  // Firebase user object representing the current logged in user

    private EditText string1EditText, string2EditText;
    private Button sendButton;

    // Bluetooth variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Request codes for enabling Bluetooth and requesting permissions
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 2;
    private static final int REQUEST_BLUETOOTH_CONNECT = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Get the default Bluetooth adapter for the device
        if (bluetoothAdapter == null) {
            Toast.makeText(getContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }

        // If Bluetooth is not enabled, request to enable it
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); // Launch an activity to prompt the user to enable Bluetooth
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        tvAdminStatus = rootView.findViewById(R.id.tvAdminStatus);
        btnLogout = rootView.findViewById(R.id.btnLogout);
        btnAdmin = rootView.findViewById(R.id.Admin); // Added view orders button
        tvDetails = rootView.findViewById(R.id.tvDetails);

        user = mAuth.getCurrentUser(); // Get the currently logged in user from Firebase Authentication
        if (user == null) {
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            getActivity().finish();
        } else {
            tvDetails.setText(user.getEmail()); // If user is logged in, display the user's email
            checkAdminStatus(user.getUid()); // Check and update the admin status of the user by accessing the Firebase Database
        }

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut(); // Sign out the current user from Firebase Authentication
                Intent intent = new Intent(getActivity(), Login.class); // Redirect the user to the Login activity after logout
                startActivity(intent);
                getActivity().finish();
            }
        });



        // Set up the admin button click listener to check admin privileges before navigating to AdminActivity
        btnAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create a reference to the current user's data in the "users" of the Firebase Database
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean isAdmin = snapshot.child("admin").getValue(Boolean.class); // Retrieve the "admin" status from the database as a Boolean
                        if (isAdmin != null && isAdmin) {
                            Intent intent = new Intent(getActivity(), AdminActivity.class); // If the user is marked as admin, open AdminActivity
                            startActivity(intent);
                        } else {
                            Toast.makeText(getContext(), "You are not an admin", Toast.LENGTH_SHORT).show(); // If the user is not an admin, display a message
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to check admin status", Toast.LENGTH_SHORT).show(); // If there is an error while checking admin status, display an error message
                    }
                });
            }
        });



        return rootView;
    }





    // Method to check the admin status of the current user using their unique ID
    private void checkAdminStatus(String uid) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(uid);  // Create a reference to the user's data in the Firebase "users"
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) { // If user data exists in the database, check the admin flag
                    Boolean isAdmin = snapshot.child("admin").getValue(Boolean.class);
                    // Update the tvAdminStatus TextView based on the admin status
                    if (isAdmin != null && isAdmin) {
                        tvAdminStatus.setText("You are an admin");
                    } else {
                        tvAdminStatus.setText("You are not an admin");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to retrieve admin status", Toast.LENGTH_SHORT).show(); // If there is an error retrieving admin status, display an error message
            }
        });
    }



}
