package com.relationaljunction.utils.concurrency;

import java.util.Iterator;

public class ConcurrentIteratorHandler<T_Iterator_Element> {
  private int maxConcurrentThreads = 5;
  private Iterator<T_Iterator_Element> iterator = null;
  private HandlerIF<T_Iterator_Element> handler = null;
  private volatile int currentHandlers = 0;

  public ConcurrentIteratorHandler(Iterator<T_Iterator_Element> iterator,
                                   HandlerIF<T_Iterator_Element> handler,
      int maxConcurrentThreads) {
    this.maxConcurrentThreads = maxConcurrentThreads;
    this.iterator = iterator;
    this.handler = handler;
  }

  private class HandlerThread extends Thread {
    T_Iterator_Element element;

    HandlerThread(T_Iterator_Element element) {
      this.element = element;
    }

    public void run() {
      try {
        currentHandlers++;
        handler.handle(element);
	currentHandlers--;
      } catch (Exception ex) {
      }
    }
  }

  public void runProcess(){
    try {
      while (iterator.hasNext()) {
        T_Iterator_Element element = iterator.next();
        while (currentHandlers >= maxConcurrentThreads) {
	  Thread.sleep(500);
        }
      }
    } catch (Exception ex) {
    }
  }

  public static void main(String[] args) {


  }
}
