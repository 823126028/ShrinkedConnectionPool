package com.gabriel.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.gabriel.pool.connection.ConnectionHolder;

public class ActiveContainer {
	public final Object PRESENT = null;
	private Map<ConnectionHolder,Object> activeMap;
	
	public void close(){
		if(activeMap != null && activeMap.size() > 0){
			for (Entry<ConnectionHolder, Object> entry : activeMap.entrySet()) {
				entry.getKey().dispose();
			}
		}
		activeMap.clear();
	}
	
	public ActiveContainer(){
		activeMap = new HashMap<ConnectionHolder,Object>();
	}
	
	public Map<ConnectionHolder,Object> getActiveMap(){
		return activeMap;
	}
	
	public void add(ConnectionHolder connectionHolder){
		synchronized (activeMap) {
			activeMap.put(connectionHolder, PRESENT);
		}
	}
	
	public void remove(ConnectionHolder connectionHolder){
		synchronized (activeMap) {
			activeMap.remove(connectionHolder);
		}
	}
	
	public int getActiveCount(){
		synchronized (activeMap) {
			return activeMap.size();
		}
	}
	
	
}
