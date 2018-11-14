package com.example.bessmertnyi.audioplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class SongAdapter extends BaseAdapter{

    private ArrayList<Song> songs;
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> theSongs){
        songs=theSongs;
        songInf=LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        //map to song layout
        ConstraintLayout songLay = (ConstraintLayout)songInf.inflate
                (R.layout.song, parent, false);
        //get title and artist views
        ImageView albumCoverView = songLay.findViewById(R.id.albumCoverImageView);
        TextView songView = songLay.findViewById(R.id.songTitleTextView);
        TextView artistView = songLay.findViewById(R.id.songArtistTextView);
        //get song using position
        Song currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());

        byte[] bitmapBytes = currSong.getAlbumCover();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0, bitmapBytes.length);
        albumCoverView.setImageBitmap(bitmap);
        //set position as tag
        songLay.setTag(position);
        return songLay;

        /*ViewHolder viewHolder;

        if (convertView == null) {
            convertView = songInf.inflate(R.layout.song, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Song currentItem = songs.get(position);
        byte[] bitmapBytes = currentItem.getAlbumCover();
        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0, bitmapBytes.length);
        viewHolder.albumCover.setImageBitmap(bitmap);
        viewHolder.songTitle.setText(currentItem.getTitle());
        viewHolder.songArtist.setText(currentItem.getArtist());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicSrv.setSong(position);
                musicSrv.playSong();
            }
        });

        return convertView;*/
    }


    private class ViewHolder {
        ImageView albumCover;
        TextView songTitle;
        TextView songArtist;

        ViewHolder(View view) {
            albumCover = view.findViewById(R.id.albumCoverImageView);
            songTitle = view.findViewById(R.id.songTitleTextView);
            songArtist = view.findViewById(R.id.songArtistTextView);
        }
    }
}
