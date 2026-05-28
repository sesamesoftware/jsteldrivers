package com.relationaljunction.jdbc.common.h2;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WatchModificationsThread extends Thread {
   private final Logger log = LoggerFactory.getLogger("WatchModificationsThread");

   protected CommonConnection2 conn;
   protected int checkPeriod = 10000;
   protected volatile boolean stopped = false;
   protected volatile boolean busy = false;

   public WatchModificationsThread(CommonConnection2 conn,
                                   int checkPeriod) {
      super("WatchModifications");
      this.conn = conn;
      this.checkPeriod = checkPeriod;
   }

   public void run() {
      try {
         com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
                 "WatchModificationsThread: thread is started.");

         while (!stopped) {
            sleep(checkPeriod);

            checkInterrupted();

            Collection<CacheTable> tablesSorted = conn.getCacheTableManager().getSortedCacheTables();

            for (CacheTable operTable : tablesSorted) {
               try {
                  checkInterrupted();
                  operTable.refreshTableModificationDate();
               } catch (Exception ex) {
                  if (conn.isClosed()) {
                     log.warn("WatchModificationsThread: connection is already closed. " +
                             "WatchModificationsThread is shuttind down.");
                     com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
                             "WatchModificationsThread: thread is done.");
                     conn = null;
                     return;
                  }

                  log.warn("Error in WatchModificationsThread while refreshing a table: "
                          + ex.getMessage(), ex);
               }
            }
         }
      } catch (InterruptedException ex) {
         log.warn("WatchModificationsThread has been interrupted");
//      interrupt();
      } catch (Exception ex) {
//      ex.printStackTrace();
         log.warn("Error in WatchModificationsThread: " + ex.getMessage(), ex);
      }

      com.relationaljunction.utils.OtherUtils.writeLogInfo(log,
              "WatchModificationsThread: thread is done.");

      conn = null;
   }

   private void checkInterrupted() throws InterruptedException {
      if (stopped || interrupted())
         throw new InterruptedException("WatchModificationsThread has been interrupted");
   }

   public boolean isBusy() {
      return busy;
   }

   public void stopThread() {
      stopped = true;
   }

}
