package me.devsilver.musicpiped;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity
public class MusicEntity {
    @PrimaryKey
    @NonNull
    public String title;

    @ColumnInfo(name = "detailJSON")
    public String detailJSON;

    @ColumnInfo(name = "TimesPlayed")
    public int timesPlayed;

    @ColumnInfo(name = "Playlists")
    public int playlists;

    @ColumnInfo(name = "Artist")
    public String artist;

    @ColumnInfo(name = "LastPlayed")
    public long lastplayed;

    @ColumnInfo(name = "Addedon")
    public long addedon;

}

@Entity
class PlaylistEntity{

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;


}