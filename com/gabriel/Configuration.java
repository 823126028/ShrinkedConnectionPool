package com.gabriel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Configuration { 
	public static final Properties PROPERTIES = new Properties();
	
	public static void load(){
		try {
			FileInputStream is = new FileInputStream("D:/workspace/ConnectionPool/src/com/gabriel/Configuration.properties");
			PROPERTIES.load(is);
			is.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static <T>  T getConfiguration(String key,Class<T> clazz){
		String result = Configuration.getConfiguration(key);
		if(result == null){
			if(clazz == Boolean.class){
				T instance = null;
				try{
					instance = clazz.getConstructor(boolean.class).newInstance(false);
				}catch(Exception e){
					e.printStackTrace();
				}
				return instance;
			}else{
				return null;
			}
		}
		try{
			if(clazz == Long.class){
				return clazz.getConstructor(String.class).newInstance(result.trim());
			}else if(clazz == Integer.class){
				return clazz.getConstructor(String.class).newInstance(result.trim());
			}else if(clazz == String.class){
				return clazz.getConstructor(String.class).newInstance(result.trim());
			}else if(clazz == Boolean.class){
				return clazz.getConstructor(boolean.class).newInstance(Integer.parseInt(result.trim()) == 1);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		throw new RuntimeException("no such type");
		
	}
	
	
	
	private static String getConfiguration(String key){
		return (String) PROPERTIES.get(key);
	}
}
