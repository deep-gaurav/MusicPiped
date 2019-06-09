import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';


class AudioPlayer{

  String _src;

  Map<String,Function> listeners = new Map();

  
  Map<String,dynamic> metadata;

  set src(dynamic source){
    platform.invokeListMethod("changeSource",{
      "src":source,
    }..addAll(metadata));
    _src=source;
  }
  String get src{
    return _src;
  }

  int _currentTime;

  set currentTime(int time){
    _currentTime = time;
    platform.invokeMethod("seek",{"position":time});
  }

  int get currentTime{
    return _currentTime;
  }
  int duration;

  var readyOpenURL=ValueNotifier<String>("");

  static const platform = const MethodChannel('me.devsilver.musicpiped/player');

  AudioPlayer(){
    print("musicpiped: Dart Handler Ready");
    platform.setMethodCallHandler(
      platformHandler
    );
    platform.invokeMethod("readyOpenURL").then((p){print("musicpiped:"+ p);readyOpenURL.value=p;});
  }
  
  Future<dynamic> platformHandler(MethodCall methodCall){
    if(methodCall.method=="timeupdate"){
      _currentTime = methodCall.arguments['currentTime'];
      duration = methodCall.arguments['duration'];
    }
    if(listeners.containsKey(methodCall.method)){
      listeners[methodCall.method](methodCall.arguments);
    }
  }

  Future<dynamic> pause(){
    return platform.invokeMethod("pause");
  }

  Future<dynamic> play(){
    return platform.invokeMethod("play");
  }

  void addEventListener(String event, Function(dynamic e) call){
    listeners[event]=call;
  }

  Future<bool> isCached(String url,String vidId)async{
    String furl = url+"&videoId="+vidId;
    var res = await platform.invokeMethod("isCached",{"url":furl});
    print("$vidId is cached : $res");
    return res==true;
  }

  void updateMetadata(){
    platform.invokeMethod("updateMetadata",metadata);
  }
}