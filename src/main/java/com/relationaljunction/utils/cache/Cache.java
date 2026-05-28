package com.relationaljunction.utils.cache;

import java.util.concurrent.ExecutionException;

public interface Cache<T extends ObjectInfo> {
   T get(String key) throws ExecutionException, InterruptedException;

   void set(String key, T info) throws ExecutionException, InterruptedException;

   void remove(String key);

   void clear();
}
