package me.devsilver.musicpiped;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {MusicEntity.class,PlaylistEntity.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MusicDao musicDao();
    public abstract PlaylistDao playlistDao();
}
