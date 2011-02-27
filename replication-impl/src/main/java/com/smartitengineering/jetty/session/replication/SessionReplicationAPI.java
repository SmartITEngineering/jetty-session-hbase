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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author imyousuf
 */
@Aggregator(contextName = "com.smartitengineering.jetty.session.replication")
public class SessionReplicationAPI {

  public static final String INITIALIZER_CLASS_SYS_PROP = "com.smartitengineering.jetty.session.replication.init";
  private static SessionReplicationAPI api;
  private static final Logger LOGGER = LoggerFactory.getLogger(SessionReplicationAPI.class);

  public static SessionReplicationAPI getInstance() {
    if (api == null) {
      LOGGER.info("Initializing Replication API");
      init();
    }
    LOGGER.info("Returning initialized API");
    return api;
  }

  private synchronized static void init() {
    if (api == null) {
      api = new SessionReplicationAPI();
      String val = System.getProperty(INITIALIZER_CLASS_SYS_PROP);
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("System parameter of the initializer " + val);
      }
      if (StringUtils.isNotBlank(val)) {
        try {
          Class.forName(val).newInstance();
        }
        catch (Exception ex) {
          LOGGER.error("Could not initialize initialzer class properly!", ex);
        }
      }
      BeanFactoryRegistrar.aggregate(api);
    }
  }
  @InjectableField(beanName = "dataReader")
  private CommonReadDao<SessionData, SessionDataId> dataReader;
  @InjectableField(beanName = "dataWriter")
  private CommonWriteDao<SessionData> dataWriter;
  @InjectableField(beanName = "idReader")
  private CommonReadDao<SessionId, String> idReader;
  @InjectableField(beanName = "idWriter")
  private CommonWriteDao<SessionId> idWriter;

  public CommonReadDao<SessionData, SessionDataId> getDataReader() {
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
