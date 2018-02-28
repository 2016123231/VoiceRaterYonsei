package com.example.ruby.voicerater;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;


public class Ab extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private boolean toDestroy;//true if user logged out, so activity has to be destroyed

    public void logout(){
        //renew Session
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        //open Aa and removeCur
        toDestroy = true;
        startActivity(new Intent(getApplicationContext(), Aa.class));
    }
    public void alert(){
        AlertDialog.Builder ask = new AlertDialog.Builder(this);
        ask.setMessage("Are you sure to exit?");
        ask.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //finish activity
                finish();
            }
        });
        ask.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        ask.create().show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ab);

        //init
        toDestroy = false;
        sharedPreferences = getSharedPreferences("LOGINSESSIONCOOKIE", Context.MODE_PRIVATE);

        //show userId
        final TextView userId = findViewById(R.id.userId);
        String showID = "UserID: " + sharedPreferences.getString("userID","");
        userId.setText(showID);

        //onclicklisteners
        ImageButton start = findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get permissions
                if(getPermission()){
                    //open ba
                    startActivity(new Intent(getApplicationContext(), Ba.class));
                }
            }
        });
        final TextView logout = findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
        TextView exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alert();
            }
        });
    }

    private boolean getPermission(){
        //overlay problem for Marshmellow version
//        try{
//            recFile = File.createTempFile("rec",".mp4",);
//        }catch(IOException e){
//            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
//        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//                && !Settings.canDrawOverlays(getApplicationContext())) {
//            getParent().startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), 1305);
//        }

        //get permissions for audio record&sdcard
        ArrayList<String> toPermit = new ArrayList<>(0);
        ArrayList<String> toRePermit = new ArrayList<>(0);
        int rPermitted = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        int sdrPermitted = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int sdwPermitted = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(rPermitted != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)){
                toRePermit.add("마이크");
            }else{
                toPermit.add(Manifest.permission.RECORD_AUDIO);
            }
        }
        if(sdrPermitted != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                toRePermit.add("저장공간(읽기)");
            }else{
                toPermit.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if(sdwPermitted != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                toRePermit.add("저장공간(쓰기)");
            }else{
                toPermit.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if(toPermit.size() != 0) {
            ActivityCompat.requestPermissions(this, toPermit.toArray(new String[toPermit.size()]), 1000);
            return false;
        }

        if(toRePermit.size() != 0){
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            String msg = "서비스를 위해 다음의 권한이 필요합니다.:";
            for(int i=0;i<toRePermit.size();i++){
                msg += "\n- " + toRePermit.get(i);
            }
            msg += "\n다음의 권한을 수락해주세요!";
            builder.setMessage(msg);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                    getApplicationContext().startActivity(intent);
                }
            });
            builder.create().show();
            return false;
        }

        return true;
    }

    @Override
    public void onBackPressed(){
        alert();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(toDestroy){
            finish();
        }
    }
}
