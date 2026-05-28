package com.relationaljunction.utils.concurrency;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;

/**
 * The class stores Future's as hash values.
 * It is used to calculate delayed values (tasks) asynchronously in multi-thread environment
 * Safe object publication with map
 * @param <K>
 * @param <V>
 */
public class FutureConcurrentHashMap<K, V> {

   private final ConcurrentHashMap<K, Future<V>> map;


   public FutureConcurrentHashMap() {
      this.map = new ConcurrentHashMap<K, Future<V>>();
   }

   public FutureConcurrentHashMap(int size) {
      this.map = new ConcurrentHashMap<K, Future<V>>(size);
   }

   /**
    * @param key
    * @param initCall
    * @return returns a new initialized value if there was no mapping for key, or
    *         the value contained in a map.
    * @throws ExecutionException
    * @throws InterruptedException
    */
   public V putIfAbsent(K key, Callable<V> initCall)
           throws ExecutionException, InterruptedException {
      final FutureTask<V> futureTask = new FutureTask<V>(initCall);
      Future<V> future = map.putIfAbsent(key, futureTask);

      if (future == null) {
         // initializing new value
         future = futureTask;
         futureTask.run();
      }

      try {
         return future.get();
      } catch (CancellationException e) {
         map.remove(key);
         throw e;
      }
   }

   public V put(K key, final V value) throws ExecutionException, InterruptedException {
      Future<V> result = map.put(key, new FutureStub<V>(value));
      return result == null ? null : result.get();
   }

   public V get(K key) throws ExecutionException, InterruptedException {
      Future<V> future = map.get(key);
      return future == null ? null : future.get();
   }

   public boolean containsKey(K key) throws ExecutionException, InterruptedException {
      return get(key) != null;
   }

   public void remove(K key) {
      map.remove(key);
   }

   public Enumeration<V> elements() throws ExecutionException, InterruptedException {
      Enumeration<Future<V>> elements = map.elements();
      List<V> initializedElements = new Vector<V>();

      while (elements.hasMoreElements()) {
         initializedElements.add(elements.nextElement().get());
      }

      return Collections.enumeration(initializedElements);
   }

   public int size() {
      return map.size();
   }

   public void clear() {
      map.clear();
   }

   public String toString() {
      StringBuilder sb = new StringBuilder();

      Enumeration<K> enumeration = map.keys();
      while (enumeration.hasMoreElements()) {
         K key = enumeration.nextElement();
         try {
            sb.append("key = '").append(key).append("' value ='").append(get(key)).append("'; ");
         } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         } catch (ExecutionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         }
      }

      return sb.toString();
   }

   public static void main(String[] args) {
      try {
         FutureConcurrentHashMap<String, String> map = new FutureConcurrentHashMap<String, String>();

         map.put("key1", "value1");
         map.put("key2", "value2");

         System.out.println(map.get("key1"));
         System.out.println(map.get("key2"));

         map.put("key2", "value2_updated");

         System.out.println(map.get("key2"));

         map.putIfAbsent("key3", new Callable<String>() {
            public String call() throws Exception {
               System.out.println("---loading a value---");

               return "value3";
            }
         });

         System.out.println(map.get("key3"));
         System.out.println(map.get("key3"));
      } catch (ExecutionException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (InterruptedException e) {
         e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
   }
}
