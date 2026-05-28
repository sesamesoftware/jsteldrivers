package com.relationaljunction.database.io;

import com.relationaljunction.utils.cache.ObjectInfo;

public class FileObjectInfo
        implements ObjectInfo {
   FileManager fileManager = null;
   boolean updated = false;

   public FileObjectInfo() {
   }

   public FileObjectInfo(boolean updated) {
      setUpdated(updated);
   }

   public boolean isUpdated() {
      return updated;
   }

   public void setUpdated(boolean updated) {
      this.updated = updated;
   }

   public FileManager getFileManager() {
      return fileManager;
   }

   public void setFileManager(FileManager fileManager) {
      this.fileManager = fileManager;
   }

   public String toString(){
      return fileManager.getPath();
   }
}