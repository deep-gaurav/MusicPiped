import 'package:flutter/material.dart';
import 'package:flutter_xlider/flutter_xlider.dart';

import 'player.dart';
import 'main.dart' as main;

class Equalizer extends StatefulWidget{
  @override
  State<StatefulWidget> createState() {
    return EqualizerState();
  }

}

class EqualizerState extends State<Equalizer>{

  List<String> presets=["Normal","Classical","Dance","Flat","Folk","Heavy Metal","Hip Hop","Jazz","Pop","Rock","Custom"];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: StreamBuilder(
          stream: main.mainStreamController.stream,
          builder: (context,ass){

            if(ass.connectionState==ConnectionState.active){

              Map data= ass.data["newB"];
              Map eqSetting=data["eqSetting"];
              print(eqSetting);
              print(data["preset"]);
              return Column(
                children:<Widget>[
                  DropdownButton<String>( 
                    value: data["preset"]==-1?presets.last: presets.elementAt(data["preset"]),
                    items: presets.map<DropdownMenuItem<String>>(
                      (String preset){
                        return DropdownMenuItem<String>(
                          value: preset,
                          child: Text(preset),
                        );
                      }
                    ).toList(),
                    onChanged: (preset){
                      invokeOnPlatform("setEQPreset", {"preset":presets.indexOf(preset)});
                    },
                  ),
                  Flexible(
                    child: StatefulBuilder(
                      builder: (context,setState){
                        int bands =eqSetting["bandNum"];
                        List<Widget> bandSliders=[];
                        for(int i=0;i<bands;i++){
                          bandSliders.add(
                            FlutterSlider(
                              rtl: true,
                              axis: Axis.vertical,
                              values: [eqSetting["bandLvl"+i.toString()].toDouble()],
                              max: eqSetting["bandMaxRange"].toDouble(),
                              min: eqSetting["bandMinRange"].toDouble(),
                              onDragCompleted: (index,lower,upper){
                                eqSetting["bandLvl"+i.toString()]=lower;
                                eqSetting=eqSetting.map((key,value){
                                  print(value);
                                  return MapEntry(key, value.toInt());
                                });
                                invokeOnPlatform("setEQ", {"eqSetting":eqSetting});
                              },
                            ) 
                          );
                        }
                        return Row(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                          children: bandSliders,
                        );
                      },
                    )
                  )
                ] 
              );
            } else{
              return Center(
                child: Text("No Data"),
              );
            }

          },
        ),
      ),
    );
  }

}
