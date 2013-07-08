/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.node;

import java.io.*;
import java.util.concurrent.locks.ReentrantLock;
import org.jppf.comm.socket.*;
import org.jppf.utils.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public class JPPFClassLoader extends ClassLoader
{
	/**
	 * Wrapper for the underlying socket connection.
	 */
	private static SocketWrapper socketClient = null;
	/**
	 * Determines whether this class loader should handle dynamic class updating.
	 */
	private boolean dynamic = false;
	/**
	 * The unique identifier for the submitting application.
	 */
	private String appUuid = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private static ReentrantLock lock = new ReentrantLock();
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private static SocketInitializer socketInitializer = new SocketInitializer();
	/**
	 * Determines whether this class loader should handle dynamic class updating.
	 */
	private static boolean initializing = false;

	/**
	 * Default instanciation of this class is not permitted, a valid host name and port number MUST be provided.
	 */
	private JPPFClassLoader()
	{
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 */
	public JPPFClassLoader(ClassLoader parent)
	{
		super(parent);
		if (parent instanceof JPPFClassLoader) dynamic = true;
		if (socketClient == null) init();
	}

	/**
	 * Initialize this class loader with a parent class loader.
	 * @param parent a ClassLoader instance.
	 * @param appUuid unique identifier for the submitting application.
	 */
	public JPPFClassLoader(ClassLoader parent, String appUuid)
	{
		this(parent);
		this.appUuid = appUuid;
	}

	/**
	 * Initialize the underlying socket connection.
	 */
	private static void init()
	{
		if (!isInitializing()) setInitializing(true);
		else
		{
			// wait until initialization is over.
			lock.lock();
			lock.unlock();
			return;
		}
		lock.lock();
		System.out.println("JPPFClassLoader.init(): attempting connection to the class server");
		try
		{
			if (socketClient == null) initSocketClient();
			socketInitializer.initializeSocket(socketClient);
			
			// we need to do this in order to dramaticaly simplify the 
			// state machine of ClassServer
			try {
				socketClient.sendBytes(new JPPFBuffer("node"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			System.out.println("JPPFClassLoader.init(): Reconnected to the class server");
		}
		finally
		{
			setInitializing(false);
			lock.unlock();
		}
	}
	
	/**
	 * Initialize the underlying socket connection.
	 */
	private static void initSocketClient()
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String host = props.getString("jppf.server.host", "localhost");
		int port = props.getInt("class.server.port", 11111);
		socketClient = new BootstrapSocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}
	
	/**
	 * Find a class already loaded by this class loader.
	 * @param name the binary name of the class
	 * @return the resulting <tt>Class</tt> object
	 */
	public synchronized Class<?> findAlreadyLoadedClass(String name)
	{
		return findLoadedClass(name);
	}

	/**
	 * @param name the binary name of the class
	 * @return the resulting <tt>Class</tt> object
	 * @throws ClassNotFoundException if the class could not be found
	 */
	public synchronized Class<?> loadJPPFClass(String name) throws ClassNotFoundException
	{
		Class c = findLoadedClass(name);
		if (c == null)
		{
			ClassLoader parent = getParent();
			if (parent instanceof JPPFClassLoader)
				c = ((JPPFClassLoader) parent).findAlreadyLoadedClass(name);
		}
		if (c == null) return findClass(name);
		return c;
	}

	/**
	 * Find a class in this class loader's classpath.
	 * @param name binary name of the resource to find.
	 * @return a defined <code>Class</code> instance.
	 * @throws ClassNotFoundException if the class could not be loaded.
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	public Class<?> findClass(String name) throws ClassNotFoundException
	{
		byte[] b = null;
		String resName = name.replace('.', '/') + ".class";
		b = loadResourceData(resName);
		if ((b == null) || (b.length == 0)) throw new ClassNotFoundException("Could not load class '" + name + "'");
		return defineClass(name, b, 0, b.length);
	}

	/**
	 * Load the specified class from a socket conenction.
	 * @param name the binary name of the class to load, such as specified in the JLS.
	 * @return an array of bye containing the class' byte code.
	 * @throws ClassNotFoundException if the class could not be loaded from the remote server.
	 */
	private byte[] loadResourceData(String name) throws ClassNotFoundException
	{
		byte[] b = null;
		try
		{
			b = loadResourceData0(name);
		}
		catch(IOException e)
		{
			init();
			try
			{
				b = loadResourceData0(name);
			}
			catch(IOException ex)
			{
			}
		}
		return b;
	}

	/**
	 * Load the specified class from a socket connection.
	 * @param name the binary name of the class to load, such as specified in the JLS.
	 * @return an array of bye containing the class' byte code.
	 * @throws ClassNotFoundException if the class could not be loaded from the remote server.
	 * @throws IOException if the connection was lost and could not be reestablished.
	 */
	private byte[] loadResourceData0(String name) throws ClassNotFoundException, IOException
	{
		byte[] b = null;
		try
		{
			lock.lock();
			StringBuilder sb = new StringBuilder();
			if (dynamic) sb.append(":");
			if (appUuid != null) sb.append(appUuid).append("|");
			sb.append(name);
			socketClient.sendBytes(new JPPFBuffer(sb.toString()));
			b = socketClient.receiveBytes(0).getBuffer();
		}
		finally
		{
			lock.unlock();
		}
		return b;
	}

	/**
	 * Get a stream from a resource file in the classpath of this class loader.
	 * @param name name of the resource to obtain a stram from. 
	 * @return an <code>InputStream</code> instance, or null if the resource was not found.
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	public InputStream getResourceAsStream(String name)
	{
		InputStream is = JPPFClassLoader.class.getClassLoader().getResourceAsStream(name);
		if (is == null)
		{
			try
			{
				byte[] b = loadResourceData(name);
				if ((b == null) || (b.length == 0)) return null;
				is = new ByteArrayInputStream(b);
			}
			catch(ClassNotFoundException e)
			{
				return null;
			}
		}
		return is;
	}

	/**
	 * Determine whether the socket client is being initialized.
	 * @return true if the socket client is being initialized, false otherwise.
	 */
	private static synchronized boolean isInitializing()
	{
		return initializing;
	}

	/**
	 * Set the socket client initialization status.
	 * @param initFlag true if the socket client is being initialized, false otherwise.
	 */
	private static synchronized void setInitializing(boolean initFlag)
	{
		initializing = initFlag;
	}
}