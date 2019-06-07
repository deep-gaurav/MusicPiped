import 'package:flutter/material.dart';

import 'package:http/http.dart' as http;
import 'dart:async';
import 'dart:convert';

import 'main.dart';
import 'playlist.dart';

class ArtistPlaylist extends Playlist {
  ArtistPlaylist(play, playnext, String playlistname,
      {usePlaylist = false, playlistdetail})
      : super(play, playnext, playlistname,
            usePlaylist: usePlaylist, playlistdetail: playlistdetail);

  @override
  _ArtistPlaylistState createState() => _ArtistPlaylistState();
}

class _ArtistPlaylistState extends PlaylistState {
  @override
  void settracks() {
    var c = Completer<List<Map>>();
    tracks=c.future;
    var url =
        MyHomePageState.InvidiosAPI + "channels/" + widget.playlistdetail['id'];
    http.get(url).then((response) {
      var jsr = json.decode(utf8.decode(response.bodyBytes));
      var vids = List<Map>();
      for (Map v in jsr['latestVideos']) {
        vids.add(v);
      }
      c.complete(vids);
    });
  }

  @override
  void initState() {
    super.initState();
  }
}
