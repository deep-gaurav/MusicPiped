import 'package:flutter/material.dart';
import 'searchScreen.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_pagewise/flutter_pagewise.dart';

import 'playlistSpcl.dart';
import 'queue.dart';

class Artists extends StatefulWidget{


  @override
  _ArtistsState createState() => _ArtistsState();
}

class _ArtistsState extends State<Artists> {
  @override
  Widget build(BuildContext context) {
    return StatefulBuilder(
      builder: (BuildContext btcx, setState){
        
        return PagewiseSliverGrid.count(
          pageSize: 10,
          pageFuture: (pageIndex){
            return platform.invokeMethod("requestArtists",{"page":pageIndex});
          },
          crossAxisCount: 2,
          itemBuilder: (context,entry,index){
            print(entry);
            return Column(
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
                                      Text(entry["author"]), Container(
                                        decoration: BoxDecoration(
                                          image: DecorationImage(
                                              image: CachedNetworkImageProvider(
                                                getThumbnaillink([entry], 0,"authorThumbnails",176,"width"),
                                              ),
                                              fit: BoxFit.cover),
                                          
                                        ),
                                        child: Container(
                                          color: Colors.black38
                                        ),
                                      ),
                                      platform.invokeMethod("requestArtistTrack",{"artistId":entry["authorId"]}) 
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
                                  imageUrl:  getThumbnaillink([entry],0, "authorThumbnails", 176,"width"),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),

                    Text(
                      entry["author"],
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                        shadows: [Shadow(
                          color: Colors.black38,
                          offset: Offset(1, 1),
                        )]
                      ),)
                  ],
                );
          },
        );
      },
    );
  }
}