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
		LOG.info("New connection received {}", session.getId());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		sessions.remove(this);
		LOG.info("Connection removed {}", session.getId());
	}

	public static void broadcast(TaskMessage message) {
		Gson gson = new Gson();
		String jsonStr = gson.toJson(message);
		TextMessage textMessage = new TextMessage(jsonStr);
		sessions.forEach(session -> {
			synchronized (session) {
				try {
					if (session.isOpen()) {
						session.sendMessage(textMessage);
					}
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		});
	}
}