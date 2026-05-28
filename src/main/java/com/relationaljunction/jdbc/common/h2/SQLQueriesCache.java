package com.relationaljunction.jdbc.common.h2;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.jdbc.common.h2.sql.SQLCommand;
import com.relationaljunction.utils.OtherUtils;
import com.relationaljunction.utils.cache.LRUCache;

public class SQLQueriesCache {
    
   private static final Logger log = LoggerFactory.getLogger("SQLQueriesCache");

   private static final int CACHE_SIZE = 100;

   private final LRUCache<String, FutureTask<SQLCommand>> cache =
           new LRUCache<String, FutureTask<SQLCommand>>(CACHE_SIZE);

   private static class InitTask implements Callable<SQLCommand> {
      private final String query;

      public InitTask(String query) {
         this.query = query;
      }

      public SQLCommand call() throws Exception {
         OtherUtils.writeTraceInfo(log, "SQLQueriesCache: parsing " + query);

         return SQLCommand.parseSQLCommand(query);
      }
   }

   public SQLCommand parseSQLQuery(String query) throws Exception {
      SQLCommand command;
      FutureTask<SQLCommand> futureTask;

      synchronized (cache) {
         futureTask = cache.get(query);

         if (futureTask == null) {
            futureTask = new FutureTask<SQLCommand>(new InitTask(query));
            cache.put(query, futureTask);
         }

         OtherUtils.writeTraceInfo(log, "SQLQueriesCache: cache hit for the query " + query);
       }

      futureTask.run();
      command = futureTask.get();

      return command;
   }

}
