package com.relationaljunction.jdbc.common.h2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.utils.OtherUtils;

public class WatchSchemaModificationsThread extends WatchModificationsThread {
   private static final int PAUSE_TIME = 1000;

   private final Logger log = LoggerFactory.getLogger("WatchSchemaModificationsThread");

   public WatchSchemaModificationsThread(CommonConnection2 conn, int checkPeriod) {
      super(conn, checkPeriod);
   }

   public void run() {
      try {
         OtherUtils.writeLogInfo(log,
                 "WatchSchemaModificationsThread: thread is started.");

         while (!stopped) {
            sleep(checkPeriod);

            checkInterrupted();

            if (conn.getSchemaIF2().isModificationDateChanged()) {
               OtherUtils.writeTraceInfo(log, "WatchSchemaModificationsThread: schema modification date is changed");

               try {
                  conn.reloadCache();
               } catch (Exception e) {
                  if (conn.isClosed()) {
                     log.warn("WatchSchemaModificationsThread: connection is already closed. " +
                             "WatchSchemaModificationsThread is shuttind down.");
                     OtherUtils.writeLogInfo(log,
                             "WatchModificationsThread: thread is done.");
                     conn = null;
                     return;
                  }

                  log.warn("Error in WatchSchemaModificationsThread while reloading schema: "
                          + e.getMessage(), e);
               }

               sleep(PAUSE_TIME);
            }
         }
      } catch (InterruptedException ex) {
         log.warn("WatchSchemaModificationsThread has been interrupted");
      } catch (Exception ex) {
//      ex.printStackTrace();
         log.warn("Error in WatchSchemaModificationsThread: " + ex.getMessage(), ex);
      }

      OtherUtils.writeLogInfo(log,
              "WatchSchemaModificationsThread: thread is done.");

      conn = null;
   }

   private void checkInterrupted() throws InterruptedException {
      if (stopped || interrupted())
         throw new InterruptedException("WatchModificationsThread has been interrupted");
   }

}
