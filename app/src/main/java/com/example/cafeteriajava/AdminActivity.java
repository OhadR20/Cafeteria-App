package com.example.cafeteriajava;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class AdminActivity extends AppCompatActivity {
    Button btnLogout, btnViewOrders;
    FirebaseAuth mAuth;
    FirebaseUser user;

    private EditText string1EditText, string2EditText;
    private Button sendButton;


    // Declare Bluetooth related variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;

    // Define a universally unique identifier (UUID) for Bluetooth connection
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 2;
    private static final int REQUEST_BLUETOOTH_CONNECT = 3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase Authentication instance
        btnViewOrders = findViewById(R.id.ViewOrders); // Added view orders button
        string1EditText = findViewById(R.id.ssid);
        string2EditText = findViewById(R.id.password);
        sendButton = findViewById(R.id.sendButton);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Initialize the Bluetooth adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(AdminActivity.this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) { // If Bluetooth is not enabled, request the user to enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        btnViewOrders.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ViewOrdersActivity.class);
                startActivity(intent);
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String string1 = string1EditText.getText().toString();
                String string2 = string2EditText.getText().toString();
                sendData(string1, string2);
            }
        });




    }

    // Method to send data over Bluetooth using the SSID and password
    public void sendData(String string1, String string2) {
        bluetoothDevice = getPairedDevice(); // Find the paired esp32
        if (bluetoothDevice == null) {
            Toast.makeText(AdminActivity.this, "Bluetooth device not found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Check permission before creating the socket
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
                return;
            }

            // Establish Bluetooth socket connection
            if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
            }

            // Send data
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            outputStream.write(string1.getBytes()); // Write the first string
            outputStream.write("\n".getBytes()); // seperate the strings
            outputStream.write(string2.getBytes()); // Write the second string

            Toast.makeText(AdminActivity.this, "Data sent", Toast.LENGTH_SHORT).show(); // Inform the user that data has been sent successfully
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(AdminActivity.this, "Bluetooth connect permission required", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(AdminActivity.this, "Error sending data", Toast.LENGTH_SHORT).show(); // Notify the user if there is an error during data transmission
        }
    }


    // Callback method to handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPairedDevice(); // attempt to get the paired Bluetooth device
            } else {
                Toast.makeText(AdminActivity.this, "Permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPairedDevice();
            } else {
                Toast.makeText(AdminActivity.this, "Bluetooth connect permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to retrieve the paired esp32
    public BluetoothDevice getPairedDevice() {
        // Check for BLUETOOTH_CONNECT permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT);
            return null;
        }

        // Retrieve the set of paired Bluetooth devices
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                if ("ESP32".equals(device.getName())) { // Replace "ESP32" with your device's name
                    return device;
                }
            }
        }
        return null;
    }


}