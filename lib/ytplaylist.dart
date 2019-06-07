import 'package:flutter/material.dart';
import 'playlist.dart';

class YTPlaylist extends Playlist{
  YTPlaylist(play, playnext, String playlistname,{usePlaylist = false,playlistdetail}) : 
  super(play, playnext, playlistname,usePlaylist:usePlaylist,playlistdetail:playlistdetail);

  @override
  _YTPlaylistState createState() => _YTPlaylistState();
}

class _YTPlaylistState extends PlaylistState {
  

  @override
  void settracks() {
    var vids = List<Map>();
    for(Map v in widget.playlistdetail['videos']){
      vids.add(v);
    }
    tracks = Future<List<Map>>.value(vids);
  }

  @override
  void initState() {
    super.initState();
  }

}