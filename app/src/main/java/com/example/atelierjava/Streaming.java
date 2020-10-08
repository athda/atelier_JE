package com.example.atelierjava;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class Streaming extends AppCompatActivity {

    VideoView videoView;
    private LocalBroadcastManager localBroadcastManager;
    private ProgressBar progressBar;
    private TextView downloadTextView;

    private static final String TAG = "ClientService";
    private static final String OUTPUT_FILE_NAME = "MyVideoStreaming.mp4";
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private Thread cThread;
    String receivedData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stream_video_layout);
        BluetoothDevice bd = getIntent().getExtras().getParcelable("SELECTED DEVICE");
        Toast.makeText(Streaming.this,bd.getName(),Toast.LENGTH_LONG).show();
        cThread = new ConnectThread(bd);
        cThread.start();

        videoView = findViewById(R.id.videoView);
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);

        Button scanButton = (Button) findViewById(R.id.view_video);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                playVideo();
            }
        });

    }

    public void playVideo() {
        Uri uri = Uri.parse("/storage/emulated/0/Video/MyVideoStreaming.mp4");
        videoView.setVideoURI(uri);
        videoView.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private InputStream is;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(
                        MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
            mAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
            connected(mmSocket);
        }
        @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
        private void connected(BluetoothSocket socket) {
           try {
                String rootDir = Environment.getExternalStorageDirectory()
                        + File.separator + "Video";
                File rootFile = new File(rootDir);
                File localFile = new File(rootFile, OUTPUT_FILE_NAME);
                String output = "PATH OF LOCAL FILE : " + localFile.getPath();
                if (rootFile.exists()) Log.d("SERVICE_ACTIVITY", output);
                if (!localFile.exists()) {
                    localFile.createNewFile();
                } else {
                    localFile.delete();
                    localFile.createNewFile();
                }
                is = socket.getInputStream();
                copyInputStreamToFile(is, localFile);
           }catch (IOException e) {
                Log.d("Error", e.toString());
           }
        }
    }
    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                int read;
                byte[] bytes = new byte[1024];

                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }
    }
}