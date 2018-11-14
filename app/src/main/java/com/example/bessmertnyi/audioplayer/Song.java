package com.example.bessmertnyi.audioplayer;

public class Song {

    private long id;
    private String title;
    private String artist;
    private byte[] albumCover;

    public Song(long songID, String songTitle, String songArtist, byte[] albumCover) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        this.albumCover = albumCover;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}

    public byte[] getAlbumCover() {
        return albumCover;
    }
}
