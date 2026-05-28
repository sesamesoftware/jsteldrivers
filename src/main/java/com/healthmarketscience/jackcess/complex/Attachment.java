/*
Copyright (c) 2011 James Ahlborn

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
USA
*/

package com.healthmarketscience.jackcess.complex;

import java.io.IOException;
import java.util.Date;

/**
 * Complex value corresponding to an attachment.
 *
 * @author James Ahlborn
 */
public interface Attachment extends ComplexValue 
{
  byte[] getFileData() throws IOException;

  void setFileData(byte[] data);

  byte[] getEncodedFileData() throws IOException;

  void setEncodedFileData(byte[] data);

  String getFileName();

  void setFileName(String fileName);
  
  String getFileUrl();

  void setFileUrl(String fileUrl);
  
  String getFileType();

  void setFileType(String fileType);
  
  Date getFileTimeStamp();

  void setFileTimeStamp(Date fileTimeStamp);
  
  Integer getFileFlags();

  void setFileFlags(Integer fileFlags);
}
