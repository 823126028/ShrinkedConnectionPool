package com.gabriel;

import com.gabriel.pool.ConnectionSource;
import com.gabriel.pool.connection.ConnectionHolder;

public class TestMain {
	
	public static void testMaxCountAPI(ConnectionSource connectionSource){
		Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				for(int i = 0; i <= 60; i++){
					ConnectionHolder c = connectionSource.getConnectionHolder();
					if(i % 2 == 0){
						if(c != null)
							c.close();
					}
				}
			}
		});
		t.start();
	}
	
	public static void testEvict(ConnectionSource connectionSource){
		//let innital time more than min idle
		while(true){
			try{
				Thread.sleep(10000);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public  static void  main(String[] args) throws InterruptedException{
		Configuration.load();
		for(int i =0 ; i<= 50; i++){
			ConnectionSource connectionSource = new ConnectionSource();
			try {
				connectionSource.init("127.0.0.1", 9093);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | InterruptedException e) {
				e.printStackTrace();
			}
			testMaxCountAPI(connectionSource);
			Thread.sleep(200);
			//testEvict(connectionSource);
			connectionSource.close();
		}
		System.out.println("结束!!!!!!");
		Thread.sleep(100000);
	}
}
