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

import com.smartitengineering.dao.common.CommonReadDao;
import com.smartitengineering.dao.common.CommonWriteDao;
import com.smartitengineering.util.bean.BeanFactoryRegistrar;
import com.smartitengineering.util.bean.annotations.Aggregator;
import com.smartitengineering.util.bean.annotations.InjectableField;

/**
 *
 * @author imyousuf
 */
@Aggregator(contextName = "com.smartitengineering.jetty.session.replication")
public class SessionReplicationAPI {

  private static SessionReplicationAPI api;

  public static SessionReplicationAPI getInstance() {
    if (api == null) {
      init();
    }
    return api;
  }

  private synchronized static void init() {
    if (api == null) {
      api = new SessionReplicationAPI();
      BeanFactoryRegistrar.aggregate(api);
    }
  }
  @InjectableField(beanName = "dataReader")
  private CommonReadDao<SessionData, String> dataReader;
  @InjectableField(beanName = "dataWriter")
  private CommonWriteDao<SessionData> dataWriter;
  @InjectableField(beanName = "idReader")
  private CommonReadDao<SessionId, String> idReader;
  @InjectableField(beanName = "idWriter")
  private CommonWriteDao<SessionId> idWriter;

  public CommonReadDao<SessionData, String> getDataReader() {
    return dataReader;
  }

  public CommonWriteDao<SessionData> getDataWriter() {
    return dataWriter;
  }

  public CommonReadDao<SessionId, String> getIdReader() {
    return idReader;
  }

  public CommonWriteDao<SessionId> getIdWriter() {
    return idWriter;
  }
}
