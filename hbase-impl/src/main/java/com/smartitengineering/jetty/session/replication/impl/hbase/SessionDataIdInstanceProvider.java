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

import com.smartitengineering.dao.impl.hbase.spi.DomainIdInstanceProvider;
import com.smartitengineering.jetty.session.replication.SessionDataId;

/**
 *
 * @author imyousuf
 */
public class SessionDataIdInstanceProvider implements DomainIdInstanceProvider {

  @Override
  public <IdType> IdType getInstance(Class<? extends IdType> clazz) {
    Object id;
    if (SessionDataId.class.isAssignableFrom(clazz)) {
      id = new SessionDataId();
    }
    else {
      id = null;
    }
    return (IdType) id;
  }
}
