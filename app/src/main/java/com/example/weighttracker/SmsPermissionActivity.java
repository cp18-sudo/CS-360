package com.example.weighttracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Prompts the user to grant or deny SMS permission for goal
 * notifications. The app works fully regardless of the choice.
 */
public class SmsPermissionActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private TextView permissionMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_permission);

        permissionMessage = findViewById(R.id.permissionMessage);
        Button allowButton = findViewById(R.id.allowButton);
        Button denyButton = findViewById(R.id.denyButton);

        allowButton.setOnClickListener(v -> requestSmsPermission());
        denyButton.setOnClickListener(v -> {
            permissionMessage.setText(R.string.notifications_disabled_message);
            Toast.makeText(this, R.string.notifications_disabled_toast,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            permissionMessage.setText(R.string.permission_already_granted);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionMessage.setText(R.string.sms_permission_granted);
                Toast.makeText(this, R.string.sms_permission_granted_toast,
                        Toast.LENGTH_SHORT).show();
            } else {
                permissionMessage.setText(R.string.sms_permission_denied);
                Toast.makeText(this, R.string.sms_permission_denied_toast,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
