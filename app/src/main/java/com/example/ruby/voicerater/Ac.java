package com.example.ruby.voicerater;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ac extends AppCompatActivity {

    // UI references.
    private MyEditText[] info;
    private CheckBox showPw;
    private View mProgressView;
    private Button signUp;
    private RadioButton gender;

    //year of birth info
    int birth = 0;

    public class SignUpPost extends AsyncTask<Void, Integer, String> {
        private String userParam;
        private String urlString;
        private String result;

        private SignUpPost(String userParam, String url){
            this.userParam = userParam;
            urlString = url;
            result = "sign up failed starting. try again";
        }

        @Override
        protected String doInBackground(Void... unused) {

            result = "url not connected. try again.";

            HttpURLConnection urlConn = null;

            //create new user
            try{
                URL url = new URL(urlString+"join/");
                urlConn = (HttpURLConnection) url.openConnection();

                // [2-1]. urlConn 설정.
                urlConn.setRequestMethod("POST");
                urlConn.setRequestProperty("Accept-Charset", "UTF-8");
                urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConn.setDoOutput(true);
                urlConn.connect();

                result = "outputstream not created. try again.";

                // [2-2]. parameter 전달 및 데이터 읽어오기.
                //post userID and password
                OutputStream os = urlConn.getOutputStream();
                os.write(userParam.getBytes("UTF-8"));
                os.flush();
                os.close();

                // [2-3]. 연결 요청 확인.
                // 실패 시 null을 리턴하고 메서드를 종료.
                int response = urlConn.getResponseCode();
                if (response != HttpURLConnection.HTTP_CREATED){
                    result = "http connection error: "+response + " ERROR";
                    if(response==400){
                        result = "userid already exists. try another id.";
                    }
                    return result;
                }
                result = "signup completed";

            } catch (MalformedURLException e) { // for URL.
                result = "ANDERROR : " + e.getMessage();
                System.out.println("exception: "+e.getMessage());
                e.printStackTrace();
                return result;
            } catch (IOException e) { // for openConnection().
                result = "ANDERROR : " + e.getMessage();
                System.out.println("exception: "+e.getMessage());
                e.printStackTrace();
                return result;
            } finally {
                if (urlConn != null) urlConn.disconnect();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            //doInBackground()로 부터 리턴된 값이 onPostExecute()의 매개변수로 넘어오므로 s를 출력한다.
            mProgressView.setVisibility(View.GONE);
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            if(result.equals("signup completed")) finish();
        }

    }

    private void setEditText(int index){
        if(index >= info.length || index < 0) return;
        EditText view = info[index];
        if(index == info.length-1){
            //last information: set signup
            final EditText cur = view;
            cur.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if(!b){
                        InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(cur.getWindowToken(), 0);
                    }
                }
            });
            cur.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                        cur.clearFocus();
                        return true;
                    }
                    return false;
                }
            });
        }else{
            final EditText nextView = info[index+1];
            view.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            view.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                        nextView.performClick();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void setCheckBox(int pwIndex, int pwReIndex){
        final EditText pw = info[pwIndex];
        final EditText pwRe = info[pwReIndex];
        showPw = findViewById(R.id.checkBox);
        showPw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if(checked){
                    pw.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    pwRe.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }else{
                    pw.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    pwRe.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });
    }

    private boolean pwChecked(String pw, String pwRe){
        return pw.equals(pwRe);
    }

    private AlertDialog dialog;
    private void attemptSignUp(String id, String gender, String birth){
        mProgressView.setVisibility(View.VISIBLE);
        AlertDialog.Builder ask = new AlertDialog.Builder(this);
        ask.setMessage("Summit with this information?\nID: "+id+"\ngender: "+gender+"\nyear of birth: "+birth);
        ask.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                closeDialog();

                //post db and finish
                postDB();
            }
        });
        ask.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                closeDialog();
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://172.24.100.14:8000/join/")));
                mProgressView.setVisibility(View.GONE);
            }
        });
        dialog = ask.show();
    }
    private void closeDialog(){
        if(dialog!=null) dialog.dismiss();
    }

    private void postDB(){
        String userParam = "username=" + info[0].getText().toString() + "&password=" + info[1].getText().toString() + "&gender=" + gender.getText().toString() + "&birth=" + birth;
        String url = "http://165.132.58.192:8000/";
//        String url = "http://172.24.101.79:8000/";
        SignUpPost sPost = new SignUpPost(userParam, url);
        sPost.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void setBirth(int birth){
        this.birth = birth;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ac);

        //set EditTexts(username, password, password2)
        info = new MyEditText[3];
        info[0] = findViewById(R.id.id);
        info[1] = findViewById(R.id.password);
        info[2] = findViewById(R.id.passwordRe);
        for(int i=0;i<info.length;i++){
            setEditText(i);
        }

        //set checkbox(password show)
        setCheckBox(1,2);

        //set spinner(year of birth)
            Spinner spinner = findViewById(R.id.spinner);

            // Initialize a String Array
            String[] years = new String[100];
            for(int i=0;i<100;i++){
                years[i] = "" + (2020-i);
            }
            final List<String> yearList = new ArrayList<>(Arrays.asList(years));

            // Initialize an ArrayAdapter
            final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,R.layout.years_layout,yearList);
            spinnerArrayAdapter.setDropDownViewResource(R.layout.years_layout);
            spinner.setAdapter(spinnerArrayAdapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    setBirth(2020-position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    birth= 2020;
                }
            });
//            spinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                    setBirth(1920+position);
//                }
//            });

        //set radiogroup(gender)
        final RadioGroup genderGroup = findViewById(R.id.gender);

        signUp = findViewById(R.id.signUp);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int i=0;i<info.length;i++){
                    if(info[i].getText().length()==0){
                        Toast.makeText(getApplicationContext(), "Please input all the informations", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                String pwString = info[1].getText().toString();
                if(pwString.length() < 8){
                    Toast.makeText(getApplicationContext(), "Password not valid. Should be over 8 digits", Toast.LENGTH_SHORT).show();
                    return;
                }
                if( ( !pwString.matches(".*\\d+.*") ) || pwString.matches("[0-9]+") ){
                    Toast.makeText(getApplicationContext(), "Password not valid. Should contain alphabet and numeric value", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(birth == 0){
                    Toast.makeText(getApplicationContext(), "Please input your year of birth", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(genderGroup.getCheckedRadioButtonId()==-1){
                    Toast.makeText(getApplicationContext(), "Please input your gender", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!pwChecked(info[1].getText().toString(), info[2].getText().toString())){
                    Toast.makeText(getApplicationContext(), "Password does not match the confirm password. Check the password again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                gender = findViewById(genderGroup.getCheckedRadioButtonId());
                attemptSignUp(info[0].getText().toString(), gender.getText().toString(), ""+birth);

            }
        });

        mProgressView = findViewById(R.id.login_progress);

    }

}