import 'package:flutter/material.dart';
import 'dart:ui';

import 'package:share/share.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:fluttery_seekbar/fluttery_seekbar.dart';

import 'trending.dart';
import 'searchScreen.dart';
import 'main.dart';

class PlayerScreen extends StatelessWidget {
  ValueNotifier<int> currentIndex;
  ValueNotifier<List> queue;
  ValueNotifier<int> repeat;
  ValueNotifier<bool> shuffle;
  ValueNotifier<int> currentPlayingTime;
  ValueNotifier<int> totalTime;
  ValueNotifier<PlayerState> playerState;

  Function(BuildContext) openQueue;
  Function play;
  Function pause;
  Function next;
  Function previous;

  GlobalKey<ScaffoldState> scaffoldKey = GlobalKey();

  PlayerScreen(
      {this.queue,
      this.currentIndex,
      this.shuffle,
      this.repeat,
      this.currentPlayingTime,
      this.totalTime,
      this.playerState,
      this.play,
      this.pause,
      this.next,
      this.previous,
      this.openQueue});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: scaffoldKey,
      body: ValueListenableBuilder(
        valueListenable: currentIndex,
        builder: (context, index, wid) {
          Map currentTrack = queue.value[currentIndex.value];
          var thumbURL =
              TrackTile.urlfromImage(currentTrack["videoThumbnails"], "medium");

          return Container(
            decoration: BoxDecoration(
                image: DecorationImage(
                    fit: BoxFit.cover,
                    image: CachedNetworkImageProvider(thumbURL))),
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 8, sigmaY: 8),
              child: Container(
                decoration: BoxDecoration(
                    color: Theme.of(context)
                        .scaffoldBackgroundColor
                        .withAlpha(100)),
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
                            onPressed: () {
                              Navigator.of(context).pop();
                            },
                          ),
                          Text(""),
                          Row(
                            children: <Widget>[
                              IconButton(
                                icon: Icon(Icons.share),
                                onPressed: () {
                                  Share.share(
                                      "https://www.youtube.com/watch?v=" +
                                          currentTrack["videoId"]);
                                },
                              ),
                              IconButton(
                                icon: Icon(Icons.playlist_play),
                                onPressed: () {
                                  openQueue(context);
                                },
                              )
                            ],
                          ),
                        ],
                      ),
                      //Text(getSubtt(
                      //    Duration(milliseconds: currentPlayingTime.value))),
                      Stack(
                        alignment: AlignmentDirectional.center,
                        children: <Widget>[
                          Align(
                              alignment: Alignment.center,
                              child: SizedBox(
                                  height:
                                      MediaQuery.of(context).size.width * 0.6,
                                  width:
                                      MediaQuery.of(context).size.width * 0.6,
                                  child: ValueListenableBuilder(
                                    valueListenable: currentPlayingTime,
                                    builder: (context, index, wid) {
                                      return RadialSeekBar(
                                        seekWidth: 5.0,
                                        progressWidth: 5.0,
                                        trackWidth: 5.0,
                                        trackColor: Colors.white,
                                        progressColor: Colors.amber[200],
                                        thumbPercent: currentPlayingTime.value /
                                            totalTime.value,
                                        progress: currentPlayingTime.value /
                                            totalTime.value,
                                        thumb: CircleThumb(
                                          color: Colors.amber[200],
                                          diameter: 18.0,
                                        ),
                                        onDragStart: (pos){
                                          ignorePositionUpdate.value=true;
                                        },
                                        onDragUpdate: (pos) {
                                          currentPlayingTime.value =
                                              (pos * totalTime.value).toInt();
                                        },
                                        onDragEnd: (pos) {
                                          ignorePositionUpdate.value=false;

                                          player.currentTime =
                                              (pos * totalTime.value).toInt();
                                        },
                                      );
                                    },
                                  ))),
                          Align(
                            alignment: Alignment.center,
                            child: Container(
                              alignment: Alignment.center,
                              width: MediaQuery.of(context).size.width * 0.5,
                              height: MediaQuery.of(context).size.width * 0.5,
                              decoration: BoxDecoration(
                                  boxShadow: [
                                    BoxShadow(
                                        color: Colors.black,
                                        offset: Offset(1, 2),
                                        blurRadius: 4)
                                  ],
                                  shape: BoxShape.circle,
                                  image: DecorationImage(
                                      fit: BoxFit.cover,
                                      image: CachedNetworkImageProvider(
                                          thumbURL))),
                            ),
                          ),
                          Positioned(
                            left: MediaQuery.of(context).size.width * 0.15,
                            top: 0,
                            child: ValueListenableBuilder(
                              valueListenable: repeat,
                              builder: (context, repeatcurrent, child) {
                                if (repeatcurrent == 0) {
                                  return IconButton(
                                    icon: Icon(Icons.repeat),
                                    onPressed: () {
                                      repeat.value = 1;
                                      scaffoldKey.currentState
                                          .removeCurrentSnackBar();

                                      scaffoldKey.currentState
                                          .showSnackBar(SnackBar(
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

                                      scaffoldKey.currentState
                                          .showSnackBar(SnackBar(
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

                                      scaffoldKey.currentState
                                          .showSnackBar(SnackBar(
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

                                      scaffoldKey.currentState
                                          .showSnackBar(SnackBar(
                                        content: Text("Repeat None"),
                                      ));
                                    },
                                  );
                                }
                              },
                            ),
                          ),
                          Positioned(
                              right: MediaQuery.of(context).size.width * 0.15,
                              top: 0,
                              child: ValueListenableBuilder(
                                valueListenable: shuffle,
                                builder: (context, newshuffle, wid) {
                                  return IconButton(
                                    color: shuffle.value
                                        ? Theme.of(context).iconTheme.color
                                        : Theme.of(context).disabledColor,
                                    icon: Icon(Icons.shuffle),
                                    onPressed: () {
                                      shuffle.value = !shuffle.value;
                                    },
                                  );
                                },
                              )),
                          Positioned(
                            left: MediaQuery.of(context).size.width * 0.15,
                            bottom: 0,
                            child: ValueListenableBuilder(
                              valueListenable: currentPlayingTime,
                              builder: (context, time, wid) {
                                return Text(formatDuration(Duration(
                                    seconds: currentPlayingTime.value)));
                              },
                            ),
                          ),
                          Positioned(
                              right: MediaQuery.of(context).size.width * 0.15,
                              bottom: 0,
                              child: Text(formatDuration(
                                  Duration(seconds: totalTime.value)))),
                        ],
                      ),
                      Container(
                        child: Text(
                          currentTrack["title"],
                          style: Theme.of(context).textTheme.title,
                          textAlign: TextAlign.center,
                        ),
                      ),
                      Container(
                        child: Text(
                          currentTrack["author"],
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
                                      icon: Icon(playerState.value ==
                                              PlayerState.Playing
                                          ? Icons.pause_circle_filled
                                          : Icons.play_circle_filled),
                                      iconSize: 80,
                                      onPressed: () {
                                        if (playerState.value ==
                                            PlayerState.Playing) {
                                          player.pause();
                                          playerState.value =
                                              PlayerState.Paused;
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
                                  Icons.play_circle_filled,
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
                        ],
                      )
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
