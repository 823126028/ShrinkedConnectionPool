package com.gabriel.pool;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import com.gabriel.Configuration;
import com.gabriel.ConfigurationConstant;
import com.gabriel.logger.Logger;
import com.gabriel.pool.checker.ValidChecker;
import com.gabriel.pool.connection.ConnectionHolder;
import com.gabriel.pool.exception.AlreadyClosedException;

public class ConnectionSource{
	private ReentrantLock lock;
	
	private AtomicInteger waitGetCount;
	
	private ActiveContainer activeContainer;
	
	private StackConnectionPool stackConnectionPool;

	private CountDownLatch initLatch = new CountDownLatch(2);
	
	private CountDownLatch closeLatch = new CountDownLatch(2);
	
	private Condition empty;
	
	private Condition notEmpty;
	
	private String ip;
	
	private int port;
	
	//检查连接是否有效
	private ValidChecker validChecker; 
	//用来创建连接的生产者.
	private Thread createThread;
	//用来缩减没用连接的线程
	private Thread destroyThread;
	//是否初始化
	private boolean isInited = false;
	//closed
	private volatile boolean isClosed = false;
	
	
	public void init(String ip,int port) throws InterruptedException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		validChecker = new ValidChecker();
		this.ip = ip;
		this.port = port;
		lock = new ReentrantLock();
		empty = lock.newCondition();
		notEmpty = lock.newCondition();
		stackConnectionPool = new StackConnectionPool(this);
		activeContainer = new ActiveContainer();
		waitGetCount = new AtomicInteger(0);
		startAssist();
		isInited = true;
	}
	
	public void close() throws InterruptedException{
		isClosed = true;
		lock.lock();
		try{
			notEmpty.signalAll();
			empty.signalAll();
		}finally{
			lock.unlock();
		}
		closeLatch.await();
		stackConnectionPool.close();
		activeContainer.close();
		stackConnectionPool.close();
	
	}
	
	public String getIp(){
		return ip;
	}
	
	public int getPort(){
		return port;
	}
	
	/**
	 * 获取连接,非异常状态阻塞
	 * @return
	 */
	public ConnectionHolder getConnectionHolder(){
		return directConnectionHolder(0);
	}
	
	/**
	 * 获取连接,最长阻塞时间为参数
	 * @param waitMills
	 * @return
	 */
	public ConnectionHolder getConnectionHolder(long waitNaos){
		return directConnectionHolder(waitNaos);
	}
	
	public ConnectionHolder directConnectionHolder(long waitNaos){
		if(!isInited){
			throw new RuntimeException("not inited");
		}
		for(;;){
			Logger.log("try to get a connection");
			ConnectionHolder connectionHolder;
			if(waitNaos == 0){
				//调用阻塞接口
				connectionHolder = takeLast();
			}else{
				//调用带时间的阻塞接口
				connectionHolder = pollLast(waitNaos);
				if(connectionHolder == null){
					return connectionHolder;
				}
			}
			if(isClosed){
				return  null;
			}
			//如果出现异常连接或者连接被丢弃,重新尝试
			if(connectionHolder == null || connectionHolder.isDisposed()){
				continue;
			}
			if(Configuration.getConfiguration(ConfigurationConstant.TEST_ON_BORROW, Boolean.class)){
				boolean valid = validChecker.check(connectionHolder);
				if(!valid){
					connectionHolder.dispose();
					Logger.log("testOnBrrow not valid disposed");
					continue;
				}
			}
			if(Configuration.getConfiguration(ConfigurationConstant.TEST_ON_IDLE, Boolean.class)){
				if(System.currentTimeMillis() - Configuration.getConfiguration(ConfigurationConstant.EVICT_IDLE_TIME, Long.class)  >= connectionHolder.getLastAccessTime()){
					boolean valid = validChecker.check(connectionHolder);
					if(!valid){
						connectionHolder.dispose();
						Logger.log("testOnIDLE not valid disposed");
						continue;
					}
				}
			}
			activeContainer.add(connectionHolder);
			Logger.log("activeContainer add one");
			connectionHolder.setConnectionTime(System.currentTimeMillis());
			return connectionHolder;
		}
	}
	
	
	public void put(ConnectionHolder connectionHolder){
		lock.lock();
		try{
			//如果越界清除连接
			if(stackConnectionPool.isFull()){
				connectionHolder.dispose();
				notEmpty.signal();
				return;
			}
			stackConnectionPool.push(connectionHolder);
			notEmpty.signal(); //唤醒正在尝试从池中获取连接的线程
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			lock.unlock();
		}
	}
	
	public ConnectionHolder takeLast(){
		lock.lock();
		try{
			while(stackConnectionPool.isEmpty()){
				if(this.isClosed){
					throw new AlreadyClosedException("the connection pool is already closed");
				}
				empty.signal(); // if pooledCount <= 0 send signal to create Thread
				waitGetCount.incrementAndGet();
				try{
					notEmpty.await(); //recycle or create
				}finally{
					waitGetCount.decrementAndGet();
				}
			}
			return stackConnectionPool.pop();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}finally{
			lock.unlock();
		}
	}
	
	public ConnectionHolder pollLast(long nanos){
		lock.lock();
		try{
			for(;;){
				if(stackConnectionPool.isEmpty()){
					if(this.isClosed){
						throw new AlreadyClosedException("the connection pool is already closed");
					}
					waitGetCount.incrementAndGet();
					empty.signal(); //请求产连接
					try{
						nanos = notEmpty.awaitNanos(nanos); //等待连接
					}finally{
						waitGetCount.decrementAndGet();
					}
					if(!stackConnectionPool.isEmpty()){
						return stackConnectionPool.pop();
					}
					if(nanos <= 0){
						return null;
					}
				}else{
					return stackConnectionPool.pop();
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}finally{
			lock.unlock();
		}
	}
	
	public ConnectionHolder createConnectionHolder() throws InstantiationException, IllegalAccessException, ClassNotFoundException, UnknownHostException, IOException{
		return new ConnectionHolder(this);
	}
	
	private class CreateThread extends Thread{
		public CreateThread(String name){
			super.setName(name);
		}
		public void run(){
			initLatch.countDown();
			//记录重试次数,如果重试大于N次那么进行休息。
			int errorCount = 0;
			while(!isClosed) {
				try{
					lock.lock();
					if(isClosed){
						break;
					}
					//当前房间里的连接大于等待的连接,防止锁一直被改线程占有,直到达到顶峰
					if((stackConnectionPool.getPooledCount().get() >= waitGetCount.get()) || (stackConnectionPool.getPooledCount().get() + activeContainer.getActiveCount()  >= Configuration.getConfiguration(ConfigurationConstant.MAX_CONNECTION_NUM, Integer.class))){
						empty.await(); //等待
						continue;
					}
				}catch(Exception e){
					e.printStackTrace();
				}finally{
					lock.unlock();
				}
				ConnectionHolder c = null;
				try{
					c = createConnectionHolder();
				}catch(Exception e){
					e.printStackTrace();
					//防止一直创建失败。休息一会儿再创建
					errorCount++;
					if(errorCount > Configuration.getConfiguration(ConfigurationConstant.MAX_CREATE_ERROR_COUNT, Integer.class)){
						try {
							Logger.log("create error too many times we will rest");
							Thread.sleep(Configuration.getConfiguration(ConfigurationConstant.CREATE_ERROR_SLEEP_TIME, Long.class));
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						//reset error times
						errorCount = 0;
					}
				}
				if(c == null){
					continue;
				}
				Logger.log("prodocue a new Thread");
				put(c);
			}
			closeLatch.countDown();
		}
	}
	
	
	private class DestroyThread extends Thread{
		public DestroyThread(String name){
			super.setName(name);
		}
		
		public void run(){
			initLatch.countDown();
			while(!isClosed){
				try {
					Thread.sleep(Math.max(Configuration.getConfiguration(ConfigurationConstant.EVICT_CHECK_INTERVAL, Long.class),100));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				shrink(true);
				dealAbandon();
			}
			closeLatch.countDown();
		}
	}
	
	private int dealAbandon(){
        int removeCount = 0;
        long currrentTime = System.currentTimeMillis();
        List<ConnectionHolder> abandonedList = new ArrayList<ConnectionHolder>();
        synchronized (activeContainer.getActiveMap()) {
            Iterator<ConnectionHolder> iter = activeContainer.getActiveMap().keySet().iterator();
            for (; iter.hasNext();) {
            	ConnectionHolder connectionHolder = iter.next();
                if (connectionHolder.isRunning()) {
                    continue;
                }
                long timeMillis = (currrentTime - connectionHolder.getConnectionTime());
                if (timeMillis >= Configuration.getConfiguration(ConfigurationConstant.ABANDON_NEED_LAST_TIME, Long.class)) {
                    iter.remove();
                    abandonedList.add(connectionHolder);
                }
            }
        }
        if (abandonedList.size() > 0) {
            for (ConnectionHolder pooledConnectionHolder : abandonedList) {
                synchronized (pooledConnectionHolder) {
                    if (pooledConnectionHolder.isRunning()) {
                        continue;
                    }
                }
                pooledConnectionHolder.dispose();
                removeCount++;
            }
        }
        if(removeCount > 0){
        	lock.lock();
        	try{
        		empty.signal();
        	}catch(Exception e){
        		e.printStackTrace();
        	}finally{
        		lock.unlock();
        	}
        	Logger.log("abandon size:" + abandonedList.size() + ",left active size:" + activeContainer.getActiveCount());
        }
        return removeCount;
	}
	
	private void shrink(boolean checkTime){
		List<ConnectionHolder> evictArray = new ArrayList<ConnectionHolder>();
		//算上正在等待的
		int checkCount = stackConnectionPool.getPooledCount().get() - waitGetCount.get() - Configuration.getConfiguration(ConfigurationConstant.IDLE_MIN_POOL_NUM, Integer.class);
		long needBeyondTime = System.currentTimeMillis() - Configuration.getConfiguration(ConfigurationConstant.EVICT_IDLE_TIME, Long.class);
		lock.lock();
		try{
			for(int i = 0; i < checkCount; i++){
				if(checkTime){
					if(stackConnectionPool.getConnectionHolder(i).getLastAccessTime() > needBeyondTime)
						break;
				}
				evictArray.add(stackConnectionPool.getConnectionHolder(i));
			}
		    stackConnectionPool.evict(evictArray.size());
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			lock.unlock();
		}
	}
	
	/**
	 * 启动createThread,destroyThread这两个辅助线程
	 */
	private void startAssist() throws InterruptedException{
		createThread = new CreateThread("create-thread-connector");
		destroyThread = new DestroyThread("destroy-thread-connector");
		createThread.start();
		destroyThread.start();
		initLatch.await();
	}
	
	
	public void recycle(ConnectionHolder connectionHolder){
		activeContainer.remove(connectionHolder);
		Logger.log("try to recycle activecount:" + activeContainer.getActiveCount());
		connectionHolder.setLastAccessTime(System.currentTimeMillis());
		put(connectionHolder);
	}
	

}
