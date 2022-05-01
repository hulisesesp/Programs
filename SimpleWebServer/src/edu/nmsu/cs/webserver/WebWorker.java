package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s){
		socket = s;
	}
	
	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run(){
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			//add things in the next 3 lines
			String filename = readHTTPRequest(is);
			File reqFile = new File(filename);
			
			if(reqFile.getName().toLowerCase().endsWith("html")){
				writeHTTPHeader(os, reqFile, "text/html");
				writeContent(os, reqFile, "text/html");
			}
			
			if(reqFile.getName().toLowerCase().endsWith("png")){
				writeHTTPHeader(os, reqFile, "image/png");
				writeContent(os, reqFile, "image/png");
			}
			
			if(reqFile.getName().toLowerCase().endsWith("gif")){
				writeHTTPHeader(os, reqFile, "image/gif");
				writeContent(os, reqFile, "image/gif");
			}
			
			if(reqFile.getName().toLowerCase().endsWith("jpeg") || reqFile.getName().toLowerCase().endsWith("jpg")){
				writeHTTPHeader(os, reqFile, "image/jpeg");
				writeContent(os, reqFile, "image/jpeg");
			}
			
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private String readHTTPRequest(InputStream is){
		int h = 0;
		String line;
		String reqFileName = null;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true){
			try{
				 
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				
				if(h == 0) {
					reqFileName = line;
					reqFileName = reqFileName.substring(5, reqFileName.length()-9);
					System.out.println(reqFileName);
				    h = 1;
				}if(line.length() == 0) {
					break;
				}
				
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
			}catch (Exception e){
				System.err.println("Request error: " + e);
				return "404";
			}
			
		}
		return "www/" + reqFileName;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, File reqFile, String contentType) throws Exception
	{
		int flag =0;
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
			if(!reqFile.exists()){
				flag = 404;   
			}//end catch

			if(flag == 0){
				os.write("HTTP/1.1 200 OK\n".getBytes());   
			}else{
				os.write("HTTP/1.1 404 Not Found\n".getBytes());
			}

			os.write("Date: ".getBytes());
			os.write((df.format(d)).getBytes());
			os.write("\n".getBytes());
			os.write("Server: My very own server\n".getBytes());
			os.write("Connection: close\n".getBytes());
			os.write("Content-Type: ".getBytes());
			os.write(contentType.getBytes());
			os.write("\n\n".getBytes());
			return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, File reqFile, String contentType)throws IOException{

		Date d = new Date();
		DateFormat dformat = DateFormat.getDateTimeInstance();
	    dformat.setTimeZone(TimeZone.getTimeZone("GMT-7"));
	    String fcont = "";
	    String address = reqFile.toString();
	    String date = dformat.format(d);
	    
	    if(contentType == "text/html") {
	      try{
	         FileReader fRead = new FileReader(reqFile);

	         System.out.println(fRead);
	         BufferedReader fBuff = new BufferedReader(fRead);
	         int i = 0;
	         String h = null;
	         while((fcont = fBuff.readLine()) != null) {
	        	 if(i==4) {
	        	 h = fcont;
	        	 }
	              i++;
	         }
	         String target = "<cs371date>";
	         String replacement = date;
	         String processed = h.replace(target, replacement);
	         //System.out.println(processed);
	         String target1 = "<cs371server>";
	         String replacement1 = "Hulises' server";
	         String processed1 = processed.replace(target1, replacement1);
	         os.write(processed1.getBytes());
	           
	      }catch(FileNotFoundException e) {
	         System.err.println("File not found: " + address);
	         os.write("HTTP/1.1 404 Not Found\n".getBytes());
	      }
	    }else {
	    		FileInputStream f = new FileInputStream(reqFile);
	    		int j = f.available();
	    		//use array to store the data
	    		byte[] B = new byte[j];
	    		f.read(B);
	    		f.close();
	    		os.write(B);
	    }
	    

	      
	    
	    
	    
	}  
}

