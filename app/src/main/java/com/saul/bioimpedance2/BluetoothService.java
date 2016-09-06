package com.saul.bioimpedance2;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

//import com.rodriguez.saul.BT.BT_SPP_clientActivity;
//import com.rodriguez.saul.BT.BluetoothService.ConnectedThread;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

/*public class BluetoothService {

}*/

public class BluetoothService {
	// Debugging
    private static final String TAG = "BLUETOOTHSERVICE";
    private static final boolean D = true;
	
	private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket mmSocket = null;
 //   private InputStream mmInStream = null;
    private OutputStream mmOutStream = null;
    private ConnectedThread mConnectedThread;
    
    
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    //private static String address = "00:06:66:46:4E:B9";   //Module 1
    //private static String address = "00:06:66:45:D4:26";	//Module 2
    //private static String address = "00:06:66:46:4D:2A";	//Module 1 (breadboard office)
    
    //LAST SELECTED
    //private static String address = "00:06:66:63:5D:9C";	//Bio1 PCB
    
    //private static String address = "00:06:66:63:60:EE";	//Reader prototype1

    private String address;
    
	private final Handler mHandler;
	private final Context mContext;
	
	byte[] circ_buffer = new byte[128];
	private int circ_head = 0;
 	
	//Constructor method
	public BluetoothService(Context context, Handler handler, String BTaddress) {
		
		mHandler = handler;
		mContext = context;
		address = BTaddress;
		
	}
	
	//on_create() only checks if bluetooth is supported and if it is enabled
	public int on_create() {
		//BT related
        if (D)
            Log.e(TAG, "+++ ON CREATE +++");
       
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
       
        if (mBluetoothAdapter == null) {
                Toast.makeText(mContext,
                        "Bluetooth is not available.",
                        Toast.LENGTH_LONG).show();
                //finish();
                return 0;
        }
        
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(mContext,
                    "Please enable your BT and re-run this program.",
                    Toast.LENGTH_LONG).show();
            //finish();
            return 0;
        }

        if (D)
            Log.e(TAG, "+++ DONE IN ON CREATE, GOT LOCAL BT ADAPTER +++");
        return 1;
	}
	
	// on_resume gets the BT device, opens a socket, and start a listening thread
	public int on_resume() {
		  if (D) {
              Log.e(TAG, "+ ON RESUME +");
              Log.e(TAG, "+ ABOUT TO ATTEMPT CLIENT CONNECT +");
		  }

		  //Getting access to the BT device with address and UUID
		  BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
      
	      try {
	              mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
	      } catch (IOException e) {
	              Log.e(TAG, "ON RESUME: Socket creation failed.", e);
	              return 0;
	      }

	      mBluetoothAdapter.cancelDiscovery();

	      // Blocking connect, for a simple client nothing else can
	      // happen until a successful connection is made, so we
	      // don't care if it blocks.
	      try {
	              mmSocket.connect();
	              Log.e(TAG, "ON RESUME: BT connection established, data transfer link open.");
	      } catch (IOException e) {
	              try {
	                      mmSocket.close();
	              } catch (IOException e2) {
	                      Log.e(TAG,
	                              "ON RESUME: Unable to close socket during connection failure", e2);
	                      return 0;
	              }
	      }
      
	      //Attach output stream for writing
	      try {
	          mmOutStream = mmSocket.getOutputStream();
	      } catch (IOException e) {
	          Log.e(TAG, "ON RESUME: Output stream creation failed.", e);
	      }
      
	      //Here the socket is already working and we can start a listening thread
	      // First Cancel any thread currently running a connection
	      stop_listening();
	
	      // Start the thread to manage the connection and perform transmissions
	      mConnectedThread = new ConnectedThread(mmSocket);
	      mConnectedThread.start();
	      
	      return 1;
      
	}
	
	//on_pause() closes the socket and stops listening the BT
	public void on_pause() {
		if (D)
            Log.e(TAG, "- ON PAUSE -");

		if (mmOutStream != null) {
            try {
                    mmOutStream.flush();
            } catch (IOException e) {
                    Log.e(TAG, "ON PAUSE: Couldn't flush output stream.", e);
            }
		}

		try     {
			stop_listening();
            mmSocket.close();
		} catch (IOException e2) {
            Log.e(TAG, "ON PAUSE: Unable to close socket.", e2);
		}
		
	}
	
	//This function closes the listening thread
	public void stop_listening() {
		
		if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null; //stops the thread!
        }
		
	}
	
	public void write(byte[] buffer) {
		 try {
             mmOutStream.write(buffer);
         } catch (IOException e) {
             Log.e(TAG, "Exception during write", e);
         }
  		
	}
	
	private class ConnectedThread extends Thread {
  
        private final InputStream myInStream;
        private Handler recHandler = new Handler();
               
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            InputStream tmpIn = null;
        
            // Get the BluetoothSocket input streams
            try {
                tmpIn = socket.getInputStream();
                
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            myInStream = tmpIn;
            
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[128];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = myInStream.read(buffer);
                    
                    for (int i=0; i<bytes; i++) {
                    	circ_buffer[circ_head++] = buffer[i];
                    	if (circ_head == 128) 
                    		circ_head = 0;
                    }
 
                    //Set/Reset a timeout function that is used to send a message to the UI Activity with the received data
                    recHandler.removeCallbacks(mUpdateTimeTask);
                    recHandler.postDelayed(mUpdateTimeTask, 50);

                    
                    String readMessage = new String(buffer, 0, bytes );
                    Log.e(TAG, "data received: " + String.valueOf(bytes) + " " + readMessage );
                     
                    
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                  //  connectionLost();
                    // Start the service over to restart listening mode
                  //  BluetoothChatService.this.start();
                    break;
                }
            }
        }

        public void cancel() {
            /*try {
                mySocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }*/
        }
        
        //mUpdateTimeTask is triggered as a timeout from the listening thread
        private Runnable mUpdateTimeTask = new Runnable() {
        	   public void run() {
        		  mHandler.obtainMessage(Measure.MESSAGE_READ, circ_head, -1, circ_buffer).sendToTarget(); 
        	      circ_head = 0;
        	   }
        	};
    
	}
	
	
	
}


