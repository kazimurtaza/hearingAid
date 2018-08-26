package com.kazimurtaza.hearingaid;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import static android.Manifest.permission.RECORD_AUDIO;

import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    public static final int RequestPermissionCode = 1;
    public static int[] audioFormats = {
            MediaRecorder.AudioEncoder.AMR_WB,
            MediaRecorder.AudioEncoder.AMR_NB,
            MediaRecorder.AudioEncoder.AAC,
            MediaRecorder.AudioEncoder.AAC_ELD,
            MediaRecorder.AudioEncoder.VORBIS,
            MediaRecorder.AudioEncoder.HE_AAC,
            AudioFormat.ENCODING_PCM_16BIT
    };
    public static int[] ChannelIns = {
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.CHANNEL_IN_STEREO
    };
    public static int[] ChannelOuts = {
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.CHANNEL_IN_STEREO
    };


    Button buttonStart, buttonStop;
    private AudioRecord recorder = null;
    private boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonStart = (Button) findViewById(R.id.button);
        buttonStop = (Button) findViewById(R.id.button2);

        final Spinner encodingDropDown = (Spinner) findViewById(R.id.encoding);
        final Spinner channelInDropDown = (Spinner) findViewById(R.id.channel_in);
        final Spinner channelOutDropDown = (Spinner) findViewById(R.id.channel_out);
        final EditText sampleRateText = (EditText) findViewById(R.id.sample_rate);

        ArrayAdapter<CharSequence> encodingAdapter =  ArrayAdapter.createFromResource(this, R.array.audio_formats, android.R.layout.simple_spinner_item);
        encodingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        encodingDropDown.setAdapter(encodingAdapter);

        ArrayAdapter<CharSequence> channelInAdapter =  ArrayAdapter.createFromResource(this, R.array.in_channels, android.R.layout.simple_spinner_item);
        channelInAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelInDropDown.setAdapter(channelInAdapter);

        ArrayAdapter<CharSequence> channelOutAdapter =  ArrayAdapter.createFromResource(this, R.array.out_channels, android.R.layout.simple_spinner_item);
        channelOutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        channelOutDropDown.setAdapter(channelOutAdapter);

        buttonStop.setEnabled(false);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playing = true;
                buttonStop.setEnabled(playing);
                final int selectedAudioFormat = audioFormats[encodingDropDown.getSelectedItemPosition()];
                final int selectedInChannel = ChannelIns[channelInDropDown.getSelectedItemPosition()];
                final int selectedOutChannel = ChannelOuts[channelInDropDown.getSelectedItemPosition()];

                final Handler handler = new Handler();
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            while(playing) {
                                if(checkPermission()) {
                                    Log.i("Audio", "Running Audio Thread");
                                    AudioRecord recorder = null;
                                    AudioTrack track = null;
                                    short[][] buffers  = new short[256][160];
                                    int ix = 0;
                                    /*
                                     * Initialize buffer to hold continuously recorded audio data, start recording, and start
                                     * playback.
                                     */
                                    try
                                    {
                                        int N = AudioRecord.getMinBufferSize(Integer.parseInt(sampleRateText.getText().toString()),selectedInChannel,selectedAudioFormat);
                                        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, Integer.parseInt(sampleRateText.getText().toString()), selectedInChannel, selectedAudioFormat, N*10);
                                        track = new AudioTrack(AudioManager.STREAM_VOICE_CALL, Integer.parseInt(sampleRateText.getText().toString()), selectedOutChannel, selectedAudioFormat, N*10, AudioTrack.MODE_STREAM);
                                        recorder.startRecording();
                                        track.play();
                                        /*
                                         * Loops until something outside of this thread stops it.
                                         * Reads the data from the recorder and writes it to the audio track for playback.
                                         */
                                        while(playing)
                                        {
                                            Log.i("Map", "Writing new data to buffer");
                                            short[] buffer = buffers[ix++ % buffers.length];
                                            N = recorder.read(buffer,0,buffer.length);
                                            track.write(buffer, 0, buffer.length);
                                        }
                                    }
                                    catch(Throwable x)
                                    {
                                        Log.w("Audio", "Error reading voice audio", x);
                                    }
                                    /*
                                     * Frees the thread's resources after the loop completes so that it can be run again
                                     */
                                    finally
                                    {
                                        recorder.stop();
                                        recorder.release();
                                        track.stop();
                                        track.release();
                                    }
                                    Toast.makeText(MainActivity.this, "Recording started",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    requestPermission();
                                }
                                handler.post(this);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                buttonStop.setEnabled(false);
                buttonStart.setEnabled(true);
                playing = false;
                if (null != recorder) {
                    recorder.stop();
                    recorder.release();
                    recorder = null;
                }
                Toast.makeText(MainActivity.this, "Recording Completed",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO}, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length> 0) {
                    boolean RecordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (RecordPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this,"Permission Denied",Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }

}