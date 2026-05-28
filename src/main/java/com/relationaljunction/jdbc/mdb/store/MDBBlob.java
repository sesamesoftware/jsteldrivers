package com.relationaljunction.jdbc.mdb.store;

import com.healthmarketscience.jackcess.util.OleBlob;
import com.relationaljunction.utils.UnexpectedException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

public class MDBBlob implements Blob, Closeable {
   OleBlob oleBlob;
   OleBlob.ContentType type;
   OleBlob.Content content;

   public MDBBlob(OleBlob oleBlob) throws IOException {
      this.oleBlob = oleBlob;
      this.type = oleBlob.getContent().getType();
      this.content = oleBlob.getContent();
   }

   public void free() throws SQLException {
      oleBlob.free();
   }

   public long length() throws SQLException {
      return ((OleBlob.EmbeddedContent) content).length();
   }

   public byte[] getBytes(long pos, int length) throws SQLException {
      return oleBlob.getBytes(pos, length);
   }

   public InputStream getBinaryStream() throws SQLException {
      try {
         if (type == OleBlob.ContentType.UNKNOWN) {
            return oleBlob.getBinaryStream();
         } else if (type == OleBlob.ContentType.LINK) {
            return ((OleBlob.LinkContent) content).getLinkStream();
         } else {
            return ((OleBlob.EmbeddedContent) content).getStream();
         }
      } catch (Exception e) {
         throw new UnexpectedException("Can't get a stream of the content for an OLE column", e);
      }
   }

   public long position(byte[] pattern, long start) throws SQLException {
      return oleBlob.position(pattern, start);
   }

   public long position(Blob pattern, long start) throws SQLException {
      return oleBlob.position(pattern, start);
   }

   public int setBytes(long pos, byte[] bytes) throws SQLException {
      return oleBlob.setBytes(pos, bytes);
   }

   public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
      return oleBlob.setBytes(pos, bytes, offset, len);
   }

   public OutputStream setBinaryStream(long pos) throws SQLException {
      return oleBlob.setBinaryStream(pos);
   }

   public void truncate(long len) throws SQLException {
      oleBlob.truncate(len);
   }

   public InputStream getBinaryStream(long pos, long length) throws SQLException {
      return oleBlob.getBinaryStream(pos, length);
   }

   public void close() throws IOException {
      oleBlob.close();
   }
}
