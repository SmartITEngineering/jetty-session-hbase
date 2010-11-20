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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author imyousuf
 */
public class SmartSessionManager extends AbstractSessionManager {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public synchronized Map getSessionMap() {
    Collection<SessionData> data = SessionReplicationAPI.getInstance().getDataReader().getAll();
    if (data == null) {
      return Collections.emptyMap();
    }
    Map sessions = new HashMap(data.size());
    for (SessionData datum : data) {
      sessions.put(datum.getId(), sessions.put(datum.getId(), new Session(datum)));
    }
    return sessions;
  }

  @Override
  protected void addSession(AbstractSessionManager.Session sn) {
    Session session = (SmartSessionManager.Session) sn;
    updateSession(session);
  }

  @Override
  public Session getSession(String string) {
    SessionData data = SessionReplicationAPI.getInstance().getDataReader().getById(string);
    return new Session(data);
  }

  @Override
  protected void invalidateSessions() {
    //Do nothing - we don't want to remove and
    //invalidate all the sessions because this
    //method is called from doStop(), and just
    //because this context is stopping does not
    //mean that we should remove the session from
    //any other nodes;
  }

  protected void invalidateSession(String idInCluster) {
    Session session = null;
    synchronized (this) {
      session = new Session(SessionReplicationAPI.getInstance().getDataReader().getById(idInCluster));
    }

    if (session != null) {
      session.invalidate();
    }
  }

  @Override
  protected Session newSession(HttpServletRequest hsr) {
    final Session session = new SmartSessionManager.Session(hsr);
    createSession(session);
    return session;
  }

  @Override
  protected void removeSession(String string) {
    deleteSession(getSession(string));
  }

  protected void createSession(Session session) {
    try {
      SessionReplicationAPI.getInstance().getDataWriter().save(session.sessionData);
    }
    catch (Exception ex) {
      logger.error("Could not save session to write dao!", ex);
    }
  }

  protected void updateSession(Session session) {
    try {
      SessionReplicationAPI.getInstance().getDataWriter().update(session.sessionData);
    }
    catch (Exception ex) {
      logger.error("Could not update session to write dao!", ex);
    }
  }

  protected void deleteSession(Session session) {
    try {
      SessionReplicationAPI.getInstance().getDataWriter().delete(session.sessionData);
    }
    catch (Exception ex) {
      logger.error("Could not delete session to write dao!", ex);
    }
  }

  public class Session extends AbstractSessionManager.Session {

    private SessionData sessionData;

    public Session(HttpServletRequest request) {
      super(request);
      sessionData = new SessionData(getId(), _sessionIdManager.getWorkerName());
    }

    Session(SessionData sessionData) {
      super(sessionData.getCreated(), sessionData.getAccessed(), sessionData.getId());
      this.sessionData = sessionData;
    }

    @Override
    protected Map newAttributeMap() {
      final ConcurrentHashMap map = new ConcurrentHashMap();
      return map;
    }

    @Override
    protected void complete() {
      super.complete();
      willPassivate();
      updateSession(this);
      didActivate();
    }

    @Override
    public String getClusterId() {
      return super.getClusterId();
    }

    @Override
    public void setAttribute(String name, Object value) {
      super.setAttribute(name, value);
      sessionData.setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
      super.removeAttribute(name);
      sessionData.removeAttribute(name);
    }

    @Override
    protected void cookieSet() {
      sessionData.setCookieSet(sessionData.getAccessed());
    }

    @Override
    protected void access(long time) {
      super.access(time);
      sessionData.setLastAccessed(sessionData.getAccessed());
      sessionData.setAccessed(time);
      sessionData.setExpiryTime(_maxIdleMs < 0 ? 0 : (time + _maxIdleMs));
    }
  }
}
