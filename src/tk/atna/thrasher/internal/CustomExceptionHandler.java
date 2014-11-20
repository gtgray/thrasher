package tk.atna.thrasher.internal;

import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;

public class CustomExceptionHandler implements UncaughtExceptionHandler {
	
//	UncaughtExceptionHandler old;
	
	
	public CustomExceptionHandler() {
//		this.old = Thread.getDefaultUncaughtExceptionHandler();
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		ex.printStackTrace();
		
		
		Log.d("myLogs", this.getClass().getSimpleName() + ".uncaughtException: " + thread.isAlive());
		
//		Looper.myLooper().quit();
		
//		old.uncaughtException(thread, ex);
	}
}
