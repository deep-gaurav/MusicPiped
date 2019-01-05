package me.devsilver.musicpiped;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface MusicDao{

    @Query("SELECT * FROM MusicEntity")
    List<MusicEntity> getAllMusicUnordered();

    @Query("SELECT * FROM MusicEntity ORDER BY TimesPlayed DESC")
    List<MusicEntity> getAllByPopularity();

    @Query("SELECT * FROM MusicEntity ORDER BY lastplayed DESC")
    List<MusicEntity> getRecent();

    @Query("SELECT * FROM MusicEntity ORDER BY addedon DESC")
    List<MusicEntity> getLastAdded();

    @Query("SELECT * FROM MusicEntity GROUP BY artist")
    List<MusicEntity> getArtists();

    @Query("SELECT * FROM MusicEntity WHERE artist = :artist")
    List<MusicEntity> getArtistTrack(String artist);

    @Query("SELECT * FROM MusicEntity WHERE title = :titlename")
    MusicEntity findByTitle(String titlename);

    @Insert
    void insertAll(MusicEntity... musicEntities);

    @Update
    int Update(MusicEntity... musicEntities);

    @Delete
    void Delete(MusicEntity... musicEntities);

}

@Dao
interface PlaylistDao{

    @Query("SELECT * FROM PlaylistEntity")
    List<PlaylistEntity> getAllPlaylists();

    @Query("SELECT * FROM PlaylistEntity WHERE id = :id")
    PlaylistEntity getPlaylistId(int id);

    @Insert
    void insertAll(PlaylistEntity... playlistEntities);

    @Delete
    void Delete(PlaylistEntity... playlistEntities);


}