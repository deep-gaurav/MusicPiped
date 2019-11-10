import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;

import 'package:flutter/material.dart';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:idb_shim/idb.dart';

import 'searchScreen.dart';
import 'trending.dart';
import 'main.dart';

class TrackDetail extends StatelessWidget {
  final Map initialtrackInfo;

  Map trackInfo;
  void Function(List<Map>) onPressed;
  Future recommendedWids;

  var db;
  var playlists = List();

  TrackDetail(this.initialtrackInfo, this.onPressed){
    setplaylists();
  }


  void setplaylists() async {
    if (db == null) {
      db = await idbFactory.open('musicDB');
    }
    var playlistobstore =
    db.transaction('playlists', 'readonly').objectStore('playlists');
    var plstream = playlistobstore.openCursor(autoAdvance: true);
    plstream.listen((pl) {
      if (!(pl.value as Map).containsKey('playlistId') && !(pl.value as Map).containsKey('mixId')) {
        playlists.add(pl.value);
      }
    }).onDone(() {});
  }
  @override
  Widget build(BuildContext context) {
    trackInfo = initialtrackInfo;
    if (trackInfo.containsKey('recommendedVideos')) {
      recommendedWids = Future.value(trackInfo);
    } else {
      var c = Completer();
      recommendedWids = c.future;
      var url = MyHomePageState.InvidiosAPI + "videos/" + trackInfo["videoId"];
      http.get(url).then((response) {
        var j = json.decode(utf8.decode(response.bodyBytes));
        trackInfo = j;
        c.complete(j);
      });
    }
    return Material(
      child: CustomScrollView(
        shrinkWrap: true,
        slivers: <Widget>[
          SliverAppBar(
            expandedHeight: 200,
            floating: true,
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                children: <Widget>[
                  Positioned.fill(
                    child: CachedNetworkImage(
                      imageUrl: TrackTile.urlfromImage(
                          trackInfo["videoThumbnails"], "high"),
                      fit: BoxFit.cover,
                    ),
                  ),
                  Container(
                    decoration: BoxDecoration(
                        gradient: LinearGradient(
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                            colors: [Colors.transparent, Colors.black])),
                  ),
                ],
              ),
              title: Text(trackInfo["title"]),
            ),
          ),
          SliverToBoxAdapter(
              child: Wrap(
            children: <Widget>[
              Chip(
                avatar: Icon(Icons.people),
                label: Text(trackInfo["author"]),
              ),
              Chip(
                avatar: Icon(Icons.timer),
                label: Text(formatDuration(
                    Duration(seconds: trackInfo["lengthSeconds"]))),
              ),
              Chip(
                avatar: Icon(Icons.trending_up),
                label: Text(trackInfo["viewCount"].toString()),
              ),
              if (trackInfo["timesPlayed"] != null)
                Chip(
                  avatar: Icon(Icons.favorite),
                  label: Text(
                      "TimesPlayed : " + (trackInfo["timesPlayed"]).toString()),
                )
            ],
          )),
          SliverToBoxAdapter(
            child: Wrap(
              spacing: 5,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: <Widget>[
                RaisedButton.icon(
                  icon: Icon(Icons.play_arrow),
                  label: Text("Play"),
                  onPressed: () {
                    List<Map> q = new List();
                    q.add(trackInfo);
                    onPressed(q);
                    Navigator.of(context).pop(trackInfo);
                  },
                  color: Theme.of(context).primaryColor,
                ),
                RaisedButton.icon(
                  icon: Icon(Icons.playlist_play),
                  label: Text("Play with Queue"),
                  onPressed: () {
                    List<Map> q = new List();
                    q.add(trackInfo);
                    for (var x in trackInfo["recommendedVideos"]) {
                      q.add(x);
                    }
                    onPressed(q);
                    Navigator.of(context).pop(trackInfo);
                  },
                  color: Theme.of(context).primaryColor,
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
                                ObjectStore ob = db
                                    .transaction('tracks', 'readwrite')
                                    .objectStore('tracks');
                                Map track = trackInfo;
                                if(await ob.getObject(track["videoId"])!=null){
                                  track= await ob.getObject(track['videoId']);
                                }
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
                RaisedButton.icon(
                  icon: Icon(Icons.queue_play_next),
                  label: Text("Add to Queue"),
                  color: Theme.of(context).primaryColor,
                  onPressed: (){
                    mainKey.currentState.playNext(trackInfo);
                    Navigator.of(context).pop();
                    Scaffold.of(context).showSnackBar(
                      SnackBar(content: Text("Added to Queue"),)
                    );
                  },
                )

              ],
            ),
          ),
          SliverToBoxAdapter(
            child: Container(
              padding: EdgeInsets.all(8),
              child: Text(
                "Recommended Queue",
                style: Theme.of(context).textTheme.title
                  ..copyWith(fontWeight: FontWeight.w800),
              ),
            ),
          ),
          FutureBuilder(
              future: recommendedWids,
              builder: (context, ass) {
                if (ass.connectionState == ConnectionState.done) {
                  var trackInfo = ass.data;
                  return SliverList(
                    delegate: SliverChildBuilderDelegate((context, i) {
                      return Card(
                        elevation: 4,
                        child: ListTile(
                          leading: Text(i.toString()),
                          title:
                              Text(trackInfo["recommendedVideos"][i]["title"]),
                        ),
                      );
                    },
                        childCount:
                            (trackInfo["recommendedVideos"] as List).length),
                  );
                } else {
                  return SliverToBoxAdapter(
                      child: Center(
                    child: CircularProgressIndicator(),
                  ));
                }
              })
        ],
      ),
    );
  }
}
