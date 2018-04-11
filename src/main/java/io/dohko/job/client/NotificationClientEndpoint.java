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
