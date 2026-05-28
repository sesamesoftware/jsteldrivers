package com.relationaljunction.utils;

import java.io.*;

public class FileExtensionFilter implements FileFilter {
   String extension = null;
   String templateName = null;

   public FileExtensionFilter(String extension, String templateName) {
      this.extension = extension;

      if (this.extension != null && this.extension.isEmpty())
         this.extension = ".";

      if (templateName != null && !templateName.trim().isEmpty()) {
         this.templateName = templateName;

         // check if templateName has already a file extension in it (e.g. some.dbf).
         // then remove it
         if (("." + StringUtils.getFileExtension(this.templateName))
                 .equalsIgnoreCase(this.extension)) {
            this.templateName = StringUtils.getFileNameWithoutExtension(
                    templateName);
         }
      }
   }

   public boolean accept(String pathname) {
      return accept(new File(pathname));
   }


   /**
    * Tests whether or not the specified abstract pathname should be
    * included in a pathname list.
    *
    * @param pathname The abstract pathname to be tested
    * @return <code>true</code> if and only if <code>pathname</code>
    *         should be included
    */
   public boolean accept(File pathname) {
      if (pathname.isDirectory())
         return false;

//      String fileName = pathname.getName();
      String fileExtension = "." +
              StringUtils.getFileExtension(pathname.toString());

      if (extension == null || fileExtension.equalsIgnoreCase(extension)) {
         if (templateName == null) {
            return true;
         } else {
            return StringUtils.isLike(StringUtils.getFileNameWithoutExtension(
                    pathname.getPath()).toLowerCase(), templateName.toLowerCase(), '%', '_');
         }
      }

      return false;
   }
}
