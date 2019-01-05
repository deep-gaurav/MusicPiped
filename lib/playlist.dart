import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'queue.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'searchScreen.dart';
import 'dart:math';
import 'dart:async';

class Playlist extends StatefulWidget{

  final Map playlist;
  final Future tracks;

  Playlist(this.playlist, this.tracks);

  @override
  State<StatefulWidget> createState() {
    return PlaylistState(playlist, tracks);
  }

}
class PlaylistState extends State<Playlist>{

  Future tracksfuture;
  List tracks;
  Map playlist;
  Timer timer;

  PlaylistState(this.playlist,this.tracksfuture){
    timer=Timer.periodic(
      Duration(seconds: 5), 
      (tim){
        setState(() {
                  
                });
      });
  }

  @override
  Widget build(BuildContext context) {
    return Theme(
      data: Theme.of(context).copyWith(
        ),
      child: Scaffold(
        
        body:FutureBuilder(
          future: tracksfuture,
          builder: (BuildContext bdctx, AsyncSnapshot ass){
            if(ass.connectionState==ConnectionState.done){
              tracks=ass.data;
              return CustomScrollView(
                physics: BouncingScrollPhysics(),
                slivers: <Widget>[
                  SliverAppBar(
                    pinned: false,
                    expandedHeight: 150,
                    flexibleSpace: FlexibleSpaceBar(
                      title: Text(
                        playlist["name"],
                        style: TextStyle(
                          color: Colors.white
                        ),
                      ),
                      background: Container(
                        decoration: BoxDecoration(
                          image: tracks.length>0?DecorationImage(
                              image: CachedNetworkImageProvider(
                                getThumbnaillink(tracks, Random().nextInt(tracks.length),
                                    "videoThumbnails", "medium", "quality"),
                              ),
                              fit: BoxFit.cover):null,
                        ),
                        child: Container(
                          color: Colors.black38,
                        ),
                      ),
                    ),
                  ),
                  SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (BuildContext bdctx, int index){
                        return Slidable(
                          key: Key(tracks[index]["title"]),
                          delegate: SlidableDrawerDelegate(),
                          slideToDismissDelegate: SlideToDismissDrawerDelegate(
                            onDismissed: (type) async {
                              invokeOnPlatform("removeTrackfromPlaylist", {"track":tracks[index]["title"],"playlistId":playlist["id"]});
                                setState(() {
                                  tracks=null;
                                  tracksfuture = invokeOnPlatform("getTracksinPlaylist", {"playlistId":playlist["id"]});                        
                                  });
                            }
                          ),
                          actions: <Widget>[
                            IconSlideAction(
                              icon: Icons.delete,
                              color: Colors.red,
                              caption: "Remove from playlist",
                              onTap: ()async{
                                invokeOnPlatform("removeTrackfromPlaylist", {"track":tracks[index]["title"],"playlistId":playlist["id"]});
                                setState(() {
                                  tracks=null;
                                  tracksfuture = invokeOnPlatform("getTracksinPlaylist", {"playlistId":playlist["id"]});                        
                                  });
                              },
                            ),
                          ],
                          child: Card(
                            child: InkWell(
                              child: ListTile(
                                leading: Container(
                                  width: 80,
                                  child: CachedNetworkImage(
                                      imageUrl: getThumbnaillink(tracks, index,
                                          "videoThumbnails", "medium", "quality"),
                                    fit: BoxFit.cover,
                                  ),
                                ),
                                title: Text(
                                  tracks.elementAt(index)["title"],
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style:
                                      TextStyle(fontWeight: FontWeight.w600, fontSize: 18),
                                ),
                                onTap: (){
                                  invokeOnPlatform("updateQueue", {"queue": tracks, "index": index});
                                  Scaffold.of(bdctx).showSnackBar(
                                    SnackBar(
                                      content: Text(
                                        "Playing  "+tracks[index]["title"]
                                      ),
                                    )
                                  );
                                  
                                },
                              ),
                            ),
                          ),
                        );
                      },
                      childCount: tracks.length 
                    ),
                  )
                ],
                
              );
            }else{
              return CircularProgressIndicator();
            }
          },
        )
      ),
    );
  }

}