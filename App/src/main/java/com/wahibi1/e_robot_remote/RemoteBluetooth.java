/*
 * Copyright (C) 2014 Mohammed Alwahibi
 */

package com.wahibi1.e_robot_remote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import com.wahibi1.Joystick.*;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class RemoteBluetooth
        extends Activity
        implements View.OnTouchListener {

    JoystickView joystick;
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    public static final short buttons [] = new short [13];
    public static final int xaxis = 0;
    public static final int yaxis = 1;
    public static final int sel = 2;
    public static final int s1 = 3;
    public static final int s2 = 4;
    public static final int s3 = 5;
    public static final int s4 = 6;
    public static final int dl = 7;
    public static final int dr = 8;
    public static final int up = 9;
    public static final int left = 10;
    public static final int down = 11;
    public static final int right = 12;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
	
	// Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for Bluetooth Command Service
    private BluetoothCommandService mCommandService = null;
	
    /** Called when the activity is first created. */
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
        //set the clear state
        findViewById(R.id.sel).setOnTouchListener(this);

        findViewById(R.id.arrow_up).setOnTouchListener(this);
        findViewById(R.id.arrow_down).setOnTouchListener(this);
        findViewById(R.id.arrow_right).setOnTouchListener(this);
        findViewById(R.id.arrow_left).setOnTouchListener(this);

        findViewById(R.id.s1).setOnTouchListener(this);
        findViewById(R.id.s2).setOnTouchListener(this);
        findViewById(R.id.s3).setOnTouchListener(this);
        findViewById(R.id.s4).setOnTouchListener(this);

        findViewById(R.id.dl).setOnTouchListener(this);
        findViewById(R.id.dr).setOnTouchListener(this);

        joystick = (JoystickView)findViewById(R.id.joystickView);
        joystick.setOnJostickMovedListener(_listener);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //Hide options if a button is already there
        if(Build.VERSION.SDK_INT <= 10 || (Build.VERSION.SDK_INT >= 14 &&
                ViewConfiguration.get(this).hasPermanentMenuKey()))
        {
            findViewById(R.id.options).setVisibility(View.GONE);
        }
    }

    private JoystickMovedListener _listener = new JoystickMovedListener() {

        @Override
        public void OnMoved(int pan, int tilt) {
            int x = (pan+10)*51;
            int y = (10-tilt)*51;

            //Log.d("joy","X:"+x+" Y:"+y);
            buttons[xaxis]= (short) x;
            buttons[yaxis]= (short) y;
            update();
        }

        @Override
        public void OnReleased() {
            defaultButton(yaxis);
            defaultButton(xaxis);
            update();
        }
    };

    @Override
	protected void onStart() {
		super.onStart();
		
		// If BT is not on, request that it be enabled.
        // setupCommand() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		// otherwise set up the command service
		else {
			if (mCommandService==null)
				setupCommand();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		// Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if (mCommandService != null) {
			if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) {
				mCommandService.start();
			}
		}

        //initiate
        defaultButton();
	}

	private void setupCommand() {
		// Initialize the BluetoothChatService to perform bluetooth connections
        mCommandService = new BluetoothCommandService(this, mHandler);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mCommandService != null)
			mCommandService.stop();
	}
	
	private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
	
	// The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothCommandService.STATE_CONNECTED:

                    break;
                case BluetoothCommandService.STATE_CONNECTING:

                    break;
                case BluetoothCommandService.STATE_LISTEN:
                case BluetoothCommandService.STATE_NONE:

                    break;
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;

                case MESSAGE_READ:

                    byte[] readBuf = (byte[]) msg.obj;
                    processMessage(readBuf, msg.arg1);
                    break;
            }
        }
    };
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mCommandService.connect(device);
                saveFavoriteDevice(address);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupCommand();
            } else {
                // User did not enable Bluetooth or an error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
        	Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte[] bytes, int size) {
        char[] hexChars = new char[size * 2];
        int v;
        for ( int j = 0; j < size; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void update(int xaxis, int yaxis){
        byte[] bytes = ByteBuffer.allocate(8).putInt(xaxis).putInt(yaxis).array();
        Log.d("e_robot_remote", "sending " + bytesToHex(bytes));
        mCommandService.write(bytes);
    }

    public void update(){

        ByteBuffer bytes = ByteBuffer.allocate(2 * buttons.length);

        for(short value : buttons)
            bytes.putShort(value);

        //swap byte order to be sent over bluetooth
        byte [] toBeSent = rearrangeBytes(bytes.array());
        toBeSent = packageBytes(toBeSent);
        //Log.d("e_robot_remote", "sending "+ bytesToHex(toBeSent));
        mCommandService.write(toBeSent);
    }

    //swap byte order to be sent over bluetooth
    public byte [] rearrangeBytes(byte[] array){
        for(int i = 0; i < (array.length-1); i +=2){
            byte tmp = array[i];
            array[i] = array[i+1];
            array[i+1] = tmp;
        }
        return array;
    }

    public byte [] packageBytes(byte [] array){

        //4 extra bytes are for Header, Size Header, Size, Checksum
        byte [] result = new byte [array.length +4];
        byte checksum = (byte)array.length;

        //prepare the header
        result[0] = 0x06;
        result[1] = (byte)0x085;
        result[2] = (byte) array.length;

        //add the message
        for(int i = 0; i < (array.length); i++){
            result[i+3] = array[i];
            checksum ^= array[i];
        }

        //last byte is the checksum
        result[result.length-1] = checksum;

        return result;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(event.getAction() == MotionEvent.ACTION_UP ){
            v.setPressed(false);
            //Log.d("e_robot_remote",v.getId() +" was released. ");
            switch (v.getId()){

                case R.id.sel:
                    buttons[sel] = 1;
                    break;

                //the arrows
                case R.id.arrow_down:
                    buttons[down] = 1;
                    break;
                case R.id.arrow_up:
                    buttons[up] = 1;
                    break;
                case R.id.arrow_right:
                    buttons[right] = 1;
                    break;
                case R.id.arrow_left:
                    buttons[left] = 1;
                    break;

                //the s's
                case R.id.s1:
                    buttons[s1] = 1;
                    break;
                case R.id.s2:
                    buttons[s2] = 1;
                    break;
                case R.id.s3:
                    buttons[s3] = 1;
                    break;
                case R.id.s4:
                    buttons[s4] = 1;
                    break;

                //the d's
                case R.id.dl:
                    buttons[dl] = 1;
                    break;
                case R.id.dr:
                    buttons[dr] = 1;
                    break;
                default:
                    return false;
            }

            update();
            return true;
        }

        if(event.getAction() == MotionEvent.ACTION_DOWN ){
            v.setPressed(true);
            //Log.d("e_robot_remote",v.getId() +" was clicked. ");
            switch (v.getId()){
                //the axises
                case R.id.sel:
                    buttons[sel] = 0;
                    break;

                //the arrows
                case R.id.arrow_down:
                    buttons[down] = 0;
                    break;
                case R.id.arrow_up:
                    buttons[up] = 0;
                    break;
                case R.id.arrow_right:
                    buttons[right] = 0;
                    break;
                case R.id.arrow_left:
                    buttons[left] = 0;
                    break;

                //the s's
                case R.id.s1:
                    buttons[s1] = 0;
                    break;
                case R.id.s2:
                    buttons[s2] = 0;
                    break;
                case R.id.s3:
                    buttons[s3] = 0;
                    break;
                case R.id.s4:
                    buttons[s4] = 0;
                    break;

                //the d's
                case R.id.dl:
                    buttons[dl] = 0;
                    break;
                case R.id.dr:
                    buttons[dr] = 0;
                    break;
                default:
                    return false;
            }
            update();
            return true;
        }

        return false;
    }

    public void defaultButton(){
        for(int i = 0; i < buttons.length; i++)
            defaultButton(i);
    }

    public void defaultButton(int index){
        if(index == xaxis || index == yaxis)
            buttons[index] = 512;
        else
            buttons[index] = 1;

    }

    private static final int readingHeader = 0;
    private static final int readingMessage = 1;
    private static final int readingChecksum = 2;

    private static final int header1 = 0;
    private static final int header2 = 1;
    private static final int size = 2;
    private static final int messageOffset = 3;


    public static int messageStatus = readingHeader;

    private static int index = 0;
    private byte [] buffer = new byte [1024];

    public void gotMessage(byte message){
        switch (messageStatus){
            case readingHeader:
                if(     index == header1 && message == 0x06 ||
                        index == header2 && message == (byte)0x085 ||
                        index == size)
                    buffer[index++] = message;

                //need more investigation here (take out the if?)
                else if(index != header1){
                    index = header1;
                    gotMessage(message);
                }

                if(index > size)
                    if(buffer[size] > 1020)
                        index = 0;
                    else
                        messageStatus = readingMessage;

                break;

            case readingMessage:
                if((index-messageOffset) < buffer[size])
                    buffer[index++] = message;

                if(buffer[size] == index-messageOffset)
                    messageStatus = readingChecksum;
                break;

            case readingChecksum:

                //compute the checksum and copy the message
                byte checksum = buffer[size];
                byte [] result = new byte[buffer[size]];


                for(int i = 0; i < buffer[size]; i++){
                    checksum ^= buffer[i+messageOffset];
                    result[i] = buffer[i+messageOffset];
                }

                //success, we have a valid message
                if(checksum == message){
                    result = rearrangeBytes(result);
                    Log.d("e_robot_remote", "got messge!!:" + bytesToHex(result)); //<- here we do the processing
                    processOutput(result);
                }


                index = 0;
                messageStatus = readingHeader;
                break;
        }
    }

    public void processMessage(byte [] message, int size){
        //Log.d("e_robot_remote","buffer before:"+bytesToHex(buffer,index));
        for(int i = 0; i < size; i++)
            gotMessage(message[i]);

        //Log.d("e_robot_remote","message:"+bytesToHex(message,size));
        //Log.d("e_robot_remote","buffer after:"+bytesToHex(buffer,index));
    }

    private static final int speaker = 1;
    private static final int motor = 3;
    public void processOutput(byte [] commands){
        //if(commands[speaker] != 0)
            playBuzzer(commands[speaker] != 0);

        //if(commands[motor] != 0)
            vibrate(commands[motor] != 0);
    }

    private MediaPlayer mp = null;
    private boolean isPlaying = false;
    public synchronized void playBuzzer( boolean activate){
        if(mp == null){
            mp = MediaPlayer.create(getApplicationContext(), R.raw.buzzer4);
            //mp.setLooping(true);
        }

        if(!activate && isPlaying){
            mp.setLooping(false);
            //mp.stop();
            isPlaying = false;
        }

        if(activate && !mp.isPlaying()){
            mp.setLooping(true);
            mp.start();
            isPlaying = true;
        }

    }

    private Vibrator v = null;
    public synchronized void vibrate( boolean activate){
        if(v == null)
            v = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        // Vibrate for 500 milliseconds
        if(activate)
            v.vibrate(new long[]{0, 100},0);
        else
            v.cancel();

    }

    public final static String DEVICE = "DEVICE";
    public void saveFavoriteDevice(String device){
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(DEVICE, device);
        editor.commit();
    }

    public void connectToFavoriteDevice(){
        SharedPreferences sharedPref = getPreferences(MODE_PRIVATE);
        String address = sharedPref.getString(DEVICE, null);

        if(address != null){
            // Get the BLuetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            // Attempt to connect to the device
            mCommandService.connect(device);
        }

    }

    public void showOption(View v){
        openOptionsMenu();
    }
}