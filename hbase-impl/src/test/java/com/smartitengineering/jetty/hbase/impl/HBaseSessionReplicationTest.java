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
package com.smartitengineering.jetty.hbase.impl;

import com.google.inject.AbstractModule;
import com.smartitengineering.jetty.session.replication.SessionReplicationAPI;
import com.smartitengineering.jetty.session.replication.impl.hbase.HBaseReplicationPersistenseInitializer;
import com.smartitengineering.util.bean.guice.GuiceUtil;
import java.util.Properties;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HBaseSessionReplicationTest {

  private static final HBaseTestingUtility TEST_UTIL = new HBaseTestingUtility();
  private static final Logger LOGGER = LoggerFactory.getLogger(HBaseSessionReplicationTest.class);

  @BeforeClass
  public static void globalSetup() {
    /*
     * Start HBase and initialize tables
     */
    //-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl
    System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                       "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    try {
      TEST_UTIL.startMiniCluster();
    }
    catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
    }
    Properties properties = new Properties();
    properties.setProperty(GuiceUtil.CONTEXT_NAME_PROP, "com.smartitengineering.dao.impl.hbase");
    properties.setProperty(GuiceUtil.IGNORE_MISSING_DEP_PROP, Boolean.TRUE.toString());
    properties.setProperty(GuiceUtil.MODULES_LIST_PROP, TestModule.class.getName());
    GuiceUtil.getInstance(properties).register();
    System.setProperty("com.smartitengineering.jetty.session.replication.init",
                       HBaseReplicationPersistenseInitializer.class.getName());
  }

  @AfterClass
  public static void globalTeardown() {
    try {
      TEST_UTIL.shutdownMiniCluster();
    }
    catch (Exception ex) {
      LOGGER.warn("Error shutting down!", ex);
    }
  }

  @Test
  public void testApiBinding() {
    Assert.assertNotNull(SessionReplicationAPI.getInstance().getDataReader());
    Assert.assertNotNull(SessionReplicationAPI.getInstance().getDataWriter());
    Assert.assertNotNull(SessionReplicationAPI.getInstance().getIdReader());
    Assert.assertNotNull(SessionReplicationAPI.getInstance().getIdWriter());
  }

  public static class TestModule extends AbstractModule {

    @Override
    protected void configure() {
      bind(Configuration.class).toInstance(TEST_UTIL.getConfiguration());
    }
  }
}
