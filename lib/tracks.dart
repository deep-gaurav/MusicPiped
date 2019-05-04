import 'dart:async';

import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'searchScreen.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:flutter_pagewise/flutter_pagewise.dart';

import 'queue.dart';

class Tracks extends StatefulWidget {

  @override
  _TracksState createState() => _TracksState();
}

class _TracksState extends State<Tracks> {
  List tracks;
  int i=0;
  @override
  Widget build(BuildContext baseContext) {
    return StatefulBuilder(
        builder: (BuildContext btcx, setState) {
            return PagewiseSliverList(
              
              pageSize: 10,
              itemBuilder: (context,entry,index){
                print("index $index");
                return Slidable(
                  delegate: SlidableDrawerDelegate(),
                  child: Card(
                    child: ListTile(
                      leading: Container(
                        width: 80,
                        child: CachedNetworkImage(
                            imageUrl: getThumbnaillink([entry], 0,
                                "videoThumbnails", "medium", "quality"),
                          fit: BoxFit.cover,
                        ),
                      ),
                      title: Text(
                        entry["title"],
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style:
                            TextStyle(fontWeight: FontWeight.w600, fontSize: 18),
                      ),
                      subtitle: Text(entry["author"]),
                      onTap: () {
                        invokeOnPlatform(
                            "updateQueue", {"queue": tracks, "index": tracks.indexOf(entry)});
                        Scaffold.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              "Playing "+entry["title"]
                            ),
                          )
                        );
                      },
                    ),
                  ),
                  actions: <Widget>[
                    IconSlideAction(
                      icon: Icons.favorite,
                      color: Colors.pink,
                      caption: "Favorite",
                      onTap: (){
                        invokeOnPlatform("addTracktoPlaylist", {"track":entry,"playlistId":1});
                        Scaffold.of(baseContext).showSnackBar(
                          SnackBar(
                            content: Text("Added to Favorite"),
                          )
                        );
                      },
                    ),
                    IconSlideAction(
                      icon: Icons.playlist_add,
                      color: Colors.teal,
                      caption: "Add to",
                      onTap: (){
                        Future playlistfuture = platform.invokeMethod("getPlaylists");
                        playlistfuture.then(
                          (value){
                            List playlists= value;
                            showDialog(
                              context: baseContext,
                              builder: (BuildContext context){

                                List<Widget> items = new List();
                                for(int index=0;index<playlists.length;index++){
                                  items.add(
                                    ListTile(
                                      title: Text(playlists[index]["name"]),
                                      onTap: (){
                                        platform.invokeMethod("addTracktoPlaylist",{"track":entry["title"],"playlistId":playlists[index]["id"]});
                                        Scaffold.of(baseContext).showSnackBar(
                                          SnackBar(
                                            content: Text("Added to "+playlists[index]["name"]),
                                            duration: Duration(seconds: 1),
                                          )
                                        );
                                        Navigator.of(context).pop();
                                      },
                                    )
                                  );
                                }
                                return SimpleDialog(
                                  title: Text(
                                    "Select Playlist",
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold
                                    ),),
                                  children: items,
                                );
                              }
                            );
                          }
                        );
                        
                      },
                    ),
                  ],
                  secondaryActions: <Widget>[
                    IconSlideAction(
                      icon: Icons.queue_music,
                      color: Colors.grey,
                      caption: "Add to Queue",
                      onTap: (){
                        platform.invokeMethod("addtoQueue",{"queue":[entry]});
                        Scaffold.of(baseContext).showSnackBar(
                          SnackBar(
                            content: Text("Added to paying Queue"),
                            duration: Duration(seconds: 1),
                          )
                        );
                      },
                    ),
                    IconSlideAction(
                      icon: Icons.delete,
                      color: Colors.red,
                      caption: "Delete",
                      onTap: () {
                        invokeOnPlatform(
                            "deletetrack", {"title": entry["title"]});
                        Scaffold.of(baseContext).showSnackBar(
                          SnackBar(
                            content: Text(
                              "Deleted "+entry["title"]
                            ),
                          )
                        );
                        setState(() {
                          
                        });
                        
                      },
                    )
                  ],
                );
              },
              pageFuture: (pageIndex){
                print("Load Page $pageIndex");
                Completer<List<dynamic>> c = Completer();
                Future f = platform.invokeMethod("requestAllTracks",{"page":pageIndex});
                f.then((tr){
                  List l = tr;
                  print(l.length);
                  if(tracks==null){
                    tracks = new List();
                    tracks.addAll(l);
                    i=0;
                  }
                  else{
                    i=i+1;
                    tracks.addAll(l);
                  }
                  c.complete(tr);
                });
                return c.future;
              },
            );
        }
      );
  }
}
