package com.saul.bioimpedance2;

import android.text.format.Time;
import android.util.Log;

public class Emailclient {
	
	public String to;
    public String subject;
    public String message;
    public String measure;
    public String calibrated;
    
    Emailclient()
    {
    	to = new String("");
    	subject = new String("");
    	message = new String("");  
    	measure = new String("");
    	calibrated = new String("");
    	
    }
    
    void setMailto(String emailaddress)
    {
    	to = emailaddress;    	
    }
    
    void prepareEmail(String freq)
    {
    	String clock;
		 	    	
		// Get the current date/time
		Time today = new Time(Time.getCurrentTimezone());
    	today.setToNow();    	
    	
    	clock = today.format2445(); //This formats a string with Y M D H M S
    	
    	//Log.d("Saul", clock);    	  
		    	
    	subject = "BIO "+ freq + " " + clock;
    	
    	Log.d("Saul", subject);  
    	
    	//Create the body of the email
    	message = "Measured Data\n";
    	message += measure;
    	message += "\n";
    	message += "Calibrated Data\n";
    	message += calibrated;    	
    	
    }
    
    void appendMessage(String newline)
    {
    	message += newline;
    	message += "\n";    	
    }
    
    void appendMeasurement(double magnitude, double phase)
    {
    	String clock,aux;
	    	
		// Get the current date/time
		Time today = new Time(Time.getCurrentTimezone());
    	today.setToNow();    	
    	clock = today.format2445(); //This formats a string with Y M D H M S
    	
    	aux = clock +", "+ String.format("%4.3f, %4.3f\n",magnitude,phase);
    	
    	measure += aux;
    	
    	Log.d(Constants.LOG,aux);    	   	
    	
    }
    
    
    void appendCalibrated(double magnitude, double phase)
    {
    	String clock,aux;
	    	
		// Get the current date/time
		Time today = new Time(Time.getCurrentTimezone());
    	today.setToNow();    	
    	clock = today.format2445(); //This formats a string with Y M D H M S
    	
    	aux = clock +", "+ String.format("%4.3f, %4.3f\n",magnitude,phase);
    	
    	calibrated += aux;
    	
    	Log.d(Constants.LOG,aux);    	   	
    	
    }
    void flushMessage()
    {
    	message = "";
    	measure = "";
    	calibrated = "";    	
    }
    
    void prepareIndicesEmail(String data)
    {
    	String clock;
 	
    	//Get the current date/time
    	Time today = new Time(Time.getCurrentTimezone());
    	today.setToNow();    	

    	clock = today.format2445(); //This formats a string with Y M D H M S

    	//Log.d("Saul", clock);    	  

    	subject = "BIO Indices " + clock;

    	Log.d("Saul", subject);  

    	//	Create the body of the email
    	message = data;    	
    	
    }
        
}
