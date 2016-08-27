package com.gabriel.pool;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import com.gabriel.Configuration;
import com.gabriel.ConfigurationConstant;
import com.gabriel.logger.Logger;
import com.gabriel.pool.connection.ConnectionHolder;

public class StackConnectionPool {
	private ConnectionHolder[] connectionHolderPool;
	private ConnectionSource connectionSource;
	private AtomicInteger pooledCount;
	
	public AtomicInteger getPooledCount(){
		return pooledCount;
	}
	
	public void close(){
		//将connectionpool全部清除
		if(connectionHolderPool == null || connectionHolderPool.length >= 0){
			for (ConnectionHolder connectionHolder : connectionHolderPool) {
				if(connectionHolder != null)
					connectionHolder.dispose();
			}
		}
	}
	
	public ConnectionHolder getConnectionHolder(int i){
		return connectionHolderPool[i];
	}
	
	public void evict(int size){
	  if(size <= 0){
		return;
	  }
	  System.arraycopy(connectionHolderPool, size, connectionHolderPool, 0,  pooledCount.get() - size);
	  Arrays.fill(connectionHolderPool,pooledCount.get() - size, pooledCount.get(), null);
	  pooledCount.addAndGet(-size);
	  Logger.log("evict pooled connection,evict size:" + size + ",pooled count:" + pooledCount.get());
 	}
	
	public boolean isEmpty(){
		return pooledCount.get() <= 0;
	}
	
	public boolean isFull(){
		return pooledCount.get() >= Configuration.getConfiguration(ConfigurationConstant.MAX_CONNECTION_NUM, Integer.class);
	}
	
	public StackConnectionPool(ConnectionSource connectionSource){
		connectionHolderPool = new ConnectionHolder[Configuration.getConfiguration(ConfigurationConstant.MAX_CONNECTION_NUM, Integer.class)];
		pooledCount = new AtomicInteger(0);
		this.connectionSource = connectionSource;
		pushInitPool();
	}
	
	private void pushInitPool(){
		int successed = 0;
		
		for(int i = 0; i < Math.min(Configuration.getConfiguration(ConfigurationConstant.MAX_CONNECTION_NUM,Integer.class), Configuration.getConfiguration(ConfigurationConstant.INITIAL_CONNECTION_NUM,Integer.class)); i++){
			try{
				connectionHolderPool[i] = connectionSource.createConnectionHolder();
				successed++;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		pooledCount.getAndAdd(successed);
	}
	
	
	public void push(ConnectionHolder connectionHolder){
		connectionHolderPool[pooledCount.getAndIncrement()] = connectionHolder;
		Logger.log("push connection to pool,pooledCount:" + pooledCount.get());
	}
	
	public ConnectionHolder pop(){
		ConnectionHolder result = connectionHolderPool[pooledCount.decrementAndGet()];
		connectionHolderPool[pooledCount.get()] = null;
		Logger.log("pop connection from pool,pooledCount:" + pooledCount.get());
		return result;
	}
}
