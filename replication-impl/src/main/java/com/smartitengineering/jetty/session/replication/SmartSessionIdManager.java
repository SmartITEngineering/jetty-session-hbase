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

import com.smartitengineering.jetty.session.replication.SmartSessionManager.Session;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.AbstractSessionIdManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author imyousuf
 */
public class SmartSessionIdManager extends AbstractSessionIdManager {

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private final Server server;
  private final ReentrantLock lock = new ReentrantLock();

  public SmartSessionIdManager(Server server, Random random) {
    super(random);
    this.server = server;
  }

  public SmartSessionIdManager(Server server) {
    this.server = server;
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();
  }

  @Override
  public boolean idInUse(String id) {
    if (logger.isInfoEnabled()) {
      logger.info("idInUse " + id);
    }
    if (StringUtils.isBlank(id)) {
      return false;
    }
    String clusterId = getClusterId(id);
    boolean inUse = false;
    Cache sessionIds = SessionReplicationAPI.getInstance().getSessionIdCache();
    if (logger.isInfoEnabled()) {
      logger.info("Checking for " + clusterId + " in " + sessionIds);
    }
    inUse = sessionIds.isKeyInCache(clusterId);
    if (inUse) {
      return true; //optimisation - if this session is one we've been managing, we can check locally
    }
    //otherwise, we need to go to the database to check
    try {
      return SessionReplicationAPI.getInstance().getIdReader().getById(clusterId) != null;
    }
    catch (Exception e) {
      logger.warn("Problem checking inUse for id=" + clusterId, e);
      return false;
    }
  }

  @Override
  public void addSession(HttpSession httpSession) {
    logger.info("addSession");
    if (httpSession == null || !(httpSession instanceof Session)) {
      return;
    }
    lock.lock();
    try {
      Session session = (Session) httpSession;
      SessionId sessionId = new SessionId();
      sessionId.setCreatedAt(new Date().getTime());
      final String id = session.getClusterId();
      sessionId.setId(id);
      if (logger.isInfoEnabled()) {
        logger.info("Session id " + sessionId.getId() + " " + sessionId.getCreatedAt());
      }
      SessionReplicationAPI.getInstance().getIdWriter().save(sessionId);
      Cache sessionIds = SessionReplicationAPI.getInstance().getSessionIdCache();
      sessionIds.put(new Element(id, System.currentTimeMillis()));
    }
    catch (Exception ex) {
      logger.error("Could not add session id!", ex);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void removeSession(HttpSession httpSession) {
    logger.info("removeSession");
    lock.lock();
    try {
      if (httpSession == null || !(httpSession instanceof Session)) {
        return;
      }
      Session session = (Session) httpSession;
      final String clusterId = session.getClusterId();
      removeSession(clusterId);
    }
    finally {
      lock.unlock();
    }
  }

  protected void removeSession(final String clusterId) {
    logger.info("removeSession");
    lock.lock();
    try {
      SessionId sessionId =
                SessionReplicationAPI.getInstance().getIdReader().getById(clusterId);
      if (sessionId != null) {
        SessionReplicationAPI.getInstance().getIdWriter().delete(sessionId);
        Cache sessionIds = SessionReplicationAPI.getInstance().getSessionIdCache();
        sessionIds.remove(sessionId.getId());
      }
    }
    catch (Exception ex) {
      logger.error("Could not remove session id!", ex);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public void invalidateAll(String id) {
    logger.info("invalidateAll");
    //take the id out of the list of known sessionids for this node
    lock.lock();
    try {
      removeSession(id);
      //tell all contexts that may have a session object with this id to
      //get rid of them
      Handler[] contexts = server.getChildHandlersByClass(ContextHandler.class);
      for (int i = 0; contexts != null && i < contexts.length; i++) {
        SessionHandler sessionHandler = (SessionHandler) ((ContextHandler) contexts[i]).getChildHandlerByClass(
            SessionHandler.class);
        if (sessionHandler != null) {
          SessionManager manager = sessionHandler.getSessionManager();

          if (manager != null && manager instanceof SmartSessionManager) {
            ((SmartSessionManager) manager).invalidateSession(id);
          }
        }
      }
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public String getClusterId(String nodeId) {
    logger.info("getClusterId");
    int dot = nodeId.lastIndexOf('.');
    return (dot > 0) ? nodeId.substring(0, dot) : nodeId;
  }

  @Override
  public String getNodeId(String clusterId, HttpServletRequest request) {
    logger.info("getNodeId");
    if (getWorkerName() != null) {
      return clusterId + '.' + getWorkerName();
    }

    return clusterId;
  }
}
