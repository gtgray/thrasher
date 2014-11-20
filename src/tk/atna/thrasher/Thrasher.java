package tk.atna.thrasher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Thrasher {
	
	private final static String ITELEPHONY_CLASS = "com.android.internal.telephony.ITelephony";
	
	private final static String PREFIX = "tel:";
	
	private final static int START_DELAY = 500;
	
	// handler to post runs
	private Handler workHandler;
	
	// UI handler
	private Handler uiHandler;
	
	private Context context;
	
	// flag to unschedule runnable
	private boolean canceled = true;
	
	// phone number to call
	private String phone;
	// delay before recall
	private long timeout = 0;
	// delay between runs
	private long interval = 0;
	
	private int callState;
	
	private Thrasher.Callback callback;
	
	private TelephonyManager teleManager;
	
	private boolean listenChanges = true;
	
//	private long now;
	
	private AudioManager audioManager;
	
	private ActivityManager activityManager;
	
	private Runnable makeCall = new Runnable() {
		
		@Override
		public void run() {
			// ends previous calls
//			endCall();
			// waits before make next call
//			try {
//				Thread.sleep(interval);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			

			// thrashing is not canceled
			if(!canceled && context != null) {
				// makes a new call
				String number = PREFIX + phone;
				Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number));
				context.startActivity(callIntent);
				
				// prepares new call in interval
//				workHandler.postDelayed(this, timeout);
				
//				getCallState(context);
				
				listenChanges = true;
				muteMicro();
				
//				bringTaskToFront();
//				Log.d("myLogs", Thrasher.class.getSimpleName() + ": am " + activityManager.getRunningTasks(1).get(0).topActivity);

			    
				// !!!!!!
				// turns microphone off
//				audioManager.setMicrophoneMute(true);
				
//				audioManager.setMode(AudioManager.MODE_NORMAL);
				
			}
		}
	};
	
	private int taskId;
	
	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}
	
	private PhoneStateListener psListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			
			switch (state) {
			
			// incoming call arrived
			case TelephonyManager.CALL_STATE_RINGING:
				Log.d("myLogs", Thrasher.class.getSimpleName() + ".onCallStateChanged: call state is CALL_STATE_RINGING");
//				postProgress("RINGING");
				
				// no incoming calls are expected
				break;
				
			// call is active
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.d("myLogs", Thrasher.class.getSimpleName() + ".onCallStateChanged: call state is CALL_STATE_OFFHOOK");
				
//				postProgress("OFFHOOK " + teleManager.getCallState());
				
//				Log.d("myLogs", Thrasher.class.getSimpleName() + ": audio mode " + audioManager.getRingerMode());
				
				// recipient does off-hook (answers the call)
				// flush and immediate recall
				
				if(callState == TelephonyManager.CALL_STATE_IDLE) {
//					try {
//						Thread.sleep(timeout);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
					
//					postProgress(getPrecisedCallState());
					
					
					recallInInterval();
					/*workHandler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							endCall();
//							workHandler.removeCallbacks(Thrasher.this);
							// makes call immediately
							workHandler.postDelayed(Thrasher.this, interval);
							
						}
					}, timeout);*/
				}
				
				// ends current call
//				endCall();
				// flushes prepared call in interval
//				workHandler.removeCallbacks(Thrasher.this);
				// makes call immediately
//				workHandler.post(Thrasher.this);
				
				
				break;
				
			// no activity
			case TelephonyManager.CALL_STATE_IDLE:
				Log.d("myLogs", Thrasher.class.getSimpleName() + ".onCallStateChanged: call state is CALL_STATE_IDLE");
				
//				postProgress("IDLE, " + teleManager.getCallState());
				
//				int vib = Settings.System.getInt(context.getContentResolver(), Settings.System.VIBRATE_ON, 100);
//				Log.d("myLogs", Thrasher.class.getSimpleName() + ": vib " + vib);
				
				
				if(callState == TelephonyManager.CALL_STATE_OFFHOOK)
					// call dropped
					recallImmediate();
				
				// 
				
				break;
			}
			
			// remembers call state
			callState = state;
		}

		@Override
		public void onServiceStateChanged(ServiceState serviceState) {
			super.onServiceStateChanged(serviceState);
			Log.d("myLogs", Thrasher.class.getSimpleName() + ".onServiceStateChanged: phone service state is " + serviceState.getState());
			
			// phone is not ready to call
			if(serviceState.getState() != ServiceState.STATE_IN_SERVICE) 
				postFailure("Phone service is not ready for calls");
		}
		
//		@Override
//		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
//	        super.onSignalStrengthsChanged(signalStrength);
//	        Log.d("myLogs", Thrasher.class.getSimpleName() + ".onSignalStrengthsChanged: >>>>>> " + signalStrength.getGsmSignalStrength());
//	    }
		
	};
	
	public Thrasher(Context context, Handler uiHandler, String phone, long timeout, long interval, Thrasher.Callback callback) {
		
		this.context = context;
		this.uiHandler = uiHandler;
		
		this.phone = phone;
		this.timeout = timeout;
		this.interval = interval;
		
		// creates new work thread
		HandlerThread workThread = new HandlerThread(this.getClass().getName());
		workThread.start();
		this.workHandler = new Handler(workThread.getLooper());
//		this.teleManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
		
		this.callback = callback;
		
		Log.d("myLogs", this.getClass().getSimpleName() + ": created " + workHandler.toString());
	}
	
	public void startThrash() {
		
		teleManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
		teleManager.listen(psListener, PhoneStateListener.LISTEN_CALL_STATE 
										| PhoneStateListener.LISTEN_SERVICE_STATE 
										/*| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS*/);
		
		audioManager = (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
		
		activityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
		
//		now = System.currentTimeMillis();
//		Log.d("myLogs", this.getClass().getSimpleName() + ": now time " + now);
		
//		listenChanges = true;
//		stateChangesListener();
		
		isCanceled(false);
		workHandler.removeCallbacks(makeCall);
		workHandler.postDelayed(makeCall, START_DELAY);
		Log.d("myLogs", this.getClass().getSimpleName() + ": thrasher started");
	}
	
	public void stopThrash() {
		isCanceled(true);
		workHandler.removeCallbacks(makeCall);
		
		listenChanges = false;
		
		teleManager.listen(psListener, PhoneStateListener.LISTEN_NONE);
		
		Log.d("myLogs", this.getClass().getSimpleName() + ": thrasher stopped");
	}
	
	public void isCanceled(boolean canceled) {
		this.canceled = canceled;
	}
	
	public void quitLooper() {
		// brings thread to dead but not stopping		
		workHandler.getLooper().quit();
	}
	
	private void bringTaskToFront() {
		
		workHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				if(taskId != 0) {
					activityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_WITH_HOME);
				}
			}
		}, 100);
	}
	
	private void muteMicro() {
		
		workHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				
//				getCallLogInfo();
//				getInfo();
				
//				Log.d("myLogs", this.getClass().getSimpleName() + ".getInfo: mode " + audioManager.isMicrophoneMute());
				
				if(!audioManager.isMicrophoneMute())
					audioManager.setMicrophoneMute(true);
				
//				if(audioManager.getMode() == AudioManager.MODE_IN_CALL)
//					audioManager.setMode(AudioManager.MODE_NORMAL);
				
//				getInfo();
				
				
				if(listenChanges)
					muteMicro();
			}
		}, 100);
	}
	
/*	
	private void getInfo() {
		
//		int mode = audioManager.getMode();
		boolean mode = audioManager.isMicrophoneMute();
		
		
		Log.d("myLogs", this.getClass().getSimpleName() + ".getInfo: mode " + mode);
		
	}
*/	
/*	
	private void getCallLogInfo() {
		
		Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, 
															CallLog.Calls.DATE + ">?", 
															new String[]{ String.valueOf(now) }, 
															CallLog.Calls.DATE + " DESC"); 
		
		cursorToString(cursor);
		
		if(cursor != null)
			cursor.close();
	}
*/	
	
	private void recallInInterval() {
		recall(true, timeout);
	}
	
	private void recallImmediate() {
		recall(false, 0L);
	}
	
	private void recall(final boolean end, long delay) {
		workHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				
				if(end)
					endCall();
					
				workHandler.removeCallbacks(makeCall);
				
				// makes call in interval
				workHandler.postDelayed(makeCall, interval);
				
			}
		}, delay);
	}
	
	private void endCall() {
		
		// !!!!!!
		// turns microphone on again
//		audioManager.setMicrophoneMute(false);
		
		listenChanges = false;
		
//		audioManager.setMode(AudioManager.MODE_IN_CALL);
		
		
		try {
//			teleManager = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
			Class<?> tmClazz = Class.forName(teleManager.getClass().getName());
			Method getITelephony = tmClazz.getDeclaredMethod("getITelephony");
			getITelephony.setAccessible(true);
			
//			ITelephony iTelefony = tmMethod.invoke(teleManager);
//			iteleMethod.invoke(iTelephony);
			
			Class<?> iteleClazz = Class.forName(ITELEPHONY_CLASS);
			Method endCall = iteleClazz.getDeclaredMethod("endCall");
			endCall.setAccessible(true);
			
			endCall.invoke(iteleClazz.cast(getITelephony.invoke(teleManager)));
			
//			Method[] meths = iteleClazz.getDeclaredMethods();
//			Log.d("myLogs", this.getClass().getSimpleName() + ".endCall: meths " + meths.length);
//			for(Method m : meths) {
//				Log.d("myLogs", this.getClass().getSimpleName() + ".endCall: method " + m.getName());
//			}
			
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			postFailure("Can't end outgoing call");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			postFailure("Can't end outgoing call");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			postFailure("Can't end outgoing call");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			postFailure("Can't end outgoing call");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			postFailure("Can't end outgoing call");
		} catch (ClassCastException e) {
			e.printStackTrace();
			postFailure("Can't end outgoing call");
		}
	}
	
	private void postFailure(final String message) {
		if(uiHandler != null)
			uiHandler.post(new Runnable() {
				
				@Override
				public void run() {
					if(callback != null)
						callback.onFailure(message);
				}
			});
		else
			canceled = true;
	}
	
	private void postProgress(final String message) {
		if(uiHandler != null)
			uiHandler.post(new Runnable() {
				
				@Override
				public void run() {
					if(callback != null)
						callback.onProgress(message);
				}
			});
		else
			canceled = true;
	}
	
/*	
	public static void cursorToString(Cursor cursor) {
		// checking if the query was succeeded
		if(cursor != null) {
			// if the result set of rows is empty
			if(cursor.moveToFirst()) {
				StringBuilder record;
				do {
					record = new StringBuilder();
					for(String str : cursor.getColumnNames()) {
						record.append(str);
						record.append(" = ");
						record.append(cursor.getString(cursor.getColumnIndex(str)));
						record.append("; ");
					}
					Log.d("myLogs", record.toString());
				} while(cursor.moveToNext());
			} else {
				// there are no rows in query result set
				Log.d("myLogs", "0 rows");
			}
			// if it is not	
		} else {
			Log.d("myLogs", "Cursor is null!");
		}
	}
*/	
	
	public interface Callback {
		
		public void onProgress(String message);
		
		public void onFailure(String message);
	}
	
}
