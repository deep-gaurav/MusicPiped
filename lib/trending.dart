import 'dart:async';
import 'dart:convert';

import 'package:cached_network_image/cached_network_image.dart';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:http/http.dart' as http;

import 'main.dart' as main;

class Trending extends StatefulWidget {
  void Function(Map trackInfo) onPressed;

  Trending(this.onPressed);

  @override
  _TrendingState createState() => _TrendingState();
}

class _TrendingState extends State<Trending>
    with AutomaticKeepAliveClientMixin {
  Future<http.Response> requestFuture;
  String trendingQuery =
      main.MyHomePageState.InvidiosAPI + "trending?type=music&region=";

  String region = "US";

  String publicIPfy = "https://api.ipify.org";

  String ipApi = "https://api.ip2country.info/ip?";

  GlobalKey<RefreshIndicatorState> refreshkey = GlobalKey();

  @override
  void initState() {
    super.initState();
    var c = Completer<http.Response>();

    http.get(publicIPfy).then((response) async {
      var response2 = await http.get(ipApi + response.body);
      Map data = json.decode(response2.body);
      region = data["countryCode"];
      c.complete(http.get(trendingQuery + region));
    });
    requestFuture = c.future;
  }

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
        key: refreshkey,
        onRefresh: () {
          var c = Completer<http.Response>();
          var c2 = Completer<void>();
          requestFuture = c.future;
          http.get(trendingQuery).then((response) {
            c.complete(response);
            setState(() {});
            c2.complete();
          });
          return c2.future;
        },
        child: FutureBuilder<http.Response>(
          future: requestFuture,
          builder: (context, ass) {
            if (ass.connectionState == ConnectionState.done) {
              var response = ass.data;
              List trendings = json.decode(utf8.decode(response.bodyBytes));
              return GridView.builder(
                gridDelegate: SliverGridDelegateWithMaxCrossAxisExtent(
                    maxCrossAxisExtent: 330, childAspectRatio: 330.0 / 190),
                itemCount: trendings.length,
                itemBuilder: (ctx, i) {
                  return TrackTile(trendings[i], widget.onPressed);
                },
              );
            } else {
              return CircularProgressIndicator();
            }
          },
        ));
  }

  @override
  bool get wantKeepAlive => true;
}

class TrackTile extends StatelessWidget {
  Map trackInfo;

  void Function(Map trackInfo) onPressed;

  TrackTile(this.trackInfo, this.onPressed);

  static String urlfromImage(List imglist, dynamic quality,
      {String param = "quality"}) {
    try {
      for (Map f in imglist) {
        var q = f[param];
        if (q == quality) {
          if((f["url"] as String).startsWith("http"))
            return f["url"];
          else{
            return "https:"+f["url"];
          }
        }
      }
    } catch (e) {
      return "";
    }
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () {
        onPressed(trackInfo);
      },
      child: Card(
        elevation: 4,
        child: Stack(
          children: <Widget>[
            CachedNetworkImage(
                imageUrl: urlfromImage(trackInfo["videoThumbnails"], "medium")),
            Positioned(
                right: 0,
                left: 0,
                bottom: 0,
                child: Container(
                  color: Theme.of(context).backgroundColor.withAlpha(200),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    mainAxisSize: MainAxisSize.min,
                    children: <Widget>[
                      Container(
                        height:
                            Theme.of(context).textTheme.subhead.fontSize + 5,
                        child: Text(
                          trackInfo["title"],
                          style: Theme.of(context).textTheme.subhead,
                          maxLines: 1,
                        ),
                      ),
                      Text(
                        trackInfo["author"],
                        style: Theme.of(context).textTheme.subtitle,
                        maxLines: 1,
                      )
                    ],
                  ),
                ))
          ],
        ),
      ),
    );
  }
}
