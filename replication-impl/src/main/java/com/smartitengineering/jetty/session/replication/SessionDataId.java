/*
 *
 * This module intended to be used for session replication of Jetty via HBase
 * and later will be cached via Ehcache
 *
 * Copyright (C) 2010  Imran M Yousuf (imyousuf@smartitengineering.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.smartitengineering.jetty.session.replication;

import com.smartitengineering.dao.impl.hbase.spi.Externalizable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author imyousuf
 */
public class SessionDataId implements Externalizable {

  private String id;
  private String canonicalContextPath;
  private String virtualHost;
  protected final transient Logger logger = LoggerFactory.getLogger(getClass());

  public String getCanonicalContextPath() {
    return canonicalContextPath;
  }

  public void setCanonicalContextPath(String canonicalContextPath) {
    this.canonicalContextPath = canonicalContextPath;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVirtualHost() {
    return virtualHost;
  }

  public void setVirtualHost(String virtualHost) {
    this.virtualHost = virtualHost;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SessionDataId other = (SessionDataId) obj;
    if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
      return false;
    }
    if ((this.canonicalContextPath == null) ? (other.canonicalContextPath != null) : !this.canonicalContextPath.equals(
        other.canonicalContextPath)) {
      return false;
    }
    if ((this.virtualHost == null) ? (other.virtualHost != null) : !this.virtualHost.equals(other.virtualHost)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 79 * hash + (this.id != null ? this.id.hashCode() : 0);
    hash = 79 * hash + (this.canonicalContextPath != null ? this.canonicalContextPath.hashCode() : 0);
    hash = 79 * hash + (this.virtualHost != null ? this.virtualHost.hashCode() : 0);
    return hash;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(id).append(':').append(canonicalContextPath).append(':').append(virtualHost).
        toString();
  }

  @Override
  public void writeExternal(DataOutput output) throws IOException {
    output.write(StringUtils.getBytesUtf8(toString()));
  }

  @Override
  public void readExternal(DataInput input) throws IOException, ClassNotFoundException {
    String idString = readStringInUTF8(input);
    if (logger.isInfoEnabled()) {
      logger.info("Trying to parse session data id: " + idString);
    }
    if (org.apache.commons.lang.StringUtils.isBlank(idString)) {
      throw new IOException("No content!");
    }
    String[] params = idString.split(":");
    if (params == null || params.length != 3) {
      throw new IOException(
          "Object should have been in the format id:canonicalpath:virtualhost");
    }
    setId(params[0]);
    setCanonicalContextPath(params[1]);
    setVirtualHost(params[2]);
  }

  public static String readStringInUTF8(DataInput in) throws IOException, UnsupportedEncodingException {
    int allocationBlockSize = 2000;
    int capacity = allocationBlockSize;
    int length = 0;
    ByteBuffer buffer = ByteBuffer.allocate(allocationBlockSize);
    boolean notEof = true;
    while (notEof) {
      try {
        buffer.put(in.readByte());
        if (++length >= capacity) {
          capacity += allocationBlockSize;
          buffer.limit(capacity);
        }
      }
      catch (EOFException ex) {
        notEof = false;
      }
    }
    String string = StringUtils.newStringUtf8(Arrays.copyOf(buffer.array(), length));
    return string;
  }
}
