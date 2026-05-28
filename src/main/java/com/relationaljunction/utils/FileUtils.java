package com.relationaljunction.utils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;

import com.relationaljunction.utils.concurrency.*;

/**
 * <p>Title: StelsCSV JDBC driver</p>
 * <p>Copyright: Copyright (c) J-Stels Software 2002-2004</p>
 * <p>Company: J-Stels Software</p>
 *
 * @author J-Stels Software
 * @version 2.0
 */

public class FileUtils {
   final static int BLOCK = 8192;

   public static String fileContentToString(Reader is) throws IOException {
      StringBuilder sb = new StringBuilder();
      char[] b = new char[BLOCK];
      int n;
      while ((n = is.read(b)) > 0)
         sb.append(b, 0, n);
      return sb.toString();
   }

   public static String fileContentToString(String filePath) throws IOException {
      Reader reader = new FileReader(filePath);
      String content = fileContentToString(reader);
      reader.close();
      return content;
   }

   public static byte[] fileContentToBytes(String filePath) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      copyStream(new FileInputStream(filePath), baos);
      return baos.toByteArray();
   }

   public static void copyFile(File source, File target) throws IOException {
      copyFile(source, target, 1024);
   }

   public static void copyFile(String source, String target) throws IOException {
      copyFile(new File(source), new File(target), 1024);
   }

   public static void saveToFile(String fileName, byte[] byteArray, boolean append)
           throws IOException {
      FileOutputStream fos = new FileOutputStream(fileName, append);
      fos.write(byteArray);
      fos.close();
   }

   public static void saveToFile(String fileName, InputStream is, boolean append)
           throws IOException {
      FileOutputStream fos = new FileOutputStream(fileName, append);
      copyStream(is, fos);
   }

   public static void copyFile(File source, File target, int bufferSize)
           throws IOException {
      FileInputStream in = new FileInputStream(source);
      FileOutputStream out = new FileOutputStream(target);
      copyStream(in, out, bufferSize);
   }

   public static void copyStream(InputStream in,
                                 OutputStream out)
           throws IOException {
      copyStream(in, out, 1024);
   }

   public static void copyStream(InputStream in,
                                 OutputStream out, int bufferSize)
           throws IOException {
      byte[] buf = new byte[bufferSize];
      int r;
      while ((r = in.read(buf)) != -1) {
         out.write(buf, 0, r);
      }
      in.close();
      out.close();
   }

   public static void concurrentCopyStream(InputStream in, OutputStream out,
                                           int bufferSize, int queueCapacity) throws
           Exception {
      SingleConsumerProducer<byte[], BytesConsumer, BytesProducer>
              consumerProducer = new SingleConsumerProducer<byte[], BytesConsumer,
              BytesProducer>(queueCapacity);

      consumerProducer.setConsumer(new BytesConsumer(out));
      consumerProducer.setProducer(new BytesProducer(in, bufferSize));
      consumerProducer.setThreadPrefixName("streamCopy");
      consumerProducer.runProcess();

      in.close();
      out.close();
   }

   public static void listFilesForFolder(final File folder,
                                  boolean recursive,
                                  CallbackHandler<File> callbackHandler) {
      for (final File fileEntry : folder.listFiles()) {
         if (fileEntry.isDirectory() && recursive) {
            listFilesForFolder(fileEntry, recursive, callbackHandler);
         } else {
            callbackHandler.process(fileEntry);
         }
      }
   }

   static class BytesConsumer implements ConsumerIF<byte[]> {
      private OutputStream out = null;

      BytesConsumer(OutputStream out) {
         this.out = out;
      }

      public void consume(byte[] buf) throws Exception {
         out.write(buf, 0, buf.length);
      }
   }

   static class BytesProducer implements ProducerIF<byte[]> {
      private InputStream in = null;
      private int bufferSize = 8192;

      BytesProducer(InputStream in, int bufferSize) {
         this.in = in;
         this.bufferSize = bufferSize;
      }

      public byte[] produce() throws Exception {
         byte[] buf = new byte[bufferSize];

         int bytesRead = in.read(buf);
         if (bytesRead == -1)
            return null;

         if (bytesRead < bufferSize) {
            byte[] buf2 = new byte[bytesRead];
            System.arraycopy(buf, 0, buf2, 0, bytesRead);
            return buf2;
         }

         return buf;
      }
   }

   public static void clearDir(File dir) throws IOException {
      if (!dir.exists())
         throw new IOException("Dir doesn't exist");
      if (!dir.isDirectory())
         throw new IOException("Variable dir is not a directory");
      File[] files = dir.listFiles();
      for (File file : files) file.delete();
   }

   public static boolean clearAllDir(File dir) throws IOException {
      if (dir.isDirectory()) {
         String[] subdirs = dir.list();
         for (String subdir : subdirs) {
            boolean success = clearAllDir(new File(dir, subdir));
            if (!success) {
               return false;
            }
         }
      }

      // The directory is now empty so delete it
      return dir.delete();
   }

   public static void copyFile(File source, File target, String charset) throws IOException {
      BufferedReader in = new BufferedReader(new InputStreamReader(new
              FileInputStream(source), "cp866"));

      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new
              FileOutputStream(target), charset));
      int r = 0;
      while ((r = in.read()) != -1) {
         out.write((char) r);
      }
      out.flush();
      in.close();
      out.close();
   }

   public static String CreateRandomSubDir(String dirPath) {
      String subDir = null;
      if (dirPath != null)
         subDir = dirPath;
      else
         subDir = new File(System.getProperty("java.io.tmpdir")).getPath();

      do {
         subDir = subDir + File.separator +
                 StringUtils.generateRandomString(4);
      }
      while (new File(subDir).exists());

      new File(subDir).mkdirs();
      return subDir;
   }

   public static long calculateFolderSize(String folder, String[] fileExlusions) {
      HashSet fileExlusionsSet = new HashSet();
      for (int i = 0; i < fileExlusions.length; i++)
         fileExlusionsSet.add(fileExlusions[i].toUpperCase());

      File fTest = new File(folder);
      File[] testFiles = fTest.listFiles();

      long sum = 0;
      for (int i = 0; i < testFiles.length; i++) {
         if (!fileExlusionsSet.contains(testFiles[i].getName().toUpperCase()))
            sum = sum + testFiles[i].length();
      }
      return sum;
   }

   public static FileLock lockChannel(FileChannel fileChannel, boolean sharedLock,
                                      int lockCheckPeriod, int lockTimeOut)
           throws Exception {
      int time = 0;

      FileLock lock = null;

      if (sharedLock) {
         while ((lock = fileChannel.tryLock(0, Long.MAX_VALUE, true)) == null) {
            Thread.sleep(lockCheckPeriod);
            //	System.out.println("waiting... time=" + time);
            time += lockTimeOut;
            if (lockTimeOut != 0 && time > lockTimeOut)
               throw new Exception("Can't apply a shared lock for the file. Lock time is out!");
         }
      } else {
         while ((lock = fileChannel.tryLock()) == null) {
            Thread.sleep(lockCheckPeriod);
            //	System.out.println("waiting... time=" + time);
            time += lockTimeOut;
            if (lockTimeOut != 0 && time > lockTimeOut)
               throw new Exception("Can't apply an exclusive lock for the file. Lock time is out!");
         }
      }

      return lock;
   }


   public static FileLock lockFile(File f,
                                   boolean sharedLock,
                                   int lockCheckPeriod,
                                   int lockTimeOut,
                                   Vector<RandomAccessFile> rafResult) throws Exception {
      if (!f.exists())
         throw new IOException("File '" + f.getPath() + "' does not exist.");

      int time = 0;
      FileLock lock;

      while (true) {
         // check if a file is not locked by another process including a shared mode.
         // Otherwise it will throw FileNotFoundException while getting Input(Output)Stream
         // or RandomAccessFile

/*
         if (f.renameTo(f)) {
            if (sharedLock) {
               RandomAccessFile raf = new RandomAccessFile(f, "r");
               // get a shared lock
               lock = raf.getChannel().tryLock(0, Long.MAX_VALUE, true);
               rafResult.add(raf);
               break;
            } else {
               RandomAccessFile raf = new RandomAccessFile(f, "rw");
               // get an exclusive lock
               lock = raf.getChannel().tryLock();
               rafResult.add(raf);
               break;
            }
         }
*/

         if (f.renameTo(f)) {
            if (sharedLock) {
               RandomAccessFile raf = null;

               boolean locked = false;
               try {
                  raf = new RandomAccessFile(f, "r");
               } catch (FileNotFoundException e) {
                  // exception. Thus the file is locked
                  locked = true;
               }

               // get a shared lock
               if (!locked) {
                  lock = raf.getChannel().tryLock(0, Long.MAX_VALUE, true);
                  rafResult.add(raf);
                  break;
               }

            } else {
               RandomAccessFile raf = null;

               boolean locked = false;
               try {
                  raf = new RandomAccessFile(f, "rw");
               } catch (FileNotFoundException e) {
                  // exception. Thus the file is locked
                  locked = true;
               }

               // get an exclusive lock
               if (!locked) {
                  lock = raf.getChannel().tryLock();
                  rafResult.add(raf);
                  break;
               }
            }
         }

         Thread.sleep(lockCheckPeriod);
         time += lockCheckPeriod;
         if (time > lockTimeOut)
            throw new Exception("Can't get a " + (sharedLock ? "shared" : "exclusive") +
                    " lock for the file '" + f.getName() + "'. Lock time is out!");
      }

      return lock;
   }

   public static FileLock lockFile(File f,
                                   int lockCheckPeriod,
                                   int lockTimeOut,
                                   Vector<InputStream> isResult) throws Exception {
      if (!f.exists())
         throw new IOException("File '" + f.getPath() + "' does not exist.");

      int time = 0;
      FileLock lock;

      while (true) {
         // check if a file is not locked by another process including a shared mode.
         // Otherwise it will throw FileNotFoundException while getting Input(Output)Stream
         // or RandomAccessFile
         if (f.renameTo(f)) {
            FileInputStream is = null;

            boolean locked = false;
            try {
               is = new FileInputStream(f);
            } catch (FileNotFoundException e) {
               // exception. Thus the file is locked
               locked = true;
            }

            // get a shared lock
            if (!locked) {
               lock = is.getChannel().tryLock(0, Long.MAX_VALUE, true);
               isResult.add(is);
               break;
            }
         }

         Thread.sleep(lockCheckPeriod);
         time += lockCheckPeriod;
         if (time > lockTimeOut)
            throw new Exception("Can't get a shared lock for the file '" + f.getName() +
                    "'. Lock time is out!");
      }

      return lock;
   }

}
