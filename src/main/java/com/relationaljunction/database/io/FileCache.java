package com.relationaljunction.database.io;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.relationaljunction.utils.cache.*;
import com.relationaljunction.utils.concurrency.FutureConcurrentHashMap;

public class FileCache<T extends ObjectInfo> implements Cache<T> {
   private final FutureConcurrentHashMap<String, T> hashFiles =
           new FutureConcurrentHashMap<String, T>();

   public FileCache() {
   }

   public T get(String fileName) throws ExecutionException, InterruptedException {
      return hashFiles.get(fileName);
   }

   public void set(String fileName, T info) throws ExecutionException, InterruptedException {
      hashFiles.put(fileName, info);
   }

   public T putIfAbsent(String fileName, Callable<T> initCall) throws ExecutionException, InterruptedException {
      return hashFiles.putIfAbsent(fileName, initCall);
   }

   public boolean containsKey(String fileName) throws ExecutionException, InterruptedException {
      return hashFiles.containsKey(fileName);
   }

   public void remove(String key) {
      hashFiles.remove(key);
   }

   public FileObjectInfo[] getFileInfos() throws ExecutionException, InterruptedException {
      FileObjectInfo[] fileInfoArray = new FileObjectInfo[hashFiles.size()];
      Enumeration enumer = hashFiles.elements();
      int index = 0;
      while (enumer.hasMoreElements()) {
         FileObjectInfo fileInfo = (FileObjectInfo) enumer.nextElement();
         fileInfoArray[index] = fileInfo;
         index++;
      }
      return fileInfoArray;
   }

   public FileManager[] getFileManagers() throws ExecutionException, InterruptedException {
      FileManager[] fileManagers = new FileManager[hashFiles.size()];
      FileObjectInfo[] fileInfoArray = getFileInfos();
      for (int i = 0; i < fileInfoArray.length; i++)
         fileManagers[i] = fileInfoArray[i].getFileManager();
      return fileManagers;
   }

   public void clear() {
      hashFiles.clear();
   }

}
