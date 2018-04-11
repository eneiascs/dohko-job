/**
 *     Copyright (C) 2013-2017  the original author or authors.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License,
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package io.dohko.job.client;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class NotificationClientEndpoint {
	private static final Logger LOG = LoggerFactory.getLogger(NotificationClientEndpoint.class);
    @OnOpen
    public void onOpen(Session session) {
    	LOG.info("Connected to endpoint: {}",session.getBasicRemote());
       
    }

    @OnMessage
    public void processMessage(String message, Session sesison) {
    	LOG.info("Received message in client: {}", message);
       
    }

    @OnError
    public void processError(Throwable t) {
    	LOG.error(t.getMessage(),t);
    }
}
