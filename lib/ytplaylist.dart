import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';

import 'playlist.dart';
import 'main.dart';

class YTPlaylist extends Playlist{
  YTPlaylist(play, playnext, String playlistname,{usePlaylist = false,playlistdetail}) : 
  super(play, playnext, playlistname,usePlaylist:usePlaylist,playlistdetail:playlistdetail);

  @override
  _YTPlaylistState createState() => _YTPlaylistState();
}

class _YTPlaylistState extends PlaylistState {
  

  @override
  void settracks() {
    String playlistId = widget.playlistdetail.containsKey('playlistId')?widget.playlistdetail['playlistId']:widget.playlistdetail['mixId'];
    var url = MyHomePageState.InvidiosAPI+"playlists/" + playlistId;
    var c = Completer<List<Map>>();
    tracks = c.future;
    http.get(url).then((response) {
      Map playlistDetail = json.decode(utf8.decode(response.bodyBytes));
      var vids = List<Map>();
      for (Map v in playlistDetail['videos']) {
        vids.add(v);
      }
      print(vids);
      c.complete(vids);
    });
  }

  @override
  void initState() {
    super.initState();
  }

}