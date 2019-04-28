import 'dart:async';

import 'package:flutter/material.dart';
import 'searchScreen.dart';
import 'package:flutter/cupertino.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'searchScreen.dart' as search;
import 'queue.dart' as queue;
import 'package:flutter_typeahead/flutter_typeahead.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'playlistSpcl.dart';


class Home extends StatelessWidget{

  final ValueSetter onreturn;
  final Future toptrackfuture;
  final Future topartistfuture;
  
  Home(this.onreturn,this.toptrackfuture,this.topartistfuture);

  @override
  Widget build(BuildContext context) {
    return SliverToBoxAdapter(
        child: Container(
        padding: EdgeInsets.all(8),
        child:
        Column(
          mainAxisAlignment: MainAxisAlignment.start,
          mainAxisSize: MainAxisSize.max,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            //SEARCH
            Container(
              margin: EdgeInsets.all(5),
              decoration: ShapeDecoration(
                shape: StadiumBorder(),
                shadows: [
                  BoxShadow(
                    color: Colors.black45,
                    offset: Offset(1, 2),
                    spreadRadius: 0,
                    blurRadius: 1
                  ),
                  BoxShadow(
                    color: Theme.of(context).canvasColor
                  ),
                ]
              ),
              padding: EdgeInsets.only(
                top: 10,
                bottom: 10,
                left: 20,
                right: 20
              ),
              child: TypeAheadField(
                textFieldConfiguration: TextFieldConfiguration(
                  maxLines: 1,
                  decoration:InputDecoration(
                    icon: Icon(Icons.search),
                    hintText: "Search",
                    
                  ),
                  onSubmitted: (suggestion) async {
                    final result  = await Navigator.push(context, CupertinoPageRoute(builder: (context)=>SearchScreen(suggestion.toString())));
                    if(result!=null){
                      onreturn(result);
                    }
                  }
                ),
                suggestionsCallback: (String pattern) async {
                  String url = "http://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q="+pattern;
                  final response = await http.get(url);
                  if(response.statusCode==200){
                    List responseData = json.decode(response.body);
                    List result = responseData.elementAt(1);
                    return result;
                  }
                },
                itemBuilder: (BuildContext bdctx, dynamic suggestion){
                  return ListTile(
                    leading: Icon(Icons.music_note),
                    title: Text(suggestion.toString()),
                  );
                },
                onSuggestionSelected: (dynamic suggestion) async {
                  final result  = await Navigator.push(context, CupertinoPageRoute(builder: (context)=>SearchScreen(suggestion.toString())));
                  if(result!=null){
                    onreturn(result);
                  }
                },
                
                
              ),
            ),
            Padding(
              padding: EdgeInsets.all(10),
            ),
            
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                //HISTORY
                InkWell(
                  onTap: (){
                    Navigator.push(context, MaterialPageRoute(
                      builder: (context)=>SpecialPlaylist(
                        Text("History"), Container(
                          color: Colors.redAccent,
                        ),
                        queue.platform.invokeMethod("requestHistory",null) 
                        )
                    ));
                  },
                  child: Column(
                    children: <Widget>[
                      Container(
                        decoration: ShapeDecoration(
                          shape: CircleBorder(),
                          gradient: LinearGradient(
                             colors: [Colors.red,Colors.pink[200]],
                             begin: Alignment.topLeft,
                             end: Alignment.bottomRight
                          ),
                          shadows: [BoxShadow(
                            color: Colors.black38,
                            offset: Offset(1, 3),
                            blurRadius: 2,
                          )]
                        ),
                        padding: EdgeInsets.all(15),
                        child: Icon(
                          Icons.history,
                          color: Colors.white,
                          size: 30,
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Text("History"),
                      )
                    ],
                  ),
                ),
                //LAST ADDED
                InkWell(
                  onTap: (){
                    Navigator.push(context, MaterialPageRoute(
                      builder: (context)=>SpecialPlaylist(
                        Text("LastAdded"), Container(
                          color: Colors.greenAccent,
                        ),
                        queue.platform.invokeMethod("requestAllTracks",null) 
                        )
                    ));
                  },
                  child: Column(
                    children: <Widget>[
                      Container(
                        decoration: ShapeDecoration(
                          shape: CircleBorder(),
                          gradient: LinearGradient(
                             colors: [Colors.green,Colors.lime[200]],
                             begin: Alignment.topLeft,
                             end: Alignment.bottomRight
                          ),
                          shadows: [BoxShadow(
                            color: Colors.black38,
                            offset: Offset(1, 3),
                            blurRadius: 2,
                          )]
                        ),
                        padding: EdgeInsets.all(15),
                        child: Icon(
                          Icons.add_to_photos,
                          color: Colors.white,
                          size: 30,
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Text("Last Added"),
                      )
                    ],
                  ),
                ),
                //Top Tracks
                InkWell(
                  onTap: (){
                    Navigator.push(context, MaterialPageRoute(
                      builder: (context)=>SpecialPlaylist(
                        Text("Top Tracks"), Container(
                          color: Colors.lightBlueAccent,
                        ),
                        queue.platform.invokeMethod("requestPopularTracks",null) 
                        )
                    ));
                  },
                  child: Column(
                    children: <Widget>[
                      Container(
                        decoration: ShapeDecoration(
                          shape: CircleBorder(),
                          gradient: LinearGradient(
                             colors: [Colors.blue,Colors.cyan[200]],
                             begin: Alignment.topLeft,
                             end: Alignment.bottomRight
                          ),
                          shadows: [BoxShadow(
                            color: Colors.black38,
                            offset: Offset(1, 3),
                            blurRadius: 2,
                          )]
                        ),
                        padding: EdgeInsets.all(15),
                        child: Icon(
                          Icons.show_chart,
                          color: Colors.white,
                          size: 30,
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Text("Top Tracks"),
                      )
                    ],
                  ),
                ),
                //Shuffle
                InkWell(
                  onTap: (){
                    Navigator.push(context, MaterialPageRoute(
                      builder: (context)=>SpecialPlaylist(
                        Text("Shuffled Tracks"), Container(
                          color: Colors.indigoAccent,
                        ),
                        queue.platform.invokeMethod("requestShuffled",null) 
                        )
                    ));
                  },
                  child: Column(
                    children: <Widget>[
                      Container(
                        decoration: ShapeDecoration(
                          shape: CircleBorder(),
                          gradient: LinearGradient(
                             colors: [Colors.indigo,Colors.lightGreen[200]],
                             begin: Alignment.topLeft,
                             end: Alignment.bottomRight
                          ),
                          shadows: [BoxShadow(
                            color: Colors.black38,
                            offset: Offset(1, 3),
                            blurRadius: 2,
                          )]
                        ),
                        padding: EdgeInsets.all(15),
                        child: Icon(
                          Icons.shuffle,
                          color: Colors.white,
                          size: 30,
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Text("Shuffle"),
                      )
                    ],
                  ),
                ),
                
              ],
            ),
            Padding(padding: EdgeInsets.all(10),),

            Text(
              "Recent Tracks",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 18
              ),
            ),
            Container(
              height: 120,
              child: FutureBuilder(
                future: toptrackfuture,
                builder: (BuildContext btcx, AsyncSnapshot asp){
                  if(asp.connectionState==ConnectionState.done){
                    List toptrack=asp.data;
                    if(toptrack!=null && toptrack.isNotEmpty){
                      return ListView.builder(
                        scrollDirection: Axis.horizontal,
                        itemCount: toptrack.length,
                        physics: BouncingScrollPhysics(),
                        itemBuilder: (BuildContext btx, int index){
                          return Container(
                            padding: EdgeInsets.only(
                              top: 8,
                              bottom: 8,
                              left: 15,
                              right: 15,
                            ),
                            child: Card(
                              elevation: 8,
                              child: InkWell(
                                onTap: ()async{
                                  await queue.platform.invokeMethod("updateQueue",{"queue":toptrack,"index":index});
                                },
                                onLongPress: (){

                                },
                                child: ClipRRect(
                                  
                                  borderRadius: BorderRadius.all(Radius.circular(10)),
                                  child: CachedNetworkImage(
                                    imageUrl:search.getThumbnaillink(toptrack, index, "videoThumbnails", "medium","quality"),
                                    fit: BoxFit.contain,
                                  ),  
                                ),
                              ),
                            ),
                          );
                        },
                      );
                    }
                    else{
                      return Icon(Icons.error);
                    }
                  }else if (asp.connectionState==ConnectionState.waiting){
                    return CircularProgressIndicator();
                  } else{
                    return Icon(Icons.error);
                  }
                },
              ),
            ),
            
            Padding(
              padding: EdgeInsets.all(8),
            ),
            Text("Top Artists",
               style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 18
              ),
            ),
            Container(
              height: 120,
              child: FutureBuilder(
                future: topartistfuture,
                builder: (BuildContext btcx, AsyncSnapshot asp){
                  if(asp.connectionState==ConnectionState.done){
                    List topartist=asp.data;
                    if(topartist!=null && topartist.isNotEmpty){
                      return ListView.builder(
                        scrollDirection: Axis.horizontal,
                        itemCount: topartist.length,
                        physics: BouncingScrollPhysics(),
                        itemBuilder: (BuildContext btx, int index){
                          return Container(
                            padding: EdgeInsets.only(
                              top: 8,
                              bottom: 8,
                              left: 15,
                              right: 15,
                            ),

                            child: Material(
                                child: InkWell(
                                  onTap: (){
                                    Navigator.push(context, MaterialPageRoute(
                                      builder: (context)=>SpecialPlaylist(
                                        Text(topartist[index]["author"]), Container(
                                          decoration: BoxDecoration(
                                            image: DecorationImage(
                                                image: CachedNetworkImageProvider(
                                                  getThumbnaillink(topartist, index,"authorThumbnails",176,"width"),
                                                ),
                                                fit: BoxFit.cover),
                                            
                                          ),
                                          child: Container(
                                            color: Colors.black38
                                          ),
                                        ),
                                        queue.platform.invokeMethod("requestArtistTrack",{"artistId":topartist[index]["authorId"]}) 
                                        )
                                    ));
                                  },
                                  onLongPress: (){

                                  },
                                  child: Container(
                                  height: 100,
                                  width: 100,
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    image: DecorationImage(
                                      image: CachedNetworkImageProvider(
                                        search.getThumbnaillink(topartist, index, "authorThumbnails", 176,"width"),
                                    
                                      ),
                                      fit: BoxFit.contain
                                    ),
                                    color: Colors.transparent,
                                    boxShadow: [BoxShadow(
                                      color: Colors.black26,
                                      offset: Offset(1, 3),
                                      blurRadius: 2
                                    )]
                                  )
                                  
                              ),
                                ),
                            ),
                          );
                        },
                      );
                    }
                    else{
                      return Icon(Icons.error);
                    }
                  }else if (asp.connectionState==ConnectionState.waiting){
                    return CircularProgressIndicator();
                  } else{
                    return Icon(Icons.error);
                  }
                },
              ),
            ),
            
            Padding(
              padding: EdgeInsets.all(10),
            ),
          ],
        )
      ),
    );
  }
}
