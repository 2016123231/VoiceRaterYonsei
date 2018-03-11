package com.example.ruby.voicerater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

//custom edittext to respond to back-key-pressed event
class MyEditText extends android.support.v7.widget.AppCompatEditText{

    private OnBackPressedListener listener;

    //constructors
    public MyEditText(Context context){
        super(context);
    }
    public MyEditText(Context context,AttributeSet attributeSet){
        super(context, attributeSet);
    }
    public MyEditText(Context context, AttributeSet attributeSet, int defStyle){
        super(context, attributeSet, defStyle);
    }

    //lister method
    public interface OnBackPressedListener{
        public void onBackPress();
    }
    public void setOnBackPressedListener(OnBackPressedListener l){
        listener = l;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event){
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if (listener != null)
            {
                listener.onBackPress();
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }
}

public class Aa extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    public void attemptLogin(){
        //set progressbar
        mProgressView.setVisibility(View.VISIBLE);

        // link server
        String url = getResources().getString(R.string.domain)+"api-token-auth/";

        //get user information
        String userInfo = "username="+id.getText().toString()+"&password="+pw.getText().toString();

        // AsyncTask를 통해 HttpURLConnection 수행.
        NetworkTask networkTask = new NetworkTask(url, userInfo);
        networkTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    //AsyncTask class for network task on independent thread
    private class NetworkTask extends AsyncTask<Void, Void, String>{
        private String urlString;
        private String userInfo;
        private String result;

        private NetworkTask(String url, String userInfo) {
            this.urlString = url;
            this.userInfo = userInfo;
        }

        private void getUserInfo(){
            HttpURLConnection con;

            //set post method
            try{
                //open url with http connection
                URL url = new URL(getResources().getString(R.string.domain)+"userinfo/");
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.domain)+"user/")));
                con = (HttpURLConnection) url.openConnection();

                //set requests
                con.setRequestMethod("GET");
                con.setRequestProperty("Authorization", "Token " + sharedPreferences.getString("token",""));
                con.setRequestProperty("Content-Type", "application/json");

                //connect url by http
                con.connect();

                //check response code
                int responseCode = con.getResponseCode();

                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                String line;
                String input = "";
                while ((line = rd.readLine()) != null) {
                    input += line;
                }

                String[] splitByIDNum = input.split("\"id_number\":");
                String id_number = splitByIDNum[1].substring(1,11);

                String[] splitByName = input.split("\"name\":");
                String[] splitNameAgain = splitByName[1].split("\"");
                String name = splitNameAgain[1];

                //save id_number and name in sharedpreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("id_number", id_number);
                editor.putString("name", name);
                editor.apply();

            } catch (MalformedURLException e) { // for URL.
                result = "AND ERROR : " + e.getMessage();
                System.out.println("exception: "+e.getMessage());
                e.printStackTrace();
            } catch (IOException e) { // for openConnection().
                result = "AND ERROR : " + e.getMessage();
                System.out.println("exception: "+e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(Void... unused) {

            result = "네트워크 확인 후 다시 시도해주세요.";
            HttpURLConnection con;

            //set post method
            try{
                //open url with http connection
                URL url = new URL(urlString);
                con = (HttpURLConnection) url.openConnection();

                //set requests
                con.setRequestMethod("POST");
                con.setRequestProperty("Accept-Charset", "UTF-8");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setDoOutput(true);

                //connect url by http
                con.connect();

                //set parameter to post
                OutputStream toPost = con.getOutputStream();
                toPost.write(userInfo.getBytes());
                toPost.flush();
                toPost.close();

                //check response code
                int responseCode = con.getResponseCode();
                if(responseCode==200){
                    result = "login completed";
                }else if(responseCode==400){
                    result = "아이디와 비밀번호를 확인하세요.";
                    return result;
                }else{
                    result = "server error: " + responseCode + "ERROR";
                    return result;
                }
            } catch (MalformedURLException e) { // for URL.
                result = "AND ERROR : " + e.getMessage();
                System.out.println("exception: "+e.getMessage());
                e.printStackTrace();
                return result;
            } catch (IOException e) { // for openConnection().
                result = "AND ERROR : " + e.getMessage();
                System.out.println("exception: "+e.getMessage());
                e.printStackTrace();
                return result;
            }

            //save session information
            try{
                BufferedReader rd;

                //get inputstream
                rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                String line;
                String responseLine = "";

                while ((line = rd.readLine()) != null) {
                    responseLine += line;
                }

                //find "token" key and get value
                String[] resultSplit = responseLine.split("\"token\":");
                String key = resultSplit[1].substring(1,resultSplit[1].length()-2);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("loggedIn", "true");
                editor.putString("token", key);
                editor.putString("userID", id.getText().toString());
                editor.apply();

            }catch(IOException e){
                e.printStackTrace();
            }catch (Exception e){
                result = "shared perference error : cannot attach log-in information";
                e.printStackTrace();
            }

//            getUserInfo();

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            mProgressView.setVisibility(View.GONE);
            if(result.equals("login completed")) {
                //set session information
                signedIn = true;
                startActivity(new Intent(getApplicationContext(), Ab.class));
            }else{
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // UI references.
    private MyEditText id;
    private MyEditText pw;
    private View mProgressView;
    private ImageView logo;
    private TextView signUp;

    boolean signedIn = false;//true if log-in finished

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int windowHeight = displayMetrics.heightPixels;
        int windowWidth = displayMetrics.widthPixels;
        System.out.println(windowWidth);

        setContentView(R.layout.activity_aa);

        //if logged in, start at Ab.
        sharedPreferences = getSharedPreferences("LOGINSESSIONCOOKIE", Context.MODE_PRIVATE);
        if(sharedPreferences.getString("loggedIn", "").equals("true")) {
            signedIn = true;
            startActivity(new Intent(getApplicationContext(), Ab.class));
        }

        logo = (ImageView) findViewById(R.id.logo);

        // Set up the login form.
        id = findViewById(R.id.id);
        id.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    pw.performClick();
                    return true;
                }
                return false;
            }
        });
        id.setOnBackPressedListener(new MyEditText.OnBackPressedListener() {
            @Override
            public void onBackPress() {
                id.clearFocus();
            }
        });
        id.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    logo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 2));
                }else{
                    logo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 3));
                }
            }
        });

        pw = (MyEditText) findViewById(R.id.password);
        pw.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(!b){
                    InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(pw.getWindowToken(), 0);
                }
            }
        });
        pw.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    pw.clearFocus();
                    return true;
                }
                return false;
            }
        });
        pw.setOnBackPressedListener(new MyEditText.OnBackPressedListener() {
            @Override
            public void onBackPress() {
                pw.clearFocus();
            }
        });
        pw.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    logo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 2));
                }else{
                    logo.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 3));
                }
            }
        });

        signUp = (TextView) findViewById(R.id.signUp);
        signUp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Ac.class));
            }
        });

        Button signIn = (Button) findViewById(R.id.signIn);
        signIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(getApplicationContext(), Ba.class));
                attemptLogin();
            }
        });

        mProgressView = findViewById(R.id.login_progress);
    }

    @Override
    public void onPause(){
        super.onPause();
        if(signedIn) finish();
    }
}