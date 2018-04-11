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
import java.net.URI;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import io.dohko.job.client.NotificationClientEndpoint;

public class WebSocketClient {

	public static void main(String args[]) throws Exception {
		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
		String uri = "ws://localhost:8080/notification/tasks";
		System.out.println("Connecting to " + uri);
		Session session = container.connectToServer(NotificationClientEndpoint.class, URI.create(uri));

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		for (;;) {
			System.out.print(" >> ");
			System.out.flush();
			String line = in.readLine();
			if (line == null)
				break;
			if (line.length() == 0)
				continue;
			session.getBasicRemote().sendText(line);
		}
	}
}
