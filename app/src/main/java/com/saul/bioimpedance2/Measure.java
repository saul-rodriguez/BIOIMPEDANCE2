package com.saul.bioimpedance2;

//import com.saul.bioimpedance.BluetoothService;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Measure extends Activity {
	
	//Activity Related
	private boolean gp0,gp1,ce,g0,g1,g2,iq,send_email;
	private boolean f0,f1,f2,f3;
	private String email_address;
	private CheckBox F2M, F1M, F500, F250, F125, F62, F31, F15, F8, F4, F2, CAL;
	private TextView currentFrequency;
	private TextView currentMeasurement;
	private TextView measuredImpedance;
	private TextView calibImpedance;
	private EditText numberMeasurements;
	private int measurementCounter;
	private int numMeasurements;
		
	//ASIC driver
	BioASIC bio = null;
	private double m_ADC;
	private double m_I;
	private double m_Q;
	private double m_offset;
	
	//BT related
	private BluetoothService mBT = null;
	public static final int MESSAGE_READ = 2; //Message identifier for the handler
	String BTaddress;			
	
	//Exception handling variables
	private static final String TAG = "THINBTCLIENT";
	private static final boolean D = true;
	
	//Email related
	Emailclient email;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_measure);
		
		// Set View widgets
		F2M = (CheckBox)findViewById(R.id.checkBox2M);
		F1M = (CheckBox)findViewById(R.id.checkBox1M);
		F500 = (CheckBox)findViewById(R.id.checkBox500k);
		F250 = (CheckBox)findViewById(R.id.checkBox250k);
		F125 = (CheckBox)findViewById(R.id.checkBox125k);
		F62 = (CheckBox)findViewById(R.id.checkBox62_5k);
		F31 = (CheckBox)findViewById(R.id.checkBox31_2k);
		F15 = (CheckBox)findViewById(R.id.checkBox15_6k);
		F8 = (CheckBox)findViewById(R.id.checkBox7_8k);
		F4 = (CheckBox)findViewById(R.id.checkBox3_9k);
		F2 = (CheckBox)findViewById(R.id.checkBox1_9k);
		CAL = (CheckBox)findViewById(R.id.checkBoxCal);
		
		currentFrequency = (TextView)findViewById(R.id.textViewFrequency);
		currentMeasurement = (TextView)findViewById(R.id.textViewNumber);
		measuredImpedance = (TextView)findViewById(R.id.MeasImpedance);
		calibImpedance = (TextView)findViewById(R.id.CalibImpedance);
		numberMeasurements = (EditText)findViewById(R.id.editTextNumber);
		
		numberMeasurements.setText("1");
		measurementCounter = 0;
		numMeasurements = 1;
		
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
		
		log_config();
		measurementCounter = 0;
		
		//initialize frequency variables
		f0 = false;
		f1 = false;
		f2 = false;
		f3 = false;		

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
	     	     
						
	}

	public void log_config() {
		if (gp0) {
			Log.d(Constants.LOG,"GP0 TRUE");	
		} else {
			Log.d(Constants.LOG,"GP0 FALSE");
		}
		
		if (gp1) {
			Log.d(Constants.LOG,"GP1 TRUE");	
		} else {
			Log.d(Constants.LOG,"GP1 FALSE");
		}
		
		if (ce) {
			Log.d(Constants.LOG,"CE TRUE");	
		} else {
			Log.d(Constants.LOG,"CE FALSE");
		}
		
		if (g0) {
			Log.d(Constants.LOG,"G0 TRUE");	
		} else {
			Log.d(Constants.LOG,"G0 FALSE");
		}
		
		if (g1) {
			Log.d(Constants.LOG,"G1 TRUE");	
		} else {
			Log.d(Constants.LOG,"G1 FALSE");
		}
		
		if (g2) {
			Log.d(Constants.LOG,"G2 TRUE");	
		} else {
			Log.d(Constants.LOG,"G2 FALSE");
		}
		
		if (iq) {
			Log.d(Constants.LOG,"IQ TRUE");	
		} else {
			Log.d(Constants.LOG,"IQ FALSE");
		}
		
		if (send_email) {
			Log.d(Constants.LOG,"SEND_EMAIL TRUE");	
		} else {
			Log.d(Constants.LOG,"SEND_EMAIL FALSE");
		}
		
		Log.d(Constants.LOG,email_address);		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.measure, menu);
		return true;
	}

	public void onClickSTART(View view) {
		String aux;
		
		Log.d(Constants.LOG,"START clicked");	
		
		
		//Resets counter and reads number of measurements to be processed  
		measurementCounter=0;
		aux = numberMeasurements.getText().toString();
		numMeasurements = Integer.parseInt(aux);
		
		//Clears the body of the email
		email.flushMessage();
		
		Log.d(Constants.LOG,aux);
		
		
		// Sweep the frequencies:
		if (F2M.isChecked()) { // 2MHz
			f3 = false;
			f2 = false;
			f1 = false;
			f0 = false;
			
			bio.setCalibrationIndex(10);
			measure();
			
		}
		
		if (F1M.isChecked()) { // 1MHz
			f3 = false;
			f2 = false;
			f1 = false;
			f0 = true;							
			
			bio.setCalibrationIndex(9);
			measure();
		}
		
		if (F500.isChecked()) { // 500k
			f3 = false;
			f2 = false;
			f1 = true;
			f0 = false;
					
			bio.setCalibrationIndex(8);
			measure();
		}
		
		if (F250.isChecked()) { // 250k
			f3 = false;
			f2 = false;
			f1 = true;
			f0 = true;
					
			bio.setCalibrationIndex(7);
			measure();
		}
		
		if (F125.isChecked()) { //125k
			f3 = false;
			f2 = true;
			f1 = false;
			f0 = false;
			
			bio.setCalibrationIndex(6);
			measure();			
		}
		
		if (F62.isChecked()) { //62.5k
			f3 = false;
			f2 = true;
			f1 = false;
			f0 = true;
			
			bio.setCalibrationIndex(5);
			measure();
		}
		
		if (F31.isChecked()) { //31.25k
			f3 = false;
			f2 = true;
			f1 = true;
			f0 = false;
			
			bio.setCalibrationIndex(4);
			measure();
		}
		
		if (F15.isChecked()) { //15.625k
			f3 = false;
			f2 = true;
			f1 = true;
			f0 = true;
						
			bio.setCalibrationIndex(3);
			measure();
		}
		
		if (F8.isChecked()) { // 7.8125k
			f3 = true;
			f2 = false;
			f1 = false;
			f0 = false;
			
			bio.setCalibrationIndex(2);
			measure();
		}
		
		if (F4.isChecked()) { // 3.90625k
			f3 = true;
			f2 = false;
			f1 = false;
			f0 = true;
			
			bio.setCalibrationIndex(1);
			measure();
		}
		
		if (F2.isChecked()) { // 1.953125k
			f3 = true;
			f2 = false;
			f1 = true;
			f0 = false;
			
			bio.setCalibrationIndex(0);
			measure();
		}
				
	}
	
	/*
	 * measure()
	 * -----------
	 * This function sets the configuration bits of the Bio ASIC and send the configuration and send a full measure command
	 * (offset, I, Q)
	 * */
	
	public void measure() 
	{
		byte[] msgBuffer = new byte[3];
		int m_word;
		String aux;
		
		//Set configuration bits
		bio.setConfig(gp0, gp1, ce, g0, g1, g2, iq, f0, f1, f2, f3);
		aux = "Frequency: " + bio.calcFreq() + " Hz";
		currentFrequency.setText(aux);
		
		//Update counter value and display it
		measurementCounter++;				
		aux = String.format("Meas. number: %d",measurementCounter);
		currentMeasurement.setText(aux);
		
		//Calculate config word
		m_word = bio.calcConfig();
    	
    	msgBuffer[0] = (byte)('d');
    	msgBuffer[1] = (byte)(m_word & 0xff);
    	msgBuffer[2] = (byte)((m_word >> 8) & 0xff);
    	
    	//Send command to the implantable device!
    	mBT.write(msgBuffer);
		
	}
	
	/*
	 *  onClickTest()
	 *  --------------
	 *  This function sends a configuration word to the implantable device. The PIC will immediately answer with a copy of the
	 *  configuration word. The only purpose of this function is to test the communication with the implantable device.
	 *  
	 * */
	
	public void onClickTest(View view) {
		Log.d(Constants.LOG,"Test clicked");	
		byte[] msgBuffer = new byte[3];
		int m_word;
		String aux;
		
		
		//Set configuration bits
		bio.setConfig(gp0, gp1, ce, g0, g1, g2, iq, f0, f1, f2, f3);
		aux = "Frequency: " + bio.calcFreq() + " Hz";
		currentFrequency.setText(aux);
				
				//Update counter value and display it
		measurementCounter++;				
		aux = String.format("Meas. number: %d",measurementCounter);
		currentMeasurement.setText(aux);
				
		//Calculate config word
		m_word = bio.calcConfig();
		
		
    	msgBuffer[0] = (byte)('c');
    	msgBuffer[1] = (byte)(m_word & 0xff);
    	msgBuffer[2] = (byte)((m_word >> 8) & 0xff);
    			
    	mBT.write(msgBuffer);   
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
	            					//MESSAGE SINGLE ENDED ADC
	            		case 'a':   value = (int)((readBuf[2] & 0xff) << 8 |
	              							(readBuf[1] & 0xff) << 0 );	
	            					readMessage = String.format("ADC: %d",value);
	            					//ADC.setText(readMessage);
	            					break;
	            		
	            					//MESSAGE DIFFERENTIAL ADC (ADC2 - ADC4)
	            		case 'b':	value = (int)((readBuf[2] & 0xff) << 8 |
	            							(readBuf[1] & 0xff) << 0 );
	            					value2 = (int)((readBuf[4] & 0xff) << 8 |
	            							(readBuf[3] & 0xff) << 0 );
	            					value -= value2;
	            					m_ADC = value/1024.0*1.8;
	            					
	        						//readMessage = String.format("ADC DIFF: %d",value);
	            					readMessage = String.format("ADC DIFF: %4.3f V",m_ADC);
	        						//ADC_DIFF.setText(readMessage);
	            					break;

	            					//MESSAGE CONFIGURATION WORD 
	            		case 'c':	value = (int)((readBuf[2] & 0xff) << 8 |
	          								(readBuf[1] & 0xff) << 0 );	
	        						readMessage = String.format("processed: %s%X",command,value);
	        						Log.d(Constants.LOG,readMessage);
	        						ConnectedMessage(readMessage);	        						
	        						//answer.setText(readMessage);
	        						
	        						
	        						break;
	        						
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
									//i_path.setText(aux);								
									
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
									//q_path.setText(aux);
									
																											
									bio.setMeasurements(m_I, m_Q, m_offset);
									
									
									//Check if calibration is selected and calibrate frequency
									if(CAL.isChecked()){
										bio.calibrateFreq();
									}
									
									aux = bio.calc_impedance();
									measuredImpedance.setText(aux);
									
									//Append measured data to send via email
									email.appendMeasurement(bio.getMagnitude(), bio.getPhase());
									
									//Calibrate results and append data to send via email
									aux = bio.calibrateResults();
									calibImpedance.setText(aux);
									email.appendCalibrated(bio.getCalibratedMagnitude(), bio.getCalibratedPhase());
																	
									//Start a new measurement if needed
									if (measurementCounter < numMeasurements) { // Start next measurement
										measure();
										
									} else { // Save results
										currentMeasurement.setText("Meas. finished");
										
										if (send_email) { //Check email flag and send results to email
											email.prepareEmail(bio.calcFreq());
											sendEmail();
										}
									}
																											
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
	
	void ConnectedMessage(String message) 
	{
		String aux;
		aux = "Config sent/receiver " + message;
		
		Toast.makeText(this, aux, Toast.LENGTH_SHORT).show();
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
