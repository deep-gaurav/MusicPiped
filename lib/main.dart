import 'package:flutter/material.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'package:marquee/marquee.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:music_piped/searchScreen.dart';
import 'package:music_piped/player.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_fab_dialer/flutter_fab_dialer.dart';
import 'dart:ui';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';

import 'home.dart';
import 'tracks.dart';
import 'artists.dart';
import 'playlists.dart';
import 'equaliser.dart';

String privacyPolicy;

void main(){
  //LicenseRegistry.addLicense(getLicenses);
  rootBundle.loadString("Privacy.md").then((onValue){
    privacyPolicy=onValue;
  });
  runApp(MyApp());
}

Stream<LicenseEntry> getLicenses(){
  StreamController<LicenseEntry> sc = StreamController();

  rootBundle.loadString("LICENSE").then(
    (privacyString){
      sc.add(
        LicenseEntryWithLineBreaks(["musicpiped"], privacyString)
      );
      sc.close();
    }
  );
  return sc.stream;
}

List<String> invidiosInstances = [
  "https://invidio.us/",
  "https://invidious.snopyta.org/",
  "https://vid.wxzm.sx/",
  "https://invidious.kabi.tk/",
  "https://invidiou.sh/",
  "https://invidious.enkirton.net/",
  "https://tube.poal.co/"
];

Map bCast;
bool pendingUpdate=false;
SharedPreferences preferences;
StreamController mainStreamController = StreamController.broadcast(); 

class MyApp extends StatefulWidget {
  const MyApp({Key key}) : super(key: key);

  @override
  _AppState createState() => new _AppState();
}

class _AppState extends State<MyApp> {
  Color _primaryColor = Colors.blue;
  Brightness _brightness = Brightness.light;

  static _AppState of(BuildContext context) =>
      context.ancestorStateOfType(const TypeMatcher<_AppState>());

  /// Sets the primary color of the example app.
  void setPrimaryColor(MaterialColor swatch,Brightness brigth) {
    setState(() {
      _primaryColor = swatch!=null?swatch:_primaryColor;
      _brightness = brigth;
    });
  }
  _AppState(){
    
    SharedPreferences.getInstance().then((val){
      preferences=val;
      setState(() {
            try{
              _brightness=preferences.getBool("dark")?Brightness.dark:Brightness.light;

            } catch(e){

            }
            });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MusicPiped',
      theme: ThemeData(
        primarySwatch: _primaryColor,
        brightness: _brightness,
      ),
      home: MyHomePage(
        key: Key("musicPiped"),
        title: 'MusicPiped'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage>
     {
  String trackName = "NO track";
  String thumbnailURL = "";
  static const platform =
      const MethodChannel("me.devsilver.musicpiped/PlayerChannel");
  static const JSONmessagechannel = const BasicMessageChannel(
      "me.devsilver.musicpiped/PlayerChannelJSON", JSONMessageCodec());
  int playingStatus = 0;
  int _currentTrackIndex = 0;
  int _currentPlayingTime = 0, _totalTrackLength = 0;

  List topTrack;

  //ELEMENTS
  Home home;
  Tracks tracks;
  Artists artists;
  Playlists playlists;

  Future topTrackfuture;
  Future topArtistfuture;
  Future allTrackfuture;
  Future playlistfuture;

  AnimationController animationController;

  BottomNavigationBar bottomNavigationBar;

  double dragStartPos = 0;
  double dragNewPos = 0;
  bool fullPlayer = false;

  List<Color> navColors = [Colors.blue, Colors.pink, Colors.teal, Colors.grey];

  final _scaffoldKey = LabeledGlobalKey<ScaffoldState>("scaffold");

  Map<String, dynamic> currentdata;
  List<dynamic> playingTrackQueue;

  String playingTrack;

  Widget header;

  int _currentBottomNavPage = 0;

  StreamController _streamController;

  StreamController get loadingStreamController => _streamController;

  set loadingStreamController(StreamController streamController) {
    _streamController = streamController;
  }
  bool shown=false;
  BuildContext popupcontext;
  bool tried=false;

  _MyHomePageState() {
    platform.setMethodCallHandler(methodHandler);
  }

  Future<void> updateQueue(List queue) async {
    try {
      print("TEST PRING IN DART $queue");
      print("\n");
      await platform.invokeMethod('updateQueue', {'queue': queue});
    } on PlatformException catch (e) {
      print(e);
    }
  }

  Future<void> addtoExistingQueue(List toadd) async {
    try {
      await platform.invokeMethod('addtoQueue', {'queue': toadd});
    } on PlatformException catch (e) {
      print(e);
    }
  }

  Future<dynamic> methodHandler(MethodCall methodCall) async {
    switch (methodCall.method) {
      case "statusUpdate":
        Map b = methodCall.arguments;
        if (b["queueUpdate"] == false) {
          b["queue"] = bCast["queue"];
        } else {
          pendingUpdate=true;
          header = Container(
            key: Key(playingTrack),
            decoration: BoxDecoration(
              image: DecorationImage(
                  image: CachedNetworkImageProvider(
                    getThumbnaillink(b["queue"], b["currentIndex"],
                        "videoThumbnails", "medium", "quality"),
                  ),
                  fit: BoxFit.cover),
            ),
            child: Container(
              color: Colors.black38,
            ),
          );
          refresh();
        }
        bCast = b;
        int c = b["currentIndex"];
        List l = b["queue"];
        int totalTrackLength = b["totaltime"];
        int currentPlayingTime = b["currentplayingtime"];

        setState(() {
          playingTrackQueue = l;
          _currentTrackIndex = c;
          _totalTrackLength = totalTrackLength;
          _currentPlayingTime = currentPlayingTime;
        });

        mainStreamController.add({"newB":bCast});
        break;
      case "loading":
        bool loading = methodCall.arguments;
        if(loading){
          setState(() {
            loadingStreamController=StreamController.broadcast();
          });

        }else{
	        Navigator.pop(popupcontext);
	        refresh();

        }

    }
  }

  void refresh() {
    topTrackfuture = platform.invokeMethod("requestTopTracks");
    topArtistfuture = platform.invokeMethod("requestArtists");
    allTrackfuture = platform.invokeMethod("requestAllTracks");
    playlistfuture = platform.invokeMethod("getPlaylists");

    setState(() {
      tracks = Tracks(allTrackfuture, refresh);
      artists = Artists(topArtistfuture);
      home = Home(((result) {
        if (result['addtoexisting'] == false) {
          setState(() {
            playingTrackQueue = result['queue'];
          });
          updateQueue(result['queue']);
        } else {
          addtoExistingQueue(result['queue']);
        }
      }), topTrackfuture, topArtistfuture);

      playlists = Playlists(playlistfuture,refresh);
    });
  }

  @override
  void initState() {
    topTrackfuture = platform.invokeMethod("requestTopTracks");
    topArtistfuture = platform.invokeMethod("requestArtists");
    allTrackfuture = platform.invokeMethod("requestAllTracks");
    playlistfuture = platform.invokeMethod("getPlaylists");




    home = Home((result) {
      if (result['addtoexisting'] == false) {
        setState(() {
          playingTrackQueue = result['queue'];
        });
        updateQueue(result['queue']);
      } else {
        addtoExistingQueue(result['queue']);
      }
    }, topTrackfuture, topArtistfuture);

    tracks = Tracks(allTrackfuture, refresh);
    artists = Artists(topArtistfuture);
    playlists = Playlists(playlistfuture,refresh);

    bottomNavigationBar = createBottomNav();
    //invokeOnPlatform("play", null);
    super.initState();
  }

  BottomNavigationBar createBottomNav() {
    bool dark = preferences.getBool("dark")==null?false:preferences.getBool("dark");
    return BottomNavigationBar(
      currentIndex: _currentBottomNavPage,
      type: dark?BottomNavigationBarType.fixed:BottomNavigationBarType.shifting,
      items: [
        BottomNavigationBarItem(
            icon: Icon(Icons.home),
            title: Text("Home"),
            backgroundColor: navColors[0]),
        BottomNavigationBarItem(
            icon: Icon(Icons.person),
            title: Text("Artists"),
            backgroundColor: navColors[1]),
        BottomNavigationBarItem(
            icon: Icon(Icons.music_note),
            title: Text("Tracks"),
            backgroundColor: navColors[2]),
        BottomNavigationBarItem(
            icon: Icon(Icons.library_music),
            title: Text("Playlists"),
            backgroundColor: navColors[3]),
      ],
      onTap: (index) {
        setState(() {
          _currentBottomNavPage = index;
          bottomNavigationBar = createBottomNav();
          _AppState.of(context).setPrimaryColor(navColors[index],Theme.of(context).brightness);
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    
    double value;
    if (playingTrackQueue == null) {
      playingTrack = "NOT PLAYING";

      value = null;
    } else {
      playingTrack = playingTrackQueue.elementAt(_currentTrackIndex)["title"];

      value = _totalTrackLength != 0
          ? _currentPlayingTime / _totalTrackLength
          : null;
    }
    Widget currentScreen;
    if (_currentBottomNavPage == 0) {
      currentScreen = home;
    } else if (_currentBottomNavPage == 1) {
      currentScreen = artists;
    } else if (_currentBottomNavPage == 2) {
      currentScreen = tracks;
    } else if (_currentBottomNavPage == 3) {
      currentScreen = playlists;
    }

    //LOADING
    if(loadingStreamController!=null  && !shown){
      shown=true;
      showDialog(context: context,
        barrierDismissible: false,
        builder: (dialogContext){
          popupcontext=dialogContext;
          return AlertDialog(
            title: Text("Upgrading.. Please Wait"),
            content: StreamBuilder(
              stream: loadingStreamController.stream,
              builder: (context, ass){
                if(ass.connectionState==ConnectionState.done){
                  Navigator.pop(dialogContext);
                  return RaisedButton(
                    child: Text("Close"),
                    onPressed: (){
                      Navigator.pop(dialogContext);
                    },
                  );

                } else{
                  return CircularProgressIndicator();
                }
              },
            ),
          ) ;
        }
      );
    }

    return WillPopScope(
      onWillPop: () {
        if (fullPlayer) {
          setState(() {
            fullPlayer = false;
            dragNewPos = 0;
          });
        } else {
          return Future(() {
            return true;
          });
        }
      },
      child: Scaffold(
        key: _scaffoldKey,
        body: CustomScrollView(
          physics: BouncingScrollPhysics(),
          slivers: <Widget>[
            SliverAppBar(
              centerTitle: true,
              automaticallyImplyLeading: true,
              pinned: true,
              expandedHeight: 150,
              elevation: 7,
              flexibleSpace: FlexibleSpaceBar(
                  title: Text("MusicPiped"),
                  background: header),
              actions: <Widget>[
                IconButton(
                  icon: Icon(Icons.settings),
                  onPressed: (){
                    if(Navigator.canPop(context)){
                      Navigator.pop(context);
                    } else{
                      _scaffoldKey.currentState.showBottomSheet(
                        (BuildContext context){
                          return StatefulBuilder(
                            builder: (BuildContext bdtx,StateSetter setBottomSheetState){
                              return Container(
                                padding: EdgeInsets.all(8),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.only(
                                    topLeft: Radius.circular(20),
                                    topRight: Radius.circular(20)
                                  ),
                                  color: Theme.of(context).canvasColor,
                                  boxShadow: [
                                    BoxShadow(
                                      color: Colors.black38,
                                      offset: Offset(0, -1),
                                      spreadRadius: 5
                                    )
                                  ]
                                ),
                                
                                child: Column(
                                  mainAxisSize: MainAxisSize.min,
                                  crossAxisAlignment: CrossAxisAlignment.stretch,                          
                                    children: <Widget>[
                                      Text(
                                        
                                        "Settings",
                                        style:TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 25
                                        )
                                      ),
                                      Padding(
                                        padding: EdgeInsets.all(8),
                                      ),
                                      SwitchListTile(
                                        value: preferences.getBool("dark")==null?false:preferences.getBool("dark"),
                                        title: Text("Dark Mode"),
                                        onChanged: (mode) async {
                                          await preferences.setBool("dark", mode);
                                          _AppState.of(context).setPrimaryColor(null, mode?Brightness.dark:Brightness.light);
                                          
                                          setState(() { 
                                            bottomNavigationBar=createBottomNav();
                                           });
                                          setBottomSheetState(() { });
                                        },
                                      ),
                                      ListTile(
                                        title: Text("Equaliser"),
                                        onTap: (){
                                          Navigator.of(context).push(MaterialPageRoute(builder: (context)=>Equalizer()));
                                        },
                                      ),
                                      StatefulBuilder(
                                        builder: (btcx,setst){
                                          List<DropdownMenuItem> items = new List();
                                          for(String i in invidiosInstances){
                                            items.add(DropdownMenuItem(
                                              value: i,
                                              child:Text(i)
                                            ));
                                          }
                                          int i =0;
                                          try{
                                            i=preferences.getInt("invidiousinstance");
                                          }catch (e){

                                          }
                                          if(i==null || i<0){
                                            i=0;
                                          }
                                          return ListTile(
                                            leading: Text("Invidious Instance"),
                                            title: SingleChildScrollView(
                                              scrollDirection: Axis.horizontal,
                                              child: DropdownButton(
                                                value: items[i].value,
                                                items: items,
                                                onChanged: (i){
                                                  Scaffold.of(btcx).showSnackBar(
                                                    SnackBar(
                                                      content: Text(
                                                        "Invidios Instance set to "+i.toString()
                                                      ),
                                                    )
                                                  );
                                                  print("new invidious instance : $i ,"+invidiosInstances.indexOf(i).toString());
                                                  preferences.setInt("invidiousinstance", invidiosInstances.indexOf(i)).then(
                                                    (b){
                                                      setst((){});
                                                    }
                                                  );
                                                  
                                                },
                                              ),
                                            ),
                                          );
                                        },
                                      ),
                                      ListTile(
                                        title: Text("Privacy Policy"),
                                        onTap: (){
                                          showDialog(
                                            context: context,
                                            builder: (btcx){

                                              return Dialog(
                                                child:Markdown(
                                                  data: privacyPolicy,
                                                )
                                              );
                                            }
                                          );
                                        },
                                      ),
                                      ListTile(
                                        title: Text("Licences"),
                                        onTap: (){
                                          showLicensePage(
                                            context: context
                                          );
                                        },
                                      )
                                      
                                    ],
                                  
                                ),
                              );
                            }
                            
                          );
                        }
                      );
                    }
                  },
                )
              ],
            ),


            currentScreen,
          ],
        ),
        bottomNavigationBar: Column(
          mainAxisAlignment: MainAxisAlignment.end,
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            BottomAppBar(
                elevation: 12,
                shape: CircularNotchedRectangle(),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: <Widget>[
                    LinearProgressIndicator(
                      value: value,
                    ),
                    Flexible(
                      child: Row(
                        mainAxisSize: MainAxisSize.max,
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: <Widget>[
                          IconButton(
                            icon: Icon(Icons.arrow_upward),
                            onPressed: () {
                              setState(() async{
                                fullPlayer = true;
                                await Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (context) =>
                                            PlayerScreen(Key("player"))));
                                    pendingUpdate=true;
                              });
                            },
                          ),
                          Expanded(
                            child: Container(
                              alignment: Alignment.bottomCenter,
                              padding: EdgeInsets.all(10),
                              height: 50,
                              child: Marquee(
                                key: Key(playingTrack),
                                text: playingTrack,
                                blankSpace: 100,
                                style: TextStyle(
                                  fontSize: 20,
                                  fontWeight: FontWeight.normal,
                                ),
                                pauseAfterRound:
                                    Duration(seconds: 2, milliseconds: 500),
                              ),
                            ),
                          ),
                          IconButton(
                            icon: bCast!=null && bCast["isplaying"]!=null && bCast["isplaying"]?Icon(Icons.pause):  Icon(Icons.play_arrow),
                            onPressed: () {
                              if(bCast!=null && bCast["isplaying"] && bCast["isplaying"]){
                                platform.invokeMethod("pause",null);
                              } else{
                                platform.invokeMethod("play",null);
                              }
                            },
                          )
                        ],
                      ),
                    ),
                  ],
                )),
            bottomNavigationBar,
          ],
        ),
        floatingActionButton: _currentBottomNavPage==3?FabDialer(
          
          [
            FabMiniMenuItem.withText(
              Icon(Icons.add), navColors[_currentBottomNavPage],5, "Add Playlist",() async{
                await showDialog(
                  context: context,
                  builder: (BuildContext bdctx){
                    String name="";
                    return AlertDialog(
                      title: Text("Create new playlist"),
                      content: TextField(
                        decoration: InputDecoration(
                          labelText: "Playlist name"
                        ),
                        onChanged: (text){
                          name=text;
                        },
                      ),
                      actions: <Widget>[

                        FlatButton(
                          child: Text("Cancel"),
                          onPressed: (){
                            Navigator.pop(bdctx,playlistfuture);
                          },
                        ),
                        FlatButton(
                          child: Text("Add"),
                          onPressed: () async {
                            if(name.isNotEmpty){
                              invokeOnPlatform("addPlaylist", {"name":name});
                              playlistfuture=platform.invokeMethod("getPlaylists");
                              Navigator.pop(bdctx,null);
                            }
                          },
                        ),
                      ],
                    );
                  }
                );
                setState(() {
                  playlists = Playlists(playlistfuture,refresh);
                });
              } ,"New Playlist" , navColors[_currentBottomNavPage], Colors.black, true),
            FabMiniMenuItem.withText(
              Icon(Icons.import_export), navColors[_currentBottomNavPage],5, "Import Playlist",() async{

              final streamController = StreamController<double>.broadcast();

              await showDialog(
                  context: context,
                  barrierDismissible: false,
                  builder: (BuildContext dialogcontext){
                    String youtubeplaylisturl="";
                    return AlertDialog(
                      title: Text("Import Playlist"),
                      
                      content: StreamBuilder(
                        key: Key("importer"),
                        stream: streamController.stream,
                        builder: (context,ass){
                          if(ass.connectionState==ConnectionState.waiting){
                            if(ass.data==null){
                              return TextField(
                                decoration: InputDecoration(
                                    labelText: "Youtube playlist URL"
                                ),
                                onChanged: (text){
                                  youtubeplaylisturl=text;
                                },
                              );
                            } else{
                              return CircularProgressIndicator(
                                value: ass.data,
                              );
                            }

                          }else if(ass.connectionState==ConnectionState.done){

                            Navigator.pop(dialogcontext);
                            return RaisedButton(
                              child: Text("Closed"),
                              onPressed: (){
                                Navigator.pop(dialogcontext);
                              },
                            );
                          } else if (ass.connectionState==ConnectionState.active){
                            return Center(
                              heightFactor: 3,
                              child: Text(
                                (ass.data*100).toString()+"%",
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 20
                                ),
                              ),
                            );
                          }
                        },
                      ),
                      actions: <Widget>[
                        FlatButton(
                          child: Text(
                              "Cancel"
                          ),
                          onPressed: (){
                            Navigator.pop(dialogcontext);
                          },
                        ),
                        FlatButton(
                          child: Text(
                              "Import"
                          ),
                          onPressed: ()async{
                            String pid = youtubeplaylisturl.split("list=")[1];
                            String invidiosApi = invidiosInstances[0];
                            try{
                              invidiosApi=invidiosInstances[preferences.getInt("invidiousinstance")];
                            }catch (e){

                            }
                            String url = invidiosApi+"api/v1/playlists/"+pid;

                            Future work() async{
                              streamController.add(0);
                              var response = await http.get(url);
                              if(response.statusCode==200){
                                var result = json.decode(utf8.decode(response.bodyBytes));
                                List vids = result["videos"];
                                List playlist = new List();
                                for(int i=0;i<vids.length;i++){
                                  Map resultitem=vids[i];
                                  String vidId = resultitem["videoId"];
                                  var newresponse = await http.get(invidiosApi+"api/v1/videos/"+vidId);
                                  if(newresponse.statusCode==200){
                                    playlist.add(json.decode(utf8.decode(newresponse.bodyBytes)));
                                    print("Imported $i");
                                    streamController.add((i+1).toDouble()/vids.length);
                                  }
                                }
                                platform.invokeMethod("importPlaylist",{"playlistname":result["title"],"playlist":playlist});
                                
                                streamController.close();
                                playlistfuture = platform.invokeMethod("getPlaylists");
                                

                              }
                            }

                            work().then((val){
                            });
                          },
                        )
                      ],
                    );
                  },

                );

                setState(() {
                  playlists = Playlists(playlistfuture,refresh);
                });
            } ,"Import Playlist" , navColors[_currentBottomNavPage], Colors.black, true),
          ], navColors[_currentBottomNavPage], Icon(Icons.more_horiz)):null,
      ),
    );
  }

  void onExitPlayer() {
    if (fullPlayer) {
      fullPlayer = false;
      dragNewPos = 0;
    }
  }
}
