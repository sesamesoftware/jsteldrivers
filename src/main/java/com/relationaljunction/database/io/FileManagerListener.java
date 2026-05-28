package com.relationaljunction.database.io;

import java.util.concurrent.ExecutionException;

public interface FileManagerListener {
  void fileManagerEventHappened(FileManagerEvent event) throws ExecutionException, InterruptedException;
}