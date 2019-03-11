import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'dart:ui';
import 'dart:ui' as ui;
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'main.dart' as main;
import 'package:fluttery_seekbar/fluttery_seekbar.dart';

import 'queue.dart';
import 'searchScreen.dart';

const platform = const MethodChannel("me.devsilver.musicpiped/PlayerChannel");

ui.Image tmpImage;

class PlayerScreen extends StatefulWidget{

  PlayerScreen(Key key):super(key:key);


  

  @override
  State<StatefulWidget> createState() {
    return PlayerScreenState();
  }

  
}
class PlayerScreenState extends State<PlayerScreen>{

  bool isDragging = false;
  double newPos =0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        child: StreamBuilder(
          stream: main.mainStreamController.stream,
          builder: (context,ass){
            if(ass.connectionState==ConnectionState.active){
              
              Map data = ass.data["newB"];
              String thumbURL = 
                  getThumbnaillink(data["queue"], data["currentIndex"], "videoThumbnails", "medium","quality");
              return Container(
                decoration: BoxDecoration(
                  image: DecorationImage(
                    fit: BoxFit.cover,
                    image: CachedNetworkImageProvider(thumbURL)
                  )
                ),
                child: BackdropFilter(
                  filter: ImageFilter.blur(
                    sigmaX: 8,
                    sigmaY: 8
                  ),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Theme.of(context).scaffoldBackgroundColor.withAlpha(100)
                    ),
                    child: SafeArea(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        mainAxisSize: MainAxisSize.max,
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: <Widget>[
                          ButtonBar(
                            mainAxisSize: MainAxisSize.max,
                            alignment: MainAxisAlignment.spaceBetween,
                            children: <Widget>[
                              IconButton(
                                icon: Icon(Icons.arrow_drop_down),
                                onPressed: (){
                                  Navigator.of(context).pop();
                                },
                              ),
                              Text(
                                ""
                              ),
                              IconButton(
                                icon: Icon(Icons.playlist_play),
                                onPressed: (){
                                  Navigator.push(context, MaterialPageRoute(
                                    builder: (context)=>(QueueScreen(data["queue"], data["currentIndex"]))
                                  ));
                                },
                              )
                            ],
                          ),
                          Stack(
                            alignment: AlignmentDirectional.center,
                            children: <Widget>[
                              Align(
                                alignment: Alignment.center,
                                child:SizedBox(
                                  height: MediaQuery.of(context).size.width*0.6,
                                  width: MediaQuery.of(context).size.width*0.6,
                                  child: StatefulBuilder(
                                    key:Key("progress"),
                                    builder: (context,setState){

                                      return RadialSeekBar(
                                    
                                        seekWidth: 5.0,
                                        progressWidth: 5.0,
                                        trackWidth: 5.0,
                                        trackColor: Colors.white,
                                        progressColor: Colors.amber[200],
                                        thumbPercent: isDragging?newPos: data["currentplayingtime"]/data["totaltime"],
                                        progress: isDragging?newPos: data["currentplayingtime"]/data["totaltime"],
                                        thumb: CircleThumb(
                                          color: Colors.amber[200],
                                          diameter: 18.0,
                                        ),
                                        onDragStart: (pos){
                                          isDragging=true;
                                          newPos=pos;
                                          setState(() {
                                            
                                          });
                                        },
                                        onDragUpdate: (pos){
                                          newPos=pos;
                                          setState(() {
                                            
                                          });
                                        },
                                        onDragEnd: (pos){
                                          newPos=pos;
                                          isDragging=false;
                                          setState((){});

                                          invokeOnPlatform("seekTo", {"msec":pos*data["totaltime"]});
                                        },
                                      );
                                    },
                                  )
                                )
                              ),
                              Align( 
                                alignment: Alignment.center,
                                child: Container(
                                  alignment: Alignment.center,
                                  width: MediaQuery.of(context).size.width*0.5,
                                  height: MediaQuery.of(context).size.width*0.5,
                                  decoration: BoxDecoration(
                                    boxShadow: [
                                      BoxShadow(
                                        color: Colors.black,
                                        offset: Offset(1, 2),
                                        blurRadius: 4
                                      )
                                    ],
                                    shape: BoxShape.circle,
                                    image: DecorationImage(
                                      fit: BoxFit.cover,
                                      image: CachedNetworkImageProvider(
                                        thumbURL
                                      )
                                    )
                                  ),
                                ),
                              ),
                              Positioned(
                                left: MediaQuery.of(context).size.width*0.15,
                                top: 0,
                                child: IconButton(
                                  color: data["repeatMode"]>0?Theme.of(context).iconTheme.color:Theme.of(context).disabledColor,
                                  icon: Icon(data["repeatMode"]==2?Icons.repeat_one:Icons.repeat),
                                  onPressed: (){
                                    invokeOnPlatform("toggleRepeatMode", {"mode":(data["repeatMode"]+1)%3});
                                  },
                                )
                              ),
                              Positioned(
                                right: MediaQuery.of(context).size.width*0.15,
                                top: 0,
                                child: IconButton(
                                  color: data["shuffle"]?Theme.of(context).iconTheme.color:Theme.of(context).disabledColor,
                                  icon: Icon(Icons.shuffle),
                                  onPressed: (){
                                    invokeOnPlatform("toggleShuffle", {"shuffle":!data["shuffle"]});
                                  },
                                )
                              ),
                              Positioned(
                                left: MediaQuery.of(context).size.width*0.15,
                                bottom: 0,
                                child: Text(
                                  formatDuration(Duration(milliseconds: data["currentplayingtime"]))
                                )
                              ),
                              Positioned(
                                right: MediaQuery.of(context).size.width*0.15,
                                bottom: 0,
                                child: Text(
                                  formatDuration(Duration(milliseconds: data["totaltime"]))
                                )
                              ),
                            ],
                          ),
                          Container(
                            child: Text(
                              data["queue"][data["currentIndex"]]["title"],
                              style: Theme.of(context).textTheme.title,
                              textAlign: TextAlign.center,
                            ),
                          ),
                          Container(
                            child: Text(
                              data["queue"][data["currentIndex"]]["author"],
                              style: Theme.of(context).textTheme.subhead,
                              textAlign: TextAlign.center,
                            ),
                          ),
                          ButtonBar(
                            mainAxisSize: MainAxisSize.max,
                            alignment: MainAxisAlignment.center,
                            children: <Widget>[
                              IconButton(
                                icon: Icon(Icons.skip_previous),
                                onPressed: (){
                                  invokeOnPlatform("playIndex", {"index":data["currentIndex"]>0?data["currentIndex"]-1:data["queue"].length-1});
                                },
                              ),
                              IconButton(
                                icon: Icon(data["isplaying"]?Icons.pause_circle_filled:Icons.play_circle_filled),
                                iconSize: 80,
                                onPressed: (){
                                  invokeOnPlatform(data["isplaying"]?"pause":"play",null);
                                },
                              ),
                              IconButton(
                                icon: Icon(Icons.skip_next),
                                onPressed: (){
                                  invokeOnPlatform("playIndex", {"index":data["currentIndex"]<data["queue"].length-1?data["currentIndex"]+1:0});
                                },
                              ),
                            ],
                          )
                        ],
                      ),
                    ),
                  ),
                ),
              );
            }
            else{
              return Center(
                child: Text(
                  "NO DATA"
                ),
              );
            }
          },
        ),
      ),
    );
  }

}
Future<dynamic> invokeOnPlatform(String method,dynamic arg) async {
  try{
    print("INVOKING METHOD $method");
    return await platform.invokeMethod(method,arg);
  } on PlatformException catch(e){
      print(e);
  }

}
String formatDuration(Duration d){
  int mins = d.inMinutes;
  int secs = d.inSeconds%60;
  return mins.toString().padLeft(1,"0")+":"+secs.toString().padLeft(2,"0"); 
}