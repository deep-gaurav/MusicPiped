import 'dart:async';

import 'package:flutter/material.dart';
import 'playlist.dart';
import 'queue.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class Playlists extends StatelessWidget{

  final Future playlistsfuture;
  final Function refresh;

  Playlists(this.playlistsfuture,this.refresh);

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: playlistsfuture,
      builder: (BuildContext bdctx, AsyncSnapshot ass){
        if(ass.connectionState==ConnectionState.done){
          List playlists = ass.data;
          List<Widget> playlistwids=List();
          for(int i=0;i<playlists.length;i++){
            playlistwids.add(
              Slidable(
                delegate: SlidableDrawerDelegate(),
                actions: <Widget>[
                  IconSlideAction(
                    icon: Icons.delete,
                    color: Colors.red,
                    onTap: (){
                      if(playlists[i]["id"]>1){
                        invokeOnPlatform("removePlaylist", {"playlistId":playlists[i]["id"]});
                        Scaffold.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              "Deleted Playlist "+playlists[i]["name"],
                            ),
                            duration: Duration(seconds: 1),
                          )
                        );
                        refresh();
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
                    title: Text(playlists[i]["name"]),
                    onTap: (){
                      Future tracks = invokeOnPlatform("getTracksinPlaylist", {"playlistId":playlists[i]["id"]});
                      Navigator.push(bdctx, MaterialPageRoute(
                        builder: (bdctx) => Playlist(playlists[i], tracks)
                      ));
                    },
                  ),
                ),
              )
            );
          }

          return SliverList(
            delegate: SliverChildListDelegate(
              playlistwids
            ),
          );


        } else{
          return SliverToBoxAdapter(child: CircularProgressIndicator());
        }
      },
    );
  }


}