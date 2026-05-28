package com.relationaljunction.utils.concurrency;

public interface Computable<K, V> {

   V compute(K key) throws InterruptedException;

}