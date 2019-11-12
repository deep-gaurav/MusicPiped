// import 'dart:async';

// import 'package:audio_service/audio_service.dart';
// import 'package:audioplayers/audioplayers.dart';
// import 'package:flutter/material.dart';

// import 'main.dart';

// class BackgroundService extends BackgroundAudioTask{

//   var   playercompleter = Completer();
//   AudioPlayer audioPlayer = AudioPlayer();



//   BackgroundService(){
//       audioPlayer.onAudioPositionChanged.listen((p) {
//     AudioServiceBackground.setState(
//       controls: [
//         mediaControlButtons["previous"],
//         mediaControlButtons["pause"],
//         mediaControlButtons["next"]
//       ],
//       basicState: BasicPlaybackState.playing,
//       position: p.inSeconds,
//     );
//   });

//   audioPlayer.onPlayerStateChanged.listen((state) {
//     if (state == AudioPlayerState.COMPLETED) {
//       AudioServiceBackground.setState(controls: [
//         mediaControlButtons["previous"],
//         mediaControlButtons["play"],
//         mediaControlButtons["next"]
//       ], basicState: BasicPlaybackState.stopped);
//     }
//   });
//   }


//   void pause() async {
//     AudioServiceBackground.setState(
//         controls: [
//           mediaControlButtons["previous"],
//           mediaControlButtons["play"],
//           mediaControlButtons["next"]
//         ],
//         basicState: BasicPlaybackState.paused,
//         position: (await audioPlayer.onAudioPositionChanged.first).inSeconds);
//     audioPlayer.pause();
//   }

//   void play() {
//     AudioServiceBackground.setState(controls: [
//       mediaControlButtons["previous"],
//       mediaControlButtons["pause"],
//       mediaControlButtons["next"]
//     ], basicState: BasicPlaybackState.playing);
//     audioPlayer.play("").catchError((e) => debugPrint(e));
//   }




//     onPlayFromMediaId(url) async {
//       audioPlayer.stop();
//       audioPlayer.play(url);

//       AudioServiceBackground.setState(controls: [
//         mediaControlButtons["previous"],
//         mediaControlButtons["pause"],
//         mediaControlButtons["next"]
//       ], basicState: BasicPlaybackState.connecting);
//     }
//     onCustomAction(action, data) async {
//       if (action == "setMetadata") {
//         audioPlayer.pause();
//         var meta = MediaItem(
//             id: data["videoId"],
//             album: data["author"],
//             title: data["title"],
//             artist: data["author"],
//             genre: data["genre"],
//             artUri: data["videoThumbnails"].last["url"],
//             displayTitle: data["title"],
//             displaySubtitle: data["author"]);
//         print("metadata $meta");
//         AudioServiceBackground.setMediaItem(meta);
//         AudioServiceBackground.setState(controls: [
//           mediaControlButtons["previous"],
//           mediaControlButtons["pause"],
//           mediaControlButtons["next"]
//         ], basicState: BasicPlaybackState.connecting, position: 0);
//       } else if (action == "Stop") {
//         print("STOPPING");
//         await audioPlayer.stop();
//         playercompleter.complete(null);
//       }
//     }
//     onStart() async {
//       print("Started");
//       await playercompleter.future;
//       return;
//     }
//     onPlay() {
//       AudioServiceBackground.setState(controls: [
//         mediaControlButtons["previous"],
//         mediaControlButtons["pause"],
//         mediaControlButtons["next"]
//       ], basicState: BasicPlaybackState.playing);
//       audioPlayer.play("").catchError((e) => debugPrint(e));
//     }
//     onPause() {
//       pause();
//     }
//     onStop() {
//       audioPlayer.stop();
//       AudioServiceBackground.setState(
//           controls: [], basicState: BasicPlaybackState.stopped);
//       playercompleter.complete();
//     }
//     onClick(MediaButton button) {
//       if (button == MediaButton.media) {
//         if (AudioServiceBackground.state.basicState ==
//             BasicPlaybackState.paused) {
//           play();
//         } else {
//           pause();
//         }
//       } else if (button == MediaButton.next) {
//         AudioServiceBackground.setState(controls: [
//           mediaControlButtons["previous"],
//           mediaControlButtons["pause"],
//           mediaControlButtons["next"]
//         ], basicState: BasicPlaybackState.skippingToNext);
//       } else if (button == MediaButton.previous) {
//         AudioServiceBackground.setState(controls: [
//           mediaControlButtons["previous"],
//           mediaControlButtons["pause"],
//           mediaControlButtons["previous"]
//         ], basicState: BasicPlaybackState.skippingToPrevious);
//       }
//     }
//     onSkipToNext () {
//       AudioServiceBackground.setState(controls: [
//         mediaControlButtons["previous"],
//         mediaControlButtons["pause"],
//         mediaControlButtons["next"]
//       ], basicState: BasicPlaybackState.skippingToNext);
//     }
//     onSkipToPrevious() {
//       AudioServiceBackground.setState(controls: [
//         mediaControlButtons["previous"],
//         mediaControlButtons["pause"],
//         mediaControlButtons["next"]
//       ], basicState: BasicPlaybackState.skippingToPrevious);
//     }
//     onAudioBecomingNoisy () {
//       pause();
//     }
//     onAudioFocusLostTransient() {
//       pause();
//     }
//     onSeekTo(p) {
//       audioPlayer.seek(Duration(seconds: p.toInt())).catchError((e) => debugPrint(e));
//     }
// }