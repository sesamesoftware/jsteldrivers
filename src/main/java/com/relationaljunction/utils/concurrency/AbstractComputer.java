package com.relationaljunction.utils.concurrency;

import java.util.concurrent.Callable;

public abstract class AbstractComputer<K, V> implements Computable<K, V>, Callable<V> {
   private K key;

    abstract public V compute(K key) throws InterruptedException;

   public V call() throws Exception {
      return compute(key);
   }
}
