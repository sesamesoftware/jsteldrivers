package com.relationaljunction.jdbc.common.h2;

import java.util.concurrent.*;

import com.relationaljunction.database.view.*;
import com.relationaljunction.utils.concurrency.FutureConcurrentHashMap;

public class Views {
   private final FutureConcurrentHashMap<String, ViewTableIF> viewMap =
           new FutureConcurrentHashMap<String, ViewTableIF>(32);


   public Views() {
   }

   void put(ViewTableIF view) throws ExecutionException, InterruptedException {
       viewMap.put(getKey(view.getName()), view);
   }

   ViewTableIF putIfAbsent(String viewName, Callable<ViewTableIF> initCall)
           throws ExecutionException, InterruptedException {
      return viewMap.putIfAbsent(getKey(viewName), initCall);
   }

   ViewTableIF get(String viewName) throws ExecutionException, InterruptedException {
      return viewMap.get(getKey(viewName));
   }

   void remove(String viewName) {
      viewMap.remove(getKey(viewName));
   }

   private String getKey(String viewName) {
      return CacheTableManager.getCacheTableName(viewName);
   }

   void clear() {
      viewMap.clear();
   }


//  void close(){
//    if (viewMap != null)
//      viewMap.clear();
//
//    viewMap = null;
//  }

   public static void main(String[] args) throws Throwable {
//      Views views = new Views();
//      views.add(new DefaultViewTable("", "select * from test"));
//      System.out.println(views.contains(""));
//      System.out.println(views.get("").getQuery());
//
//      views.add(new DefaultViewTable("a", "select * from test2"));
//      System.out.println(views.contains("a"));
//      System.out.println(views.get("a").getQuery());
   }


}
