package com.example.ruby.voicerater;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringTokenizer;

public class Ba extends Activity {

    boolean nowRecording = false; //check if recording is now in process
    boolean stopped = true;
    boolean finishedTask = false;
    int totalTime = 0; //total recorded length
    static String userId = "";
    private SharedPreferences sharedPreferences;
    private Dialog processing;

    //progressBar and timer
    ProgressBar pB;
    TextView timer;
    int curLeft;
    int playTime;
    CountDownTimer tick;

    //text for users to read
    TextView sampleText;

    //buttons to be toggled. initialized on create
    Button recBtn;
    Button stopBtn;
    Button playBtn;

    //submit and retry buttons
    Button submit;
    Button retry;

    //recorder&player
    private final int bufferSize = 1024;
    private final int bytesPerElement = 2;
    private final int[] sampleRates = new int[] {16000, 8000};
    private final short[] audioFormats = new short[] {AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT};
    private int sampleRate;
    private short audioFormat;
    private AudioRecord recorder = null; //recorder object
    private Thread recordingThread = null; //thread for recording
    private Thread playerThread = null; //thread for playing sound
    private AudioTrack audioTrack = null; //audio player object
    private String wavPath = "";//record file path
    private File wavFile; //file object
    private byte[] wavData; //file byte-data

    public class uploadRecFile extends AsyncTask<Void, Void, String> {
        String result;//result after connection
        String urlString;//server domain url
        Bundle resultBundle;//user's points to send to result screen

        //strings for writing multitype-part data
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "3FS4PKI7YH9S";


        final String FILEPATH;//recorded file's path

        private uploadRecFile(String filePath, String url) {
            FILEPATH = filePath;
            urlString = url;
        }

        @Override
        protected String doInBackground(Void... params) {

            try{
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                con.setRequestProperty("Authorization", "Token " + sharedPreferences.getString("token",""));
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                con.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(con.getOutputStream());

                wr.writeBytes(lineEnd + twoHyphens + boundary + lineEnd);
                wr.writeBytes("Content-Disposition: form-data; name=\"datafile\"; filename=\"rec.wav\"" + lineEnd);
                wr.writeBytes("Content-Type: audio/wav" + lineEnd);
                wr.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
                wr.writeBytes(lineEnd);

                //load wav bytes
                FileInputStream fileInputStream = new FileInputStream(new File(wavPath));
                int bytesAvailable = fileInputStream.available();
                int maxBufferSize = 1024;
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[] buffer = new byte[bufferSize];

                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0)
                {
                    // wrile wav file bytes
                    wr.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);

                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                wr.writeBytes(twoHyphens + boundary + lineEnd);

                //write device information
                String deviceInfo = Build.MODEL;
                wr.writeBytes("Content-Disposition: form-data; name=\"device\";" + lineEnd);
                wr.writeBytes(lineEnd);
                wr.writeBytes(deviceInfo + lineEnd);

                //finish writing and send
                wr.writeBytes("\r\n--" + boundary + "--\r\n");
                fileInputStream.close();
                wr.flush();
                wr.close();

                // get response
                int responseCode = con.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_CREATED){
                    BufferedReader rd ;

                    rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                    String line;
                    String responseLine = "";

                    while ((line = rd.readLine()) != null) {
                        responseLine += line;
                    }
                    System.out.println(responseLine);

                    String[] resultSplit = responseLine.split("\"result\":");
                    StringTokenizer tk = new StringTokenizer(resultSplit[1].substring(2,resultSplit[1].length()-3), ",");

                    int tkCount = tk.countTokens();
                    if(tkCount==0){
                        result = "error: no parameter returned";
                        return result;
                    }
                    resultBundle = new Bundle(tkCount);
                    for(int i=0;i<tkCount;i++){
                        resultBundle.putDouble("p"+(i+1), Double.parseDouble(tk.nextToken()) );
                    }

                    result = "completed";
                }else{
                    result = "connection error: " + responseCode+"\n네트워크를 확인해주세요.";
                    BufferedReader rd;

                    InputStream str = con.getInputStream();
                    rd = new BufferedReader(new InputStreamReader(str, "UTF-8"));
                    String line;
                    String responseLine = "";

                    while ((line = rd.readLine()) != null) {
                        responseLine += line;
                    }
                    System.out.println(responseLine);
                }

            }catch(Exception e){
                e.printStackTrace();
            }

            return result;

        }

        @Override
        protected void onPostExecute(String s){
            processing.dismiss();

            if(result.equals("completed")){
                //open C
                Intent c = new Intent(getApplicationContext(), C.class);
                finishedTask = true;
                c.putExtra("results", resultBundle);
                startActivity(c);
            }else{
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                System.out.println(result);
            }

        }
    }

    //sampleText methods
    public void getText(){
        GetStatic getStatic = new GetStatic(getResources().getString(R.string.domain)+"statics/?text/");
        getStatic.execute();
    }
    public class GetStatic extends AsyncTask<Void, Void, String>{
        String urlString;

        private GetStatic(String url){
            urlString = url;
        }

        @Override
        protected String doInBackground(Void... params) {
            String result = "Not Loaded. Try Again.";

            HttpURLConnection con;
            String input = ""; //final text to show

            try{
                URL url = new URL(urlString);
                con = (HttpURLConnection) url.openConnection();

                //set property and connect
                con.setRequestProperty("Authorization", "Token " + sharedPreferences.getString("token",""));
                con.setRequestMethod("GET");
                con.addRequestProperty("accept", "application/json");
                con.connect();

                //get response code
                int code = con.getResponseCode();
                if(code != 200){
                    result = "connection error: " + code;
                }

                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));

                String line;
                while ((line = rd.readLine()) != null) {
                    input += line;
                }

                if(!input.equals("")) result = input.substring(9,input.length()-2);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s){
            super.onPostExecute(s);

            s = s.replace("\\n", "\n");
            s = s.replace("\\r", "\r");
            sampleText.setText(s);
        }
    }

    //progressBar and timer methods
    public void presetTimer(){
        //setting progressBar
        pB = findViewById(R.id.progressBar);
        pB.setMax(120);
        pB.setProgress(120);

        //setting timer
        timer = findViewById(R.id.timer);
        timer.setText("02:00");
        curLeft = 120;

        //set countdowntimer
        tick = new CountDownTimer(120000,1000) {
            @Override
            public void onTick(long l) {
                if(curLeft<=0){
                    stopBtn.performClick();
                    Toast.makeText(getApplicationContext(),"제한시간이 끝났습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                curLeft -= 1;

                //change progressBar
                pB.setProgress(curLeft);

                //setting timer's format to MM:SS
                String min = "" + curLeft/60;
                String sec = "" + curLeft%60;
                if(min.length()==1){
                    min = "0"+min;
                }
                if(sec.length()==1){
                    sec = "0"+sec;
                }
                //change timer
                String toS = min + ":" + sec;
                timer.setText(toS);
            }

            @Override
            public void onFinish() {
                String msg;
                if(curLeft>0){
                    msg = "제출 버튼을 클릭하면 평가결과를 확인할 수 있습니다.";
                }else{
                    msg = "제한시간이 끝났습니다.";
                }
                Toast.makeText(getApplicationContext(),msg, Toast.LENGTH_SHORT).show();
                stopBtn.performClick();
            }
        };
    }

    private AudioRecord createAudioRecord(){
        for (int rate : sampleRates) {
            for (short format : audioFormats) {
                try {
                    int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, format);
                    if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                        sampleRate = rate;
                        audioFormat = format;
                        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, AudioFormat.CHANNEL_IN_MONO, audioFormat, bufferSize);
                        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                            return recorder;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    public static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
        //write exact bit-depth
        short bps = (bitDepth==AudioFormat.ENCODING_PCM_16BIT)? (short) 16: (short) 8;

        //create byte array containing audio information
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)//mono always in this app
                .putInt(sampleRate)
                .putInt(sampleRate * 1 * bps)
                .putShort((short) (1 * bps))
                .putShort(bps)
                .array();

        //write wav header
        out.write(new byte[]{
                'R', 'I', 'F', 'F', // Chunk ID
                0, 0, 0, 0, // Chunk Size (나중에 업데이트 될것)
                'W', 'A', 'V', 'E', // Format
                'f', 'm', 't', ' ', //Chunk ID
                16, 0, 0, 0, // Chunk Size
                1, 0, // AudioFormat
                1, 0, // Num of Channels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // Byte Rate
                littleBytes[10], littleBytes[11], // Block Align
                littleBytes[12], littleBytes[13], // Bits Per Sample
                'd', 'a', 't', 'a', // Chunk ID
                0, 0, 0, 0, //Chunk Size (나중에 업데이트 될 것)
        });
    }
    public static void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                // 아마 이 두 개를 계산할 때 좀 더 좋은 방법이 있을거라 생각하지만..
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Chunk Size
                .array();
        RandomAccessFile accessWave = null;
        try {
            accessWave = new RandomAccessFile(wav, "rw"); // 읽기-쓰기 모드로 인스턴스 생성
            // ChunkSize
            accessWave.seek(4); // 4바이트 지점으로 가서
            accessWave.write(sizes, 0, 4); // 사이즈 채움
            // Chunk Size
            accessWave.seek(40); // 40바이트 지점으로 가서
            accessWave.write(sizes, 4, 4); // 채움
        } catch (IOException ex) {
            // 예외를 다시 던지나, finally 에서 닫을 수 있음
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    // 무시
                }
            }
        }
    }
    // 실제 녹음한 data를 file에 쓰는 함수
    private void writeAudioDataToFile() {
        String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
        wavPath = sd + "/record.wav";
        short[] sData = new short[bufferSize];
        FileOutputStream sWav;

        try {
            sWav = new FileOutputStream(wavPath);
            writeWavHeader(sWav, (short)AudioFormat.CHANNEL_IN_MONO, sampleRate, audioFormat);

            while (nowRecording) {
                recorder.read(sData, 0, bufferSize);
                wavData = short2byte(sData);
                sWav.write(wavData, 0, bufferSize * bytesPerElement);
            }

            sWav.close();
            wavFile = new File(wavPath);
            updateWavHeader(wavFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // short array형태의 data를 byte array형태로 변환하여 반환하는 함수
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    //mainRec methods
    public void onRecordClicked(View v){
        //toggling buttons
        stopBtn.bringToFront();
        recBtn.setClickable(false);
        stopBtn.setClickable(true);
        nowRecording = true;

        //recording methods <server>
        //set recorder
        recorder = createAudioRecord();
        if(recorder==null){
            Toast.makeText(getApplicationContext(), "recorder not created. try again.\n다시 시도해 주십시오.", Toast.LENGTH_SHORT).show();
        }
        recorder.startRecording();
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        //start countdowntimer
        tick.start();
    }
    public void onStopClicked(View v){
        //toggling buttons
        playBtn.bringToFront();
        stopBtn.setClickable(false);
        playBtn.setClickable(true);

        //enable submit&retry buttons
        enableRetry();
        enableSubmit();

        if(nowRecording){
            //finish recording <server> <record>
            if (recorder != null) {
                nowRecording = false;
                recorder.stop();
                recorder.release();
                recorder = null;
                recordingThread = null;
            }

            //renew totalTime
            totalTime = 120 - curLeft;
        }else{
            //stop playing
            stopped = true;
        }

        nowRecording = false;
        playTime = 0;

        //stop countdown and set timer to totalTime
        tick.cancel();

        String min = "" + totalTime/60;
        String sec = "" + totalTime%60;
        if(min.length()==1){
            min = "0"+min;
        }
        if(sec.length()==1){
            sec = "0"+sec;
        }
        String toS = min + ":" + sec;
        timer.setText(toS);

        //set progressBar to zero
        pB.setProgress(0);
    }
    public void onPlayClicked(View v){
        //toggling buttons
        stopBtn.bringToFront();
        playBtn.setClickable(false);
        stopBtn.setClickable(true);

        disableSubmit();
        disableRetry();

        //play recorded voice <play>
        playerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat);
                audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, sampleRate, AudioFormat.CHANNEL_OUT_MONO, audioFormat, minBufferSize, AudioTrack.MODE_STREAM);
                int count = 0;
                byte[] data = new byte[bufferSize];
                stopped = false;
                try {
                    FileInputStream fis = new FileInputStream(wavPath);
                    DataInputStream dis = new DataInputStream(fis);
                    audioTrack.play();

                    while ((count = dis.read(data, 0, bufferSize)) > -1 && !stopped) {
                        audioTrack.write(data, 0, count);
                    }
                    audioTrack.stop();
                    audioTrack.release();
                    dis.close();
                    fis.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        playerThread.start();

        //start progressBar and timer from zero to totalTime
        tick = new CountDownTimer(totalTime*1000, 1000) {
            @Override
            public void onTick(long l) {
                playTime ++;
                pB.setMax(totalTime);
                pB.setProgress(playTime);

                String min = "" + playTime/60;
                String sec = "" + playTime%60;
                if(min.length()==1){
                    min = "0"+min;
                }
                if(sec.length()==1){
                    sec = "0"+sec;
                }
                String toS = min + ":" + sec;
                timer.setText(toS);
            }

            @Override
            public void onFinish() {
                stopBtn.performClick();
            }
        }.start();
    }

    //submit and retry methods
    public void onSubmitClicked(View v){
       askSubmit().show();
    }
    public AlertDialog askSubmit(){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setMessage("Are you sure to submit?");
        dialog.setPositiveButton("yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //show circular progressbar
                processing.show();

                //send to server

                String urlToSend = getResources().getString(R.string.domain)+"recordings/";
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlToSend)));

                uploadRecFile upload = new uploadRecFile(wavPath, urlToSend);
                upload.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        dialog.setNegativeButton("no", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        return dialog.create();
    }
    public Dialog processing(){
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(new ProgressBar(getApplicationContext()));
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        return dialog;
    }

    public void onRetryClicked(View v){
        //go back to preset
        presetTimer();
        stopBtn.setClickable(false);
        playBtn.setClickable(false);
        disableSubmit();
        disableRetry();
        recBtn.setClickable(true);
        recBtn.bringToFront();
    }

    public void disableSubmit(){
        submit.setTextColor(Color.LTGRAY);
        submit.setBackgroundColor(Color.parseColor("#22CCCCCC"));
        submit.setClickable(false);
    }
    public void disableRetry(){
        retry.setTextColor(Color.LTGRAY);
        retry.setBackgroundColor(Color.parseColor("#22CCCCCC"));
        retry.setClickable(false);
    }
    public void enableSubmit(){
        submit.setTextColor(Color.BLACK);
        submit.setBackgroundColor(Color.LTGRAY);
        submit.setClickable(true);
    }
    public void enableRetry(){
        retry.setTextColor(Color.BLACK);
        retry.setBackgroundColor(Color.LTGRAY);
        retry.setClickable(true);
    }

    //alert before closure
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_ba);

        sharedPreferences = getSharedPreferences("LOGINSESSIONCOOKIE", Context.MODE_PRIVATE);
        userId = sharedPreferences.getString("userID","");

        processing = processing();

        //setting button objects
        sampleText = findViewById(R.id.sampleText);
        recBtn = findViewById(R.id.record);
        stopBtn = findViewById(R.id.stop);
        playBtn = findViewById(R.id.play);
        submit = findViewById(R.id.submit);
        retry = findViewById(R.id.retry);

        //pre-setting: sampleText, progressBar&timer
        getText();
        presetTimer();

        //pre-setting: buttons
        stopBtn.setClickable(false);
        playBtn.setClickable(false);
        disableSubmit();
        disableRetry();
    }

    //remove activity when its off screen
    @Override
    public void onPause(){
        super.onPause();
        if(recorder !=null && !nowRecording) recorder.release();
        if(finishedTask) finish();
    }

    @Override
    public void onStop(){
        super.onStop();
        if(!nowRecording) stopBtn.performClick();
    }

    @Override
    public void onBackPressed(){
        alert();
    }

}
