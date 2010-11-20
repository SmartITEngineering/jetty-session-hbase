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

import com.smartitengineering.domain.AbstractGenericPersistentDTO;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.SessionIdManager;

/**
 *
 * @author imyousuf
 */
public class SessionData extends AbstractGenericPersistentDTO<SessionData, String, Long> {

  private final String id;
  private String rowId;
  private long accessed;
  private long lastAccessed;
  private long maxIdleMs;
  private long cookieSet;
  private long created;
  private Map attributes;
  private String lastNode;
  private String canonicalContext;
  private long lastSaved;
  private long expiryTime;
  private String virtualHost;

  public SessionData(String sessionId, String lastNode) {
    id = sessionId;
    created = System.currentTimeMillis();
    accessed = created;
    attributes = new ConcurrentHashMap();
  }

  @Override
  public synchronized String getId() {
    return id;
  }

  public synchronized long getCreated() {
    return created;
  }

  public synchronized void setCreated(long ms) {
    this.created = ms;
  }

  public synchronized long getAccessed() {
    return accessed;
  }

  public synchronized void setAccessed(long ms) {
    this.accessed = ms;
  }

  public synchronized void setMaxIdleMs(long ms) {
    this.maxIdleMs = ms;
  }

  public synchronized long getMaxIdleMs() {
    return maxIdleMs;
  }

  public synchronized void setLastAccessed(long ms) {
    this.lastAccessed = ms;
  }

  public synchronized long getLastAccessed() {
    return lastAccessed;
  }

  public void setCookieSet(long ms) {
    this.cookieSet = ms;
  }

  public synchronized long getCookieSet() {
    return cookieSet;
  }

  public synchronized void setRowId(String rowId) {
    this.rowId = rowId;
  }

  public synchronized String getRowId() {
    return rowId;
  }

  public void setAttribute(String key, Object val) {
    attributes.put(key, val);
  }

  public void removeAttribute(String key) {
    attributes.remove(key);
  }

  public Object getAttrbute(String key) {
    return attributes.get(key);
  }

  public synchronized Map getAttributeMap() {
    return Collections.unmodifiableMap(attributes);
  }

  public synchronized void setAttributeMap(Map map) {
    this.attributes.clear();
    this.attributes.putAll(map);
  }

  public synchronized void setLastNode(String node) {
    this.lastNode = node;
  }

  public synchronized String getLastNode() {
    return lastNode;
  }

  public synchronized void setCanonicalContext(String str) {
    this.canonicalContext = str;
  }

  public synchronized String getCanonicalContext() {
    return canonicalContext;
  }

  public synchronized long getLastSaved() {
    return this.lastSaved;
  }

  public synchronized void setLastSaved(long time) {
    lastSaved = time;
  }

  public synchronized void setExpiryTime(long time) {
    this.expiryTime = time;
  }

  public synchronized long getExpiryTime() {
    return expiryTime;
  }

  public synchronized void setVirtualHost(String vhost) {
    this.virtualHost = vhost;
  }

  public synchronized String getVirtualHost() {
    return virtualHost;
  }

  @Override
  public String toString() {
    return "Session rowId=" + rowId + ",id=" + id + ",lastNode=" + lastNode +
        ",created=" + created + ",accessed=" + accessed +
        ",lastAccessed=" + lastAccessed + ",cookieSet=" + cookieSet +
        "lastSaved=" + lastSaved;
  }

  @Override
  public boolean isValid() {
    return StringUtils.isNotBlank(getId());
  }
}
