package com.saul.bioimpedance2;

public class Indices {

	double lowMag, lowMagCal;
	double lowPha, lowPhaCal;
	double medMag, medMagCal;
	double medPha, medPhaCal;
	double higMag, higMagCal;
	double higPha, higPhaCal;
	
	double lemix,lepix,lerix,leimix;
	double hemix,hepix,herix,heimix;
	double femix,fepix,ferix,feimix;
	
	
	Indices()
	{
		lowMag = 0;
		lowMagCal = 0;
		lowPha = 0;
		lowPhaCal = 0;
		medMag = 0;
		medMagCal = 0;
		medPha = 0;
		medPhaCal = 0;
		higMag = 0;
		higMagCal = 0;
		higPha = 0;
		higPhaCal = 0;
	}

	// MIX = |Z_low|/|Z_high|
	double calcMix(double Zlow, double Zhigh) 
	{
		double aux;
		
		aux = Zlow/Zhigh;
		return aux;
	}
	
	//PIX = Pha(Z_high) - Pha(Z_low)
	double calcPix(double Phigh, double Plow)
	{
		return (Phigh - Plow);
	}
	
	//RIX = Re{Z_low}/|Z_high|
	double calcRix(double Zlow, double Plow, double Zhigh)
	{
		double aux;
		double angleRad;
		
		angleRad = Plow/180.0*Math.PI;
		aux = Zlow*Math.cos(angleRad)/Zhigh;
		
		return aux;
	}
	
	//Setters for raw data
	void setLow(double Z, double P)
	{
		lowMag = Z;
		lowPha = P;		
	}
	
	void setMed(double Z, double P)
	{
		medMag = Z;
		medPha = P;
	}
	
	void setHig(double Z, double P)
	{
		higMag = Z;
		higPha = P;
	}
	
	//Setters for calibrated data
	void setLowCal(double Z, double P)
	{
		lowMagCal = Z;
		lowPhaCal = P;		
	}
	
	void setMedCal(double Z, double P)
	{
		medMagCal = Z;
		medPhaCal = P;
	}
	
	void setHigCal(double Z, double P)
	{
		higMagCal = Z;
		higPhaCal = P;
	}
	
	String calcAllIndices()
	{
		double aux;
		
		String result = new String();
		
		//LEMIX (low and med)
		aux = calcMix(lowMag,medMag);
		result+= "\nLEMIX = " + String.format("%4.3f   ", aux);
		
		//Calibrated LEMIX (low and med)
		aux = calcMix(lowMagCal,medMagCal);
		result+= "Cal. LEMIX = " + String.format("%4.3f\n", aux);
		
		//LEPIX
		aux = calcPix(medPha,lowPha);
		result+= "LEPIX = " + String.format("%4.3f   ", aux);
		
		//Calibrated LEPIX
		aux = calcPix(medPhaCal,lowPhaCal);
		result+= "Cal. LEPIX = " + String.format("%4.3f\n", aux);
		
		//HEMIX (med and high)
		aux = calcMix(medMag,higMag);
		result+= "HEMIX = " + String.format("%4.3f   ", aux);
		
		//Calibrated HEMIX (med and high)
		aux = calcMix(medMagCal,higMagCal);
		result+= "Cal. HEMIX = " + String.format("%4.3f\n", aux);
		
		//HEPIX
		aux = calcPix(higPha,medPha);
		result+= "HEPIX = " + String.format("%4.3f   ", aux);
		
		//Calibrated HEPIX
		aux = calcPix(higPhaCal,medPhaCal);
		result+= "Cal. HEPIX = " + String.format("%4.3f\n", aux);
		
		//FEMIX (low and high)
		aux = calcMix(lowMag,higMag);
		result+= "FEMIX = " + String.format("%4.3f   ", aux);
		
		//Calibrated FEMIX (low and high)
		aux = calcMix(lowMagCal,higMagCal);
		result+= "Cal. FEMIX = " + String.format("%4.3f\n", aux);
		
		//FEPIX (low and high)
		aux = calcPix(higPha,lowPha);
		result+= "FEPIX = " + String.format("%4.3f   ", aux);
		
		//Calibrated FEPIX (low and high)
		aux = calcPix(higPhaCal,lowPhaCal);
		result+= "Cal. FEPIX = " + String.format("%4.3f\n", aux);
			
		return result;
	}
	
	
}
