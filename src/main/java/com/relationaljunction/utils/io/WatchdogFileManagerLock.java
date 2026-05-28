package com.relationaljunction.utils.io;

import java.io.*;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.relationaljunction.database.io.FileManager;

public class WatchdogFileManagerLock {
	
   private final Logger log = LoggerFactory.getLogger("WatchdogFileManagerLock");

   private final FileManager lockFile;
   private final String randomStuff = "" + new Random().nextLong();

   private long interval = 200;
   private boolean locked = false;
   private boolean watchDogQuit = false;
   private final Object lock = new Object();


   private final Runnable watchDog = new Runnable() {
      public void run() {
         com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, WatchdogFileManagerLock.this.toString(),
                 ". watchdog (", this.toString(), "): started...");

         while (true)
            try {
               synchronized (lock) {
                  //sleep for interval, and than check if out file was modified
                  com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, WatchdogFileManagerLock.this.toString(),
                          ". watchdog (", this.toString(), "): waiting an interval...");

                  lock.wait(interval);

                  //check if file is going to be unlocked
                  if (watchDogQuit) {
                     watchDogQuit = false;
                     break;
                  }

                  if (wasFileModified()) {
                     //if yes, overwrite with our changes
                     createAndFillLockFile();
                     com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, WatchdogFileManagerLock.this.toString(),
                             ". watchdog (", this.toString(), "). Lock changed. wrote: ", randomStuff);
                  }
               }
            } catch (Exception e) {
               //this thread must keep spinning
               e.printStackTrace();
            }

         com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, WatchdogFileManagerLock.this.toString(),
                 ". watchdog (", this.toString(), "): exiting...");
      }
   };


   public WatchdogFileManagerLock(FileManager lockFile, int lockCheckInterval) {
      this.lockFile = lockFile;
      this.interval = lockCheckInterval;
   }

   public synchronized boolean tryLock() throws Exception {
      if (locked) return false;

      com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(),
              ": trying to lock... ");

      //create new file
      if (!lockFile.exists()) {
         //protect from race condition, give other watch dogs time to create file
         sleep(interval);
      }

      if (!lockFile.exists()) {
         //create new file and we are done
         try {
            lockFile.create();
         } catch (Exception e) {
            // file already exists
            com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(),
                    ": error creating lock file. Already exists. Exiting");
            return false;
         }

         createAndFillLockFile();
         com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(),
                 ": created a lock file with ", randomStuff);
      } else {
         //lock file exists, overwrite it with our data
         createAndFillLockFile();

         com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(),
                 " Already locked. Wrote: ", randomStuff, ". Waiting 4x interval");

         //wait for other watch dogs to overwrite file
         sleep(interval * 4);
         //check if file was modified by other watch dog
         if (wasFileModified()) {
            com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(),
                    " File was modified. Exiting with false.");
            return false;
         }
      }

      //start watch dog thread
      Thread watchDogThread = new Thread(watchDog);
      watchDogThread.setName("file lock: " + lockFile);
      watchDogThread.setDaemon(true);
      watchDogThread.start();

      locked = true;

      com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(),
              ": file is locked.");

      return true;
   }

   public void lock(int lockTimeOut) throws Exception {
      int time = 0;

      while (true) {
         boolean result = tryLock();

         if (result) {
            break;
         }

         sleep(interval);
         time += interval;

//         System.out.println(time);

         if (time > lockTimeOut)
            throw new IOException("Lock time is out!");
      }

      com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(), ": exit lock()");
   }

   public synchronized void unlock() throws Exception {
//      if (!locked) throw new IllegalAccessError("not locked");
      if (!locked) return;

      //stop watch dog
      synchronized (lock) {
         watchDogQuit = true;
         lock.notify();
      }


      if (lockFile.exists()) {
         lockFile.delete();
      }

      locked = false;

      com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(), ": unlock()");
   }

   private void createAndFillLockFile() throws Exception {
//      if (!lockFile.exists()) {
//         System.out.println(lockFile.getPath());
//         lockFile.create();
//      }
      OutputStream out = lockFile.getOutputStream(false);
      out.write(randomStuff.getBytes());
      out.close();
   }

   private boolean wasFileModified() throws Exception {
//      if (!lockFile.exists()) return true;

      InputStream in;
      try {
         in = lockFile.getInputStream();
      } catch (Exception e) {
         // file may be already deleted due to the network delay
         com.relationaljunction.utils.OtherUtils.writeTraceInfo(log, this.toString(), ": can't open an InputStream. " +
                 "Lock file does not exist.");
         return true;
//         e.printStackTrace();
//         throw new Exception(e);
      }

      String content = new BufferedReader(new InputStreamReader(in)).readLine();
      in.close();

//      if (!randomStuff.equals(content))
//         System.out.println(this.toString() + " lock was modified by: " + content +
//                 ". expected: " + randomStuff);

      return !randomStuff.equals(content);
   }

   private void sleep(long i) throws IOException {
      try {
         Thread.sleep(i);
      } catch (InterruptedException e) {
         throw new IOException(e);
      }
   }
}
