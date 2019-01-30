import 'package:flutter/material.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter_swiper/flutter_swiper.dart';
import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'dart:ui';
import 'dart:ui' as ui;
import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';
import 'package:collection/collection.dart';
import 'main.dart' as main;
import 'dart:convert';

import 'queue.dart';

const platform = const MethodChannel("me.devsilver.musicpiped/PlayerChannel");

ui.Image tmpImage;

class PlayerScreen extends StatefulWidget{

  PlayerScreen(Key key):super(key:key);


  

  @override
  State<StatefulWidget> createState() {
    return PlayerState();
  }

  
}
class PlayerState extends State<PlayerScreen> with SingleTickerProviderStateMixin{


  double value=0;
  List l;
  int index=0;
  SwiperController sc;
  ProgressBar progressBar;
  bool isPlaying=false;
  int repeatMode=0;
  bool shuffle=false;
  AnimationController playpausecontroller;
  ButtonBar buttonBar;
  bool dark=true;
  Color dominantcolor=Colors.black;

  Map _data=Map();
  Timer loopTimer;
  bool dont=false;

  List favoritetracks;
  bool initialised=false;
 

  void handleRefresh(Timer t){
    _data=main.bCast;
    if(main.bCast["queueUpdate"]==true || main.pendingUpdate){
      main.pendingUpdate=false;
      setState(() {
              l=_data["queue"];
            });
      
    }
    if(_data.isEmpty){
      print("_data is null");
      return;
    }
    l=_data["queue"];
    progressBar.updateProgress(_data);
    checkSwiper();
    if(_data.isNotEmpty ){
      isPlaying=_data["isplaying"];
      if(isPlaying && (playpausecontroller.status!=AnimationStatus.completed || playpausecontroller.status != AnimationStatus.forward)){
        playpausecontroller.forward();
      }
      else if (!isPlaying && (playpausecontroller.status!=AnimationStatus.dismissed ||  playpausecontroller.status != AnimationStatus.forward)){
        playpausecontroller.reverse();
      }
    }
    
    if(repeatMode!=_data["repeatMode"]){
      setState(() {
              repeatMode=_data["repeatMode"];
              buttonBar.update(repeatMode,shuffle, playpausecontroller, isPlaying, dark, sc, dominantcolor);
            });
    }
    if(shuffle != _data["shuffle"]){
      setState(() {
        shuffle = _data["shuffle"];
        buttonBar.update(repeatMode, shuffle, playpausecontroller, isPlaying, dark, sc, dominantcolor);
      });
    }
    if(index!=_data["currentIndex"]){
      setState(() {
              index=_data["currentIndex"];
            });
    }
  }



  @override
    void initState() {

      Duration timerDuration= Duration(
        seconds: 1
      );
      if(_data.isEmpty){
        index=0;
        l=[{"title":"NOTHING PLAYING","author":"NONE"},];
      }
      else{
        index=_data["currentIndex"];
        l = _data["queue"];
      }

      loopTimer=Timer.periodic(timerDuration, handleRefresh);
      
      int _totalTrackLength = _data.isEmpty?0:_data["totaltime"];
      int _currentPlayingTime = _data.isEmpty?0:_data["currentplayingtime"];
      value=_totalTrackLength!=0?_currentPlayingTime/_totalTrackLength:0;
      sc=SwiperController();
      progressBar=ProgressBar();
      
      playpausecontroller=AnimationController(
        value: 0,
        lowerBound: 0,
        upperBound: 1,
        duration: Duration(milliseconds: 400),
        vsync: this
      );
      playpausecontroller.addListener(
        (){
          buttonBar.update(repeatMode,shuffle, playpausecontroller, isPlaying, dark, sc, dominantcolor);
        }
      );
      playpausecontroller.addStatusListener(
        (AnimationStatus status){
          if(status==AnimationStatus.completed){
            isPlaying=true;
          }
          if(status==AnimationStatus.dismissed){
            isPlaying=false;
          }
        }
      );
      buttonBar=ButtonBar(
                      Key(index.toString()),
                      repeatMode,
                      shuffle,
                      playpausecontroller,
                      isPlaying,
                      dark,
                      sc,
                      dominantcolor,
                      (bool play){
                        if(play){
                          isPlaying=true;
                          main.bCast["isplaying"]=isPlaying;
                          playpausecontroller.forward();
                        } else{
                          isPlaying=false;
                          main.bCast["isplaying"]=isPlaying;
                          playpausecontroller.reverse();
                        }
                      }
      );
      super.initState();

    }

  @override
  void dispose(){
    print("Dispose called");
    loopTimer.cancel();
    super.dispose();
  }
  void checkSwiper(){
    int i= (_data.isEmpty)?0:_data["currentIndex"];
    print("Swiper index $i");
    if(i!=index){
      sc.move(i);
      setState(() {
              index=i;
            });
    }
  }
  Swiper makeSwiper(){
    return Swiper(
                  key: Key(l.toString()),
                  index: index,
                  itemWidth: 320,
                  controller: sc,
                itemBuilder: (BuildContext context,int index){
                  return Card(
                    
                    margin: EdgeInsets.all(8),
                    elevation: 10,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12)
                    ),
                    child:Container(
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(12)
                      ),
                      child:ClipRRect(
                        borderRadius: BorderRadius.circular(12),
                        child: CachedNetworkImage(
                          imageUrl: findThumbnailURL(index, "medium"),
                          fit: BoxFit.fill,
                        ),
                      )
                    )
                  );
                },
                itemCount: l.length,
                onIndexChanged: (i){
                  print("new index $i");
                  
                  if(dont!=true){
                    invokeOnPlatform("playIndex", {'index':i});
                  }else{
                    dont=false;
                  }
                },
                viewportFraction: 1,
                scale: 0.6,
              );
  }
  @override
  Widget build(BuildContext context) {

    //makePalette(CachedNetworkImageProvider(findThumbnailURL(index,"medium")));

    bool dark=true;
    Color c=Colors.black;
    
    
    this.dark=dark;
    dominantcolor=c;
    String next="";
    print(l);
    if(l.length>index+1){
      next=l.elementAt(index+1)["title"];
    }else{
      next=l.elementAt(0)["title"];
    }
    
    return Scaffold(
        body:Container(
          decoration: BoxDecoration(
            image: DecorationImage(
              image: CachedNetworkImageProvider(findThumbnailURL(index,"medium")),
              fit: BoxFit.cover,
              colorFilter: ColorFilter.mode(Colors.grey, BlendMode.darken)
            ),
            color: _data.isEmpty?Colors.black45:Colors.transparent,
          ),
          child: BackdropFilter(
            filter: ImageFilter.blur(
              sigmaX: 10,
              sigmaY: 10
            ),
            child: Container(
              decoration: BoxDecoration(
                color: Colors.transparent
              ),
                child: SafeArea(
                  child: Column(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  mainAxisSize: MainAxisSize.max,
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: <Widget>[
                    
                    Container(
                      padding: EdgeInsets.only(top: 20),
                      child: AspectRatio(
                          aspectRatio: 320/180,
                            child: makeSwiper()
                        ),
                    ),
                    progressBar,
                    Container(
                      child: Column(
                        children: <Widget>[
                          Hero(
                            tag: "trackName",
                            child: Text(
                              utf8.decode(utf8.encode(l.elementAt(index)["title"])),
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 25,
                                fontWeight: FontWeight.bold,
                                color: dark?Colors.white:Colors.black
                              ),
                            ),
                          ),
                          Text(
                            l.elementAt(index)["author"],
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 17.6,
                              color: dark?Colors.white70:Colors.black87
                            ),
                          )
                        ],
                      ),
                    ),
                    Container(
                      child: buttonBar),
                    Container(
                      decoration:BoxDecoration(
                        
                        borderRadius: BorderRadius.only(
                          topLeft: Radius.elliptical(250, 30),
                          topRight: Radius.elliptical(250, 30)
                        ),
                        color: Colors.black38
                      
                      ),
                      padding: EdgeInsets.all(0),
                      child: Stack(
                        
                        alignment: Alignment.topCenter,
                        children: <Widget>[
                          Positioned(
                            
                            bottom: 20,
                            child: IconButton(
                              icon: Icon(Icons.arrow_drop_up),
                              color: Colors.white54,
                              onPressed: (){},
                            ),
                          ),
                          Container(
                            
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.center,
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: <Widget>[
                                IconButton(
                                  color: Colors.white54,
                                  alignment: Alignment.bottomCenter,
                                  icon: Icon(Icons.close),
                                  onPressed: (){
                                    Navigator.pop(context);
                                  },
                                ),
                                Expanded(
                                  child: InkWell(
                                    onTap: () async {
                                      List l = main.bCast["queue"];
                                      if(l==null || l.isEmpty){
                                        showDialog(
                                          context: context,
                                          builder: (BuildContext context){
                                            return AlertDialog(
                                              title:Text("Empty Queue"),
                                              content:Text(
                                                "Queue is empty, play/search a music before opening queue"
                                              ),
                                              actions:[
                                                FlatButton(
                                                  onPressed: (){
                                                    Navigator.pop(context);

                                                  },
                                                  child: Text("OK"),
                                                )
                                              ]
                                            );
                                          }
                                        );
                                        return;
                                      }
                                      else{
                                        loopTimer.cancel();
                                        await Navigator.push(context, MaterialPageRoute(
                                        builder:(context)=>QueueScreen(l,index),
                                        maintainState: true));
                                        
                                        dont=true;
                                        index=main.bCast["currentIndex"];
                                        sc.move(index);

                                        loopTimer=Timer.periodic(Duration(seconds: 1), handleRefresh);
                                      }
                                    },
                                    child: Text(
                                      next,
                                      maxLines: 1,
                                      textAlign: TextAlign.center,
                                      style: TextStyle(
                                        fontSize: 18,
                                        color: Colors.white54,
                                        
                                      ),
                                    ),
                                  ),
                                ),
                                StatefulBuilder(
                                  builder: (BuildContext context, StateSetter setIconState){
                                    update(){
                                      platform.invokeMethod("getTracksinPlaylist",{"playlistId":1}).then(
                                        (favlist){
                                          favoritetracks=favlist;
                                          setIconState((){});
                                        }
                                      );
                                    }
                                    update();
                                    bool fav=false;
                                    for(Map x in favoritetracks){
                                      if(x["title"]==l[index]["title"]){
                                        fav=true;
                                        break;
                                      }
                                    }
                                    return IconButton(
                                      icon: fav?Icon(Icons.favorite):Icon(Icons.favorite_border),
                                      color: Colors.white54,
                                      onPressed: (){
                                        if(fav){
                                          invokeOnPlatform("removeTrackfromPlaylist", {
                                            "track":l[index]["title"],
                                            "playlistId":1
                                          });
                                          update();
                                        } else{
                                          invokeOnPlatform("addTracktoPlaylist", {
                                            "track":l[index]["title"],
                                            "playlistId":1
                                          });
                                          update();
                                        }
                                      },
                                    );
                                  },
                                  
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        )
    );
  }
  String findThumbnailURL(int index,String quality){
    if(_data.isEmpty){
      return "https://via.placeholder.com/150";
    }
    List items = _data["queue"];
    Map item = items.elementAt(index);
    List thumbnails = item["videoThumbnails"];
    for(int i=0;i<thumbnails.length;i++){
      if(thumbnails.elementAt(i)["quality"]==quality){
        return thumbnails.elementAt(i)["url"];
      }
    }
    return null;
  }
  
}
Future<ui.Image> getImagefromProvider(ImageProvider imageProvider) async{
  Duration timeout = Duration(seconds: 10);
  final ImageStream stream = imageProvider.resolve(
      ImageConfiguration( devicePixelRatio: 1.0),
    );
    final Completer<ui.Image> imageCompleter = Completer<ui.Image>();
    Timer loadFailureTimeout;
    void imageListener(ImageInfo info, bool synchronousCall) {
      loadFailureTimeout?.cancel();
      stream.removeListener(imageListener);
      imageCompleter.complete(info.image);
    }

    if (timeout != Duration.zero) {
      loadFailureTimeout = Timer(timeout, () {
        stream.removeListener(imageListener);
        imageCompleter.completeError(
          TimeoutException(
              'Timeout occurred trying to load from $imageProvider'),
        );
      });
    }
    stream.addListener(imageListener);
    return await imageCompleter.future;
}
Future<Color> generatePalette( registry)async{
  
  List l=await registry.lookup(tags:["provider"]);
  print("ELEMENTS FOUND IN REGISTRY $l");
  
  return null;

}

class ProgressBar extends StatefulWidget{
  
  ProgressBar({Key key}) : super(key:key);
  final ProgressBarState _state = ProgressBarState(); 
  @override
  State<StatefulWidget> createState() {
    return _state;
  }
  void updateProgress(Map _data){
    _state.progressUpdate(_data);
  }
  
  
}
class ProgressBarState extends State<ProgressBar>{
  
  double value = 0;
  Map _data=Map();
  Timer t;

  void progressUpdate(Map _datanew){
    if(_datanew!=_data){
      setState(() {
              _data=_datanew;
            });
    }
    
    int _totalTrackLength = _data["totaltime"];
    int _currentPlayingTime = _data["currentplayingtime"];
    setState(() {
              value=_totalTrackLength!=0?_currentPlayingTime/_totalTrackLength:0;
        });
  }
  @override
  Widget build(BuildContext context) {
    
    return Container(
      padding: EdgeInsets.all(8),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        color: Color.fromRGBO(1, 1, 1, 0.2)
      ),
      margin: EdgeInsets.all(8),
      
      child: Row(
              children: <Widget>[
                Text(
                  _data.isEmpty?"0:0":formattime(_data["currentplayingtime"]),
                  style: TextStyle(
                    color: Colors.white54
                  ),),
                Expanded(
                  child: CupertinoSlider(
                    value: value,
                    activeColor: Theme.of(context).accentColor,
                    onChanged: (double pos){
                      if(t!=null){
                        t.cancel();
                      }
                      t=Timer(Duration(milliseconds: 400), (){

                        platform.invokeMethod("seekTo",{"msec":pos*_data["totaltime"]});
                      });
                      setState(() {
                                    _data["currentplayingtime"]=(pos*_data["totaltime"]).toInt();
                                      value=pos;
                                    });
                    },
                  ),
                ),
                Text(
                  formattime(_data.isEmpty?0:_data["totaltime"]),
                  style: TextStyle(
                    color: Colors.white54
                  ),)
              ],
      ),
    );
  }
  String formattime( milli){
    Duration duration = Duration(milliseconds: milli);
    int min=duration.inMinutes;
    int secs=duration.inSeconds%60;
    return min.toString()+":"+secs.toString();
  }

}
class ButtonBar extends StatefulWidget{
  
  final ButtonBarState barState;

  ButtonBar(
    Key key,
    int repeatMode,
    bool shuffle,
    AnimationController playpausecontroller,
    bool isPlaying,
    bool dark,
    SwiperController sc,
    Color dominantColor,
    Function toggle
  ):
  barState=ButtonBarState(
    repeatMode,
    shuffle,
    playpausecontroller,
    isPlaying,
    dark,
    sc,
    dominantColor,
    toggle
  ),
  super(key:key);
  
  State<StatefulWidget> createState() {
    return barState;
  }
  
  void update(
    int repeatMode,
    bool shuffle,
    AnimationController playpausecontroller,
    bool isPlaying,
    bool dark,
    SwiperController sc,
    Color dominantColor,
  ){
   barState.update(repeatMode,shuffle, playpausecontroller, isPlaying, dark, sc, dominantColor);
  }

}
class ButtonBarState extends State<ButtonBar>{

  int repeatMode=0;
  bool shuffle=false;
  AnimationController playpausecontroller;
  bool isPlaying;
  bool dark;
  SwiperController sc;
  Color c;
  Function toggle;

  ButtonBarState(
    int rM,
    bool shuffle,
    AnimationController playPause,
    bool isP,
    bool col,
    SwiperController sc,
    Color c,
    Function toggele
  ):repeatMode=rM,
    shuffle=shuffle,
    playpausecontroller=playPause,
    isPlaying=isP,
    sc=sc,
    dark=col,
    c=c,
    toggle=toggele;

  void update(
    int rM,
    bool shuff,
    AnimationController playPause,
    bool isP,
    bool col,
    SwiperController sc,
    Color c,
  ){
    if (
      rM!=repeatMode ||
      shuff!=shuffle ||
      playpausecontroller!= playPause ||
      isPlaying != isP ||
      dark != col ||
      this.sc!=sc ||
      this.c != c
    )
    setState(() {
        repeatMode=rM;
        shuffle=shuff;
        playpausecontroller=playPause;
        isPlaying=isP;
        dark=col;
        this.sc=sc;
        this.c=c;
        });
  }


  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      mainAxisSize: MainAxisSize.max,
      crossAxisAlignment: CrossAxisAlignment.center,
      children: <Widget>[
        IconButton(
          iconSize: 25,
          icon: Icon(
            repeatMode!=2?Icons.repeat:Icons.repeat_one),
          onPressed: (){
            if(repeatMode==2){
              repeatMode=0;
            }
            else{
              repeatMode+=1;
            }
            invokeOnPlatform("toggleRepeatMode",{"mode":repeatMode});
            setState(() {
                          
                        });
          },
          color: repeatMode>0?c:dark?Colors.white:Colors.black,
        ),
        IconButton(
          iconSize: 35,
          icon: Icon(Icons.skip_previous),
          onPressed: (){
            sc.previous();
          },
          color: dark?Colors.white:Colors.black,
        ),
        IconButton(
          iconSize: 65,
          color: Colors.white,
          icon: AnimatedIcon(
            progress: playpausecontroller,
            icon:AnimatedIcons.play_pause,
            textDirection: TextDirection.ltr,
            ),
          onPressed: (){
            if(isPlaying){
              invokeOnPlatform("pause", null);
              toggle(false);
            }
            else {
              invokeOnPlatform("play", null);
              playpausecontroller.forward();
              toggle(true);
            }
          },
        ),
        IconButton(
          iconSize: 35,
          icon: Icon(Icons.skip_next),
          onPressed: (){
            sc.next();
          },
          color: dark?Colors.white:Colors.black,
        ),
        IconButton(
          iconSize: 25,
          icon: Icon(Icons.shuffle),
          onPressed: (){
            if(shuffle){
              shuffle=false;
            }
            else{
              shuffle=true;
            }
            invokeOnPlatform("toggleShuffle",{"shuffle":shuffle});
            setState(() {

            });
          },
          color: !shuffle?Colors.white:Colors.black,
        )
      ],
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