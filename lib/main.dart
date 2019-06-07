import 'dart:async';
import 'dart:math' as math;
import 'dart:typed_data';
import 'dart:ui';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/services.dart';
import 'package:pedantic/pedantic.dart';
import 'dart:io';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

import 'package:idb_shim/idb.dart';
import 'package:idb_shim/idb_io.dart';
import 'package:path_provider/path_provider.dart';

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

Database musicDB;
Database settingDB;

var brightness = ValueNotifier("dark");
var invidiosAPI = ValueNotifier("https://invidio.us/");
var quality = ValueNotifier("best");

var ignorePositionUpdate = ValueNotifier(false);

AudioPlayer player = AudioPlayer();

void main() async {
  var appDir = await getApplicationDocumentsDirectory();

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
  runApp(MyApp());
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
    return MaterialApp(
      title: 'MusicPiped Pro',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        brightness:
            brightness.value == "dark" ? Brightness.dark : Brightness.light,
      ),
      home: MyHomePage(title: 'MusicPiped'),
      debugShowCheckedModeBanner: false,
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
    with SingleTickerProviderStateMixin {
  TextEditingController textEditingController = TextEditingController();

  dynamic howlerId = 0;

  static String InvidiosAPI = invidiosAPI.value + "api/v1/";

  var queue = ValueNotifier<List>([]);

  var playerState = ValueNotifier(PlayerState.Stopped);

  var currentIndex = ValueNotifier(0);

  var totalLength = ValueNotifier(0);

  String state;

  Timer syncTimer;

  Database db;

  String debugString;

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

    db = musicDB;
    _tabController = TabController(length: 3, vsync: this);

    _tabController.addListener(() {
      setState(() {});
    });

    player.addEventListener('timeupdate', (e) {
      if(ignorePositionUpdate.value){
        return;
      }
      positionNotifier.value = player.currentTime;
      totalLength.value = player.duration;
    });
    /*
    player.addEventListener('ended', (e) {
      playerState.value = PlayerState.Stopped;
    });
    */
    player.addEventListener('playing', (e) {
      playerState.value = PlayerState.Playing;
    });
    player.addEventListener('play', (e) {
      playerState.value = PlayerState.Playing;
    });
    player.addEventListener('pause', (e) {
      playerState.value = PlayerState.Paused;
    });
    player.addEventListener('loadstart', (e) {
      playerState.value = PlayerState.Loading;
    });
    player.addEventListener('error', (e) {
      playerState.value = PlayerState.Error;
    });
    player.addEventListener('next', (e) {
      next();
    });
    player.addEventListener('previous', (e) {
      previous();
    });

    positionNotifier.addListener((){
      if(totalLength.value>0
       && positionNotifier.value>0
       && positionNotifier.value>=totalLength.value){
         next();
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
    return player.isCached("https://dummyurl.com/abc.mp4?a=1", s["videoId"]);
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

  Future<ByteData> imagefromURL(String url) {
    var provider = CachedNetworkImageProvider(url);
    var stream = provider.resolve(ImageConfiguration());
    var completer = Completer<ByteData>();
    stream.addListener((info, callflag) {
      completer.complete(info.image.toByteData(format: ImageByteFormat.png));
    });
    return completer.future;
  }

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
    player.pause();
    playerState.value = PlayerState.Loading;
    Map s = queue.value[currentIndex.value];
    var image;
    try {
      image = await imagefromURL(
          TrackTile.urlfromImage(s["videoThumbnails"], "medium"));
    } catch (e) {
      image = null;
    }
    var barrray = image.buffer.asUint8List();
    player.metadata = {
      "title": s["title"],
      "artist": s["author"],
      "thumb": barrray,
      "vidId": s["videoId"]
    };
    player.updateMetadata();
  }

  Future<void> loadCurrent() async {
    Map s = this.queue.value[currentIndex.value];
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
      url = await fixURLAccess(s);
    }

    player.pause();
    player.currentTime = 0;

    //howlerId = howler.callMethod("play");
    var image;
    try {
      image = await imagefromURL(
          TrackTile.urlfromImage(s["videoThumbnails"], "medium"));
    } catch (e) {
      image = null;
    }
    var barrray = image.buffer.asUint8List();
    player.metadata = {
      "title": s["title"],
      "artist": s["author"],
      "thumb": barrray,
      "vidId": s["videoId"]
    };
    player.src = url;

    unawaited(player.play().catchError((e) {
      playerState.value = PlayerState.Error;
    }));

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

  void playCurrent() {
    updateMetadata();
    loadCurrent().catchError((e) {
      loadCurrent();
    });
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

  void onEnd() {
    if (repeat.value == 2) {
      //IF repeatSingle
      player.play();
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
                String searchQ = await showSearch<String>(
                    context: context, delegate: _searchDelegate);
                if (searchQ.isNotEmpty) {
                  var result = await Navigator.of(context).push(
                      MaterialPageRoute(
                          builder: (context) => (SearchScreen(searchQ))));
                  if (result == null) {
                    return;
                  }
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
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
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
                          )
                        ],
                      )));
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
                  bool reload = await showDialog(
                      context: context,
                      builder: (context) {
                        var _controller = TextEditingController();
                        return SimpleDialog(
                          title: Text("Local Playlist"),
                          children: <Widget>[
                            TextField(
                              controller: _controller,
                              decoration:
                                  InputDecoration(labelText: "Playlist Name"),
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
                                    if (!db.objectStoreNames
                                        .contains('playlists')) {
                                      var ob = db.createObjectStore(
                                        'playlists',
                                        keyPath: 'title',
                                      );
                                    }
                                    var ob = db
                                        .transaction('playlists', 'readwrite')
                                        .objectStore('playlists');
                                    ob.add({'title': _controller.text});
                                    Navigator.pop(context, true);
                                  },
                                )
                              ],
                            )
                          ],
                        );
                      });
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
                  player.play();
                },
                pause: () {
                  playerState.value = PlayerState.Paused;
                  player.pause();
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
                  return Slider(
                    max: totalLength.value.toDouble() > position.toDouble()
                        ? totalLength.value.toDouble()
                        : position.toDouble(),
                    value: position.toDouble(),
                    onChangeStart: (val){
                      ignorePositionUpdate.value=true;
                    },
                    onChanged: (newpos) {
                      positionNotifier.value = newpos.round();
                    },
                    onChangeEnd: (pos) {
                      ignorePositionUpdate.value=false;
                      player.currentTime = pos.round();
                    },
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
                                      player.pause();
                                      playerState.value = PlayerState.Paused;
                                    } else {
                                      player.play();
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
                          return Text(formatDuration(Duration(
                                  milliseconds: (positionNotifier.value * 1000)
                                      .toInt())) +
                              "/" +
                              formatDuration(Duration(
                                  milliseconds:
                                      (totalLength.value * 1000).toInt())));
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

  void showQueue(context) {
    showModalBottomSheet(
        context: context,
        builder: (context) {
          return Column(children: <Widget>[
            Text(
              "Queue",
              style: Theme.of(context).textTheme.title,
            ),
            ValueListenableBuilder(
              valueListenable: currentIndex,
              builder: (context, i, wid) {
                return Expanded(
                  child: ListView.builder(
                    itemCount: queue.value.length,
                    shrinkWrap: true,
                    itemBuilder: (ctx, i) {
                      return Card(
                        child: ListTile(
                          leading: i == currentIndex.value
                              ? Icon(Icons.play_arrow)
                              : Text((i - currentIndex.value).toString()),
                          title: Text(queue.value[i]["title"]),
                          onTap: (){
                            currentIndex.value=i;
                            playCurrent();
                          },
                        ),
                      );
                    },
                  ),
                );
              },
            )
          ]);
        });
  }
}

class YoutubeSuggestion extends SearchDelegate<String> {
  static String corsanywhere = 'https://cors-anywhere.herokuapp.com/';
  String suggestionURL =
      'http://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=';

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
      IconButton(
        icon: Icon(Icons.search),
        onPressed: () {
          close(context, query);
        },
      ),
    ];
  }

  @override
  Widget buildLeading(BuildContext context) {
    return IconButton(
      icon: Icon(Icons.arrow_back),
      onPressed: () {
        close(context, "");
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
      Future results = http.get(queryURL);
      return FutureBuilder(
        future: results,
        builder: (context, ass) {
          if (ass.connectionState == ConnectionState.done) {
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
                close(context, str);
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
