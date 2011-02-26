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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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
  private final Object lockObject = new Object();

  public SmartSessionIdManager(Server server, Random random) {
    super(random);
    this.server = server;
  }

  public SmartSessionIdManager(Server server) {
    this.server = server;
  }

  @Override
  public boolean idInUse(String id) {
    logger.info("idInUse");
    return SessionReplicationAPI.getInstance().getIdReader().getById(id) != null;
  }

  @Override
  public void addSession(HttpSession httpSession) {
    logger.info("addSession");
    if (httpSession == null || !(httpSession instanceof Session)) {
      return;
    }
    Session session = (Session) httpSession;
    try {
      SessionId sessionId = new SessionId();
      sessionId.setCreatedAt(new Date().getTime());
      sessionId.setId(session.getClusterId());
      SessionReplicationAPI.getInstance().getIdWriter().save(sessionId);
    }
    catch (Exception ex) {
      logger.error("Could not add session id!", ex);
    }
  }

  @Override
  public void removeSession(HttpSession httpSession) {
    logger.info("removeSession");
    if (httpSession == null || !(httpSession instanceof Session)) {
      return;
    }
    Session session = (Session) httpSession;
    final String clusterId = session.getClusterId();
    removeSession(clusterId);
  }

  protected void removeSession(final String clusterId) {
    logger.info("removeSession");
    try {
      SessionId sessionId =
                SessionReplicationAPI.getInstance().getIdReader().getById(clusterId);
      if (sessionId != null) {
        SessionReplicationAPI.getInstance().getIdWriter().delete(sessionId);
      }
    }
    catch (Exception ex) {
      logger.error("Could not remove session id!", ex);
    }
  }

  @Override
  public void invalidateAll(String id) {
    logger.info("invalidateAll");
    //take the id out of the list of known sessionids for this node
    removeSession(id);

    synchronized (lockObject) {
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
    if (_workerName != null) {
      return clusterId + '.' + _workerName;
    }

    return clusterId;
  }
}
