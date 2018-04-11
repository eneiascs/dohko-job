package io.dohko.job.resource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;

import io.dohko.job.batch.JobService;
import io.dohko.job.batch.TaskMessage;

@Component
public class NotificationHandler extends TextWebSocketHandler {
	

	private static final Logger LOG = LoggerFactory.getLogger(JobService.class);
	
	private static List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {

		
		sessions.add(session);
		LOG.info("New connection received {}",session.getId());
	}
	
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessions.remove(this);
		LOG.info("Connection removed {}", session.getId());
	}
	
		

	public static void broadcast(TaskMessage message) {
		Gson gson = new Gson();
		String jsonStr = gson.toJson(message);
		TextMessage textMessage=new TextMessage(jsonStr);
		sessions.forEach(session -> {
			synchronized (session) {
				try {
					session.sendMessage(textMessage);
				} catch (IOException e) {
					LOG.error(e.getMessage(),e);
				}
			}
		});
	}
}