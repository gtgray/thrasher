package tk.atna.thrasher;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements OnClickListener {
	
	private final static int STOPPED = 0x00000001;
	private final static int STARTED = 0x00000002;
	
	private final static String STATE = "state";
	private int state = STOPPED; // default state
	
	private final static String PHONE = "phone";
	private final static String TIMEOUT = "timeout";
	private final static String INTERVAL = "interval";
	
//	private int timeout;
//	private int interval;
	
	private final static int MIN_TIMEOUT = 3000;
	private final static int MIN_INTERVAL = 4000;
	
	private final static String EMPTY_STRING = "";
	
	private final static int START_DELAY = 500;
	
	private EditText etPhone, etTimeout, etInterval;
	private TextView btnStart, btnStop;
	
	private Thrasher thrasher;
	
	private Thrasher.Callback callback = new Thrasher.Callback() {
		
		@Override
		public void onProgress(String message) {
			// TODO show some thrashing progress
			
			Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
		}
		
		@Override
		public void onFailure(String message) {
			// notifies user
			Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
			// imitates btnStop click thrasher
			onClick(btnStop);
		}
	};
/*	
	public class CustomBroadcastReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if ("android.provider.Telephony.SPN_STRINGS_UPDATED".equals(action)) {
				
//				abortBroadcast();
				
				Log.d("myLogs", MainActivity.class.getSimpleName() + ".receiver.onReceive: action " + action);
				
//		        updateNetworkName(intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
//		                intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
//		                intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
//		                intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
			}
		}
		
	}
	private  CustomBroadcastReceiver receiver = new CustomBroadcastReceiver();
*/	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d("myLogs", this.getClass().getSimpleName() + ".onCreate: --- ");
		
// 		
//		Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());
//		
		
		etPhone = (EditText) findViewById(R.id.etPhone);
		etTimeout = (EditText) findViewById(R.id.etTimeout);
		etInterval = (EditText) findViewById(R.id.etInterval);
		
		btnStart = (TextView) findViewById(R.id.btnStart);
		btnStart.setOnClickListener(this);
		
		btnStop = (TextView) findViewById(R.id.btnStop);
		btnStop.setOnClickListener(this);
		
		// pulls from SharedPreferences file
		SharedPreferences params = getPreferences(MODE_PRIVATE);
		
		if(etPhone != null)
			etPhone.setText(params.getString(PHONE, EMPTY_STRING));
		if(etTimeout != null)
			etTimeout.setText(params.getString(TIMEOUT, EMPTY_STRING));
		if(etInterval != null)
			etInterval.setText(params.getString(INTERVAL, EMPTY_STRING));
		
//		state = params.getInt(STATE, STOPPED);
//		prepareViews(state);
		postNewState();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d("myLogs", this.getClass().getSimpleName() + ".onStart: --- ");
		
//		registerReceiver(receiver, new IntentFilter("android.provider.Telephony.SPN_STRINGS_UPDATED"));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Log.d("myLogs", this.getClass().getSimpleName() + ".onStop: --- ");
		
//		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d("myLogs", this.getClass().getSimpleName() + ".onDestroy: --- ");
		
		// drops to SharedPreferences file
		SharedPreferences params = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = params.edit();
		
//		if(etPhone.getText().length() > 0)
		editor.putString(PHONE, etPhone.getText().toString().trim());
//		if(etTimeout.getText().length() > 0)
		editor.putString(TIMEOUT, etTimeout.getText().toString().trim());
//		if(etInterval.getText().length() > 0)
		editor.putString(INTERVAL, etInterval.getText().toString().trim());
		
//		editor.putInt(STATE, state);
		
		editor.apply(); // async dropping without failure notification
		
		// stop calling on activity destroy
		stopThrash();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// nothing to do
		
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
// or may be better way to do it in onCreate()
	}

	@Override
	public void onClick(View v) {
		
		switch (v.getId()) {
		
		case R.id.btnStart:
			
			try {
				startThrash(etPhone.getText().toString().trim(), 
							etTimeout.getText().toString().trim(), 
							etInterval.getText().toString().trim());
				
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
				callback.onFailure(e.getMessage());
				return;
			}
			
			state = STARTED;
			break;
			
		case R.id.btnStop:
			
			stopThrash();
			
			state = STOPPED;
			break;
		}
		
		postNewState();
	}

/*	
	@Override
	public void onBackPressed() {
		
		ThrasherWithCustomGsmPhone tr = new ThrasherWithCustomGsmPhone(this, new Handler(), "", 0, 0, null);
//		tr.endCall();
		
		tr.startThrash();
		
		
//		tr.getCallLogInfo();
		
//		tr.quitLooper();
		
//		super.onBackPressed();
	}
	
	
	// TESTING
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		
		// menu button was clicked
		if(keyCode == KeyEvent.KEYCODE_MENU) {
			
//			ThrasherWithCustomGsmPhone tr = new ThrasherWithCustomGsmPhone(this, new Handler(), "", 0, 0, null);
//			tr.endCall();
			
//			tr.startThrash();
			
			
//			tr.getCallLogInfo();
			
//			tr.quitLooper();
			
			
			
			
			return true;
		}
		
		// any other key was pressed
		return super.onKeyUp(keyCode, event);
	}
// TESTING
*/	
	
	
	private void startThrash(String phone, String timeout, String interval) throws IllegalArgumentException {
		
		if(phone == null || phone.length() < 1)
			throw new IllegalArgumentException("phone number can't be empty");
		if(timeout == null || timeout.length() < 1)
			throw new IllegalArgumentException("timeout can't be empty");
		if(interval == null || interval.length() < 1)
			interval = "0";
//			throw new IllegalArgumentException("interval can't be empty");
		
		long iTimeout = 0;
		long iInterval = 0;
		
		try {
			iTimeout = Long.valueOf(timeout) * 1000;
			iInterval = Long.valueOf(interval) * 1000;
			
		} catch (NumberFormatException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
		
		if(iTimeout < MIN_TIMEOUT)
			throw new IllegalArgumentException("timeout can't be less than " 
												+ (MIN_TIMEOUT / 1000) + " seconds");
//		if(iInterval < MIN_INTERVAL)
//			throw new IllegalArgumentException("interval can't be less than " 
//												+ (MIN_INTERVAL / 1000) + " seconds");
		
		thrasher = new Thrasher(this, new Handler(), phone, iTimeout, iInterval, callback);
		thrasher.setTaskId(getTaskId());
		thrasher.startThrash();
	}
	
	private void stopThrash() {
		if(thrasher != null) {
			thrasher.stopThrash();
			thrasher.quitLooper();
			thrasher = null;
		}
	}
	
	private void postNewState() {
		(new Handler()).postDelayed(new Runnable() {
			
			@Override
			public void run() {
				fallIntoState(state);
			}
		}, START_DELAY);
	}
	
	private void fallIntoState(int newState) {
		// applies new state to views
		switch (newState) {
		case STOPPED:
			prepareViews(true, true, true, true, false);
			break;
			
		case STARTED:
			prepareViews(false, false, false, false, true);
			break;
		}
	}
	
	private void prepareViews(boolean phone, boolean timeout, boolean interval, boolean start, boolean stop) {
		etPhone.setEnabled(phone);
		etTimeout.setEnabled(timeout);
		etInterval.setEnabled(interval);
		btnStart.setVisibility(start ? View.VISIBLE : View.GONE);
		btnStop.setVisibility(stop ? View.VISIBLE : View.GONE);
	}
	
	private boolean checkPhone(String phone) {
		
		
		
		
		return false;
	}
	
	
	
	
	
}