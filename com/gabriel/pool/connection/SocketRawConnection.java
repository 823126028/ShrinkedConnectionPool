package com.gabriel.pool.connection;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketRawConnection implements Connection{
	private Socket socket;

	public void init(String ip,int port) throws UnknownHostException, IOException{
		socket = new Socket(ip, port);
	}
	
	public boolean checkValid(){
		try {
			socket.sendUrgentData(1);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public void close() {
		if(socket != null)
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void write(Object object) {
		try {
			socket.getOutputStream().write(object.toString().getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//socket.getChannel().write(src)
	}
}
