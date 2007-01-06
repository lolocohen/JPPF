/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server.app;

import java.net.Socket;
import org.jppf.JPPFException;
import org.jppf.server.*;

/**
 * Instances of this class listens for incoming connections from client applications.<br>
 * For each incoming connection, a new connection thread is created. This thread listens to incoming
 * connection requests and puts them on the execution queue.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFApplicationServer extends JPPFServer
{
	/**
	 * Initialize this socket server with a specified execution service and port number.
	 * @param port the port this socket server is listening to.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public JPPFApplicationServer(int port) throws JPPFException
	{
		super(port,"Application Server Thread");
	}
	
	/**
	 * Instanciate a wrapper for the socket connection opened by this socket server.
	 * Subclasses must implement this method.
	 * @param socket the socket connection obtained through a call to
	 * {@link java.net.ServerSocket#accept() ServerSocket.accept()}.
	 * @return a <code>JPPFServerConnection</code> instance.
	 * @throws JPPFException if an exception is raised while creating the socket handler.
	 */
	protected JPPFConnection createConnection(Socket socket) throws JPPFException
	{
		return new ApplicationConnection(this, socket);
	}
}
