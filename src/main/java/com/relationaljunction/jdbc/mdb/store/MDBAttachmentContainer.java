package com.relationaljunction.jdbc.mdb.store;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.relationaljunction.utils.FileUtils;
import com.relationaljunction.utils.StringUtils;

public class MDBAttachmentContainer implements Serializable {
   public static class Attachment implements Serializable {
      private final byte[] data;
      private String fileName = "noname", fileType = "", fileUrl = "";
      private Integer fileFlags;
      private Date fileTimeStamp = new Date();

      public Attachment(byte[] data) {
         this.data = data;
         fileName += StringUtils.generateRandomString(4);
      }

      public byte[] getData() {
         return data;
      }

      public String getFileName() {
         return fileName;
      }

      public void setFileName(String fileName) {
         this.fileName = fileName;
      }

      public Integer getFileFlags() {
         return fileFlags;
      }

      public void setFileFlags(Integer fileFlags) {
         this.fileFlags = fileFlags;
      }

      public Date getFileTimeStamp() {
         return fileTimeStamp;
      }

      public void setFileTimeStamp(Date fileTimeStamp) {
         this.fileTimeStamp = fileTimeStamp;
      }

      public String getFileType() {
         return fileType;
      }

      public void setFileType(String fileType) {
         this.fileType = fileType;
      }

      public String getFileUrl() {
         return fileUrl;
      }

      public void setFileUrl(String fileUrl) {
         this.fileUrl = fileUrl;
      }
   }

   private final List<Attachment> attachments;

   public MDBAttachmentContainer() {
      attachments = new ArrayList<Attachment>();
   }

//   public MDBAttachmentContainer(List<Attachment> attachments) {
//      this.attachments = attachments;
//   }

   void addAttachment(Attachment attachment) {
      attachments.add(attachment);
   }

//   public void addAttachment(byte[] data) {
//      attachments.add(new Attachment(data));
//   }

   public void addAttachment(byte[] data, String fileName, String fileUrl) {
      Attachment attachment = new Attachment(data);
      attachment.setFileName(fileName);
      attachment.setFileUrl(fileUrl);
      attachment.setFileType(StringUtils.getFileExtension(fileName));
      attachments.add(attachment);
   }

   public void addAttachment(File file) throws IOException {
      Attachment attachment = new Attachment(FileUtils.fileContentToBytes(file.getPath()));
      attachment.setFileName(file.getName());
      attachment.setFileUrl(file.getPath());
      attachment.setFileType(StringUtils.getFileExtension(file.getName()));
      attachments.add(attachment);
   }

   public void clear() {
      attachments.clear();
   }

   public List<Attachment> getAttachments() {
      return attachments;
   }

   @Override
   public String toString() {
      StringBuilder result = new StringBuilder("Attachments(");
      for (Attachment attachment : getAttachments()) {
         result.append(attachment.getFileName()).append(",").
                 append(attachment.getFileType()).append(",").
                 append(attachment.getFileUrl()).append(",");
         result.append("; ");
      }

      result.append(")");

      return result.toString();
   }
}
