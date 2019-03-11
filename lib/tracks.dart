import 'dart:async';

import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'searchScreen.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'queue.dart';

class Tracks extends StatelessWidget {
  final Future tracksfuture;

  final Function refresh;

  Tracks(this.tracksfuture, this.refresh);
  @override
  Widget build(BuildContext baseContext) {
    return FutureBuilder(
        future: tracksfuture,
        builder: (BuildContext btcx, AsyncSnapshot ass) {
          if (ass.connectionState == ConnectionState.done) {
            List tracks = ass.data;
            List<Slidable> items = List();

            for (int i = 0; i < tracks.length; i++) {
              Slidable s = Slidable(
                delegate: SlidableDrawerDelegate(),
                child: Card(
                  child: ListTile(
                    leading: Container(
                      width: 80,
                      child: CachedNetworkImage(
                          imageUrl: getThumbnaillink(tracks, i,
                              "videoThumbnails", "medium", "quality"),
                        fit: BoxFit.cover,
                      ),
                    ),
                    title: Text(
                      tracks.elementAt(i)["title"],
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style:
                          TextStyle(fontWeight: FontWeight.w600, fontSize: 18),
                    ),
                    subtitle: Text(tracks.elementAt(i)["author"]),
                    onTap: () {
                      invokeOnPlatform(
                          "updateQueue", {"queue": tracks, "index": i});
                    },
                  ),
                ),
                actions: <Widget>[
                  IconSlideAction(
                    icon: Icons.favorite,
                    color: Colors.pink,
                    caption: "Favorite",
                    onTap: (){
                      invokeOnPlatform("addTracktoPlaylist", {"track":tracks[i]["title"],"playlistId":1});
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
                                      platform.invokeMethod("addTracktoPlaylist",{"track":tracks[i]["title"],"playlistId":playlists[index]["id"]});
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
                      platform.invokeMethod("addtoQueue",{"queue":[tracks[i]]});
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
                          "deletetrack", {"title": tracks[i]["title"]});
                      Scaffold.of(baseContext).showSnackBar(
                        SnackBar(
                          content: Text(
                            "Deleted "+tracks[i]["title"]
                          ),
                        )
                      );
                      refresh();
                    },
                  )
                ],
              );
              items.add(s);
            }
            return SliverList(
              key: Key(tracks.hashCode.toString()),
              delegate: SliverChildListDelegate(items),
            );
          } else {
            return SliverToBoxAdapter(
              child: Container(
                child: Center(
                  child: CircularProgressIndicator(),
                ),
              ),
            );
          }
        });
  }
}
