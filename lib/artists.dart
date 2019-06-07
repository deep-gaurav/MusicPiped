import 'dart:async';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:idb_shim/idb.dart';
import 'package:idb_shim/idb_io.dart';

import 'artistPlaylist.dart';
import 'trending.dart';
import 'main.dart';

class Artists extends StatelessWidget {
  void Function(List<Map>) play;
  void Function(Map) playnext;

  Artists(this.play, this.playnext);

  @override
  Widget build(BuildContext context) {
    var futuredb = idbFactory.open("musicDB");

    return FutureBuilder<Database>(
      future: futuredb,
      builder: (context, ass) {
        if (ass.connectionState == ConnectionState.done) {
          var db = ass.data;
          var ob = db
              .transaction("tracks", "readonly")
              .objectStore("tracks")
              .openCursor(autoAdvance: true);
          var lc = Completer<List<Map>>();
          var l = lc.future;
          var list = List<Map>();
          ob.listen((data) {
            list.add(data.value);
          }, onDone: () {
            lc.complete(list);
          });
          return FutureBuilder<List<Map>>(
            future: l,
            builder: (ctx, ass) {
              if (ass.connectionState == ConnectionState.done) {
                var list = ass.data;
                var newl = Map<String, Map<String, String>>();

                var arthumbs = List<Widget>();
                for (var c in list) {
                  print(c["author"]);
                  if (newl.containsKey(c["author"]) &&
                      newl[c["author"]]["thumbURL"] != "") {
                    continue;
                  } else {
                    var thumburl = TrackTile.urlfromImage(
                        c["authorThumbnails"], 176,
                        param: "width");
                    newl[c["author"]] = {
                      "name": c["author"],
                      "thumbURL": thumburl
                    };
                  }
                }
                for (var artist in newl.keys) {
                  arthumbs.add(Padding(
                    padding: EdgeInsets.all(8),
                    child: InkWell(
                      onTap: () {
                        Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (context) => (ArtistPlaylist(
                                      play,
                                      playnext,
                                      newl[artist]["name"],
                                      usePlaylist: true,
                                      playlistdetail: {'id': artist},
                                    ))));
                      },
                      child: Container(
                        height: 176,
                        width: 176,
                        decoration: BoxDecoration(
                            boxShadow: [
                              BoxShadow(
                                  color: Colors.black,
                                  blurRadius: 2,
                                  offset: Offset(2, 2))
                            ],
                            shape: BoxShape.circle,
                            image: DecorationImage(
                                image: CachedNetworkImageProvider(
                                    newl[artist]["thumbURL"] == ""
                                        ? "https://via.placeholder.com/150"
                                        : (newl[artist]["thumbURL"])),
                                fit: BoxFit.cover)),
                      ),
                    ),
                  ));
                }
                return GridView(
                    gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                      crossAxisCount: 2,
                    ),
                    children: arthumbs);
              } else {
                return Center(
                  child: CircularProgressIndicator(),
                );
              }
            },
          );
        } else {
          return Center(
            child: CircularProgressIndicator(),
          );
        }
      },
    );
  }
}
