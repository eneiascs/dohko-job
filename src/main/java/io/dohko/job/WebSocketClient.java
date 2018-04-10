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
package io.dohko.job;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import io.dohko.job.batch.TaskMessage;

public class WebSocketClient {
	static public class MyStompSessionHandler extends StompSessionHandlerAdapter {

		private void subscribeTopic(String topic, StompSession session) {
			session.subscribe(topic, new StompFrameHandler() {

				@Override
				public Type getPayloadType(StompHeaders headers) {
					return TaskMessage.class;
				}

				@Override
				public void handleFrame(StompHeaders headers, Object payload) {
					TaskMessage message = (TaskMessage) payload;
					System.err.println(message.toString());
				}
			});
		}

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			System.err.println("Connected! Headers:");

			subscribeTopic("/topic/tasks", session);

		}
	}

	public static void main(String args[]) throws Exception {
		StandardWebSocketClient simpleWebSocketClient = new StandardWebSocketClient();
		List<Transport> transports = new ArrayList<>(1);
		transports.add(new WebSocketTransport(simpleWebSocketClient));

		SockJsClient sockJsClient = new SockJsClient(transports);
		WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
		stompClient.setMessageConverter(new MappingJackson2MessageConverter());

		String url = "ws://localhost:8080/notification";
		String userId = "spring-" + ThreadLocalRandom.current().nextInt(1, 99);
		StompSessionHandler sessionHandler = new MyStompSessionHandler();
		StompSession session = stompClient.connect(url, sessionHandler).get();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		for (;;) {
			System.out.print(userId + " >> ");
			System.out.flush();
			String line = in.readLine();
			if (line == null)
				break;
			if (line.length() == 0)
				continue;
			// ClientMessage msg = new ClientMessage(userId, line);
			// session.send("/app/chat/java", msg);
		}
	}
}
