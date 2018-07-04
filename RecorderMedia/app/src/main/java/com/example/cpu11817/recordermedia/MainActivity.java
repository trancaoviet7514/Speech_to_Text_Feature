package com.example.cpu11817.recordermedia;

import android.Manifest;
import android.content.pm.PackageManager;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.speech.v1beta1.Speech;
import com.google.api.services.speech.v1beta1.SpeechRequestInitializer;
import com.google.api.services.speech.v1beta1.model.RecognitionAudio;
import com.google.api.services.speech.v1beta1.model.RecognitionConfig;
import com.google.api.services.speech.v1beta1.model.SpeechRecognitionResult;
import com.google.api.services.speech.v1beta1.model.SyncRecognizeRequest;
import com.google.api.services.speech.v1beta1.model.SyncRecognizeResponse;

import org.apache.commons.io.IOUtils;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class MainActivity extends AppCompatActivity {

    private ImageButton btnRecord, btnTrasnlate;
    private MediaRecorder myAudioRecorder;
    boolean isRecord = false;
    TextView speechToTextResult;

    private final String CLOUD_API_KEY ="AIzaSyCcMmAM71lCwwjF6wcQ_dX84Ktvo1oNKgA";
    String transcript = "";
    String outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnRecord = findViewById(R.id.start);
        btnTrasnlate = findViewById(R.id.play);
        speechToTextResult = (TextView)findViewById(R.id.speech_to_text_result);

        btnTrasnlate.setEnabled(false);


        AndroidAudioConverter.load(this, new ILoadCallback() {
            @Override
            public void onSuccess() {
                // Great!
            }
            @Override
            public void onFailure(Exception error) {
                // FFmpeg is not supported by device
            }
        });



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                //Permisson don't granted
                if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                    Toast.makeText(MainActivity.this, "Permission isn't granted ", Toast.LENGTH_SHORT).show();
                }
                // Permisson don't granted and dont show dialog again.
                else {
                    Toast.makeText(MainActivity.this, "Permisson don't granted and dont show dialog again ", Toast.LENGTH_SHORT).show();
                }
                //Register permission
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);

            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                //Permisson don't granted
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(MainActivity.this, "abc ", Toast.LENGTH_SHORT).show();
                }
                // Permisson don't granted and dont show dialog again.
                else {
                    Toast.makeText(MainActivity.this, "dialog again ", Toast.LENGTH_SHORT).show();
                }
                //Register permission
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},  2);

            }
        }




        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isRecord){
                    btnRecord.setImageResource(R.drawable.microphone);
                    myAudioRecorder.stop();
                    myAudioRecorder.release();
                    myAudioRecorder = null;

                    isRecord = false;
                    btnTrasnlate.setEnabled(true);

                }else{
                    btnRecord.setImageResource(R.drawable.stop);
                    myAudioRecorder = new MediaRecorder();
                    myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

                    myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
                    myAudioRecorder.setOutputFile(outputFile);

                    try {
                        myAudioRecorder.prepare();
                        myAudioRecorder.start();



                    } catch (IllegalStateException ise) {
                        // make something ...
                    } catch (IOException ioe) {
                        Log.e("Log_tag", ioe.toString());
                    }


                    isRecord = true;
                }
            }
        });

        btnTrasnlate.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                speechToTextResult.setText("translating...");

                File flacFile = new File(outputFile);
                IConvertCallback callback = new IConvertCallback() {
                    @Override
                    public void onSuccess(final File convertedFile) {
                        AsyncTask.execute(new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {
                                try {

                                    //Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/raw/mono");
                                    Uri soundUri = Uri.fromFile(convertedFile);


                                    InputStream stream = getContentResolver().openInputStream(soundUri);
                                    byte[] audioData = IOUtils.toByteArray(stream);
                                    stream.close();

                                    String base64EncodedData = Base64.encodeBase64String(audioData);

                                    processSpeech(base64EncodedData);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception error) {
                        // Oops! Something went wrong
                    }
                };
                AndroidAudioConverter.with(MainActivity.this)
                        // Your current audio file
                        .setFile(flacFile)

                        // Your desired audio format
                        .setFormat(AudioFormat.FLAC)

                        // An callback to know when conversion is finished
                        .setCallback(callback)

                        // Start conversion
                        .convert();


            }
        });


    }

    private void processSpeech(String data){
        Speech speechService = new Speech.Builder(
                AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(),
                null
        ).setSpeechRequestInitializer(new SpeechRequestInitializer(CLOUD_API_KEY))
                .build();

        RecognitionConfig recognitionConfig = new RecognitionConfig();
        recognitionConfig.setLanguageCode("vi-VN");



        RecognitionAudio recognitionAudio = new RecognitionAudio();
        recognitionAudio.setContent(data);

        SyncRecognizeRequest request = new SyncRecognizeRequest();
        request.setConfig(recognitionConfig);
        request.setAudio(recognitionAudio);

        SyncRecognizeResponse response = null;
        try {
            response = speechService.speech()
                    .syncrecognize(request)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(response.size()==0){
            transcript = "";
        }
        else{
            for(int i = 0; i < response.getResults().size();i++){
                SpeechRecognitionResult result = response.getResults().get(i);
                transcript += result.getAlternatives().get(0).getTranscript();
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                speechToTextResult.setText(transcript);
            }
        });

    }


}
