import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'dart:async';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:flutter_reorderable_list/flutter_reorderable_list.dart';
import 'main.dart' as main;

const platform = const MethodChannel("me.devsilver.musicpiped/PlayerChannel");

class QueueScreen extends StatefulWidget{

  final List q;
  final int currentIndex; 

  QueueScreen(this.q,this.currentIndex);

  @override
  State<StatefulWidget> createState() {
    return QueueState(q,currentIndex);
  }

}
class QueueState extends State<QueueScreen>{

  List realq;
  List q;//dragging q
  int currentIndex;

  int olddragpos=0;
  bool dragging=false;
  
  QueueState(this.realq,this.currentIndex){
    q=List.from(realq);
    
  }

  int indexOfTitle(ValueKey title){
    for(int i=0;i<q.length;i++){
      if(ValueKey(q[i]["title"])==title){
        return i;
      }
    }
    return 0;
  }

  rebuildQ(){
    setState(() {
          
        });
  }

  @override
  Widget build(BuildContext context) {
    
    List<int> items = new List();
    for(int i=0;i<q.length;i++){
      items.add(i);
    }

    return WillPopScope(
      onWillPop: (){
        Navigator.pop(context);
      },
      child: Scaffold(
        
        body: SafeArea(
          child: ReorderableList(
            onReorder: (Key old,Key newk){
              int dragindex=indexOfTitle(old);
              int newPosindex = indexOfTitle(newk);
              if(!dragging){
                olddragpos=dragindex;
                dragging=true;
              }
              var item =q.removeAt(dragindex);
              q.insert(newPosindex, item);
              setState(() {  
                            });
              return true;
            },
            onReorderDone: (keynew){
              int oldindex = olddragpos;
              int newindex = indexOfTitle(keynew);
              dragging=false;
              platform.invokeMethod("reorderqueue",{"oldpos":oldindex,"newpos":newindex});
            },
            child: CustomScrollView(
              slivers: <Widget>[

                SliverAppBar(
                  expandedHeight: 150,
                  pinned: true,
                  flexibleSpace: FlexibleSpaceBar(
                    title: Text("Queue"),
                    background: Container(
                      color: Theme.of(context).primaryColor,
                    ),
                  ),
                ),
                SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (BuildContext context, int i){
                      return ReorderableItem(
                        key: ValueKey(q[i]["title"]),
                        childBuilder: (BuildContext context, ReorderableItemState state){
                          if(state==ReorderableItemState.placeholder){
                            return Card(
                              child: ListTile(),
                            );
                          }else{
                            return Slidable(
                              key: ValueKey(q[i]["title"]),
                              delegate: SlidableDrawerDelegate(),
                              actions: <Widget>[
                                IconSlideAction(
                                  icon: Icons.delete,
                                  color: Colors.red,
                                  caption: "Remove from Queue",
                                  onTap: ()async{
                                    q.removeAt(i);
                                    await platform.invokeMethod("removefromQueue",{"index":i});
                                    
                                    setState(() {
                                      currentIndex=main.bCast["currentIndex"];
                                    });
                                  },
                                )
                              ],
                              slideToDismissDelegate: SlideToDismissDrawerDelegate(
                                onDismissed: (type)async{
                                    q.removeAt(i);
                                    await platform.invokeMethod("removefromQueue",{"index":i});
                                    
                                    setState(() {
                                      currentIndex=main.bCast["currentIndex"];
                                    });
                                }
                              ),
                              child: InkWell(
                                onTap: (){
                                  platform.invokeMethod("playIndex",{"index":i});
                                },
                                child: Card(
                                  child: ListTile(
                                    leading: i-currentIndex==0?Icon(Icons.play_arrow):Text((i-currentIndex).toString()),
                                    title: Text(q[i]["title"]),
                                    trailing: ReorderableListener(
                                      child: Container(
                                        padding: EdgeInsets.only(left: 18,right: 18),
                                        child: Icon(Icons.reorder)
                                      ),

                                    ),
                                  ),
                                ),
                              ),
                            );
                          }
                        }, 
                      );
                    },
                    childCount: q.length
                  ),
                )
              ],
            ),
          )
            
        )
      ),
    );
  }

}


Future<dynamic> invokeOnPlatform(String method,dynamic arg) async {
  try{
    print("INVOKING METHOD $method");
    return await platform.invokeMethod(method,arg);
  } on PlatformException catch(e){
      print(e);
  }
}