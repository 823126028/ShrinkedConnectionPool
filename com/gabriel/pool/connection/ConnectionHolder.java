package com.gabriel.pool.connection;

import java.io.IOException;
import java.net.UnknownHostException;

import com.gabriel.Configuration;
import com.gabriel.ConfigurationConstant;
import com.gabriel.pool.ConnectionSource;

public class ConnectionHolder {
	/**上次执行的时间*/
	private long lastAccessTime;
	/**是否正在被执行(如果出现没有run长时间不归还的,主动关闭连接交还)*/
	private boolean running;
	/**是否被丢弃了*/
	private boolean disposed;
	/**上次连接时间*/
	private long connectionTime;
	/**实际的物理连接包裹器*/
	private Connection physicalWrapper;
	/**连接池*/
	private ConnectionSource connectionSource;
	
	public boolean checkValid(){
		return physicalWrapper.checkValid();
	}
	
	public void write(Object object){
		this.beforeExecute();
		try{
			physicalWrapper.write(object);
		}finally{
			this.afterExecute();
		}
	}
	
	public boolean isDisposed(){
		return disposed;
	}
	
	public void beforeExecute(){
		//设置状态正在执行
		running = true;
	}
	
	public void afterExecute(){
		//设置状态执行完毕
		lastAccessTime = System.currentTimeMillis();
		running = false;
	}
	
	public ConnectionHolder(ConnectionSource connectionSource) throws InstantiationException, IllegalAccessException, ClassNotFoundException, UnknownHostException, IOException {
		this.connectionSource = connectionSource;
		physicalWrapper =  (Connection) Class.forName(Configuration.getConfiguration(ConfigurationConstant.RAW_CONNECTION_CLASS, String.class)).newInstance();
		physicalWrapper.init(connectionSource.getIp(), connectionSource.getPort());
		lastAccessTime = System.currentTimeMillis();
		connectionTime = System.currentTimeMillis();
		running = false;
		disposed = false;
	}
	
	public void dispose(){
		disposed = true;
		running = false;
		physicalWrapper.close();
	}
	
	public long getConnectionTime(){
		return connectionTime;
	}
	
	public void setConnectionTime(long connectionTime){
		this.connectionTime = connectionTime;
	}
	
	public boolean isRunning(){
		return running;
	}

	public long getLastAccessTime(){
		return lastAccessTime;
	}
	
	public void setLastAccessTime(long lastAccessTime){
		this.lastAccessTime = lastAccessTime;
	}
	
	public void close(){
		if(disposed){
			return;
		}
		running = false;
		getConnectionSource().recycle(this);
	}

	public ConnectionSource getConnectionSource() {
		return connectionSource;
	}
}
