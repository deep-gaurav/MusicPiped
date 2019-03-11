import 'dart:async';

import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'searchScreen.dart';
import 'queue.dart';

class SpecialPlaylist extends StatelessWidget{

  final Widget title;
  final Widget background;
  final Future trackfuture;

  SpecialPlaylist(this.title,this.background,this.trackfuture);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        physics: BouncingScrollPhysics(),
        slivers: <Widget>[
          SliverAppBar(
            pinned: true,
            expandedHeight: 150,
            flexibleSpace: FlexibleSpaceBar(
              title: title,
              background: background,
            ),
          ),
          FutureBuilder(
            future: trackfuture,
            builder: (BuildContext context,AsyncSnapshot ass){
              if(ass.connectionState==ConnectionState.done){
                List tracks = ass.data;
                return SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (BuildContext context, int index){
                      return Card(
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
                                    Scaffold.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text(
                                          "Playing  "+tracks[index]["title"]
                                        ),
                                      )
                                    );
                                    
                                  },
                                ),
                              ),
                            );
                    },
                    childCount: tracks.length
                  ),
                );
              }else{
                return SliverToBoxAdapter(
                  child: CircularProgressIndicator(),
                );
              }
            },
          )
        ],
      ),
    );
  }


}