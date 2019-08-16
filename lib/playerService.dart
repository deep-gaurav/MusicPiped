//import 'package:flutter/foundation.dart';
//import 'package:audioplayer/audioplayer.dart' as AP;
//import 'package:http/http.dart' as http;
//import 'package:youtube_extractor/youtube_extractor.dart';
//import 'package:audio_service/audio_service.dart';
//import 'dart:async';
//
//class AudioPlayerS{
//
//  String _src;
//
//  Map<String,Function> listeners = new Map();
//
//
//  Map<String,dynamic> metadata;
//
//  set src(dynamic source){
//    // platform.invokeListMethod("changeSource",{
//    //   "src":source,
//    // }..addAll(metadata));
//    _src=source;
//  }
//  String get src{
//    return _src;
//  }
//
//  int _currentTime;
//
//  set currentTime(int time){
//    _currentTime = time;
//    if(AudioService.playbackState.basicState==BasicPlaybackState.playing){
//      AudioService.seekTo(0);
//    }
//  }
//
//  int get currentTime{
//    return _currentTime;
//  }
//  int duration;
//
//  var readyOpenURL=ValueNotifier<String>("");
//
//  var extractor = YouTubeExtractor();
//
//  AudioPlayerS(){
//    print("musicpiped: Dart Handler Ready");
//    AudioService.connect();
//    AudioService.start(        // When user clicks button to start playback
//      backgroundTask: myBackgroundTask,
//      androidNotificationChannelName: 'Music Player',
//      androidNotificationIcon: "mipmap/ic_launcher",
//    );
//    // platform.invokeMethod("readyOpenURL").then((p){print("musicpiped:"+ p);readyOpenURL.value=p;});
//  }
//
//  void myBackgroundTask() {
//    var player = AP.AudioPlayer();
//    var completer = Completer();
//    AudioServiceBackground.run(
//      onPlayFromMediaId: (url){
//        print("url $url");
//        player.play(url);
//
//      },
//      onStart: () async {
//        return await completer.future;
//      },
//      onPlay: () {
//        player.play("");
//      },
//      onPause: () {
//        player.pause();
//      },
//      onStop: () {
//        player.stop();
//      },
//      onClick: (MediaButton button) {
//
//      },
//    );
//  }
//
//  // Future<dynamic> platformHandler(MethodCall methodCall){
//  //   if(methodCall.method=="timeupdate"){
//  //     _currentTime = methodCall.arguments['currentTime'];
//  //     duration = methodCall.arguments['duration'];
//  //   }
//  //   if(listeners.containsKey(methodCall.method)){
//  //     listeners[methodCall.method](methodCall.arguments);
//  //   }
//  // }
//
//  Future<dynamic> pause(){
//    return AudioService.pause();
//  }
//
//  Future<dynamic> play()async{
//    var r=await http.head(_src);
//    if(r.statusCode==403){
//      print("Error 403, refresh vidId ${metadata['vidId']}");
//      var streamInfo = await extractor.getMediaStreamsAsync(metadata["vidId"]);
//      for(int i=0;i<streamInfo.video.length;i++){
//        var r2 = await http.head(streamInfo.video[i].url);
//        print("Vid URL $i response ${r2.statusCode}");
//      }
//      for(int i=0;i<streamInfo.audio.length;i++){
//        var r2 = await http.head(streamInfo.audio[i].url);
//        print("Audio URL $i response ${r2.statusCode}");
//      }
//      _src=streamInfo.audio.first.url;
//      return await play();
//    }
//    print("Play $_src");
//
//    return AudioService.playFromMediaId(_src);
//  }
//
//  // void addEventListener(String event, Function(dynamic e) call){
//  //   listeners[event]=call;
//  // }
//
//  Future<bool> isCached(String url,String vidId)async{
//    // String furl = url+"&videoId="+vidId;
//    // var res = await platform.invokeMethod("isCached",{"url":furl});
//    // print("$vidId is cached : $res");
//    // return res==true;
//    //TODO fix cache
//
//    return false;
//  }
//
//  // void updateMetadata(){
//  //   platform.invokeMethod("updateMetadata",metadata);
//  // }
//
//  // void openFX(){
//  //   platform.invokeMethod('openSystemEqualizer');
//  // }
//}