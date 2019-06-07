import 'dart:async';

import 'package:flutter/material.dart';

import 'package:idb_shim/idb.dart';
import 'package:share/share.dart';
import 'package:cached_network_image/cached_network_image.dart';

import 'trending.dart';
import 'trackDetail.dart';
import 'playerService.dart';
import 'main.dart';

class Playlist extends StatefulWidget {
  void Function(List<Map>) play;
  void Function(Map) playnext;

  String playlistname = 'All Tracks';
  bool usePlaylist = false;
  Map playlistdetail;
  AudioPlayer player;

  bool offline;

  Playlist(this.play, this.playnext, this.playlistname,
      {this.usePlaylist = false, this.playlistdetail, this.offline = false});

  @override
  PlaylistState createState() => PlaylistState();
}

class PlaylistState extends State<Playlist> {
  Future<List<Map>> tracks;
  Database db;

  List<Map> trackscomplete;

  List<Map> playlists = List();

  @override
  void initState() {
    super.initState();

    settracks();
    setplaylists();
  }

  void settracks() {
    var c = Completer<List<Map>>();
    tracks = c.future;

    idbFactory.open("musicDB").then((newdb) {
      db = newdb;
      var transaction = db.transactionList(["tracks", 'playlists'], "readonly");
      var obstore = transaction.objectStore("tracks");
      var stream = obstore.openCursor(autoAdvance: true);
      print("readingDB");
      print(obstore.count().then((i) {
        print(i);
      }));
      List<Map> alltracks = List();
      stream.listen((cursor) async {
        Map track = cursor.value;

        if (widget.usePlaylist) {
          if (track.containsKey('inPlaylists') &&
              (track['inPlaylists'] as List).contains(widget.playlistname)) {
            alltracks.add(track);
          }
        } else {
          alltracks.add(track);
        }
      }, onDone: () async {
        var tmptracks = List.from(alltracks);
        for (Map s in tmptracks) {
          if (widget.offline) {
            if (!await player.isCached(
                "https://dummyurl.com/?a=b", s["videoId"])) {
              alltracks.remove(s);
            }
          }
        }
        c.complete(alltracks);
        print("Completed");
      });
    });
  }

  void setplaylists() async {
    if (db == null) {
      db = await idbFactory.open('musicDB');
    }
    var playlistobstore =
        db.transaction('playlists', 'readonly').objectStore('playlists');
    var plstream = playlistobstore.openCursor(autoAdvance: true);
    plstream.listen((pl) {
      if (!(pl.value as Map).containsKey('playlistId')) {
        playlists.add(pl.value);
      }
    }).onDone(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.playlistname),
      ),
      body: FutureBuilder(
        future: tracks,
        builder: (context, ass) {
          if (ass.connectionState == ConnectionState.done) {
            List alltracks = ass.data;
            trackscomplete = alltracks;
            return ListView.builder(
              itemCount: alltracks.length,
              itemBuilder: (context, i) {
                return ExpansionTile(
                  leading: CachedNetworkImage(
                    imageUrl: TrackTile.urlfromImage(
                        alltracks[i]["videoThumbnails"], "medium"),
                    height: 100,
                    width: 100,
                  ),
                  title: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(alltracks[i]["title"]),
                      Text(
                        alltracks[i]["author"],
                        style: Theme.of(context).textTheme.subtitle,
                      )
                    ],
                  ),
                  children: <Widget>[
                    Wrap(
                      spacing: 8,
                      children: <Widget>[
                        RaisedButton.icon(
                          icon: Icon(Icons.play_arrow),
                          label: Text("Play Now"),
                          color: Theme.of(context).primaryColor,
                          onPressed: () {
                            widget.play([alltracks[i]]);
                            Navigator.pop(context);
                          },
                        ),
                        RaisedButton.icon(
                          icon: Icon(Icons.skip_next),
                          label: Text("Play Next"),
                          color: Theme.of(context).primaryColor,
                          onPressed: () {
                            widget.playnext(alltracks[i]);
                            Navigator.pop(context);
                          },
                        ),
                        RaisedButton.icon(
                          icon: Icon(Icons.add),
                          label: Text("Add to Playlist"),
                          color: Theme.of(context).primaryColor,
                          onPressed: () {
                            showDialog(
                                context: context,
                                builder: (context) {
                                  var l = List();
                                  for (var m in playlists) {
                                    l.add(FlatButton(
                                      child: Text(m['title']),
                                      onPressed: () async {
                                        var ob = db
                                            .transaction('tracks', 'readwrite')
                                            .objectStore('tracks');
                                        Map track = Map.from(alltracks[i]);
                                        if (track.containsKey('inPlaylists') &&
                                            !(track['inPlaylists'] as List)
                                                .contains(m['title'])) {
                                          (track['inPlaylists'] as List)
                                              .add(m['title']);
                                        } else {
                                          track['inPlaylists'] = List();
                                          (track['inPlaylists'] as List)
                                              .add(m['title']);
                                        }
                                        await ob.put(track);
                                        Navigator.pop(context);
                                      },
                                    ));
                                  }
                                  return SimpleDialog(
                                    title: Text("Add to Playlist"),
                                    children: <Widget>[...l],
                                  );
                                });
                          },
                        ),
                        if (widget.usePlaylist)
                          RaisedButton.icon(
                            icon: Icon(Icons.remove_circle),
                            label: Text("Remove from Playlist"),
                            color: Theme.of(context).primaryColor,
                            onPressed: () async {
                              var ob = db
                                  .transaction('tracks', 'readwrite')
                                  .objectStore('tracks');
                              Map track = Map.from(alltracks[i]);
                              if (track.containsKey('inPlaylists')) {
                                track['inPlaylists'] =
                                    List.from(track['inPlaylists']);
                                (track['inPlaylists'] as List)
                                    .remove(widget.playlistname);
                              }
                              await ob.put(track);
                              setState(() {
                                alltracks.removeAt(i);
                              });
                            },
                          ),
                        RaisedButton.icon(
                          icon: Icon(Icons.share),
                          label: Text("Share"),
                          color: Theme.of(context).primaryColor,
                          onPressed: () {
                            try {
                              Share.share("https://www.youtube.com/watch?v=");
                            } catch (e) {
                              
                              Scaffold.of(context).showSnackBar(SnackBar(
                                content: Text("Not supported by browser"),
                              ));
                            }
                          },
                        ),
                        RaisedButton.icon(
                          icon: Icon(Icons.delete),
                          label: Text("Delete"),
                          color: Theme.of(context).primaryColor,
                          onPressed: () {
                            var ob = db
                                .transaction("tracks", "readwrite")
                                .objectStore("tracks");
                            ob.delete(alltracks[i]["videoId"]);
                            setState(() {
                              alltracks.removeAt(i);
                            });
                          },
                        ),
                        RaisedButton.icon(
                          icon: Icon(Icons.info),
                          label: Text("Detail"),
                          color: Theme.of(context).primaryColor,
                          onPressed: () {
                            showBottomSheet(
                                context: context,
                                builder: (ctx) {
                                  return TrackDetail(alltracks[i], widget.play);
                                });
                          },
                        ),
                      ],
                    ),
                  ],
                );
              },
            );
          } else {
            return Center(
              child: CircularProgressIndicator(),
            );
          }
        },
      ),
      floatingActionButton: FloatingActionButton(
        child: Icon(Icons.play_arrow),
        onPressed: () async {
          widget.play(trackscomplete as List<Map>);
          Navigator.pop(context);
        },
      ),
    );
  }
}
