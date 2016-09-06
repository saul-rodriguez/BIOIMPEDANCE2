package com.saul.bioimpedance2;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import android.content.Context;
import java.io.FileInputStream;
import java.util.Locale;

import android.util.Log;

public class BioASIC {
	private boolean gp0,gp1,ce,g0,g1,g2,iq;
	private boolean f0,f1,f2,f3;
	
	private double m_ADC;
	private double m_I;
	private double m_Q;
	private double m_offset;
	
	private double magnitude;
	private double phase;
	
	private double magnitude_calibrated;
	private double phase_calibrated;
	private int cal_freq_index;
	
	private final Context mContext;
	//private double[] calib_magnitude;
	
	private double[] calib_magnitude = {0.982,
			0.980,
			0.969,
			0.969,
			0.971,
			0.979,
			0.992,
			1.001,
			1.016,
			1.000,
			1.109};
		
	private double[] calib_phase = {-0.372,
			-0.289,
			-0.483,
			0.034,
			0.109,
			0.547,
			1.755,
			3.176,
			8.951,
			21.550,
			36.683};
	
	BioASIC(Context context) 
	{
		// Initialization of Settings 
		gp0 = true;
		gp1 = true;
		ce = false;
		g0 = true;
		g1 = true;
		g2 = true; // g2 is always 1!!!!!!!
		iq = false;
		f0 = false;
		f1 = false;
		f2 = false;
		f3 = false;
		
		cal_freq_index = 0;
		
		mContext = context;
		
		//Read calibration file
		readCalFile();
			
	}
	
	/*
	 *  setConfig(boolean configbit1, ....) 
	 *  --------------------------------------
	 *  The function setConfig() sets all the configuration bits that are used to form the configuration word for the BIO chip. 
	 * 
	 * */
	
	public void setConfig(boolean gp0, boolean gp1, boolean ce, boolean g0, boolean g1, boolean g2, boolean iq, boolean f0, boolean f1, boolean f2, boolean f3) {
		this.gp0 = gp0;
		this.gp1 = gp1;
		this.ce = ce;
		this.g0 = g0;
		this.g1 = g1;
		this.g2 = g2;
		this.iq = iq;
		this.f0 = f0;
		this.f1 = f1;
		this.f2 = f2;
		this.f3 = f3;		
		
		//Only for debug purposes
		//calcFreq();
	}
	
	
	/*
	 *  calcConfig()
	 *  --------------
	 *  Returns the configuration word that will be sent to the BIO chip
	 *  
	 * */
	
	public int calcConfig() {
		int aux;
		aux = 0x0000;
		
		if (gp0) {
			aux |= 0x0001;
		}
		
		if (gp1) {
			aux |= 0x0002;
		}
		
		if (ce) {
			aux |= 0x0004;
		}
				
		if (g0) {
			aux |= 0x0008;
		}
		
		if (g1) {
			aux |= 0x0010;
		}
		
		// note that g2 is not modified in config, and it is always 1!!!!!
		if (g2) {  
			aux |= 0x0020;
		}
		
		if (iq) { // Q = 0, I = 1
			aux |= 0x0040;
		}
		
		if (f0) {
			aux |= 0x0080;
		}
		
		if (f1) {
			aux |= 0x0100;
		}
		
		if (f2) {
			aux |= 0x0200;
		}
		
		if (f3) {
			aux |= 0x0400;
		}
				
		String word = String.format("Configuration word 0x%X",aux);
		//config_word.setText(word);
		Log.d(Constants.LOG,word);
		//calcFreq();
		
		return aux;
	}


	/*
	 * calcFreq()
	 * --------------
	 * Returns the selected frequency as a String
	 * 
	 * */
	
	public String calcFreq() {
		int aux;
		aux = 0;
		double freq;
	
		if (f0) {
			aux |= 0x0001;
		}
	
		if (f1) {
			aux |= 0x0002;
		}
	
		if (f2) {
			aux |= 0x0004;
		}
	
		if (f3) {
			aux |= 0x0008;
		}
	
		freq = 2e6/Math.pow(2, (aux));
	
	
		//String my_freq = String.format("Frequency: %4.2f Hz",freq);
		String my_freq = String.format("%4.2f",freq);
	
		Log.d(Constants.LOG,my_freq);
		
		return my_freq;
	}
	
	/*
	 * setMeasurements(I,Q,offset)
	 * ---------------------------
	 * Sets measured values for I,Q and offset in Volts. This function must be called before calculating impedance (calc_impedance()) 
	 * 
	 * */
	
	public void setMeasurements(double I, double Q, double offset) 
	{
		m_I = I;
		m_Q = Q;
		m_offset = offset;		
	}
	
	/*
	 * calc_impedance()
	 * -----------------
	 * Calculates impedance and phase. The function uses the saved measurements of m_I,m_Q and m_offset in order to perform calculations.
	 * 
	 * Returns: A string with the impedance magnitude [Ohms] and the phase [degrees]
	 * 
	 * TODO: Add calibration function
	 * */
	
	public String calc_impedance() {
		double MAG;
		double Z;
		double angle;
		double I1, Q1;
		double dg0,dg1,dg2;
		String aux;
		
		// Extract the value of the gain factors
		if (g0) {
			dg0 = 1;
		} else {
			dg0 = 0;
		}
		
		if (g1) {
			dg1 = 1;
		} else {
			dg1 = 0;
		}
		
		if (g2) {
			dg2 = 1;
		} else {
			dg2 = 0;
		}
			
		// Remove the offset
		I1 = m_I - m_offset;
		Q1 = m_Q - m_offset;
				
		//Calculate the Magnitude and Angle
		//IMP1 = MAG/(9.4314e-05*(1+2*G0)*(1+2*G1)*(1+2*G2))
		MAG = Math.sqrt((I1*I1 + Q1*Q1));   //Absolute value in Volts!		
		angle = Math.atan((Q1/I1))*180/Math.PI;
		angle += 22.5; // Phase correction due to I/Q - Signal generator generation
		
		Z = MAG/(9.4314e-05*(1+2*dg0)*(1+2*dg1)*(1+2*dg2));
		
		aux = String.format("|Z| = %4.3f, ANGLE = %4.3f",Z,angle);
		
		Log.d(Constants.LOG, aux);
		//impedance.setText(aux);
		
		magnitude = Z;
		phase = angle;		
		
		return aux;				
				
	}
		
	public double getMagnitude()
	{
		return magnitude;
	}
	
	public double getPhase()
	{
		return phase;		
	}
	
	public String calibrateResults()
	{
		String aux;
		
		magnitude_calibrated = magnitude*calib_magnitude[cal_freq_index];
		phase_calibrated = phase - calib_phase[cal_freq_index];
		
		aux = String.format("|Z| = %4.3f, ANGLE = %4.3f",magnitude_calibrated, phase_calibrated);
		
		return aux;
	}
	
	public void setCalibrationIndex(int cal)
	{
		cal_freq_index = cal;	
	}
	
	public double getCalibratedMagnitude()
	{
		return magnitude_calibrated;		
	}
	
	public double getCalibratedPhase()
	{
		return phase_calibrated;
	}
	
	public void calibrateFreq() {
		calc_impedance();
		
		double newCalMag, newCalPha;
		
		newCalMag = 100./magnitude;
		newCalPha = phase;
		
		Log.d(Constants.LOG, "newCalMag: " + String.format("%f", newCalMag));
		Log.d(Constants.LOG, "newCalPha: " + String.format("%f", newCalPha));
		
		calib_magnitude[cal_freq_index] = newCalMag;
		calib_phase[cal_freq_index] = newCalPha;
		
		saveCalFile();
		
	}
	
	public void readCalFile()
	{
		Log.d("Saul","Reading calibration file");
		
		try {

			BufferedReader inputReader = new BufferedReader(new InputStreamReader(mContext.openFileInput("calibrationdata")));

			String inputString;
			StringBuffer stringBuffer = new StringBuffer();                

			int i = 0;
			
			for (i = 0; i < calib_magnitude.length; i++) {
				//calibration factor
				inputString = inputReader.readLine();
				calib_magnitude[i] = Double.parseDouble(inputString);
				//stringBuffer.append(inputString + "\n");
				Log.d("Saul", String.format("%4.3f", calib_magnitude[i]));
				
				//calibration phase
				inputString = inputReader.readLine();
				calib_phase[i] = Double.parseDouble(inputString);
				//stringBuffer.append(inputString + "\n");
				Log.d("Saul", String.format("%4.3f", calib_phase[i]));
				
			}

			//Log.d("Saul", stringBuffer.toString());
			
			
		} catch (IOException e) {
			
			e.printStackTrace();
			//Problem opening the file (File not found)
			//Create a new calibration file
			
			SaveFirstCal();
			Log.d(Constants.LOG,"Creating Calibration file");
			
		}
	}
	
	public void saveCalFile()
	{
						
		try {

		    FileOutputStream fos = mContext.openFileOutput("calibrationdata", Context.MODE_PRIVATE);

		    
		    String aux;
		    for (int i= 0; i < calib_magnitude.length; i++) {
		    	aux = String.format(Locale.ENGLISH,"%5.4f\n", calib_magnitude[i]);
		    	fos.write(aux.getBytes());
		    	aux = String.format(Locale.ENGLISH,"%5.4f\n", calib_phase[i]);
		    	fos.write(aux.getBytes());  	
		    	
		    }
		    

		    fos.close();

		} catch (Exception e) {

		    e.printStackTrace();

		}
	}
	
	public void SaveFirstCal()
	{
		//Create the "calibrationdata" file with calibration magnitude =  1 and calibration phase = 0 (noncalibrated == calibrated)
		
		for (int i=0; i < calib_magnitude.length; i++) {
			calib_magnitude[i] = 1.0;
			calib_phase[i] = 0.0;
		}
		
		try {

		    FileOutputStream fos = mContext.openFileOutput("calibrationdata", Context.MODE_PRIVATE);
		    
		    
		    String aux;
		    for (int i= 0; i < calib_magnitude.length; i++) {
		    	aux = String.format(Locale.ENGLISH,"%5.4f\n", calib_magnitude[i]);
		    	fos.write(aux.getBytes());
		    	aux = String.format(Locale.ENGLISH,"%5.4f\n", calib_phase[i]);
		    	fos.write(aux.getBytes());  	
		    	
		    }

		    fos.close();

		} catch (Exception e) {

		    e.printStackTrace();

		}
		
	}
	
}
