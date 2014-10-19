package com.example.android.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class LoggerExample {

	public static void main(String[] args) {  

	    Logger logger = Logger.getLogger("MyLog");  
	    FileHandler fh;  

	    try {  

	        // This block configure the logger with handler and formatter  
	    	logger.setUseParentHandlers(false);
	        fh = new FileHandler("PATH>use environment.getexternalpublicstorage....");  
	        logger.addHandler(fh);
	        
	        CustomFormatter formatter = new CustomFormatter();
	        fh.setFormatter(formatter);  

	        // the following statement is used to log any messages  
	        logger.info("My first log");  

	    } catch (SecurityException e) {  
	        e.printStackTrace();  
	    } catch (IOException e) {logger.setUseParentHandlers(false);  
	        e.printStackTrace();  
	    }  

	    logger.info("Hi How r u?");  

	}
}
