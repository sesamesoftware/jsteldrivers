package com.relationaljunction.database.io;

public class FileManagerEvent {
  public final static int ALL_EVENTS = 0;
  public final static int FILE_CHANGED = 1;
  private FileManager fm = null;
  private int eventCode = ALL_EVENTS;

  public FileManagerEvent(FileManager fm, int eventCode) {
    this.fm = fm;
    this.eventCode = eventCode;
  }

  public int getEventCode() {
    return eventCode;
  }

  public FileManager getFileManager() {
    return fm;
  }

}