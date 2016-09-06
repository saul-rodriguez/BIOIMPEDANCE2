package com.saul.bioimpedance2;


import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//import com.example.btspin.MainActivity.CustomOnItemSelectedListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public final static String MESS_GP0 = "MESS_GP0";
	public final static String MESS_GP1 = "MESS_GP1";
	public final static String MESS_CE = "MESS_CE";
	public final static String MESS_G0 = "MESS_G0";
	public final static String MESS_G1 = "MESS_G1";
	public final static String MESS_G2 = "MESS_G2";
	public final static String MESS_IQ = "MESS_IQ";
	public final static String MESS_EMAIL_BOX = "MESS_EMAIL_BOX";
	public final static String MESS_EMAIL = "MESS_EMAIL";
	public final static String MESS_BTADDRESS = "MESS_BTADDRESS";
		
	
	private CheckBox GP0,GP1,CE,G0,G1,G2,IQ,email_box;
	private EditText email;
	
	//BT module selection
	final String MLOG = "BT";
	final int REQUEST_ENABLE_BT = 1;	
	private Spinner spinner1;
	private List<String> BTlist;
	private String[] BTAddress;
	private String[] BTNames;
	
	String SelectedBTname;
	String SelectedBTaddress;
	private TextView SelectedBT;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Initialize the widgets
				GP0 = (CheckBox)findViewById(R.id.GP0);
				GP1 = (CheckBox)findViewById(R.id.GP1);
				CE = (CheckBox)findViewById(R.id.CE);
				G0 = (CheckBox)findViewById(R.id.G0);
				G1 = (CheckBox)findViewById(R.id.G1);
				G2 = (CheckBox)findViewById(R.id.G2);
				IQ = (CheckBox)findViewById(R.id.IQ);
		
				email_box = (CheckBox)findViewById(R.id.checkBoxEmail);
				email = (EditText)findViewById(R.id.editTextEmail);
				
				email.setText("saul@kth.se");
				
				SelectedBT = (TextView)findViewById(R.id.TextViewBT);
				spinner1 = (Spinner)findViewById(R.id.spinner1);        
			    BTlist = new ArrayList<String>();
			    BTAddress = new String[20];
			    BTNames = new String[20];
			     
			    readBTaddress();
				
				startBTspinner();
				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void smallMes(View view) {
		OnConnect(view);
	}
	
	public void smallInd(View view) {
		onIndices(view);
	}
	
	public void OnConnect(View view) {
		boolean gp0,gp1,ce,g0,g1,g2,iq,send_email;
		String email_address;
		
		gp0 = GP0.isChecked();
		gp1 = GP1.isChecked();
		ce = CE.isChecked();
		g0 = G0.isChecked();
		g1 = G1.isChecked();
		g2 = G2.isChecked();
		iq = IQ.isChecked();
		send_email = email_box.isChecked();
		email_address = email.getText().toString();
				
		Log.d(Constants.LOG, "Connecting to Reader");
		
		Intent intent = new Intent(this, Measure.class);
		
		//String message = "Hola";
		intent.putExtra(MESS_GP0,gp0);
		intent.putExtra(MESS_GP1,gp1);
		intent.putExtra(MESS_CE,ce);
		intent.putExtra(MESS_G0,g0);
		intent.putExtra(MESS_G1,g1);
		intent.putExtra(MESS_G2,g2);
		intent.putExtra(MESS_IQ,iq);
		intent.putExtra(MESS_EMAIL_BOX,send_email);
		intent.putExtra(MESS_EMAIL,email_address);
		intent.putExtra(MESS_BTADDRESS, SelectedBTaddress);
				
		startActivity(intent);	

	}

	public void onIndices(View view)
	{
		boolean gp0,gp1,ce,g0,g1,g2,iq,send_email;
		String email_address;
			
		gp0 = GP0.isChecked();
		gp1 = GP1.isChecked();
		ce = CE.isChecked();
		g0 = G0.isChecked();
		g1 = G1.isChecked();
		g2 = G2.isChecked();
		iq = IQ.isChecked();
		send_email = email_box.isChecked();
		email_address = email.getText().toString();
					
		Log.d(Constants.LOG, "Opening Indices Activity");
		
		Intent intent = new Intent(this, IndexActivity.class);
		
		//String message = "Hola";
		intent.putExtra(MESS_GP0,gp0);
		intent.putExtra(MESS_GP1,gp1);
		intent.putExtra(MESS_CE,ce);
		intent.putExtra(MESS_G0,g0);
		intent.putExtra(MESS_G1,g1);
		intent.putExtra(MESS_G2,g2);
		intent.putExtra(MESS_IQ,iq);
		intent.putExtra(MESS_EMAIL_BOX,send_email);
		intent.putExtra(MESS_EMAIL,email_address);
		intent.putExtra(MESS_BTADDRESS, SelectedBTaddress);
		
		startActivity(intent);	
	}
	
	 @SuppressLint("NewApi")
	void startBTspinner()
	{
			Log.d(MLOG, "Entering startBT");
			BluetoothAdapter mBluetoothAdapter = null;
			
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			
			//Check if there is BT in the device
			if (mBluetoothAdapter == null) {
				// Device does not support Bluetooth
				Log.d(MLOG, "Device does not have BT...");
				return;
			}
					
			//Check if the BT is enabled
			if (!mBluetoothAdapter.isEnabled()) {
			    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			    
			    mBluetoothAdapter = null;
			    
			    return;
			}
			
					//List<String> list = new ArrayList<String>();
					//ArrayList mArrayAdapter = new ArrayList();		
					Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
					// If there are paired devices
					if (pairedDevices.size() > 0) {
					    // Loop through paired devices
					    for (BluetoothDevice device : pairedDevices) {
					        // Add the name and address to an array adapter to show in a ListView
					        //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
					    	BTlist.add(device.getName() + " " + device.getAddress());
					    	BTNames[BTlist.size()-1] = device.getName();
					    	BTAddress[BTlist.size()-1] = device.getAddress();					    	
					        Log.d(MLOG, device.getName() + ": "+ device.getAddress());
					        Log.d(MLOG, BTAddress[BTlist.size()-1]);
					    }		    
					}
					
					ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
			    			android.R.layout.simple_spinner_item, BTlist);
			    	dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			    	spinner1.setAdapter(dataAdapter);		    	
			    	//spinner1.setOnItemSelectedListener(new CustomOnItemSelectedListener());
					
			    	mBluetoothAdapter = null;
			    	
	}
	 
	 protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    	startBTspinner();
	    	
	 }
	 
	 /*
	  * Listener not used since the update of selected item is done using a button
	  * 
	 public class CustomOnItemSelectedListener implements OnItemSelectedListener {
    	 
		//Item selected 
   	  	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
   	  		Toast.makeText(parent.getContext(), 
   			"OnItemSelectedListener : " + parent.getItemAtPosition(pos).toString(),
   			Toast.LENGTH_SHORT).show();
   	  		
   	  	}
   	 
   	  	@Override
   	  	public void onNothingSelected(AdapterView<?> arg0) {
   	  		// TODO Auto-generated method stub
   	  	}
   	 
	 }
	 */
	 
	 	 
	 public void readBTaddress()
		{
			Log.d("Saul","Reading BT address file");
			
			try {

				BufferedReader inputReader = new BufferedReader(new InputStreamReader(openFileInput("BTaddress")));

				SelectedBTname = inputReader.readLine();;
				SelectedBTaddress = inputReader.readLine();

				SelectedBT.setText("Selected BT mod: " + SelectedBTname);
				
				Log.d("Loaded BT name", SelectedBTname);
				Log.d("Loaded BT address", SelectedBTaddress);
				
				
			} catch (IOException e) {
				
				e.printStackTrace();
				//Problem opening the file (File not found)
				//Create a new calibration file
				
				SaveBTaddress1st();
				Log.d(Constants.LOG,"Creating BT address file");
				
			}
		}
	 
	 void SaveBTaddress()
	 {
		 int pos;
		 
		 pos = spinner1.getSelectedItemPosition();
		 
		 try {

			    FileOutputStream fos = openFileOutput("BTaddress", Context.MODE_PRIVATE);
			    
			    String aux;
			   
			    aux = BTNames[pos] + "\n";
			    fos.write(aux.getBytes());
			    aux = BTAddress[pos] + "\n";
			    fos.write(aux.getBytes());		   
			    
			    Log.d("Saved BT name", BTNames[pos]);
				Log.d("Saved BT address", BTAddress[pos]);

			    fos.close();

			} catch (Exception e) {

			    e.printStackTrace();

			}
		 
	 }
	 
	 void SaveBTaddress1st()
	 {
		 try {

			    FileOutputStream fos = openFileOutput("BTaddress", Context.MODE_PRIVATE);
			    
			    String aux;
			  
			    aux = "N/A\n";
			    fos.write(aux.getBytes());
			    aux = "N/A\n";
			    fos.write(aux.getBytes());		   
			    
			    SelectedBT.setText("Selected BT mod: N/A");
			    
			    fos.close();

			} catch (Exception e) {

			    e.printStackTrace();

			}
	 }
	 
	 public void onSaveBT(View view)
	 {
		 String aux;
		 
		 int pos = spinner1.getSelectedItemPosition();
		 
		 aux = "Selected BT mod: " + BTNames[pos];
		 SelectedBT.setText(aux);
		 
		 SelectedBTaddress = BTAddress[pos];
		 Log.d("Selected BT Address", SelectedBTaddress);
		 SaveBTaddress();
		 
		 
	 }
}
