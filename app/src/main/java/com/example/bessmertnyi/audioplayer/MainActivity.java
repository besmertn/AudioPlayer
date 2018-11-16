package com.example.bessmertnyi.audioplayer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int RUNTIME_PERMISSION_CODE = 1;
    private static final int PHONE_STATE_PERMISSION_CODE = 2;
    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound = false;
    private boolean doubleBackToExitPressedOnce = false;
    private boolean paused = false, playbackPaused = true;
    private BroadcastReceiver phoneReceiver;
    private ImageButton playBatton;
    private ImageButton skipPrevButton;
    private ImageButton skipNextButton;
    private AppCompatSeekBar seekBar;
    private TextView currTimeTextView;
    private TextView maxTimeTextView;
    private Handler mHandler = new Handler();

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent == null){
            playIntent = new Intent(this, MusicService.class);
            musicSrv = new MusicService();
            startService(playIntent);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        if (musicConnection != null) {
            unbindService(musicConnection);
        }
        stopService(playIntent);
        musicSrv = null;
        unregisterReceiver(phoneReceiver);
        super.onDestroy();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.AndroidRuntimePermission();

        playBatton = findViewById(R.id.playImageButton);
        skipPrevButton = findViewById(R.id.skipPrevImageButton);
        skipNextButton = findViewById(R.id.skipNextImageButton);
        seekBar = findViewById(R.id.seekBar);
        currTimeTextView = findViewById(R.id.curr_duration_text_view);
        maxTimeTextView = findViewById(R.id.max_duration_text_view);

        phoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                    if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        Toast.makeText(context, "Ringing State ", Toast.LENGTH_SHORT).show();
                        musicSrv.pausePlayer();
                    }
                    if ((state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))) {
                        Toast.makeText(context, "Received State", Toast.LENGTH_SHORT).show();
                    }
                    if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        Toast.makeText(context, "Idle State", Toast.LENGTH_SHORT).show();
                        musicSrv.go();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        IntentFilter intentFIlter = new IntentFilter("android.intent.action.PHONE_STATE");

        this.registerReceiver(phoneReceiver, intentFIlter);

        songView = findViewById(R.id.song_list);
        songList = new ArrayList<>();

        getSongList();

        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

        playBatton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int seeked_progess;
            @Override
            public void onProgressChanged(final SeekBar seek, int progress, boolean fromUser) {
                seeked_progess = progress;
                seeked_progess = seeked_progess * 1000;

                if (fromUser) {
                    Runnable mRunnable = new Runnable() {

                        @Override
                        public void run() {
                            int min, sec;

                            if (musicSrv != null ) {
                                int mCurrentPosition = seekBar.getProgress();
                                min = mCurrentPosition / 60;
                                sec = mCurrentPosition % 60;

                                Log.e("Music Player Activity", "Minutes : "+min +" Seconds : " + sec);
                            }
                            mHandler.postDelayed(this, 1000);
                        }
                    };
                    mRunnable.run();}
                }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicSrv.seek(seeked_progess);

            }
        });

        skipNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicSrv.playNext();
                if(MainActivity.this.playbackPaused) {
                    playbackPaused = false;
                    playBatton.setImageDrawable(getDrawable(R.drawable.ic_pause));
                }
                startSong();
            }
        });

        skipPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicSrv.playPrev();
                if(MainActivity.this.playbackPaused) {
                    playbackPaused = false;
                    playBatton.setImageDrawable(getDrawable(R.drawable.ic_pause));
                }
                startSong();
            }
        });

    }

    public void play(){
        if(playbackPaused){
            playbackPaused = false;
            playBatton.setImageDrawable(getDrawable(R.drawable.ic_pause));
            musicSrv.go();

        } else {
            playbackPaused = true;
            playBatton.setImageDrawable(getDrawable(R.drawable.ic_play_arrow));
            musicSrv.pausePlayer();
        }
        startSong();
    }

    private void startSong() {
        MainActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(musicSrv != null ){
                    int mCurrentPosition = musicSrv.getPosn();
                    seekBar.setProgress(mCurrentPosition / 1000);
                    int millisMax = musicSrv.getDur();
                    seekBar.setMax(millisMax / 1000);

                    String durationTime = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(millisMax),
                            TimeUnit.MILLISECONDS.toSeconds(millisMax) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisMax))
                    );

                    String currentTime = String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition),
                            TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition))
                    );

                       currTimeTextView.setText(currentTime);
                    maxTimeTextView.setText(durationTime);
                }
                mHandler.postDelayed(this, 1000);
            }
        });
    }

    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
        playBatton.setImageDrawable(getDrawable(R.drawable.ic_pause));
        playbackPaused = false;
        startSong();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };



    public void getSongList() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources() ,R.drawable.ic_music_note);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, stream);
        byte[] bitmapBytes = stream.toByteArray();

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri,
                null,
                null,
                null,
                null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist, bitmapBytes));
            }
            while (musicCursor.moveToNext());
        }
       if(musicCursor != null) {
           musicCursor.close();
       }
    }

    // Creating Runtime permission function.
    public void AndroidRuntimePermission() {

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                AlertDialog.Builder alert_builder = new AlertDialog.Builder(MainActivity.this);
                alert_builder.setMessage("External Storage Permission is Required.");
                alert_builder.setTitle("Please Grant Permission.");
                alert_builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                RUNTIME_PERMISSION_CODE
                        );
                    }
                });

                alert_builder.setNeutralButton("Cancel", null);
                AlertDialog dialog = alert_builder.create();
                dialog.show();
            } else {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        RUNTIME_PERMISSION_CODE
                );
            }
        }

        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {

                AlertDialog.Builder alert_builder = new AlertDialog.Builder(MainActivity.this);
                alert_builder.setMessage("Phone State Permission is Required.");
                alert_builder.setTitle("Please Grant Permission.");
                alert_builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{Manifest.permission.READ_PHONE_STATE},
                                PHONE_STATE_PERMISSION_CODE
                        );
                    }
                });

                alert_builder.setNeutralButton("Cancel", null);
                AlertDialog dialog = alert_builder.create();
                dialog.show();
            } else {
                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        PHONE_STATE_PERMISSION_CODE
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RUNTIME_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
            }

            case PHONE_STATE_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
            }
        }
    }

    /*private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused = false;
        }
        controller.show(0);
    }*/

    /*private void setController(){
        if(controller != null) {
            controller.forceHide();
        }
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    @Override
    public void start() {
        playbackPaused = false;
        musicSrv.go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng()) {
            return musicSrv.getDur();
        } else {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng()) {
            return musicSrv.getPosn();
        }else {
            return 0;
        }
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound){
            return musicSrv.isPng();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }*/
}
