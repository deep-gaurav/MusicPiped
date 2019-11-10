import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:idb_shim/idb.dart';
import 'package:musicpiped_pro/main.dart';

class Queue extends StatelessWidget {
  MyHomePageState mainState;

  Queue() {
    mainState = mainKey.currentState;
  }

  Future<List> getPlaylists() async {
    Database db = await idbFactory.open('musicDB');
    var ob = db.transaction('playlists', 'read').objectStore('playlists');
    var stream = ob.openCursor(autoAdvance: true);
    var list = List();
    await for (var pl in stream) {
      list.add(pl.value);
    }
    return list;
  }

  List<Widget> buildList() {
    var l = List<Widget>();

    for (int i = 0; i < queue.value.length; i++) {
      l.add(Card(
        key: Key(queue.value[i]["title"] + "$i"),
        child: ListTile(
          leading: i == mainState.currentIndex.value
              ? Icon(Icons.play_arrow)
              : Text((i - mainState.currentIndex.value).toString()),
          title: Text(queue.value[i]["title"]),
          onTap: () {
            mainState.currentIndex.value = i;
            mainState.playCurrent();
          },
        ),
      ));
    }

    return l;
  }

  void addTracktoPlaylist(track, playlist) async {
    Database db = await idbFactory.open('musicDB');

    ObjectStore ob =
        db.transaction('tracks', 'readwrite').objectStore('tracks');
    if (await ob.getObject(track["videoId"]) != null) {
      track = await ob.getObject(track['videoId']);
    }
    if (track.containsKey('inPlaylists') &&
        !(track['inPlaylists'] as List).contains(playlist)) {
      (track['inPlaylists'] as List).add(playlist);
    } else {
      track['inPlaylists'] = List();
      (track['inPlaylists'] as List).add(playlist);
    }
    await ob.put(track);
  }

  @override
  Widget build(BuildContext context) {
    return Column(children: <Widget>[
      Container(
        padding: EdgeInsets.all(8),
        child: Text(
          "Queue",
          style: Theme.of(context).textTheme.title,
        ),
      ),
      ButtonBar(
        children: <Widget>[
          RaisedButton.icon(
            icon: Icon(Icons.queue_music),
            color: Theme.of(context).primaryColor,
            label: Text("Save as Queue"),
            onPressed: () async {
              var pname = await mainKey.currentState.createPlaylist();
              if (pname != false) {
                for (var t in queue.value) {
                  await addTracktoPlaylist(t, pname);
                }
                Navigator.of(context).pop();
              }
            },
          ),
          RaisedButton.icon(
            icon: Icon(Icons.add_to_queue),
            color: Theme.of(context).primaryColor,
            label: Text("Add to Queue"),
            onPressed: () async {
              var pl = await getPlaylists();
              showDialog(
                  context: context,
                  builder: (context) {
                    return AlertDialog(
                      title: Text("Choose Playlist"),
                      content: ListView(
                        children: <Widget>[
                          for (var x in pl)
                            Card(
                              child: ListTile(
                                title: Text(x["title"]),
                                onTap: () async {
                                  for (var t in queue.value) {
                                    await addTracktoPlaylist(t, x['title']);
                                  }
                                  Navigator.of(context).pop();
                                },
                              ),
                            )
                        ],
                      ),
                    );
                  });
            },
          ),
        ],
      ),
      ValueListenableBuilder(
        valueListenable: mainState.currentIndex,
        builder: (context, i, wid) {
          return StatefulBuilder(builder: (context, setState) {
            return Expanded(
              child: ReorderableListView(
                children: buildList(),
                onReorder: (i, j) {
                  setState(() {
                    var current = queue.value[mainState.currentIndex.value];
                    var t = queue.value.removeAt(i);
                    queue.value.insert(i < j ? j - 1 : j, t);
                    mainState.currentIndex.value = queue.value.indexOf(current);
                  });
                },
              ),
            );
          });
        },
      )
    ]);
  }
}
