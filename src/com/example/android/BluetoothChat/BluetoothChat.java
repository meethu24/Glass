/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothChat;

//import android.app.ActionBar;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.android.utils.CustomFormatter;
import com.google.android.glass.app.Card;
import com.google.android.glass.app.Card.ImageLayout;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
	
	/*Code break-up
	 * P = Practice, A = Main Activity, S = Location Practice, L = Location Main Activity
	 * S = Small, M = Medium, L = Large
	 * 001 = ParticipantID
	 * */
	//========PRE_PRACTICE========
	//String pathcode = "RS019";
	
	//===============================TABLE===============================
	
	//=======Small========
	//String pathcode = "PS019";
	//String pathcode = "AS019";
	
	//=======Medium========
	//String pathcode = "PM019";
	//String pathcode = "AM019";
	
	//=======Large========
	//String pathcode = "PL019";
	//String pathcode = "AL019";
	
	//===============================LOCATION===============================
	
	//=======Small========
	//String pathcode = "SS019";
	String pathcode = "LS019";

	//=======Medium========
	//String pathcode = "SM019";
	//String pathcode = "LM019";
	
	//=======Large========
	//String pathcode = "SL019";
	//String pathcode = "LL019";
	
	int participantId;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;

	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothChatService mChatService = null;

	//Cards and Card Scroll View
	private List<Card> mCards;
	private	CardScrollView mCardScrollView;
	
	//Gestures
	public static final String tapforward = "                               Tap Forward";
	public static final String tapbackward = "                               Tap Backward";
	public static final String tapselect = "                                Tap Select";
	public static final String tapcancel = "                                Tap Cancel";
	public static final String success = "Success!";
	public static final String fail = "Try Again";
	public static final String splashscreentext = "Are you ready to begin?";
	public static final String finish = "Thank You!";
	
	//Timer Seconds
	int waitseconds = 0;

	AudioManager audio;
	Timer timer;
	
	//Position of the Card
	int position;
	
	//Logging information
	Logger logger = Logger.getLogger("MyLog");  
    FileHandler fh;
    Date starttime = new Date();
    long starttimeinms;
    int errorcount = 0;
    int successcount = 0;
    long timeelapsed;
    String previousMessage;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//Keep the screen on all the time
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		
		switch(pathcode.charAt(0)){
		case 'R':
			createCards("bf");
			break;
		case 'P':
			createCards("bfbf");
			break;
		case 'A':
			createCards("bfbfbfbfbfbfbfbf");
			break;
		case 'S':
			createCards("cscbfbsf");
			break;
		case 'L':
			createCards("cfcsfcbfsfcbcbscbsfbcfbcsfsfbsbs");
			break;
		}
		
		//PiD
		participantId = Integer.parseInt(pathcode.substring(2, pathcode.length()));
		
		//Creating Logger
	    try {  
	        // This block configure the logger with handler and formatter  
	    	logger.setUseParentHandlers(false);
	    	String path = Environment.getExternalStorageDirectory().getPath() + "/" + pathcode + ".csv";
	        fh = new FileHandler(path);
	        logger.addHandler(fh);
	        
	        CustomFormatter formatter = new CustomFormatter();
	        fh.setFormatter(formatter);  

	    } catch (SecurityException e) {  
	        e.printStackTrace();  
	    } catch (IOException e) {logger.setUseParentHandlers(false);  
	        e.printStackTrace();  
	    }
		
		mCardScrollView = new CardScrollView(this);
		MyCardScrollAdapter adapter = new MyCardScrollAdapter();
		mCardScrollView.setAdapter(adapter);
		mCardScrollView.activate();
		setContentView(mCardScrollView);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null) setupChat();
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		if (mChatService != null) {
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				mChatService.start();
			}
		}
	}

	private void setupChat() {
		mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
		mChatService = new BluetoothChatService(this, mHandler);
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		fh.close();
		if (mChatService != null) mChatService.stop();
	}

	private final void setStatus(int resId) {
		//    final ActionBar actionBar = getActionBar();
		//  actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		//final ActionBar actionBar = getActionBar();
		//actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
					mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;

			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				
				position = mCardScrollView.getSelectedItemPosition();
				CharSequence c = (mCards.get(position)).getText();
				
				if((readMessage.equals("F") || readMessage.equals("f"))){
					if(c.toString().equals(tapforward)){
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:"+"," + c + "," + "Performed:"+"," + readMessage + "," + " <-- Success"+ ","+ "Time Elapsed" +"," + diff;
						logger.info(logmessage);
						successcount++;
						audio.playSoundEffect(Sounds.SUCCESS);
						if(position == mCards.size()-1){
							mCardScrollView.setSelection(mCards.size()-1);
							timeelapsed = new Date().getTime();
						}else{
							mCardScrollView.setSelection(position + 1);
							timeelapsed = new Date().getTime();
						}

					}else{
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:"+"," + c + "," + "Performed:"+"," + readMessage + "," + " <-- Error"+ ","+ "Time Elapsed" +"," + diff;
						logger.info(logmessage);
						errorcount++;
						audio.playSoundEffect(Sounds.ERROR);
					}


					//TAP BACKWARD	
				} else if((readMessage.equals("B") || readMessage.equals("b"))){
					if(c.toString().equals(tapbackward)){
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:"+"," + c + "," + "Performed:"+"," + readMessage + "," + " <-- Success"+ ","+ "Time Elapsed" +"," + diff;
						logger.info(logmessage);
						successcount++;
						audio.playSoundEffect(Sounds.SUCCESS);
						if(position == mCards.size()-1){
							mCardScrollView.setSelection(mCards.size()-1);
							timeelapsed = new Date().getTime();
						}else{
							mCardScrollView.setSelection(position + 1);
							timeelapsed = new Date().getTime();
						}

					}else{
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:" +","+ c + "," + "Performed:" +"," + readMessage + "," + " <-- Error"+ ","+ "Time Elapsed" +"," +diff;
						logger.info(logmessage);
						errorcount++;
						audio.playSoundEffect(Sounds.ERROR);
					}

					//TAP SELECT
				}else if((readMessage.equals("S") || readMessage.equals("s"))){
					if(c.toString().equals(tapselect)){
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:" +","+ c + "," + "Performed:" +"," + readMessage + "," + " <-- Success"+ ","+ "Time Elapsed" +"," +diff;
						logger.info(logmessage);
						successcount++;
						audio.playSoundEffect(Sounds.SUCCESS);
						if(position == mCards.size()-1){
							mCardScrollView.setSelection(mCards.size()-1);
							timeelapsed = new Date().getTime();
						}else{
							mCardScrollView.setSelection(position + 1);
							timeelapsed = new Date().getTime();
						}

					}else{
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:" +"," + c + "," + "Performed:" +"," + readMessage + "," + " <-- Error"+ ","+ "Time Elapsed" +"," +diff;
						logger.info(logmessage);
						errorcount++;
						audio.playSoundEffect(Sounds.ERROR);
					}

					//TAP CANCEL
				}else if((readMessage.equals("C") || readMessage.equals("c"))){
					if(c.toString().equals(tapcancel)){
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:"+"," + c + "," + "Performed:"+"," + readMessage + "," + " <-- Success"+ ","+ "Time Elapsed" +"," + diff;
						logger.info(logmessage);
						successcount++;
						audio.playSoundEffect(Sounds.SUCCESS);
						if(position == mCards.size()-1){
							timeelapsed = new Date().getTime();
							mCardScrollView.setSelection(mCards.size()-1);
						}else{
							mCardScrollView.setSelection(position + 1);
							timeelapsed = new Date().getTime();
						}

					}else{
						long diff = (new Date().getTime()) - timeelapsed;
						String logmessage = participantId + "," + "Screen:" +","+ c + "," + "Performed:" +","+ readMessage + "," + " <-- Error"+ ","+ "Time Elapsed" +"," +diff;
						logger.info(logmessage);
						errorcount++;
						audio.playSoundEffect(Sounds.ERROR);
					}

					//Splash Screen
				} else if ((readMessage.equals("Y")) || readMessage.equals("y")){
					if(position == 0){
						String Firstmessage = "Participant ID" + "," + "Screen:" +"," + "[text]" + "," + "Performed:" +"," + "[value]" + "," + " <-- Start" + "," + "Start Time:" +"," + "[value]";
						logger.info(Firstmessage);
						starttimeinms = new Date().getTime();
						String logmessage = participantId + "," + "Screen:" +"," + c + "," + "Performed:" +"," + readMessage + "," + " <-- Start" + "," + "Start Time:" +"," + starttimeinms;
						logger.info(logmessage);
						audio.playSoundEffect(Sounds.SUCCESS);
						if(position == mCards.size()-1){
							timeelapsed = new Date().getTime();
							mCardScrollView.setSelection(mCards.size()-1);
						}else{
							mCardScrollView.setSelection(position + 1);
							timeelapsed = new Date().getTime();
						}
					}else if(position == mCards.size()/2){
						String logmsg1 = "END OF BLOCK A";
						logger.info(logmsg1);
						String logmsg2 = "START OF BLOCK B";
						logger.info(logmsg2);
						audio.playSoundEffect(Sounds.SUCCESS);
						mCardScrollView.setSelection(position + 1);
						timeelapsed = new Date().getTime();
					}
				}	
				else if ((readMessage.equals("X"))||readMessage.equals("x")) {
					audio.playSoundEffect(Sounds.DISMISSED);
					Date d = new Date();
					long timenow = d.getTime();
					long diff = timenow - starttimeinms;
					String logmessage = participantId + "," + "Screen:" + "," + c + "," + "Performed:" + "," + readMessage + "," + " <-- END" + "," + "Start to End:" +"," + diff;
					logger.info(logmessage);
					//ID,Task,Practice?,BlockNum,SuccessCount,ErrorCount,TotalElapsedTimeForBlock
					boolean ifpractice = false;
					if(pathcode.charAt(0)== 'R' || pathcode.charAt(0) == 'P' || pathcode.charAt(0) == 'S'){
						ifpractice = true;
					}
					String summarymessage1 = "PID" + "," + "Task:" + ","  + "Practice?" + ","  + 
							"Success Count" + ","  + "Error Count" + ","  + "Total Time Elapsed";
					logger.info(summarymessage1);
					String summarymessage2 = participantId +"," + pathcode.charAt(0) + "," + ifpractice + "," + successcount + "," + errorcount + "," +  diff;
					logger.info(summarymessage2);
					System.gc();
					onStop();
					finish();
					System.exit(0);
				} 
				else{
					String logmessage = participantId + "," + "Screen:" + "," + c + "," + "Performed:" + "," + readMessage + "," + " <-- Error" + ","+ "Time Elapsed" +"," ;
					logger.info(logmessage);
					errorcount++;
					audio.playSoundEffect(Sounds.ERROR);
					break;
				}
				previousMessage = readMessage;
				break;
			case MESSAGE_DEVICE_NAME:
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(), "Connected to "
						+ mConnectedDeviceName, Toast.LENGTH_SHORT).show();
				break;

			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
						Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				setupChat();
			} else {
				Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras()
				.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER){
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_BACK){
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_TAB){
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_CAMERA){
			return true;
		}
		return false;
	}

	private class MyCardScrollAdapter extends CardScrollAdapter {
		@Override
		public int getPosition(Object item) {
			return mCards.indexOf(item);
		}

		@Override
		public int getCount() {
			return mCards.size();
		}

		@Override
		public Object getItem(int position) {
			return mCards.get(position);
		}

		@Override
		public int getViewTypeCount() {
			return Card.getViewTypeCount();
		}

		@Override
		public int getItemViewType(int position){
			return mCards.get(position).getItemViewType();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return mCards.get(position).getView(convertView, parent);
		}

		@Override
		public boolean hasStableIds(){
			return true;
		}
	}

	private void createCards(String incomingCardString){
		mCards = new ArrayList<Card>();
		Card card = new Card(this);
		
		//SPLASH SCREEN
		card = new Card(this);
		card.setText(splashscreentext);
		card.addImage(R.drawable.lights);
		card.setImageLayout(Card.ImageLayout.FULL);
		mCards.add(card);
		int sizeofloop = incomingCardString.length();
		if(pathcode.charAt(0) == 'L'){
			sizeofloop = incomingCardString.length()/2;
		}
		for(int i=0;i<sizeofloop;i++){

			switch(incomingCardString.charAt(i)){

			case 's':
				card = new Card(this);
				card.setText(tapselect);
				mCards.add(card);
				break;
			case 'f':
				card = new Card(this);
				card.setText(tapforward);
				mCards.add(card);
				break;
			case 'b':
				card = new Card(this);
				card.setText(tapbackward);
				mCards.add(card);
				break;
			case 'c':
				card = new Card(this);
				card.setText(tapcancel);
				mCards.add(card);
				break;
			}
		}
		if(pathcode.charAt(0)=='L'){
			card = new Card(this);
			card.setText("Let take a break at this point !!");
			card.setFootnote("Let me know once you are ready to continue");
			mCards.add(card);
			for(int i=sizeofloop;i<incomingCardString.length();i++){

				switch(incomingCardString.charAt(i)){

				case 's':
					card = new Card(this);
					card.setText(tapselect);
					mCards.add(card);
					break;
				case 'f':
					card = new Card(this);
					card.setText(tapforward);
					mCards.add(card);
					break;
				case 'b':
					card = new Card(this);
					card.setText(tapbackward);
					mCards.add(card);
					break;
				case 'c':
					card = new Card(this);
					card.setText(tapcancel);
					mCards.add(card);
					break;
				}
			}
		}

		//Thank You Screen!!
				card = new Card(this);
				card.setText(finish);
				card.addImage(R.drawable.ic_done_150);
				card.setImageLayout(ImageLayout.LEFT);
				mCards.add(card);

	}
		
}
