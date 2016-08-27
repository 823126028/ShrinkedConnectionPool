package com.gabriel.pool.connection;

import java.io.IOException;
import java.net.UnknownHostException;

public interface Connection {
	public abstract void init(String ip,int port) throws UnknownHostException, IOException;
	public abstract void close();
	public abstract boolean checkValid();
	public abstract void write(Object object);
}
