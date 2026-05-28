package com.relationaljunction.utils.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureStub<T> implements Future<T> {
   private final T returnedObject;

   public FutureStub(T returnedObject) {
      this.returnedObject = returnedObject;
   }

   public boolean cancel(boolean mayInterruptIfRunning) {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public boolean isCancelled() {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public boolean isDone() {
      return false;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public T get() throws InterruptedException, ExecutionException {
      return returnedObject;
   }

   public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }
}
