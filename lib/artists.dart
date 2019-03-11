import 'dart:async';

import 'package:flutter/material.dart';
import 'searchScreen.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'queue.dart' as queue;
import 'playlistSpcl.dart';

class Artists extends StatelessWidget{

  final Future artistfuture;
  Artists(this.artistfuture);

  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: artistfuture,
      builder: (BuildContext btcx, AsyncSnapshot ass){
        if(ass.connectionState==ConnectionState.done){
          List tracks = ass.data;
          List<Widget> wids=new List();
          for(int i=0;i<tracks.length;i++){
            wids.add(
                Column(
                  children: <Widget>[
                    Expanded(
                      child: Container(
                        padding: EdgeInsets.only(
                          top: 20,
                          bottom: 20,
                          left: 10,
                          right: 10,
                        ),

                        child: Material(

                          child: InkWell(
                            onTap: (){
                              Navigator.push(context, MaterialPageRoute(
                                    builder: (context)=>SpecialPlaylist(
                                      Text(tracks[i]["author"]), Container(
                                        decoration: BoxDecoration(
                                          image: DecorationImage(
                                              image: CachedNetworkImageProvider(
                                                getThumbnaillink(tracks, i,"authorThumbnails",176,"width"),
                                              ),
                                              fit: BoxFit.cover),
                                          
                                        ),
                                        child: Container(
                                          color: Colors.black38
                                        ),
                                      ),
                                      queue.platform.invokeMethod("requestArtistTrack",{"artistId":tracks[i]["authorId"]}) 
                                      )
                                  ));
                            },
                            onLongPress: (){

                            },
                            child:Container(
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.all(Radius.circular(20)),
                                boxShadow: [BoxShadow(
                                  color: Colors.black38,
                                  offset: Offset(2, 2),
                                  spreadRadius: 1
                                )]
                              ),
                              child: ClipRRect(
                                borderRadius: BorderRadius.all(Radius.circular(20)),
                                child: CachedNetworkImage(
                                  imageUrl:  getThumbnaillink(tracks,i, "authorThumbnails", 176,"width"),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),

                    Text(
                      tracks[i]["author"],
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                        shadows: [Shadow(
                          color: Colors.black38,
                          offset: Offset(1, 1),
                        )]
                      ),)
                  ],
                )
            );
          }

          return SliverGrid.count(
            crossAxisCount: 2,
            children: wids,
            
          );
        } else{
          return SliverToBoxAdapter(
            child: CircularProgressIndicator(),
          );
        }
      },
    );
  }

}