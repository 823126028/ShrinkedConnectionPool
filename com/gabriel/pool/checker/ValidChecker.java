package com.gabriel.pool.checker;

import com.gabriel.pool.connection.ConnectionHolder;

public class ValidChecker {
	public boolean check(ConnectionHolder connectionHodler){
		connectionHodler.beforeExecute();
		boolean isValid = connectionHodler.checkValid();
		connectionHodler.afterExecute();
		return isValid;
	}
}
