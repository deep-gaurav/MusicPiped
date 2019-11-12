import 'dart:async';
import 'dart:math' as math;
import 'dart:typed_data';
import 'dart:ui';
// import 'package:audio_service/audio_service.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/services.dart';
import 'package:flutter_exoplayer/audio_notification.dart';
import 'package:flutter_exoplayer/audioplayer.dart';

import 'package:flutter_exoplayer/audioplayer.dart' as ap;

import 'package:musicpiped_pro/queue.dart';
import 'package:package_info/package_info.dart';
import 'package:pedantic/pedantic.dart';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'package:idb_shim/idb.dart';
import 'package:idb_shim/idb_io.dart';
import 'package:path_provider/path_provider.dart';
import 'package:url_launcher/url_launcher.dart';

import 'backgroundControl.dart';

import 'searchScreen.dart';
import 'trending.dart';
import 'home.dart';
import 'trackDetail.dart';
import 'artists.dart';
import 'library.dart';
import 'playerService.dart';
import 'playerScreen.dart';

import 'package:flutter/foundation.dart';

var idbFactory;

const platform = const MethodChannel('deep.musicpiped/urlfix');

Database musicDB;
Database settingDB;
PackageInfo packageInfo;

var brightness = ValueNotifier("dark");
var invidiosAPI = ValueNotifier("https://invidio.us/");
var quality = ValueNotifier("best");

var ignorePositionUpdate = ValueNotifier(false);

var queue = ValueNotifier<List>([]);

GlobalKey<MyHomePageState> mainKey = GlobalKey();

// const mediaControlButtons = {
//   "play": MediaControl(
//       androidIcon: "drawable/ic_play_arrow_black_24dp",
//       action: MediaAction.play,
//       label: "Play"),
//   "pause": MediaControl(
//       androidIcon: "drawable/ic_pause_black_24dp",
//       action: MediaAction.pause,
//       label: "Pause"),
//   "next": MediaControl(
//       androidIcon: "drawable/ic_skip_next_black_24dp",
//       action: MediaAction.skipToNext,
//       label: "Next"),
//   "previous": MediaControl(
//       androidIcon: "drawable/ic_skip_previous_black_24dp",
//       action: MediaAction.skipToPrevious,
//       label: "Previous")
// };

void main() async {
  runApp(MyApp());
}

Future init() async {
  var appDir = await getApplicationDocumentsDirectory();
  packageInfo = await PackageInfo.fromPlatform();

  var dbDir = appDir.path + Platform.pathSeparator + "musicDB";

  idbFactory = getIdbSembastIoFactory(dbDir);

  brightness.addListener(() {
    putSetting('brightness', brightness.value);
  });

  invidiosAPI.addListener(() {
    putSetting('invidiosAPI', invidiosAPI.value);
  });

  quality.addListener(() {
    putSetting('quality', quality.value);
  });

  settingDB = await initSettings();
  musicDB = await idbFactory.open("musicDB", version: 6, onUpgradeNeeded: (e) {
    Database db = e.database;
    if (e.oldVersion < 1) {
      var ob = db.createObjectStore("tracks", keyPath: "videoId");
      ob.createIndex("videoId", "videoId", unique: true);
      ob.createIndex("timesPlayed", "timesPlayed");
      ob.createIndex("lastPlayed", "lastPlayed");
      var ob2 = db.createObjectStore(
        'playlists',
        keyPath: 'title',
      );
      ob2.add({'title': 'Favorites'});
    }
  });

  platform.setMethodCallHandler((method) async {
    if (method.method == "close") {
      print("Removed from Recent EXITING");
      exit(0);
    }
  });
}

Future<Database> initSettings() async {
  Database db =
      await idbFactory.open("settings", version: 1, onUpgradeNeeded: (e) {
    var db = e.database;
    db.createObjectStore("setting", autoIncrement: true);
  });
  var ob = db.transaction("setting", "readwrite").objectStore("setting");
  if ((await ob.getObject('brightness')) == null) {
    ob.put(brightness.value, 'brightness');
  } else {
    brightness.value = await ob.getObject('brightness');
  }
  if ((await ob.getObject('invidiousApi')) == null) {
    ob.put(invidiosAPI.value, 'invidiousApi');
  } else {
    invidiosAPI.value = await ob.getObject("invidiousApi");
  }
  if ((await ob.getObject('quality')) == null) {
    ob.put(quality.value, 'quality');
  } else {
    quality.value = await ob.getObject("quality");
  }
  return db;
}

dynamic getSetting(String key) async {
  var ob = settingDB.transaction("setting", "readonly").objectStore("setting");
  return await ob.getObject(key);
}

dynamic putSetting(String key, value) async {
  var ob = settingDB.transaction("setting", "readwrite").objectStore("setting");
  return await ob.put(value, key);
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return FutureBuilder(
      future: init(),
      builder: (context, ass) {
        if (ass.connectionState == ConnectionState.done) {
          return MaterialApp(
            title: 'MusicPiped Pro',
            theme: ThemeData(
              primarySwatch: Colors.blue,
              brightness: brightness.value == "dark"
                  ? Brightness.dark
                  : Brightness.light,
            ),
            home: MyHomePage(key: mainKey, title: 'MusicPiped'),
            debugShowCheckedModeBanner: false,
          );
        } else {
          return Center(
            child: CircularProgressIndicator(),
          );
        }
      },
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  MyHomePageState createState() => MyHomePageState();
}

enum PlayerState { Loading, Playing, Paused, Stopped, Error }

class MyHomePageState extends State<MyHomePage>
    with SingleTickerProviderStateMixin, WidgetsBindingObserver {
  TextEditingController textEditingController = TextEditingController();

  dynamic howlerId = 0;

  static String InvidiosAPI = invidiosAPI.value + "api/v1/";
  static int PreloadWindow = 25;

  var playerState = ValueNotifier(PlayerState.Stopped);

  var currentIndex = ValueNotifier(0);

  AudioPlayer audioPlayer;

  var totalLength = ValueNotifier(0);

  String state;

  Database db;

  int mediaSessionId = 0;

  String debugString;

  String preloadTrackId;
  Map preloadObject;

  var repeat = ValueNotifier(0);
  var shuffle = ValueNotifier(false);

  ValueNotifier<Map> currentTrack = new ValueNotifier({});

  var positionNotifier = ValueNotifier(0);

  GlobalKey<ScaffoldState> scaffoldKey = GlobalKey();

  static const int ToMobileWidth = 600;

  SearchDelegate _searchDelegate = YoutubeSuggestion();

  Widget emptyWidget = Container(
    width: 0,
    height: 0,
  );

  TabController _tabController;

  @override
  void initState() {
    super.initState();

    WidgetsBinding.instance.addObserver(this);

    db = musicDB;
    _tabController = TabController(length: 3, vsync: this);

    audioPlayer = AudioPlayer();

    _tabController.addListener(() {
      setState(() {});
    });

    // AudioService.connect(); // When UI becomes visible
    // AudioService.start(
    //   // When user clicks button to start playback
    //   backgroundTaskEntrypoint: myBackgroundTask,
    //   androidNotificationChannelName: 'Music Player',
    //   androidNotificationIcon: "mipmap/ic_launcher",
    //   resumeOnClick: false,
    //   androidStopForegroundOnPause: true,
    //   androidNotificationOngoing: false
    // );
    // Timer.periodic(Duration(seconds: 2), (t) {
    //   AudioService.customAction("tick");
    // });

    // queue.addListener((){
    //   for(var x in AudioService.queue){
    //     AudioService.removeQueueItem(x);
    //   }
    //           queue.value.map<MediaItem>((qVal)=>MediaItem(id: qVal["videoId"], album: qVal["genre"], title: qVal["title"],
    //       artist: qVal["author"],
    //       genre: qVal["genre"],
    //       artUri: (qVal["videoThumbnails"] as List).last["url"]
    //     )).forEach((f)=>AudioService.addQueueItem(f));
    // });

    // AudioService.playbackStateStream.listen((state) {
    //   if (ignorePositionUpdate.value) {
    //     return;
    //   }
    //   positionNotifier.value = state.position;
    //   switch (state.basicState) {
    //     case BasicPlaybackState.stopped:
    //       playerState.value = PlayerState.Stopped;
    //       break;
    //     case BasicPlaybackState.playing:
    //       playerState.value = PlayerState.Playing;
    //       break;
    //     case BasicPlaybackState.paused:
    //       playerState.value = PlayerState.Paused;
    //       break;
    //       break;
    //     case BasicPlaybackState.none:
    //       // TODO: Handle this case.
    //       break;
    //     case BasicPlaybackState.fastForwarding:
    //       // TODO: Handle this case.
    //       break;
    //     case BasicPlaybackState.rewinding:
    //       // TODO: Handle this case.
    //       break;
    //     case BasicPlaybackState.buffering:
    //       playerState.value = PlayerState.Loading;
    //       break;
    //     case BasicPlaybackState.error:
    //       playerState.value = PlayerState.Error;
    //       break;
    //     case BasicPlaybackState.connecting:
    //       playerState.value = PlayerState.Loading;
    //       break;
    //     case BasicPlaybackState.skippingToPrevious:
    //       previous();
    //       break;
    //     case BasicPlaybackState.skippingToNext:
    //       next();
    //       break;
    //     case BasicPlaybackState.skippingToQueueItem:
    //       // TODO: Handle this case.
    //       break;
    //   }
    // });
    /*
    player.addEventListener('ended', (e) {
      playerState.value = PlayerState.Stopped;
    });
    */

    audioPlayer.onDurationChanged.listen((Duration d) {
      print('Max duration: $d');
      totalLength.value = d.inSeconds;
    });
    audioPlayer.onAudioPositionChanged.listen((Duration p) {
      print('Current position: $p');
      positionNotifier.value = p.inSeconds;

      if (totalLength.value - positionNotifier.value < PreloadWindow &&
          currentIndex.value < queue.value.length - 1 &&
          preloadTrackId != queue.value[currentIndex.value + 1]["videoId"]) {
        preload();
      }
    });

    audioPlayer.onAudioSessionIdChange.listen((session) {
      mediaSessionId = session;
    });

    audioPlayer.onPlayerStateChanged.listen((state) {
      if (state == ap.PlayerState.PLAYING) {
        playerState.value = PlayerState.Playing;
      } else if (state == ap.PlayerState.PAUSED) {
        playerState.value = PlayerState.Paused;
      } else if (state == ap.PlayerState.BUFFERING) {
        playerState.value = PlayerState.Loading;
      } else if (state == ap.PlayerState.COMPLETED) {
        next();
      }
    });
    audioPlayer.onPlayerCompletion.listen((complete) {
      next();
    });
    audioPlayer.onNotificationActionCallback.listen((event) {
      if (event == NotificationActionName.NEXT) {
        next();
      } else if (event == NotificationActionName.PREVIOUS) {
        previous();
      } else if (event == NotificationActionName.PAUSE) {
        audioPlayer.pause();
        playerState.value = PlayerState.Paused;
      } else if (event == NotificationActionName.PLAY) {
        audioPlayer.resume();
        playerState.value = PlayerState.Playing;
      }
    });

    playerState.addListener(() {
      if (playerState.value == PlayerState.Stopped) {
        onEnd();
      } else if (playerState.value == PlayerState.Error) {
        scaffoldKey.currentState.removeCurrentSnackBar();
        scaffoldKey.currentState.showSnackBar(SnackBar(
          content: Text("This track can't be played"),
        ));
        onEnd();
      }
    });
  }

  Future<bool> checkCache(Map s) async {
    return false;
  }

  Future<Map> refreshLink(String vidId, Map s) async {
    if (!s.containsKey("authorThumbnails")) {
      var response = await http.get(InvidiosAPI + "videos/" + s["videoId"]);
      s = json.decode(utf8.decode(response.bodyBytes));
    }
    return s;
  }

  Future<Map> fetchVid(Map s) async {
    s = await refreshLink(s["videoId"], s);

    return s;
  }

  // Future<ByteData> imagefromURL(String url) {
  //   var provider = CachedNetworkImageProvider(url);
  //   var stream = provider.resolve(ImageConfiguration());
  //   var completer = Completer<ByteData>();
  //   stream.addListener(ImageStreamListener((info, callflag) {
  //     completer.complete(info.image.toByteData(format: ImageByteFormat.png));
  //   }));
  //   return completer.future;
  // }

  Future<String> fixURLAccess(Map s) async {
    List formats = List.from(s["adaptiveFormats"]);
    formats.sort((t1, t2) {
      return t1["bitrate"].compareTo(t2["bitrate"]);
    });

    for (Map f in quality.value == "best" ? formats.reversed : formats) {
      String type = f["type"];
      if (type.contains("audio")) {
        String url = f["url"];
        return url;
      }
    }
    return "";
  }

  void next() {
    onEnd();
  }

  void previous() {
    if (currentIndex.value > 0) {
      currentIndex.value -= 1;
      playCurrent();
    } else {
      playCurrent();
    }
  }

  void updateMetadata() async {
//    player.pause();
//    playerState.value = PlayerState.Loading;
//    Map s = queue.value[currentIndex.value];
//    var image;
//    try {
//      image = await imagefromURL(
//          TrackTile.urlfromImage(s["videoThumbnails"], "medium"));
//    } catch (e) {
//      image = null;
//    }
//    var barrray = image.buffer.asUint8List();
//    player.metadata = {
//      "title": s["title"],
//      "artist": s["author"],
//      "thumb": barrray,
//      "vidId": s["videoId"]
//    };
    // player.updateMetadata();
    // TODO metadataupdate
  }

  Future<Map> load(Map s) async {
    bool cached = false;
    if (await checkCache(s)) {
      cached = true;
      print("cached, playing offline");
    } else {
      cached = false;
      print("not cached, refreshing");
      s = await fetchVid(s);
    }
    var url = "https://dummyurl.com?a=2&videoId=" + s["videoId"];
    if (!cached) {
      try {
        url = await fixURLAccess(s);
      } catch (e) {
        print("Doesnt have url");
      }
    }
//
//    player.pause();
//    player.currentTime = 0;

    //howlerId = howler.callMethod("play");
    var image;
    try {
      image = TrackTile.urlfromImage(s["videoThumbnails"], "medium");
    } catch (e) {
      image = null;
    }
    totalLength.value = s["lengthSeconds"];

    // AudioService.customAction("setMetadata", s);

    print("getNewPipe URL for vid ${s['videoId']}");
    url += "&videoId=${s['videoId']}";
    url = await platform.invokeMethod("getURL", {"url": url});

    print("url received");
    print(url);
    // print("Received Newpipe URL $url");

    // AudioService.playFromMediaId(url);
    // MediaFile mediaFile = MediaFile(
    //     title: s["title"],
    //     type: "audio",
    //     source: url,
    //     desc: s["author"],
    //     image: TrackTile.urlfromImage(s["videoThumbnails"], "medium"));

    return {"image": image, "url": url};
  }

  Future<void> loadCurrent() async {
    Map s = queue.value[currentIndex.value];

    audioPlayer.stop();

    var loadval;
    if (s["videoId"] == preloadTrackId && preloadObject != null) {
      print("using preload $preloadObject");
      loadval = preloadObject;
    } else {
      print("Not Preloaded Loading");
      loadval = await load(s);
    }

    if (queue.value[currentIndex.value]["videoId"] != s["videoId"] ||
        playerState.value != PlayerState.Loading) {
      return;
    }
    audioPlayer.play(loadval["url"],
        respectAudioFocus: true,
        playerMode: PlayerMode.FOREGROUND,
        audioNotification: AudioNotification(
            smallIconFileName: "@mipmap/ic_launcher",
            largeIconUrl: loadval["image"],
            title: s["title"],
            subTitle: s["author"],
            notificationDefaultActions: NotificationDefaultActions.ALL,
            notificationActionCallbackMode:
                NotificationActionCallbackMode.CUSTOM));

    // await mediaPlayer.initialize();
    // await mediaPlayer.setSource(mediaFile);
    // mediaPlayer.play();

    s = Map.from(s);
    if (s.containsKey('timesPlayed')) {
      s['timesPlayed'] += 1;
    } else {
      s['timesPlayed'] = 1;
    }
    s['lastPlayed'] = DateTime.now().millisecondsSinceEpoch;
    String id = await db
        .transaction("tracks", "readwrite")
        .objectStore("tracks")
        .put(s);
    print("added to db $id");
    queue.value[currentIndex.value] = s;
  }

  Future preload() async {
    preloadTrackId = queue.value[currentIndex.value + 1]["videoId"];
    preloadObject = null;

    var val = await load(queue.value[currentIndex.value + 1]);
    preloadObject = val;

    var title = queue.value[currentIndex.value + 1]["title"];

    var httpclient = HttpClient();
    var req = await httpclient.getUrl(Uri.parse(val["url"]));
    try {
      var res = await req.close();
      res.last.then((last) {
        print("stream for vid $title ended");
      }).catchError((e) => print(e));
    } catch (e) {
      print("Stream down Error $e");
    }
  }

  void playCurrent() {
    audioPlayer.stop();
    playerState.value = PlayerState.Loading;
    updateMetadata();
    loadCurrent().catchError((e) {
      loadCurrent();
    });
  }

  void playNext(track) {
    if (queue.value != null && queue.value.isNotEmpty) {
      queue.value.insert(currentIndex.value + 1, track);
    } else {
      queue.value = [track];
      currentIndex.value = 0;
      playCurrent();
    }
  }

  void addtoQueue(track) {
    if (queue.value != null && queue.value.isNotEmpty) {
      queue.value.add(track);
    } else {
      queue.value = [track];
      playCurrent();
    }
  }

  /*
  void setMediaSession() {
    Map s = queue.value[currentIndex];
    try {
      var metadata = MediaMetadata({
        "title": s["title"],
        "artist": s["author"],
      });
      try {
        metadata.artwork = [
          {"src": TrackTile.urlfromImage(s["videoThumbnails"], "medium")}
        ];
      } catch (e) {
        print(e);
      }
      print(window.navigator);
      print(window.navigator.mediaSession);
      try {
        if (window.navigator.mediaSession.metadata.title
                .compareTo(s["title"]) ==
            0) {
          return;
        }
      } catch (e) {
        print(e);
      }

      window.navigator.mediaSession.metadata = metadata;

      window.navigator.mediaSession.setActionHandler("play", () {
        player.play();
      });
      window.navigator.mediaSession.setActionHandler("pause", () {
        player.pause();
        playerState.value = PlayerState.Paused;
      });
      window.navigator.mediaSession.setActionHandler("nexttrack", () {
        next();
      });
      window.navigator.mediaSession.setActionHandler("previoustrack", () {
        previous();
      });
    } catch (e) {
      debugString = e.toString();
    }
  }
  */

  void exit() async {
    // await mediaPlayer.pause();
    audioPlayer.stop();
    audioPlayer.release();
    SystemNavigator.pop();
  }

  void onEnd() {
    if (repeat.value == 2) {
      //IF repeatSingle
      playCurrent();
    } else {
      if (currentIndex.value == queue.value.length - 1) {
        if (repeat.value == 3) {
          queue.value.add(
              (queue.value[currentIndex.value]["recommendedVideos"] as List)
                  .first);
          currentIndex.value += 1;

          playCurrent();
        } else if (repeat.value == 1) {
          currentIndex.value = 0;
          playCurrent();
        }
      } else {
        if (shuffle.value) {
          currentIndex.value = math.Random().nextInt(queue.value.length);
        } else {
          currentIndex.value += 1;
        }
        playCurrent();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    var titleBar = ValueListenableBuilder(
      valueListenable: currentIndex,
      builder: (context, index, wid) {
        return Expanded(
          child: Column(
            children: <Widget>[
              Container(
                height: Theme.of(context).textTheme.title.fontSize + 10,
                child: Text(
                  queue.value.isEmpty
                      ? ""
                      : queue.value[currentIndex.value]["title"],
                  style: Theme.of(context).textTheme.title,
                  maxLines: 1,
                ),
              ),
              Text(
                queue.value.isEmpty
                    ? ""
                    : queue.value[currentIndex.value]["author"],
                style: Theme.of(context).textTheme.subtitle,
                maxLines: 1,
              )
            ],
          ),
        );
      },
    );

    return Scaffold(
      key: scaffoldKey,
      appBar: AppBar(
        title: Text(widget.title),
        actions: <Widget>[
          StatefulBuilder(builder: (ctx, setstate) {
            return IconButton(
              icon: Icon(Icons.search),
              onPressed: () async {
                Map searchR = await showSearch<Map>(
                    context: context, delegate: _searchDelegate);
                String searchQ = searchR["query"];
                String type = searchR["type"];
                if (searchQ.isNotEmpty) {
                  var result =
                      await Navigator.of(context).push(MaterialPageRoute(
                          builder: (context) => (SearchScreen(
                                searchQ,
                                type,
                                (q) {
                                  queue.value = q;
                                  currentIndex.value = 0;
                                  playCurrent();
                                },
                                playNext,
                              ))));
                  if (result == null) {
                    return;
                  }
                  print(result);
                  queue.value = result["queue"];
                  currentIndex.value = 0;
                  playerState.value = PlayerState.Loading;
                  playCurrent();
                }
              },
            );
          }),
          IconButton(
            icon: Icon(Icons.more_vert),
            onPressed: () {
              showModalBottomSheet(
                context: context,
                builder: (context) => Container(
                  color: Theme.of(context).backgroundColor,
                  child: ListView(
                    children: <Widget>[
                      Padding(
                        padding: EdgeInsets.all(8),
                        child: Text(
                          "Settings",
                          style: Theme.of(context).textTheme.title,
                        ),
                      ),
                      SwitchListTile.adaptive(
                        value: brightness.value == "dark",
                        title: Text("Dark Mode"),
                        onChanged: (val) {
                          if (val) {
                            brightness.value = "dark";
                          } else {
                            brightness.value = "light";
                          }
                          scaffoldKey.currentState.removeCurrentSnackBar();

                          scaffoldKey.currentState.showSnackBar(SnackBar(
                            content: Text("Reload to take effect"),
                          ));
                          Navigator.pop(context);
                        },
                      ),
                      ListTile(
                        title: Text("Quality"),
                        trailing: SingleChildScrollView(
                          scrollDirection: Axis.horizontal,
                          child: DropdownButton(
                            value: quality.value,
                            items: [
                              DropdownMenuItem(
                                value: 'best',
                                child: Text("Best Quality"),
                              ),
                              DropdownMenuItem(
                                value: 'worst',
                                child: Text("Minimize Data"),
                              )
                            ],
                            onChanged: (newquality) {
                              quality.value = newquality;
                              Navigator.pop(context);
                            },
                          ),
                        ),
                      ),
                      ListTile(
                        title: Text("Invidious API"),
                        onTap: () async {
                          var controller = TextEditingController.fromValue(
                              TextEditingValue(text: invidiosAPI.value));
                          await showDialog(
                              context: context,
                              builder: (context) => SimpleDialog(
                                    title: Text("Invidious API"),
                                    children: <Widget>[
                                      TextField(
                                        controller: controller,
                                      ),
                                      ButtonBar(
                                        children: <Widget>[
                                          FlatButton(
                                            child: Text("Cancel"),
                                            onPressed: () {
                                              Navigator.pop(context);
                                            },
                                          ),
                                          RaisedButton(
                                            child: Text("Apply"),
                                            onPressed: () {
                                              invidiosAPI.value =
                                                  controller.text;
                                              Navigator.pop(context);
                                            },
                                          )
                                        ],
                                      )
                                    ],
                                  ));
                          scaffoldKey.currentState.removeCurrentSnackBar();

                          scaffoldKey.currentState.showSnackBar(SnackBar(
                            content: Text("Reload to take effect"),
                          ));
                        },
                      ),
                      ListTile(
                        title: Text("Open System Equalizer"),
                        onTap: () {
                          print("Open Equalizer");
                          // player.openFX();
                          platform.invokeMethod(
                              "openEqualizer", {"sessionid": mediaSessionId});
                        },
                      ),
                      ListTile(
                        title: Text("About"),
                        onTap: () {
                          showAboutDialog(
                              context: context,
                              applicationName: "MusicPiped",
                              applicationIcon: SizedBox(
                                child: Image.asset("logo.png"),
                                width: 50,
                                height: 50,
                              ),
                              applicationLegalese: "Licensed under GPL v3",
                              applicationVersion: packageInfo.version,
                              children: [
                                Text(
                                    "MusicPiped is an Material Designed inspired music player, using NewPipeExtractor and Invidio.us APIs"),
                                Text("Thank You"),
                                InkWell(
                                  child: Container(
                                    padding: EdgeInsets.all(8),
                                    child: Text(
                                      "https://github.com/deep-gaurav/MusicPiped",
                                      style: TextStyle(
                                          fontWeight: FontWeight.bold),
                                    ),
                                  ),
                                  onTap: () async {
                                    var url =
                                        "https://github.com/deep-gaurv/MusicPiped";
                                    if (await canLaunch(url)) {
                                      await launch(url);
                                    } else {
                                      throw 'Could not launch $url';
                                    }
                                  },
                                ),
                                RaisedButton.icon(
                                  icon: Icon(Icons.star),
                                  label: Text("Star on Github"),
                                  onPressed: () async {
                                    var url =
                                        "https://github.com/deep-gaurav/MusicPiped/stargazers";
                                    if (await canLaunch(url)) {
                                      await launch(url);
                                    } else {
                                      throw 'Could not launch $url';
                                    }
                                  },
                                ),
                                RaisedButton.icon(
                                  icon: Icon(Icons.rate_review),
                                  label: Text("Rate/Review on Play Store"),
                                  onPressed: () async {
                                    var url =
                                        "https://play.google.com/store/apps/details?id=deep.ryd.rydplayer&hl=en_US";
                                    if (await canLaunch(url)) {
                                      await launch(url);
                                    } else {
                                      throw 'Could not launch $url';
                                    }
                                  },
                                ),
                                RaisedButton.icon(
                                  icon: Icon(Icons.redeem),
                                  label: Text("Buy me a coffee"),
                                  onPressed: () async {
                                    var url =
                                        "https://www.buymeacoffee.com/deepgaurav";
                                    if (await canLaunch(url)) {
                                      await launch(url);
                                    } else {
                                      throw 'Could not launch $url';
                                    }
                                  },
                                ),
                              ]);
                        },
                      ),
                      ListTile(
                        title: Text("Show License"),
                        onTap: () {
                          showLicensePage(
                              context: context,
                              applicationName: "MusicPiped",
                              applicationIcon: Image.asset("logo.png"),
                              applicationLegalese: "Licensed under GPL v3",
                              applicationVersion: packageInfo.version);
                        },
                      ),
                      ListTile(
                        title: Text("Exit"),
                        onTap: () {
                          exit();
                        },
                      )
                    ],
                  ),
                ),
              );
            },
          )
        ],
        bottom: TabBar(
          controller: _tabController,
          tabs: <Widget>[
            Tab(
              icon: Icon(Icons.home),
              text: "Home",
            ),
            Tab(
              icon: Icon(Icons.person),
              text: "Artists",
            ),
            Tab(
              icon: Icon(Icons.library_music),
              text: "Library",
            )
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: <Widget>[
          Home((trackdata) {
            Navigator.of(context).push(MaterialPageRoute(
                builder: (context) => (TrackDetail(trackdata, (q) {
                      queue.value = q;
                      currentIndex.value = 0;
                      playerState.value = PlayerState.Loading;
                      playCurrent();
                    }))));
          }),
          Artists((q) {
            queue.value = q;
            currentIndex.value = 0;
            playCurrent();
          }, (track) {
            if (queue.value != null && queue.value.isNotEmpty) {
              queue.value.insert(currentIndex.value + 1, track);
            } else {
              queue.value = [track];
              currentIndex.value = 0;
              playCurrent();
            }
          }),
          Library((q) {
            queue.value = q;
            currentIndex.value = 0;
            playCurrent();
          }, (track) {
            if (queue.value != null && queue.value.isNotEmpty) {
              queue.value.insert(currentIndex.value + 1, track);
            } else {
              queue.value = [track];
              currentIndex.value = 0;
              playCurrent();
            }
          }, db),
        ],
      ),
      floatingActionButton: _tabController.index == 2
          ? FloatingActionButton(
              child: Icon(Icons.add),
              onPressed: () async {
                int create = await showDialog(
                    context: context,
                    builder: (context) {
                      return SimpleDialog(
                        title: Text("New Playlist"),
                        children: <Widget>[
                          FlatButton(
                            child: Text("Local Playlist"),
                            onPressed: () {
                              Navigator.pop(context, 1);
                            },
                          ),
                          FlatButton(
                            child: Text("Import from YouTube"),
                            onPressed: () {
                              Navigator.pop(context, 2);
                            },
                          )
                        ],
                      );
                    });
                if (create == 1) {
                  bool reload = await createPlaylist();
                  setState(() {});
                } else if (create == 2) {
                  bool reload = await showDialog(
                      context: context,
                      builder: (context) {
                        var _controller = TextEditingController();
                        return SimpleDialog(
                          title: Text("Import Playlist"),
                          children: <Widget>[
                            TextField(
                              controller: _controller,
                              decoration: InputDecoration(
                                  labelText: "Youtube Playlist URL"),
                            ),
                            ButtonBar(
                              children: <Widget>[
                                FlatButton(
                                  child: Text("Cancel"),
                                  onPressed: () {
                                    Navigator.pop(context, false);
                                  },
                                ),
                                RaisedButton(
                                  child: Text("Import"),
                                  onPressed: () async {
                                    var url = InvidiosAPI +
                                        "playlists/" +
                                        _controller.text.split('list=').last;
                                    var response = await http.get(url);
                                    Map jsresponse = await json.decode(
                                        utf8.decode(response.bodyBytes));
                                    if (jsresponse.containsKey('title')) {
                                      var ob = db
                                          .transaction('playlists', 'readwrite')
                                          .objectStore('playlists');
                                      await ob.add(jsresponse);
                                      Navigator.pop(context, true);
                                    }
                                  },
                                )
                              ],
                            )
                          ],
                        );
                      });
                  setState(() {});
                }
              },
            )
          : null,
      bottomNavigationBar: InkWell(
        onTap: () {
          if (queue.value.isNotEmpty) {
            Navigator.of(context).push(MaterialPageRoute(builder: (context) {
              return PlayerScreen(
                queue: queue,
                currentIndex: currentIndex,
                shuffle: shuffle,
                repeat: repeat,
                currentPlayingTime: positionNotifier,
                totalTime: totalLength,
                playerState: playerState,
                play: () {
                  playerState.value = PlayerState.Playing;
                  audioPlayer.resume();
                },
                pause: () {
                  playerState.value = PlayerState.Paused;
                  audioPlayer.pause();
                },
                next: next,
                previous: previous,
                openQueue: showQueue,
              );
            }));
          }
        },
        child: Container(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              /*
                if (debugString != null && debugString.isNotEmpty && kDebugMode)
                  Text(debugString),
                */
              ValueListenableBuilder(
                valueListenable: positionNotifier,
                builder: (context, int position, child) {
                  return LinearProgressIndicator(
                    value: positionNotifier.value < totalLength.value
                        ? positionNotifier.value / totalLength.value
                        : 0,
                  );
                },
              ),
              MediaQuery.of(context).size.width > ToMobileWidth
                  ? emptyWidget
                  : Row(
                      children: <Widget>[titleBar],
                    ),
              Row(
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      IconButton(
                        icon: Icon(Icons.skip_previous),
                        onPressed: previous,
                      ),
                      ValueListenableBuilder(
                        valueListenable: playerState,
                        builder: (context, state, child) {
                          if (state == PlayerState.Playing ||
                              state == PlayerState.Paused) {
                            return ValueListenableBuilder(
                              valueListenable: playerState,
                              builder: (context, playing, child) {
                                return IconButton(
                                  icon: Icon(
                                      playerState.value == PlayerState.Playing
                                          ? Icons.pause
                                          : Icons.play_arrow),
                                  iconSize: 36,
                                  onPressed: () {
                                    if (playerState.value ==
                                        PlayerState.Playing) {
                                      audioPlayer.pause();
                                      playerState.value = PlayerState.Paused;
                                    } else {
                                      audioPlayer.resume();
                                    }
                                  },
                                );
                              },
                            );
                          } else if (state == PlayerState.Loading) {
                            return CircularProgressIndicator();
                          } else {
                            return Icon(
                              Icons.play_arrow,
                              size: 36,
                              color: Theme.of(context).disabledColor,
                            );
                          }
                        },
                      ),
                      IconButton(
                        icon: Icon(Icons.skip_next),
                        onPressed: next,
                      ),
                      ValueListenableBuilder(
                        valueListenable: positionNotifier,
                        builder: (context, int position, child) {
                          return Text(
                            formatDuration(
                                  Duration(
                                    milliseconds:
                                        (positionNotifier.value * 1000).toInt(),
                                  ),
                                ) +
                                "/" +
                                formatDuration(
                                  Duration(
                                    milliseconds:
                                        (totalLength.value * 1000).toInt(),
                                  ),
                                ),
                          );
                        },
                      ),
                    ],
                  ),
                  MediaQuery.of(context).size.width > ToMobileWidth
                      ? titleBar
                      : Expanded(child: emptyWidget),
                  Row(
                    children: <Widget>[
                      ValueListenableBuilder(
                        valueListenable: shuffle,
                        builder: (context, shuflecurrent, child) {
                          return IconButton(
                            icon: Icon(Icons.shuffle),
                            color: shuflecurrent
                                ? Theme.of(context).iconTheme.color
                                : Theme.of(context).disabledColor,
                            onPressed: () {
                              shuffle.value = !shuffle.value;
                            },
                          );
                        },
                      ),
                      ValueListenableBuilder(
                        valueListenable: repeat,
                        builder: (context, repeatcurrent, child) {
                          if (repeatcurrent == 0) {
                            return IconButton(
                              icon: Icon(Icons.repeat),
                              onPressed: () {
                                repeat.value = 1;
                                scaffoldKey.currentState
                                    .removeCurrentSnackBar();

                                scaffoldKey.currentState.showSnackBar(SnackBar(
                                  content: Text("Repeat All"),
                                ));
                              },
                              color: Theme.of(context).disabledColor,
                            );
                          } else if (repeatcurrent == 1) {
                            return IconButton(
                              icon: Icon(Icons.repeat),
                              onPressed: () {
                                repeat.value = 2;
                                scaffoldKey.currentState
                                    .removeCurrentSnackBar();

                                scaffoldKey.currentState.showSnackBar(SnackBar(
                                  content: Text("Repeat One"),
                                ));
                              },
                            );
                          } else if (repeatcurrent == 2) {
                            return IconButton(
                              icon: Icon(Icons.repeat_one),
                              onPressed: () {
                                repeat.value = 3;
                                scaffoldKey.currentState
                                    .removeCurrentSnackBar();

                                scaffoldKey.currentState.showSnackBar(SnackBar(
                                  content: Text("Autoplay Recommended"),
                                ));
                              },
                            );
                          } else {
                            return IconButton(
                              icon: Icon(Icons.sync),
                              onPressed: () {
                                repeat.value = 0;
                                scaffoldKey.currentState
                                    .removeCurrentSnackBar();

                                scaffoldKey.currentState.showSnackBar(SnackBar(
                                  content: Text("Repeat None"),
                                ));
                              },
                            );
                          }
                        },
                      ),
                      IconButton(
                        icon: Icon(Icons.playlist_play),
                        onPressed: queue.value != null && queue.value.isNotEmpty
                            ? () {
                                showQueue(context);
                              }
                            : null,
                      )
                    ],
                  )
                ],
              )
            ],
          ),
        ),
      ),
    );
  }

  Future createPlaylist() async {
    return await showDialog(
        context: context,
        builder: (context) {
          var _controller = TextEditingController();
          return SimpleDialog(
            title: Text("Local Playlist"),
            children: <Widget>[
              TextField(
                controller: _controller,
                decoration: InputDecoration(labelText: "Playlist Name"),
              ),
              ButtonBar(
                children: <Widget>[
                  FlatButton(
                    child: Text("Cancel"),
                    onPressed: () {
                      Navigator.pop(context, false);
                    },
                  ),
                  RaisedButton(
                    child: Text("Create"),
                    onPressed: () {
                      if (!db.objectStoreNames.contains('playlists')) {
                        var ob = db.createObjectStore(
                          'playlists',
                          keyPath: 'title',
                        );
                      }
                      var ob = db
                          .transaction('playlists', 'readwrite')
                          .objectStore('playlists');
                      ob.add({'title': _controller.text});
                      Navigator.pop(context, _controller.text);
                    },
                  )
                ],
              )
            ],
          );
        });
  }

  void showQueue(context) {
    showModalBottomSheet(
        context: context,
        builder: (context) {
          return Queue();
        });
  }
}

class YoutubeSuggestion extends SearchDelegate<Map> {
  static String corsanywhere = 'https://cors-anywhere.herokuapp.com/';
  String suggestionURL =
      'http://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=';

  String searchType = "all";
  var types = ["all", "video", "playlist"];

  @override
  ThemeData appBarTheme(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return theme.copyWith(
        inputDecorationTheme: InputDecorationTheme(
            hintStyle: TextStyle(color: theme.primaryTextTheme.title.color)),
        primaryColor: theme.primaryColor,
        primaryIconTheme: theme.primaryIconTheme,
        primaryColorBrightness: theme.primaryColorBrightness,
        primaryTextTheme: theme.primaryTextTheme,
        textTheme: theme.textTheme.copyWith(
            title: theme.textTheme.title
                .copyWith(color: theme.primaryTextTheme.title.color)));
  }

  @override
  List<Widget> buildActions(BuildContext context) {
    return <Widget>[
      IconButton(
        icon: Icon(Icons.clear),
        onPressed: () {
          query = "";
          showSuggestions(context);
        },
      ),
      PopupMenuButton<String>(
        icon: Icon(Icons.filter_list),
        itemBuilder: (context) {
          var p = List<PopupMenuEntry<String>>();
          for (var i in types) {
            p.add(PopupMenuItem<String>(
              value: i,
              child: Text(i),
            ));
          }
          return p;
        },
        onSelected: (type) {
          searchType = type;
        },
      ),
      IconButton(
        icon: Icon(Icons.search),
        onPressed: () {
          close(context, {"query": query, "type": searchType});
        },
      ),
    ];
  }

  @override
  Widget buildLeading(BuildContext context) {
    return IconButton(
      icon: Icon(Icons.arrow_back),
      onPressed: () {
        close(context, {"query": "", "type": searchType});
      },
    );
  }

  @override
  Widget buildResults(BuildContext context) {
    return Container();
  }

  @override
  Widget buildSuggestions(BuildContext context) {
    if (query.isNotEmpty) {
      String queryURL = suggestionURL + query;
      Future results;
      try {
        results = http.get(queryURL);
      } on Exception catch (e) {
        results = Future.error(Error());
      }
      return FutureBuilder(
        future: results,
        builder: (context, ass) {
          if (ass.connectionState == ConnectionState.done && !ass.hasError) {
            http.Response response = ass.data;
            List l = json.decode(response.body);
            List<String> suggestions = List();
            for (var el in l[1]) {
              suggestions.add(el.toString());
            }
            return _SuggestionList(
              suggestions: suggestions,
              query: query,
              onSelected: (str) {
                query = str;
                close(context, {"query": str, "type": searchType});
              },
              onAdd: (str) {
                query = str;
                showSuggestions(context);
              },
            );
          } else {
            return CircularProgressIndicator();
          }
        },
      );
    } else {
      return Container();
    }
  }
}

class _SuggestionList extends StatelessWidget {
  const _SuggestionList(
      {this.suggestions, this.query, this.onSelected, this.onAdd});

  final List<String> suggestions;
  final String query;
  final ValueChanged<String> onSelected;
  final ValueChanged<String> onAdd;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    return ListView.builder(
      itemCount: suggestions.length,
      itemBuilder: (BuildContext context, int i) {
        final String suggestion = suggestions[i];
        return ListTile(
          leading: query.isEmpty ? const Icon(Icons.history) : const Icon(null),
          title: RichText(
            text: TextSpan(
              text: suggestion.substring(0, query.length),
              style:
                  theme.textTheme.subhead.copyWith(fontWeight: FontWeight.bold),
              children: <TextSpan>[
                TextSpan(
                  text: suggestion.substring(query.length),
                  style: theme.textTheme.subhead,
                ),
              ],
            ),
          ),
          onTap: () {
            onSelected(suggestion);
          },
          trailing: Transform.rotate(
            angle: -math.pi / 4,
            child: IconButton(
              icon: Icon(Icons.arrow_upward),
              onPressed: () {
                onAdd(suggestion);
              },
            ),
          ),
        );
      },
    );
  }
}

// void myBackgroundTask() {
//   AudioServiceBackground.run(() => BackgroundService());
// }
