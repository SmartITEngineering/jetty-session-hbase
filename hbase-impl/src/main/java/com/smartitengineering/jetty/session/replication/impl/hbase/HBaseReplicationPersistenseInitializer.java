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

import com.smartitengineering.dao.hbase.ddl.HBaseTableGenerator;
import com.smartitengineering.dao.hbase.ddl.config.json.ConfigurationJsonParser;
import com.smartitengineering.dao.impl.hbase.HBaseConfigurationFactory;
import com.smartitengineering.util.bean.guice.GuiceUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author imyousuf
 */
public class HBaseReplicationPersistenseInitializer {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  public HBaseReplicationPersistenseInitializer() {
    init();
  }

  private void init() {
    logger.info("Initializing factory!");
    GuiceUtil.getInstance("com/smartitengineering/jetty/session/replication/impl/hbase/HBaseImpl.properties").register();
    logger.info("Guice injection done!");
    Configuration config = HBaseConfigurationFactory.getConfigurationInstance();
    logger.info("HBase configuration retrieved!");
    try {
      logger.info("Trying to create tables!");
      new HBaseTableGenerator(ConfigurationJsonParser.getConfigurations(getClass().getClassLoader().
          getResourceAsStream("com/smartitengineering/jetty/session/replication/impl/hbase/schema.json")), config,
                              false).generateTables();
    }
    catch (MasterNotRunningException ex) {
      logger.error("Master could not be found!", ex);
    }
    catch (Exception ex) {
      logger.error("Could not create table!", ex);
    }
  }
}
