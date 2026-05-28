package com.relationaljunction.utils.concurrency;

import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.OtherUtils;

public class SingleConsumerProducer<T_Interaction_Object,
        T_Consumer extends
                ConsumerIF<T_Interaction_Object>,
        T_Producer extends
                ProducerIF<T_Interaction_Object>> {
   private final Logger log = LoggerFactory.getLogger("SingleConsumerProducer");

   private final static String TERMINATION_SIGNAL = "t";
   private final static int QUEUE_CAPACITY = 50;
   private final static int WAIT_SECONDS = 50;
   private T_Consumer consumer;
   private T_Producer producer;
   private BlockingQueue queue = null;
   private int consumerPriority = Thread.NORM_PRIORITY;
   private int producerPriority = Thread.NORM_PRIORITY;
   private String threadPrefixName = null;

   public SingleConsumerProducer(int queueCapacity) {
      this(queueCapacity, Thread.NORM_PRIORITY, Thread.NORM_PRIORITY);
   }

   public SingleConsumerProducer(int queueCapacity, int consumerPriority,
                                 int producerPriority) {
      this.queue = new ArrayBlockingQueue(queueCapacity);
      this.consumerPriority = consumerPriority;
      this.producerPriority = producerPriority;
   }

   private class Consumer extends Thread {
      private String error;

      Consumer() {
         super(threadPrefixName + " consumer");
      }

      String getError() {
         return error;
      }

      public void run() {
//      if (log.isTraceEnabled())
//        log.trace("'consumer' is run");

         while (true) {
//	if(log.isTraceEnabled())
//	  log.trace("queue size = "+ queue.size());

            Object objectReceived = null;

            try {
               objectReceived = queue.poll(WAIT_SECONDS, TimeUnit.SECONDS);

               if (objectReceived == null) {
                  // time is out to wait
                  error = "Time is out in 'consumer' thread";
                  break;
               }

               // termination signal received
               if (objectReceived == TERMINATION_SIGNAL)
                  break;
            } catch (InterruptedException ex) {
               ex.printStackTrace();
               error = "Unexpected error in 'consumer' thread: " +
                       ex.getMessage();
               break;
            }

            try {
               consumer.consume((T_Interaction_Object) objectReceived);
            } catch (Exception ex) {
               ex.printStackTrace();
               error = "Error in 'consumer' thread: " +
                       ex.getMessage();
               break;
            }
         }

//      if (log.isTraceEnabled())
//        log.trace("'consumer' is done." +
//                  (error == null ? "" : " Error = " + error));
      }
   }


   private class Producer extends Thread {
      private String error;

      Producer() {
         super(threadPrefixName + " producer");
      }

      String getError() {
         return error;
      }

      public void run() {
         T_Interaction_Object objectProduced = null;

//      if (log.isTraceEnabled())
//        log.trace("'producer' is run");

         try {
            while ((objectProduced = producer.produce()) != null) {
               boolean result = queue.offer(objectProduced, WAIT_SECONDS,
                       TimeUnit.SECONDS);
               if (!result) {
                  // time is out to wait
                  error = "Time is out in 'producer' thread";
                  break;
               }
            }

            // termination signal
            boolean result = queue.offer(TERMINATION_SIGNAL, WAIT_SECONDS,
                    TimeUnit.SECONDS);
            if (!result) {
               // time is out to wait
               error =
                       "Time is out to send a termination signal in 'producer' thread";
            }
         } catch (Exception ex) {
//        ex.printStackTrace();
            try {
               queue.offer(TERMINATION_SIGNAL, WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex2) {
               ex2.printStackTrace();
            }
            error = "Error in 'producer' thread: " + ex.getMessage();
         }

//      if (log.isTraceEnabled())
//        log.trace("'producer' is done." +
//                  (error == null ? "" : " Error = " + error));
      }
   }


   public void setConsumer(T_Consumer consumer) {
      this.consumer = consumer;
   }

   public void setProducer(T_Producer producer) {
      this.producer = producer;
   }

   public void setThreadPrefixName(String threadPrefixName) {
      this.threadPrefixName = threadPrefixName;
   }

   public void runProcess() throws Exception {
      Producer producerThread = new Producer();
      Consumer consumerThread = new Consumer();

      try {
         producerThread.setPriority(producerPriority);
         producerThread.start();
         Thread.sleep(10);

         consumerThread.setPriority(consumerPriority);
         consumerThread.start();

         producerThread.join();
         consumerThread.join();


         OtherUtils.writeTraceInfo(log, "'producer' and 'consumer' is done" + (consumerThread.getError() == null ? "" :
                 ". Error in 'consumer': " + consumerThread.getError()) +
                 (producerThread.getError() == null ? "" : ". Error in 'producer': " + producerThread.getError()));
      } catch (Exception ex) {
         throw new Exception(
                 "Unexpected error in SingleConsumerProducer.runProcess(): " +
                         ex.getMessage());
      }

      if (consumerThread.getError() != null)
         throw new Exception(consumerThread.getError());

      if (producerThread.getError() != null)
         throw new Exception(producerThread.getError());
   }

}

