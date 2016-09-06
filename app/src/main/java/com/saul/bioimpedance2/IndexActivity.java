package com.saul.bioimpedance2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class IndexActivity extends Activity {

	//Activity Related
	private boolean gp0,gp1,ce,g0,g1,g2,iq,send_email;
	private boolean f0,f1,f2,f3;
	private String email_address;
	private TextView results;
	private String resultsText;
	private TextView status;
	
	//BT related
	private BluetoothService mBT = null;
	public static final int MESSAGE_READ = 2; //Message identifier for the handler
	String BTaddress;
	
	//Exception handling variables
	private static final String TAG = "THINBTCLIENT";
	private static final boolean D = true;
	
	//ASIC driver
	BioASIC bio = null;
	private double m_ADC;
	private double m_I;
	private double m_Q;
	private double m_offset;
		
	//Email related
	Emailclient email;
	
	//State Machine
	final static int IDLE = 0;
	final static int LOWFREQ = 1;
	final static int MEDFREQ = 2;
	final static int HIGFREQ = 3;
	int statemachine;
	
	Indices indices;
	
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_index);
		
		status = (TextView)findViewById(R.id.textViewStatus);
		results = (TextView)findViewById(R.id.textViewIndex);
		results.setTextSize(15);
		
		resultsText = new String();
		
		// Get the configuration via intent
		Intent intent = getIntent();
				
		gp0 = intent.getBooleanExtra(MainActivity.MESS_GP0,false);		
		gp1 = intent.getBooleanExtra(MainActivity.MESS_GP1,false);
		ce = intent.getBooleanExtra(MainActivity.MESS_CE,false);
		g0 = intent.getBooleanExtra(MainActivity.MESS_G0,false);
		g1 = intent.getBooleanExtra(MainActivity.MESS_G1,false);
		g2 = intent.getBooleanExtra(MainActivity.MESS_G2,false);
		iq = intent.getBooleanExtra(MainActivity.MESS_IQ,false);
		send_email = intent.getBooleanExtra(MainActivity.MESS_EMAIL_BOX,false);				
		email_address = intent.getStringExtra(MainActivity.MESS_EMAIL);
		BTaddress = intent.getStringExtra(MainActivity.MESS_BTADDRESS);
		
		//Create a object for the BIO ASIC driver
		bio = new BioASIC(this);
		m_ADC = 0;
		m_I = 0;
		m_Q = 0;
		m_offset = 0;
		
		//BT related
		mBT = new BluetoothService(this,mHandler,BTaddress);	       
	    if (mBT.on_create() == 0) finish(); // BT unsupported in the device
	     
	     //Email related
	     email = new Emailclient();
	     email.setMailto(email_address);
	     
	     //State Machine
	     statemachine = IDLE;
	     indices = new Indices();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.index, menu);
		return true;
	}
	
	public void measure() 
	{
		byte[] msgBuffer = new byte[3];
		int m_word;
		String aux;
		
		//Set configuration bits
		bio.setConfig(gp0, gp1, ce, g0, g1, g2, iq, f0, f1, f2, f3);
		aux = "Frequency: " + bio.calcFreq() + " Hz";
		//currentFrequency.setText(aux);		
		status.setText(aux);
		
		//Calculate config word
		m_word = bio.calcConfig();
    	
    	msgBuffer[0] = (byte)('d');
    	msgBuffer[1] = (byte)(m_word & 0xff);
    	msgBuffer[2] = (byte)((m_word >> 8) & 0xff);
    	
    	//Send command to the implantable device!
    	mBT.write(msgBuffer);
		
	}

	//Button Measure Indices pressed
	public void onMeasureIndex(View view)
	{
		Log.d(Constants.LOG,"Measuring Indexes");
		
		//Start execution only in IDLE state
		if (statemachine == IDLE) {
			resultsText = "";
			results.setText("");
			changeState();
		} else  { // REset the state machine
			statemachine = IDLE;
			resultsText = "";
			results.setText("");
		}

	}
	
	public void onConnect(View view)
	{
		 mBT.on_pause();  //Here all the BT services should be stopped (threads, etc)
		 if (mBT.on_resume() == 0) finish(); 
	}
	
	public void changeState()
	{
		String aux1,aux2;
		double mag,pha;
		Log.d(Constants.LOG,"Changing indices state");
		
		switch (statemachine) {
			case IDLE:		
							statemachine = LOWFREQ; // Change state to7.8125KHz
							f3 = true;
							f2 = false;
							f1 = false;
							f0 = false;
							bio.setCalibrationIndex(2);
							measure();
							break;
							
			case LOWFREQ:	
							//Read results of measurement							
							aux1 = bio.calc_impedance();				
							aux2 = bio.calibrateResults();
							resultsText += "8KHz\n";
							resultsText += "RAW: " + aux1 + "\n";
							resultsText += "CAL: " + aux2 + "\n";
							results.setText(resultsText);
							
							//mag = bio.getMagnitude();
							//pha = bio.getPhase();
							indices.setLow(bio.getMagnitude(), bio.getPhase());
							indices.setLowCal(bio.getCalibratedMagnitude(),bio.getCalibratedPhase());
							
							//Change state to 125kHz and start measurement
							statemachine = MEDFREQ; 
							f3 = false;
							f2 = true;
							f1 = false;
							f0 = false;
							bio.setCalibrationIndex(6);
							measure();					
							break;
							
			case MEDFREQ:		
							// Read results of measurement
							aux1 = bio.calc_impedance();				
							aux2 = bio.calibrateResults();
							resultsText += "125KHz\n";
							resultsText += "RAW: " + aux1 + "\n";
							resultsText += "CAL: " + aux2 + "\n";
							results.setText(resultsText);
							indices.setMed(bio.getMagnitude(), bio.getPhase());
							indices.setMedCal(bio.getCalibratedMagnitude(),bio.getCalibratedPhase());
				
							//Change state to 1MHz and start measurement
							statemachine = HIGFREQ; 
							f3 = false;
							f2 = false;
							f1 = false;
							f0 = true;
							bio.setCalibrationIndex(9);
							measure();
							break;
							
			case HIGFREQ:
							// Read results of measurement
							aux1 = bio.calc_impedance();				
							aux2 = bio.calibrateResults();
							resultsText += "1MHz\n";
							resultsText += "RAW: " + aux1 + "\n";
							resultsText += "CAL: " + aux2 + "\n";
							//results.setText(resultsText);
							indices.setHig(bio.getMagnitude(), bio.getPhase());
							indices.setHigCal(bio.getCalibratedMagnitude(),bio.getCalibratedPhase());
							
							resultsText += indices.calcAllIndices();
							results.setText(resultsText);
							
							//Change state to idle
							statemachine = IDLE;
							status.setText("STATUS: IDLE"); // Change status to IDLE
							
							//Send email if selected
							if (send_email) { //Check email flag and send results to email
								email.prepareIndicesEmail(resultsText);
								sendEmail();
							}
							break;
		}
		 
		
	}
	
	private final Handler mHandler = new Handler() {
	  	  @Override
	        public void handleMessage(Message msg) {
	  		  	int value, value2;
	  		  	String readMessage;
	  		  	
	            switch (msg.what) {
	            	case MESSAGE_READ: 
	            		
	            		//construct a string from the valid bytes in the buffer
	            	byte[] readBuf = (byte[]) msg.obj;
	            	String command = new String(readBuf,0,1); // First character is the command
	            	String aux;
	            	
	            	switch (readBuf[0]) {
	            				        						
	        						//MESSAGE IMPEDANCE MEASUREMENT
	            		case 'd':	//Offset
	            					value = (int)((readBuf[2] & 0xff) << 8 |
											(readBuf[1] & 0xff) << 0 );
									value2 = (int)((readBuf[4] & 0xff) << 8 |
											(readBuf[3] & 0xff) << 0 );
									value -= value2;
									m_ADC = value/1024.0*1.8;
									
									m_offset = m_ADC;
									aux = String.format("Offset = %4.3f V",m_offset);
									Log.d(Constants.LOG, aux);									
									//offset.setText(aux);
									
									//I path
									value = (int)((readBuf[6] & 0xff) << 8 |
											(readBuf[5] & 0xff) << 0 );
									value2 = (int)((readBuf[8] & 0xff) << 8 |
											(readBuf[7] & 0xff) << 0 );
									value -= value2;
									m_ADC = value/1024.0*1.8;
									
									m_I = m_ADC;
									aux = String.format("m_I = %4.3f V",m_I);
									Log.d(Constants.LOG, aux);
											
									
									//Q path
									value = (int)((readBuf[10] & 0xff) << 8 |
											(readBuf[9] & 0xff) << 0 );
									value2 = (int)((readBuf[12] & 0xff) << 8 |
											(readBuf[11] & 0xff) << 0 );
									value -= value2;
									m_ADC = value/1024.0*1.8;
									
									m_Q = m_ADC;
									aux = String.format("m_Q = %4.3f V",m_Q);
									Log.d(Constants.LOG, aux);
																											
									bio.setMeasurements(m_I, m_Q, m_offset);
									
									aux = bio.calc_impedance();
									//measuredImpedance.setText(aux);
									
									//Append measured data to send via email
									//email.appendMeasurement(bio.getMagnitude(), bio.getPhase());
									
									//Calibrate results and append data to send via email
									aux = bio.calibrateResults();
									//calibImpedance.setText(aux);
									//email.appendCalibrated(bio.getCalibratedMagnitude(), bio.getCalibratedPhase());
																	
									//Start a new measurement if needed
									//if (measurementCounter < numMeasurements) { // Start next measurement
									//	measure();
										
									//} else { // Save results
									//	currentMeasurement.setText("Meas. finished");
									changeState();
									
										//if (send_email) { //Check email flag and send results to email
										//	email.prepareEmail(bio.calcFreq());
										//	sendEmail();
										//}
									//}
																											
	            		default:	break;            	
	            	
	            	}	            	
	              
	              break;
	            }
	  	  }
	
	};
	
	void sendEmail()
	{
		Intent emailint = new Intent(Intent.ACTION_SEND);
        emailint.putExtra(Intent.EXTRA_EMAIL, new String[] { email.to });
        emailint.putExtra(Intent.EXTRA_SUBJECT, email.subject);
        emailint.putExtra(Intent.EXTRA_TEXT, email.message);

        // need this to prompts email client only
        emailint.setType("message/rfc822");

        startActivity(Intent.createChooser(emailint, "Choose an Email client"));
		
	}
		
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
		 mBT.on_pause();  //Here all the BT services should be stopped (threads, etc)
		 Log.d(Constants.LOG,"ON PAUSE");
		
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		
		if (mBT.on_resume() == 0) finish(); 
		Log.d(Constants.LOG,"ON RESUME");
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		
		//if (mBT.on_resume() == 0) finish();
		Log.d(Constants.LOG,"ON START");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		
		Log.d(Constants.LOG,"ON STOP");
		
	}
	  	  
	
}
