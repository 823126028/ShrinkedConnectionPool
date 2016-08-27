package com.gabriel.pool.exception;

public class AlreadyClosedException extends Exception{
	
	private static final long serialVersionUID = 1L;

	public AlreadyClosedException(String msg,Throwable throwable){
		super(msg, throwable);
	}
	public AlreadyClosedException(String msg){
		super(msg);
	}
}
