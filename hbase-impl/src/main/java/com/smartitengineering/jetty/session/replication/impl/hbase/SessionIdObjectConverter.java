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
package com.smartitengineering.jetty.session.replication.impl.hbase;

import com.smartitengineering.dao.impl.hbase.spi.ExecutorService;
import com.smartitengineering.dao.impl.hbase.spi.impl.AbstractObjectRowConverter;
import com.smartitengineering.jetty.session.replication.SessionId;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

/**
 *
 * @author imyousuf
 */
public class SessionIdObjectConverter extends AbstractObjectRowConverter<SessionId, String> {

  public static final byte[] FAMILY_SELF = Bytes.toBytes("self");
  public static final byte[] CELL_CREATED = Bytes.toBytes("created");

  @Override
  protected String[] getTablesToAttainLock() {
    return new String[]{getInfoProvider().getMainTableName()};
  }

  @Override
  protected void getDeleteForTable(SessionId instance, ExecutorService service, Delete put) {
    //Nothing to do, delete whole row
  }

  @Override
  protected void getPutForTable(SessionId instance, ExecutorService service, Put put) {
    put.add(FAMILY_SELF, CELL_CREATED, Bytes.toBytes(instance.getCreatedAt()));
  }

  @Override
  public SessionId rowsToObject(Result startRow, ExecutorService executorService) {
    try {
      SessionId id = new SessionId();
      id.setId(getInfoProvider().getIdFromRowId(startRow.getRow()));
      id.setCreatedAt(Bytes.toLong(startRow.getValue(FAMILY_SELF, CELL_CREATED)));
      return id;
    }
    catch (Exception ex) {
      logger.error("Could convert row to session id!", ex);
      throw new RuntimeException(ex);
    }
  }
}
