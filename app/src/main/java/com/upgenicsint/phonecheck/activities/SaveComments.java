package com.upgenicsint.phonecheck.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.upgenicsint.phonecheck.Loader;
import com.upgenicsint.phonecheck.R;
import com.upgenicsint.phonecheck.misc.ReadTestJsonFile;
import com.upgenicsint.phonecheck.misc.WriteObjectFile;
import com.upgenicsint.phonecheck.models.ClientCustomization;

import java.io.File;

public class SaveComments extends AppCompatActivity {

    private Button saveComments, backButton, skipButton;
    private EditText comments;
    public static File commentsFile = new File(Loader.getBaseFile() + "/TestComments.json");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_comments);
        comments = findViewById(R.id.enter_comments);
        saveComments = findViewById(R.id.save_comments);
        backButton = findViewById(R.id.backButton);
        skipButton = findViewById(R.id.skipButton);
        String fileContent = ReadTestJsonFile.getInstance().returnNewObject(commentsFile);
        if (fileContent != null && !fileContent.equals("")) {
            comments.setText(fileContent);

        }
        saveComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WriteObjectFile.getInstance().writeObject(comments.getText().toString(),"/TestComments.json");
                checkCustomizations();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkCustomizations();
            }
        });

    }

    private void checkCustomizations() {
        ClientCustomization clientCustomization = Loader.getInstance().clientCustomization;
        if (clientCustomization != null){
            if (clientCustomization.isAutoBatteryDrain()) {
                Intent intent = new Intent(SaveComments.this, BatteryDiagnosticActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(SaveComments.this, TestCompletionActivity.class);
                startActivity(intent);
                finish();
            }
            /*else if (clientCustomization.isAutoStartBatteryDrain()){
                Intent intent = new Intent(SaveComments.this, BatteryDiagnosticActivity.class);
                startActivity(intent);
                finish();
            }*/
        } else {
            Intent intent = new Intent(SaveComments.this, TestCompletionActivity.class);
            startActivity(intent);
            finish();
        }
    }

}