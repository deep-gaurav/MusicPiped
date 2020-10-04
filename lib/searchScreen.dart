import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:http/http.dart' as http;

import 'package:cached_network_image/cached_network_image.dart';

import 'trackDetail.dart';
import 'dart:convert';

import 'artistPlaylist.dart';
import 'main.dart' as main;

class SearchScreen extends StatefulWidget {
  final String _initialSearch;
  final String type;
  void Function(List<Map>) play;
  void Function(Map) playnext;
  SearchScreen(this._initialSearch, this.type, this.play, this.playnext);

  @override
  State<StatefulWidget> createState() {
    return SearchScreenState(_initialSearch, type, play, playnext);
  }
}

class SearchScreenState extends State<SearchScreen> {
  String _searchquery;
  String _type;

  void Function(List<Map>) _play;
  void Function(Map) _playnext;

  List<dynamic> results;
  bool isSearching = true;
  String title;
  List<int> selected = new List();

  Future<dynamic> resultFuture;

  SearchScreenState(searcgQuery, type, play, playnext) {
    _searchquery = searcgQuery;
    _type = type;
    _play = play;
    _playnext = playnext;
    title = searcgQuery;
    resultFuture = searchVid(_searchquery);
  }

  void exit(out){
    Navigator.pop(context,out);
  }

  Widget body() {
    return FutureBuilder(
      future: resultFuture,
      builder: (context, ass) {
        if (ass.connectionState == ConnectionState.done) {
          if (ass.hasError) {
            return Column(
              children: <Widget>[
                Text("Fetch Error cant load"),
                RaisedButton(
                  child: Text("RETRY"),
                  onPressed: () {
                    setState(() {
                      resultFuture = searchVid(_searchquery);
                    });
                  },
                ),
                Text(ass.error.toString(),
                    style: Theme.of(context)
                        .textTheme
                        .body1
                        .copyWith(color: Theme.of(context).errorColor))
              ],
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.center,
            );
          } else {
            return ListView.builder(
              itemCount: results.length,
              itemBuilder: (BuildContext contextList, int index) {
                Color cardcolor;
                if (selected.contains(index)) {
                  cardcolor = Theme.of(context).highlightColor;
                } else {
                  cardcolor = Theme.of(context).canvasColor;
                }
                String type = results[index]["type"];
                print(type);
                if (type == "video" || type == "playlist") {
                  return ListTile(
                    title: Card(
                      color: cardcolor,
                      elevation: 6,
                      margin: EdgeInsets.all(6),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.all(Radius.circular(10))),
                      child: InkWell(
                        onTap: () async {
                          if (type == "video") {
                            if (selected.isEmpty) {
                              List<dynamic> returnValue = new List();
                              returnValue.add(results[index]);

                              //List updatedreturnvalue = await resultSearchtoVideoHash(returnValue);
                              //Navigator.pop(context,{'queue':updatedreturnvalue,'addtoexisting':false});
                              var toreturn;
                              var c = showBottomSheet(
                                  context: context,
                                  builder: (context) {
                                    return TrackDetail(results[index], (info) {
                                      toreturn = info;
                                    });
                                  });
                              await c.closed;
                              if (toreturn != null) {
                                Navigator.pop(context, {
                                  'queue': toreturn,
                                  'addtoexisting': false
                                });
                              }
                            }
                            setState(() {
                              if (selected.isEmpty) {
                              } else {
                                if (selected.contains(index)) {
                                  selected.remove(index);
                                  if (selected.isEmpty) {
                                    title = _searchquery;
                                  }
                                } else {
                                  selected.add(index);
                                  title = selected.length.toString();
                                }
                              }
                            });
                          } else {
                            var result = showBottomSheet(
                                context: context,
                                builder: (context2) {
                                  var playlist;
                                  return CustomScrollView(
                                    slivers: <Widget>[
                                      SliverToBoxAdapter(
                                        child: Text(
                                          results[index]["title"],
                                          maxLines: 1,
                                          style: Theme.of(context)
                                              .primaryTextTheme
                                              .title,
                                        ),
                                      ),
                                      SliverToBoxAdapter(
                                        child: ButtonBar(
                                          children: <Widget>[
                                            RaisedButton.icon(
                                              icon: Icon(Icons.play_arrow),
                                              label: Text("Play"),
                                              onPressed: () {
                                                if (playlist != null) {
                                                  var l = List<Map>();
                                                  for(var x in playlist["videos"]){
                                                    l.add(x);
                                                  }
                                                  _play(
                                                    l
                                                  );
                                                  Navigator.popUntil(context, (route){
                                                    return route.isFirst;
                                                  });
                                                }
                                              },
                                            ),
                                            RaisedButton.icon(
                                              icon: Icon(Icons
                                                  .settings_backup_restore),
                                              label: Text("Import"),
                                              onPressed: () async {
                                                var url = main.MyHomePageState
                                                        .InvidiosAPI +
                                                    "playlists/" +
                                                    results[index]
                                                        ["playlistId"];
                                                var response =
                                                    await http.get(url);
                                                Map jsresponse = await json
                                                    .decode(utf8.decode(
                                                        response.bodyBytes));
                                                if (jsresponse
                                                    .containsKey('title')) {
                                                  var db = main.musicDB;
                                                  var ob = db
                                                      .transaction('playlists',
                                                          'readwrite')
                                                      .objectStore('playlists');
                                                  await ob.add(jsresponse);
                                                  Navigator.pop(context2, true);

                                                  Scaffold.of(context)
                                                      .showSnackBar(SnackBar(
                                                    content: Text(
                                                        "Playlist Imported"),
                                                  ));
                                                }
                                              },
                                            )
                                          ],
                                        ),
                                      ),
                                      FutureBuilder<http.Response>(
                                        future: http.get(
                                            main.MyHomePageState.InvidiosAPI +
                                                "playlists/" +
                                                results[index]["playlistId"]),
                                        builder: (context3, ass) {
                                          if (ass.connectionState ==
                                              ConnectionState.done) {
                                            playlist = json.decode(utf8
                                                .decode(ass.data.bodyBytes));
                                            print(playlist);
                                            return SliverList(
                                              delegate:
                                                  SliverChildBuilderDelegate(
                                                      (context, i) {
                                                return Card(
                                                  child: ListTile(
                                                    title: Text(
                                                        playlist["videos"][i]
                                                            ["title"]),
                                                  ),
                                                );
                                              },
                                                      childCount:
                                                          playlist["videos"]
                                                              .length),
                                            );
                                          } else {
                                            return SliverToBoxAdapter(
                                              child: Center(
                                                child:
                                                    CircularProgressIndicator(),
                                              ),
                                            );
                                          }
                                        },
                                      ),
                                    ],
                                  );
                                });
                          
                          }
                        },
                        onLongPress: () {
                          setState(() {
                            if (selected.contains(index))
                              selected.remove(index);
                            else
                              selected.add(index);

                            if (selected.isEmpty) {
                              title = _searchquery;
                            } else {
                              title = selected.length.toString();
                            }
                          });
                        },
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: <Widget>[
                            Stack(
                              children: <Widget>[
                                AnimatedContainer(
                                  duration: Duration(seconds: 3),
                                  padding: EdgeInsets.all(10),
                                  child: CachedNetworkImage(
                                    imageUrl: type == "video"
                                        ? getThumbnaillink(
                                            results,
                                            index,
                                            "videoThumbnails",
                                            "medium",
                                            "quality")
                                        : type == "playlist"
                                            ? getThumbnaillink(
                                                results[index]["videos"],
                                                0,
                                                "videoThumbnails",
                                                "medium",
                                                "quality")
                                            : "",
                                  ),
                                ),
                                if (type == "playlist")
                                  Positioned(
                                    top: 0,
                                    bottom: 0,
                                    right: 0,
                                    width: 100,
                                    child: Container(
                                        color: Colors.black.withAlpha(225),
                                        child: Column(
                                          mainAxisAlignment:
                                              MainAxisAlignment.center,
                                          crossAxisAlignment:
                                              CrossAxisAlignment.center,
                                          children: <Widget>[
                                            Text(
                                              results[index]["videoCount"]
                                                  .toString(),
                                              style: Theme.of(context)
                                                  .primaryTextTheme
                                                  .title,
                                            ),
                                            Icon(Icons.playlist_play)
                                          ],
                                        )),
                                  )
                              ],
                            ),
                            Row(
                              children: <Widget>[
                                Flexible(
                                    child: Container(
                                  padding: EdgeInsets.all(8),
                                  child: Text(
                                    results[index]["title"],
                                    style: Theme.of(context).textTheme.title,
                                    overflow: TextOverflow.ellipsis,
                                    maxLines: 2,
                                  ),
                                )),
                              ],
                            ),
                            Container(
                              padding: EdgeInsets.all(10),
                              child: Row(
                                children: <Widget>[
                                  Expanded(
                                    child: Text(
                                      results[index]["author"],
                                      style:
                                          Theme.of(context).textTheme.subtitle,
                                    ),
                                  ),
                                  if (type == "video")
                                    Text(
                                      formatDuration(Duration(
                                          seconds: results[index]
                                              ["lengthSeconds"])),
                                      style:
                                          Theme.of(context).textTheme.subtitle,
                                    )
                                ],
                              ),
                            )
                          ],
                        ),
                      ),
                    ),
                  );
                } else if (type == "channel") {
                  return InkWell(
                    onTap: () {
                      Navigator.push(
                          context,
                          MaterialPageRoute(
                              builder: (context) => (ArtistPlaylist(
                                    _play,
                                    _playnext,
                                    results[index]["author"],
                                    usePlaylist: true,
                                    playlistdetail: {
                                      'id': results[index]["authorId"]
                                    },
                                  ))));
                    },
                    child: Column(
                      children: <Widget>[
                        Container(
                          height: 176,
                          width: 176,
                          decoration: BoxDecoration(
                              boxShadow: [
                                BoxShadow(
                                    color: Colors.black,
                                    blurRadius: 2,
                                    offset: Offset(2, 2))
                              ],
                              shape: BoxShape.circle,
                              image: DecorationImage(
                                  image: CachedNetworkImageProvider(
                                      getThumbnaillink(results, index,
                                          "authorThumbnails", 176, "width")),
                                  fit: BoxFit.cover)),
                        ),
                        Text(
                          results[index]["author"],
                          style: Theme.of(context).primaryTextTheme.title,
                        )
                      ],
                    ),
                  );
                }
              },
            );
          }
        } else {
          return Center(
            child: CircularProgressIndicator(),
          );
        }
      },
    );
  }

  Future<dynamic> searchVid(searchquery) async {
    String invidiosApi = "https://invidious.snopyta.org/";
    String apiurl =
        invidiosApi + "api/v1/search?type=" + _type + "&q=" + searchquery;
    print(apiurl);

    try {
      final response = await http.get(apiurl);

      if (response.statusCode == 200) {
        // If server returns an OK response, parse the JSON
        dynamic js = json.decode(utf8.decode(response.bodyBytes));
        setState(() {
          results = js;
          isSearching = false;
        });
        return js;
      } else {
        print("Fetch ERROR");
        // If that response was not OK, throw an error.
        throw Exception('Failed to load post');
      }
    } catch (e) {
      return Future.error(Exception(e.toString()));
    }
  }

  Future<List<dynamic>> resultSearchtoVideoHash(List vidlist) async {
    List<Map> newVidList = new List();
    for (Map x in vidlist) {
      Map y = await fetchVid(x["videoId"]);
      newVidList.add(y);
    }
    return newVidList;
  }

  Future<Map> fetchVid(id) async {
    String invidiosApi = "https://invidious.snopyta.org/";
    String apiurl = invidiosApi + "api/v1/videos/";
    String videoId = id;
    final response = await http.get(apiurl + videoId);
    print("received");
    if (response.statusCode == 200) {
      // If server returns an OK response, parse the JSON
      Map<String, dynamic> js = json.decode(utf8.decode(response.bodyBytes));

      return js;
    } else {
      print("Fetch ERROR");
      // If that response was not OK, throw an error.
      throw Exception('Failed to load post');
    }
  }

  @override
  Widget build(BuildContext context) {
    if (selected.isEmpty) {
      title = _searchquery;
    } else {
      title = selected.length.toString();
    }
    return WillPopScope(
      onWillPop: () {
        return Future.value(selected.isEmpty);
      },
      child: Scaffold(
        appBar: AppBar(
            actions: <Widget>[
              Opacity(
                opacity: selected.isEmpty ? 0 : 1,
                child: IconButton(
                  icon: Icon(Icons.library_add),
                  onPressed: () async {
                    if (selected.isNotEmpty) {
                      List<dynamic> returnValue = new List();
                      for (int x in selected) {
                        returnValue.add(results[x]);
                      }
                      setState(() {
                        isSearching = true;
                      });
                      List updatedreturnvalue =
                          await resultSearchtoVideoHash(returnValue);
                      Navigator.pop(context,
                          {'queue': updatedreturnvalue, 'addtoexisting': true});
                    }
                  },
                ),
              ),
              Opacity(
                  opacity: selected.isEmpty ? 0 : 1,
                  child: IconButton(
                    onPressed: () async {
                      if (selected.isNotEmpty) {
                        List<dynamic> returnValue = new List();
                        for (int x in selected) {
                          returnValue.add(results[x]);
                        }
                        setState(() {
                          isSearching = true;
                        });
                        List updatedreturnvalue =
                            await resultSearchtoVideoHash(returnValue);
                        Navigator.pop(context, {
                          'queue': updatedreturnvalue,
                          'addtoexisting': false
                        });
                      }
                    },
                    icon: Icon(Icons.play_arrow),
                  )),
            ],
            title: Text(title),
            leading: IconButton(
              color: Colors.white,
              icon: Icon(Icons.arrow_back),
              onPressed: () {
                if (selected.length == 0) {
                  Navigator.pop(context);
                } else {
                  setState(() {
                    selected.clear();
                  });
                }
              },
            )),
        body: body(),
      ),
    );
  }
}

String getThumbnaillink(
    List list, int index, String type, dynamic quality, String finder) {
  List result = list[index][type];
  for (int i = 0; i < result.length; i++) {
    if (result[i][finder] == quality) {
      String url = result[i]["url"];
      if (url.startsWith("http")) {
        return url;
      } else {
        return "https:" + url;
      }
    }
  }
  return null;
}

String formatDuration(Duration d) {
  int mins = d.inMinutes;
  int secs = d.inSeconds % 60;
  return mins.toString().padLeft(1, "0") +
      ":" +
      secs.toString().padLeft(2, "0");
}
