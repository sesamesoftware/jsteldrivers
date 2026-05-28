package com.relationaljunction.utils.concurrency;

import java.util.concurrent.*;

public class ConcurrentUtils {

   public static <K, V> V futurePutToMapIfAbsent(ConcurrentMap<K, FutureTask<V>> concurrentMap,
                                                 K key, Callable<V> initCall)
           throws ExecutionException, InterruptedException, CancellationException {

      final FutureTask<V> futureTask = new FutureTask<V>(initCall);
      Future<V> future = concurrentMap.putIfAbsent(key, futureTask);

      if (future == null) {
         future = futureTask;
         futureTask.run();
      }

      try {
         return future.get();
      } catch (CancellationException e) {
         concurrentMap.remove(key);
         throw e;
      }
   }

   public static <K, V> void futurePutToMap(ConcurrentMap<K, FutureTask<V>> concurrentMap,
                                            K key, Callable<V> initCall)
           throws ExecutionException, InterruptedException, CancellationException {

      FutureTask<V> futureTask = new FutureTask<V>(initCall);
      concurrentMap.putIfAbsent(key, futureTask);
   }


   public static void joinThreads(Thread... threads) throws InterruptedException {
      for (Thread thread : threads) {
         thread.join();
      }
   }


   public static void executeTaskForLimitedTime(Callable<Void> task, int milliSeconds)
           throws ExecutionException, TimeoutException, InterruptedException {
      ExecutorService service = Executors.newSingleThreadExecutor();

      try {
         Future<?> f = service.submit(task);

         // attempt the task for some time
         f.get(milliSeconds, TimeUnit.MILLISECONDS);
//         catch (final InterruptedException e) {
//             The thread was interrupted during sleep, wait or join
//         }
//         catch (final TimeoutException e) {
//             Took too long!
//         }
//         catch (final ExecutionException e) {
//             An exception from within the Runnable task
//         }
      } finally {
         service.shutdown();
      }
   }

}
