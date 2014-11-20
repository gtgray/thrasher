package tk.atna.thrasher.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.CallLog;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;

public class ThrasherWithCustomGsmPhone implements Runnable {
	
	private final static String ITELEPHONY_CLASS = "com.android.internal.telephony.ITelephony";
	private final static String CALL_MANAGER_CLASS = "com.android.internal.telephony.CallManager";
	
	
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
	
	private ThrasherWithCustomGsmPhone.Callback callback;
	
	private TelephonyManager teleManager;
	private AudioManager audioManager;
	
	private boolean listenChanges = true;
	
	private long now;
	
	private SparseArray<Object> data;
	
	private PhoneStateListener psListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			
			switch (state) {
			
			// incoming call arrived
			case TelephonyManager.CALL_STATE_RINGING:
				Log.d("myLogs", ThrasherWithCustomGsmPhone.class.getSimpleName() + ".onCallStateChanged: call state is CALL_STATE_RINGING");
//				postProgress("RINGING");
				
				// no incoming calls are expected
				break;
				
			// call is active
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.d("myLogs", ThrasherWithCustomGsmPhone.class.getSimpleName() + ".onCallStateChanged: call state is CALL_STATE_OFFHOOK");
				
				postProgress("OFFHOOK " + teleManager.getCallState());
				
//				Log.d("myLogs", Thrasher.class.getSimpleName() + ": audio mode " + audioManager.getMode());
				
				// recipient does off-hook (answers the call)
				// flush and immediate recall
				
				if(callState == TelephonyManager.CALL_STATE_IDLE) {
//					try {
//						Thread.sleep(timeout);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
					
//					postProgress(getPrecisedCallState());
					
					
					recall();
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
				Log.d("myLogs", ThrasherWithCustomGsmPhone.class.getSimpleName() + ".onCallStateChanged: call state is CALL_STATE_IDLE");
				
				postProgress("IDLE, " + teleManager.getCallState());
				
//				Log.d("myLogs", Thrasher.class.getSimpleName() + ": audio mode " + audioManager.getMode());
				
				
				
				if(callState == TelephonyManager.CALL_STATE_OFFHOOK)
					// call dropped
					recall();
				
				
				
				
//				AudioManager
//			    public static final int MODE_INVALID            = -2;
//			    public static final int MODE_CURRENT            = -1;
//			    public static final int MODE_NORMAL             = 0;
//			    public static final int MODE_RINGTONE           = 1;
//			    public static final int MODE_IN_CALL            = 2;
//			    public static final int MODE_IN_COMMUNICATION   = 3;
//			    public static final int NUM_MODES               = 4;
				
				
				
				
				// 
				
				
				break;
			}
			
			// remembers call state
			callState = state;
		}

		@Override
		public void onServiceStateChanged(ServiceState serviceState) {
			super.onServiceStateChanged(serviceState);
			Log.d("myLogs", ThrasherWithCustomGsmPhone.class.getSimpleName() + ".onServiceStateChanged: phone service state is " + serviceState.getState());
			
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
	
	public ThrasherWithCustomGsmPhone(Context context, Handler uiHandler, String phone, long timeout, long interval, ThrasherWithCustomGsmPhone.Callback callback) {
		
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
		
		
		
		audioManager =  (AudioManager) context.getSystemService(Service.AUDIO_SERVICE);
		
		
//		getPhone(context);
		
		now = System.currentTimeMillis();
		Log.d("myLogs", this.getClass().getSimpleName() + ": now time " + now);
		
		listenChanges = true;
//		stateChangesListener();
		getCallState(context);
		
		
		isCanceled(false);
		workHandler.removeCallbacks(this);
//		workHandler.postDelayed(this, START_DELAY);
		Log.d("myLogs", this.getClass().getSimpleName() + ": thrasher started");
	}
	
	public void stopThrash() {
		isCanceled(true);
		workHandler.removeCallbacks(this);
//		Thread.currentThread().interrupt();
		
		
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

	@Override
	public void run() {
		// ends previous calls
//		endCall();
		// waits before make next call
//		try {
//			Thread.sleep(interval);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
		
		
		
		// thrashing is not canceled
		if(!canceled && context != null) {
			// makes a new call
			String number = PREFIX + phone;
			Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(number));
			context.startActivity(callIntent);
			
			// prepares new call in interval
//			workHandler.postDelayed(this, timeout);
			
//			getCallState(context);
		}
	}
	
	private void stateChangesListener() {
		
		workHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
//				postProgress(getPrecisedCallState());
//				Log.d("myLogs", this.getClass().getSimpleName() + ": precised call state " + getPrecisedCallState());
//				getPrecisedCallState();
				
//				getCallState(context);
				
//				getCallLogInfo();
				
				if(listenChanges)
					stateChangesListener();
			}
		}, 100);
	}
	
	private void getCallLogInfo() {
		
		Cursor cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, 
															CallLog.Calls.DATE + ">?", 
															new String[]{ String.valueOf(now) }, 
															CallLog.Calls.DATE + " DESC"); 
		
//		Log.d("myLogs", this.getClass().getSimpleName() + ": rows " + cursor.getCount());
		cursorToString(cursor);
		
//		postProgress(String.valueOf(cursor.getCount()));
		
		if(cursor != null)
			cursor.close();
	}
	
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
	
	private void getCallState(final Context context) {
		
		HandlerThread work = new HandlerThread(this.getClass().getName());
		work.start();
		final Handler h = new Handler(work.getLooper());
		work.setUncaughtExceptionHandler(new CustomExceptionHandler());
		Runnable prepare = new Runnable() {
			
			@Override
			public void run() {
//				getPhone(context);
				
				long ts = System.currentTimeMillis();
				while(true) {
					if(System.currentTimeMillis() - ts > 500)
						break;
				}
				
				data = preparePhone(context);
//				getPhone(context, data);
				
			}
		};
		h.post(prepare);
		
		Runnable phone = new Runnable() {
			
			@Override
			public void run() {
				
				getPhone(context, data);
				
//				if(listenChanges)
//					h.postDelayed(this, 500);
//				else {
//					h.getLooper().quit();
//				}
				
			}
		};
		h.post(phone);
		
	}
	
	@SuppressLint("NewApi")
	public SparseArray<Object> preparePhone(Context context) {
		
		try {
//			String phoneFactoryClass = "com.android.internal.telephony.PhoneFactory";
//			String phoneClass = "com.android.internal.telephony.Phone";
			String callClass = "com.android.internal.telephony.Call";
			String uiccControllerClass = "com.android.internal.telephony.uicc.UiccController";
			String gsmPhoneClass = "com.android.internal.telephony.gsm.GSMPhone";
			
			final String PREFERRED_NETWORK_MODE = "preferred_network_mode";
			final int RILConstants_NETWORK_MODE_WCDMA_PREF = 0; // GSM/WCDMA (WCDMA preferred)
			final int RILConstants_NETWORK_MODE_GLOBAL = 7; // GSM/WCDMA, CDMA, and EvDo (auto mode, according to PRL)
			final int RILConstants_LTE_ON_CDMA_TRUE = 1;
			final int RILConstants_PREFERRED_NETWORK_MODE = RILConstants_NETWORK_MODE_WCDMA_PREF;
			final int PhoneConstants_LTE_ON_CDMA_TRUE = RILConstants_LTE_ON_CDMA_TRUE;
			final int Phone_NT_MODE_GLOBAL = RILConstants_NETWORK_MODE_GLOBAL;
			
			//CdmaSubscriptionSourceManager.class from 16 api
//			final String CDMA_SUBSCRIPTION_MODE = "subscription_mode";
			final int CdmaSubscriptionSourceManager_SUBSCRIPTION_FROM_NV = 1; // CDMA subscription from NV
			final int CdmaSubscriptionSourceManager_PREFERRED_CDMA_SUBSCRIPTION = CdmaSubscriptionSourceManager_SUBSCRIPTION_FROM_NV;
			
//			String cdmaSubscriptionSourceManagerClass = "com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager";
			String commandsInterfaceClass = "com.android.internal.telephony.CommandsInterface";
			String rilClass = "com.android.internal.telephony.RIL";
			String phoneNotifierClass = "com.android.internal.telephony.PhoneNotifier";
			String defaultPhoneNotifierClass = "com.android.internal.telephony.DefaultPhoneNotifier";
			
			int preferredNetworkMode = RILConstants_PREFERRED_NETWORK_MODE;
			
			Class<?> tmClazz = TelephonyManager.class;
//			Method getLteOnCdmaModeStatic = tmClazz.getDeclaredMethod("getLteOnCdmaModeStatic"); // from 17 api
			Method getLteOnCdmaMode = tmClazz.getDeclaredMethod("getLteOnCdmaMode");
			getLteOnCdmaMode.setAccessible(true);
//			int lteOnCdmaModeStatic = (int) getLteOnCdmaModeStatic.invoke(null);
			int lteOnCdmaModeStatic = (int) getLteOnCdmaMode.invoke(teleManager);
			
			if (lteOnCdmaModeStatic == PhoneConstants_LTE_ON_CDMA_TRUE) {
				preferredNetworkMode = Phone_NT_MODE_GLOBAL;
			}
			
			int networkMode = 0;
			if (Build.VERSION.SDK_INT < 17)
				networkMode = Settings.System.getInt(context.getContentResolver(), 
														 PREFERRED_NETWORK_MODE, 
														 preferredNetworkMode);
			else 
				networkMode = Settings.Global.getInt(context.getContentResolver(), 
						 							 PREFERRED_NETWORK_MODE, 
						 							 preferredNetworkMode);
	
//			Class<?> cssmClazz = Class.forName(cdmaSubscriptionSourceManagerClass);
//			Method getDefault = cssmClazz.getDeclaredMethod("getDefault", Context.class);
//			getDefault.setAccessible(true);
//			int cdmaSubscription = (int) getDefault.invoke(null, context);

//			int cdmaSubscription = Settings.Secure.getInt(context.getContentResolver(),
//	                									  CDMA_SUBSCRIPTION_MODE, 
//	                									  CdmaSubscriptionSourceManager_PREFERRED_CDMA_SUBSCRIPTION);
			
			int cdmaSubscription = CdmaSubscriptionSourceManager_PREFERRED_CDMA_SUBSCRIPTION;
			
			Class<?> rilClazz = Class.forName(rilClass);
			Constructor<?> rilConstructor = rilClazz.getConstructor(Context.class, int.class, int.class);
			Object ril = rilConstructor.newInstance(context, networkMode, cdmaSubscription);
			
			Class<?> dpnClazz = Class.forName(defaultPhoneNotifierClass);
			Constructor<?> defaultPhoneNotifierConstructor = dpnClazz.getDeclaredConstructor();
			defaultPhoneNotifierConstructor.setAccessible(true);
			Object defaultPhoneNotifier = defaultPhoneNotifierConstructor.newInstance();
			
			Class<?> ciClazz = Class.forName(commandsInterfaceClass);
			Class<?> ucClazz = Class.forName(uiccControllerClass);
			Method make = ucClazz.getDeclaredMethod("make", Context.class, ciClazz);
			make.setAccessible(true);
			make.invoke(null, context, ciClazz.cast(ril));
			
//			Constructor<?>[] consts = gpClazz.getDeclaredConstructors();
//			Log.d("myLogs", this.getClass().getSimpleName() + " >>>>>> consts " + consts.length);
//			for(Constructor<?> c : consts) {
//				Log.d("myLogs", this.getClass().getSimpleName() + " >>>>>> constructor " + c.getName());
//			}
			
			SparseArray<Object> data = new SparseArray<>(2);
			data.put(0, ril);
			data.put(1, defaultPhoneNotifier);
			
			return data;
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (ClassCastException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (InstantiationException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		}
		return null;
	}
	
	// returns custom gsmPhone instance and it is separate from system phone.
	// it throws security exceptions after creation (sends denied broadcasts)
	// can't use it to get call states of system phone 
	public void getPhone(Context context, SparseArray<Object> data) {
		
		try {
			Object ril = data.get(0);
			Object defaultPhoneNotifier = data.get(1);
			
			String callClass = "com.android.internal.telephony.Call";
			String gsmPhoneClass = "com.android.internal.telephony.gsm.GSMPhone";
			String commandsInterfaceClass = "com.android.internal.telephony.CommandsInterface";
			String phoneNotifierClass = "com.android.internal.telephony.PhoneNotifier";
//			String defaultPhoneNotifierClass = "com.android.internal.telephony.DefaultPhoneNotifier";
			
			Class<?> ciClazz = Class.forName(commandsInterfaceClass);
			Class<?> pnClazz = Class.forName(phoneNotifierClass);
			Class<?> gpClazz = Class.forName(gsmPhoneClass);
			Constructor<?> gsmPhoneConstructor = gpClazz.getDeclaredConstructor(Context.class, ciClazz, pnClazz);
			gsmPhoneConstructor.setAccessible(true);
			Object gsmPhone = gsmPhoneConstructor.newInstance(context, 
															  ciClazz.cast(ril), 
															  pnClazz.cast(defaultPhoneNotifier));
			
			Field mSST = gpClazz.getDeclaredField("mSST");
			mSST.setAccessible(true);
			mSST.set(gsmPhone, null);
			
			
			
			
			
			Log.d("myLogs", this.getClass().getSimpleName() + ": gsm phone " + gsmPhone);
			
			Method dial = gpClazz.getMethod("dial", String.class);
			dial.setAccessible(true);
			Object connection = dial.invoke(gsmPhone, "89037373864");
			Log.d("myLogs", this.getClass().getSimpleName() + ": >>>>>>>> " + connection);
			
			Method getForegroundCall = gpClazz.getMethod("getForegroundCall");
			getForegroundCall.setAccessible(true);
			Object what = getForegroundCall.invoke(gsmPhone);
			Log.d("myLogs", this.getClass().getSimpleName() + ": >>>>>>>> " + what);
			
			Method getBackgroundCall = gpClazz.getMethod("getBackgroundCall");
			getBackgroundCall.setAccessible(true);
			Object whatt = getBackgroundCall.invoke(gsmPhone);
			Log.d("myLogs", this.getClass().getSimpleName() + ": >>>>>>>> " + whatt);
			
			Class<?> cClazz = Class.forName(callClass);
			Method getState = cClazz.getMethod("getState");
			getState.setAccessible(true);
			Object state = getState.invoke(cClazz.cast(what));
//			Log.d("myLogs", this.getClass().getSimpleName() + ": >>>>>>>> " + state);
			
			Object statee = getState.invoke(cClazz.cast(whatt));
//			Log.d("myLogs", this.getClass().getSimpleName() + ": >>>>>>>> " + statee);
			
			// remove all event registrations
//			Method dispose = gpClazz.getMethod("dispose");
//			dispose.setAccessible(true);
//			dispose.invoke(gsmPhone);
			
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (ClassCastException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (InstantiationException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			postFailure("Can't do it");
		}
	}
	
	private String getPrecisedCallState() {
		
		try {
			Class<?> cmClazz = Class.forName(CALL_MANAGER_CLASS);
			Method getInstance = cmClazz.getDeclaredMethod("getInstance");
//			getInstance.setAccessible(true);
			Object callManager = getInstance.invoke(null);
			
/*
// active fg call state
			Method getActiveFgCallState = cmClazz.getDeclaredMethod("getActiveFgCallState");
//			getActiveFgCallState.setAccessible(true);
			Object activeFgCallState = getActiveFgCallState.invoke(cmClazz.cast(callManager));
			Log.d("myLogs", this.getClass().getSimpleName() + ": precised call state " + activeFgCallState.toString());
			
// active fg call
			Method getActiveFgCall = cmClazz.getDeclaredMethod("getActiveFgCall");
			Object activeFgCall = getActiveFgCall.invoke(cmClazz.cast(callManager));
			Log.d("myLogs", this.getClass().getSimpleName() + ": active fg call " + activeFgCall);
			
// has active fg call			
			Method hasActiveFgCall = cmClazz.getDeclaredMethod("hasActiveFgCall");
			Object hasFg = hasActiveFgCall.invoke(cmClazz.cast(callManager));
			Log.d("myLogs", this.getClass().getSimpleName() + ": has active fg call " + hasFg);

// has active bg call			
			Method hasActiveBgCall = cmClazz.getDeclaredMethod("hasActiveBgCall");
			Object hasBg = hasActiveBgCall.invoke(cmClazz.cast(callManager));
			Log.d("myLogs", this.getClass().getSimpleName() + ": has active bg call " + hasBg);
*/			
			
//			return activeFgCallState.toString();
			return null;
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			postFailure("Can't get precised call state");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			postFailure("Can't get precised call state");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			postFailure("Can't get precised call state");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			postFailure("Can't get precised call state");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			postFailure("Can't get precised call state");
		} catch (ClassCastException e) {
			e.printStackTrace();
			postFailure("Can't get precised call state");
		}
		return null;
	}
	
	private void recall() {
		workHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				endCall();
//				workHandler.removeCallbacks(Thrasher.this);
				// makes call immediately
				workHandler.postDelayed(ThrasherWithCustomGsmPhone.this, interval);
				
			}
		}, timeout);
	}
	
	private void endCall() {
		
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
	
	public interface Callback {
		
		public void onProgress(String message);
		
		public void onFailure(String message);
	}
	
}
