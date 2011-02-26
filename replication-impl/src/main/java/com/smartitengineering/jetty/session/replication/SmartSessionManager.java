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
import java.util.concurrent.Semaphore;
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
  protected final Semaphore semaphore = new Semaphore(1);

  @Override
  public Map getSessionMap() {
    logger.info("getSessionMap");
    semaphore.acquireUninterruptibly();
    try {
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
    finally {
      semaphore.release();
    }
  }

  @Override
  protected void addSession(AbstractSessionManager.Session sn) {
    logger.info("addSession");
    semaphore.acquireUninterruptibly();
    try {
      Session session = (SmartSessionManager.Session) sn;
      updateSession(session);
    }
    finally {
      semaphore.release();
    }
  }

  @Override
  public Session getSession(String string) {
    logger.info("getSession");
    semaphore.acquireUninterruptibly();
    try {
      SessionData data = SessionReplicationAPI.getInstance().getDataReader().getById(string);
      if (data != null) {
        return new Session(data);
      }
      else {
        return null;
      }
    }
    finally {
      semaphore.release();
    }
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
    logger.info("invalidateSession");
    semaphore.acquireUninterruptibly();
    try {
      Session session = null;
      synchronized (this) {
        session = new Session(SessionReplicationAPI.getInstance().getDataReader().getById(idInCluster));
      }

      if (session != null) {
        session.invalidate();
      }
    }
    finally {
      semaphore.release();
    }
  }

  @Override
  protected Session newSession(HttpServletRequest hsr) {
    logger.info("newSession");
    semaphore.acquireUninterruptibly();
    try {
      final Session session = new SmartSessionManager.Session(hsr);
      createSession(session);
      return session;
    }
    finally {
      semaphore.release();
    }
  }

  @Override
  protected boolean removeSession(String idInCluster) {
    logger.info("getSessionMap");
    semaphore.acquireUninterruptibly();
    try {
      return deleteSession(getSession(idInCluster));
    }
    finally {
      semaphore.release();
    }
  }

  protected void createSession(Session session) {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Creating session with id " + session.sessionData.getId());
      }
      SessionReplicationAPI.getInstance().getDataWriter().save(session.sessionData);
    }
    catch (Exception ex) {
      logger.error("Could not save session to write dao!", ex);
    }
  }

  protected void updateSession(Session session) {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Updating session with id " + session.sessionData.getId());
      }
      SessionReplicationAPI.getInstance().getDataWriter().update(session.sessionData);
    }
    catch (Exception ex) {
      logger.error("Could not update session to write dao!", ex);
    }
  }

  protected boolean deleteSession(Session session) {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Deleting session with id " + session.sessionData.getId());
      }
      SessionReplicationAPI.getInstance().getDataWriter().delete(session.sessionData);
      return true;
    }
    catch (Exception ex) {
      logger.error("Could not delete session to write dao!", ex);
      return false;
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
    protected void complete() {
      semaphore.acquireUninterruptibly();
      try {
        super.complete();
        willPassivate();
        updateSession(this);
        didActivate();
      }
      finally {
        semaphore.release();
      }
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
