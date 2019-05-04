import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_pagewise/flutter_pagewise.dart';

import 'playlist.dart';
import 'queue.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class Playlists extends StatefulWidget{

  @override
  _PlaylistsState createState() => _PlaylistsState();
}

class _PlaylistsState extends State<Playlists> {
  List playlists;
  
  @override
  Widget build(BuildContext context) {
    return StatefulBuilder(
      builder: (BuildContext bdctx, setState){
        return PagewiseSliverList(
          pageSize: 10,
          pageFuture: (pageIndex){
                Completer<List<dynamic>> c = Completer();
                Future f = platform.invokeMethod("getPlaylists",{"page":pageIndex});
                f.then((tr){
                  List l = tr;
                  print(l.length);
                  if(playlists==null){
                    playlists = new List();
                    playlists.addAll(l);
                    
                  }
                  else{
                    playlists.addAll(l);
                  }
                  c.complete(tr);
                });
                return c.future;
          },
          itemBuilder: (context,entry,index){
            return Slidable(
                delegate: SlidableDrawerDelegate(),
                actions: <Widget>[
                  IconSlideAction(
                    icon: Icons.delete,
                    color: Colors.red,
                    onTap: (){
                      if(entry["id"]>1){
                        invokeOnPlatform("removePlaylist", {"playlistId":entry["id"]});
                        Scaffold.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              "Deleted Playlist "+entry["name"],
                            ),
                            duration: Duration(seconds: 1),
                          )
                        );
                        setState(() {
                        });;
                      } else{
                        Scaffold.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              "Cannot Delete Favorite"
                            ),
                            duration: Duration(seconds: 1),
                          )
                        );
                      }
                    },
                  )
                ],
                child: Card(
                  child: ListTile(
                    title: Text(entry["name"]),
                    onTap: (){
                      Future tracks = invokeOnPlatform("getTracksinPlaylist", {"playlistId":entry["id"]});
                      Navigator.push(bdctx, MaterialPageRoute(
                        builder: (bdctx) => Playlist(entry, tracks)
                      ));
                    },
                  ),
                ),
              );
          },
        );
      },
    );
  }
}