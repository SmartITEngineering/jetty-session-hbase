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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.AbstractSessionManager;
import org.eclipse.jetty.util.LazyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author imyousuf
 */
public class SmartSessionManager extends AbstractSessionManager {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  protected final Semaphore semaphore = new Semaphore(1);
  private final static long DEFAULT_INTERVAL = 300;
  private Map<String, Session> sessions;
  private long saveInterval = 0;

  @Override
  public void doStart() throws Exception {
    super.doStart();
    sessions = new ConcurrentHashMap<String, SmartSessionManager.Session>();
  }

  @Override
  public void doStop() throws Exception {
    super.doStop();
    sessions.clear();
    sessions = null;
  }

  @Override
  public int getSessions() {
    return sessions.size();
  }

  @Override
  public Map getSessionMap() {
    logger.info("getSessionMap");
    return Collections.unmodifiableMap(sessions);
  }

  @Override
  protected void addSession(AbstractSessionManager.Session sn) {
    logger.info("addSession");
    try {
      Session session = (SmartSessionManager.Session) sn;
      sessions.put(session.getClusterId(), session);
      session.willPassivate();
      semaphore.acquireUninterruptibly();
      try {
        updateSession(session);
      }
      finally {
        semaphore.release();
      }
      session.didActivate();
    }
    catch (Exception ex) {
      logger.warn("Could not add session!", ex);
    }
  }

  @Override
  public Session getSession(String idInCluster) {
    logger.info("getSession");
    semaphore.acquireUninterruptibly();
    try {
      Session session = sessions.get(idInCluster);
      final SessionData data;
      long now = System.currentTimeMillis();
      if (session == null || ((now - session.sessionData.getLastSaved()) >= (getSaveInterval() * 1000))) {
        data = loadSession(idInCluster);
      }
      else if ((now - session.sessionData.getLastSaved()) >= (getSaveInterval() * 1000)) {
        data = loadSession(idInCluster);
      }
      else {
        data = session.sessionData;
      }
      if (data != null) {
        if (!data.getLastNode().equals(getIdManager().getWorkerName()) || session == null) {
          //if the session in the database has not already expired
          if (data.getExpiryTime() > now) {
            //session last used on a different node, or we don't have it in memory
            session = new Session(now, data);
            sessions.put(idInCluster, session);
            session.didActivate();
            data.setLastNode(getIdManager().getWorkerName());
            updateSession(session);
          }
        }
      }
      else {
        //No session in db with matching id and context path.
        session = null;
      }

      return session;
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
  public void removeSession(AbstractSessionManager.Session sn, boolean invalidate) {
    // Remove session from context and global maps
    boolean removed = false;
    Session session = (SmartSessionManager.Session) sn;

    semaphore.acquireUninterruptibly();
    try {
      //take this session out of the map of sessions for this context
      if (getSession(session.getClusterId()) != null) {
        removed = true;
        removeSession(session.getClusterId());
      }
    }
    finally {
      semaphore.release();
    }

    if (removed) {
      // Remove session from all context and global id maps
      _sessionIdManager.removeSession(session);

      if (invalidate) {
        _sessionIdManager.invalidateAll(session.getClusterId());
      }

      if (invalidate && _sessionListeners != null) {
        HttpSessionEvent event = new HttpSessionEvent(session);
        for (int i = LazyList.size(_sessionListeners); i-- > 0;) {
          ((HttpSessionListener) LazyList.get(_sessionListeners, i)).sessionDestroyed(event);
        }
      }
      if (!invalidate) {
        session.willPassivate();
      }
    }
  }

  @Override
  protected boolean removeSession(String idInCluster) {
    logger.info("getSessionMap");
    final Session session = getSession(idInCluster);
    if (session != null) {
      semaphore.acquireUninterruptibly();
      try {
        sessions.remove(idInCluster);
        return deleteSession(session);
      }
      finally {
        semaphore.release();
      }
    }
    return false;
  }

  protected void invalidateSession(String idInCluster) {
    logger.info("invalidateSession");
    semaphore.acquireUninterruptibly();
    try {
      final SessionData sessionData = SessionReplicationAPI.getInstance().getDataReader().getById(idInCluster);
      if (sessionData == null) {
        return;
      }
      Session session = new Session(sessionData);
      session.invalidate();
    }
    finally {
      semaphore.release();
    }
  }

  protected SessionData loadSession(String string) {
    SessionData data = SessionReplicationAPI.getInstance().getDataReader().getById(string);
    if (data != null) {
      logger.info("Returning session");
      return data;
    }
    else {
      logger.info("No session data thus returning null");
      return null;
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

  public long getSaveInterval() {
    return saveInterval <= 0 ? DEFAULT_INTERVAL : saveInterval;
  }

  public void setSaveInterval(long saveInterval) {
    this.saveInterval = saveInterval;
  }

  public class Session extends AbstractSessionManager.Session {

    private SessionData sessionData;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public Session(HttpServletRequest request) {
      super(request);
      sessionData = new SessionData(getId(), _sessionIdManager.getWorkerName());
    }

    Session(SessionData sessionData) {
      this(sessionData.getAccessed(), sessionData);

    }

    Session(long accessed, SessionData sessionData) {
      super(sessionData.getCreated(), accessed, sessionData.getId());
      this.sessionData = sessionData;
    }

    @Override
    protected void didActivate() {
      super.didActivate();
    }

    @Override
    protected void willPassivate() {
      super.willPassivate();
    }

    @Override
    protected void complete() {
      super.complete();
      if (dirty.get()) {
        willPassivate();
        semaphore.acquireUninterruptibly();
        try {
          updateSession(this);
        }
        finally {
          semaphore.release();
        }
        didActivate();
        dirty.compareAndSet(true, false);
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
      dirty.compareAndSet(false, true);
    }

    @Override
    public void removeAttribute(String name) {
      super.removeAttribute(name);
      sessionData.removeAttribute(name);
      dirty.compareAndSet(false, true);
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
      if ((sessionData.getAccessed() - sessionData.getLastSaved()) >= (getSaveInterval() * 1000)) {
        dirty.compareAndSet(false, true);
      }
    }
  }

  private String getVirtualHost(ContextHandler.Context context) {
    String vhost = "0.0.0.0";

    if (context == null) {
      return vhost;
    }

    String[] vhosts = context.getContextHandler().getVirtualHosts();
    if (vhosts == null || vhosts.length == 0 || vhosts[0] == null) {
      return vhost;
    }

    return vhosts[0];
  }

  private String canonicalize(String path) {
    if (path == null) {
      return "";
    }

    return path.replace('/', '_').replace('.', '_').replace('\\', '_');
  }
}
