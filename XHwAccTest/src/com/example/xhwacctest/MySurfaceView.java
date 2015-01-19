package com.example.xhwacctest;

import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class MySurfaceView extends SurfaceView implements Callback {

	private SurfaceHolder mHolder;
	private Handler mDrawHandler;
	private DrawThread mDrawThread;
	private Bitmap mBitmapBackground;
	private Bitmap mBitmapFocus;
	private Rect mRectFocus;
	public static boolean bHardwareAccelerated;
	private final Rect RECT_LEFT_TOP = new Rect(130, 60, 406, 381);
	private final Rect RECT_RIGHT_TOP = new Rect(1514, 60, 1790, 381);
	private final int FOCUS_MOVING_STEP = 60;
	private final int defaultFocusWidth = 276;
	private final int defaultFocusHeight = 321;
	private static final int MSG_DRAW = 0x0001;
	private static final int MSG_MOVE_LEFT = 0x0002;
	private static final int MSG_MOVE_RIGHT = 0x0003;

	public MySurfaceView(Context context) {
		super(context);
		init(context);
	}

	public MySurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public MySurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		mHolder = getHolder();
		mHolder.setFormat(PixelFormat.TRANSPARENT);
		mHolder.addCallback(this);
		mRectFocus = new Rect();
		mRectFocus.set(RECT_LEFT_TOP);
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image_focus);
		mBitmapFocus = Bitmap.createScaledBitmap(bitmap, defaultFocusWidth, defaultFocusHeight, true);
		mBitmapBackground = BitmapFactory.decodeResource(getResources(), R.drawable.image_background);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(mDrawThread == null) {
			mDrawThread = new DrawThread("DrawThread");
			mDrawThread.setPriority(Thread.MAX_PRIORITY);
			mDrawThread.start();
			mDrawHandler = new Handler(mDrawThread.getLooper(), mDrawThread);
			Canvas canvas = mHolder.lockCanvas();
			bHardwareAccelerated = canvas.isHardwareAccelerated();
			mHolder.unlockCanvasAndPost(canvas);
			refresh();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mDrawThread.quit();
		mDrawThread.interrupt();
		mDrawThread = null;
		mDrawHandler = null;
	}
	
	private class DrawThread extends HandlerThread implements
			android.os.Handler.Callback {

		public DrawThread(String name) {
			super(name);
		}

		public DrawThread(String name, int priority) {
			super(name, priority);
		}

		@Override
		public boolean handleMessage(Message msg) {
			TimeElapsed.start(msg.what);
			switch(msg.what) {
			case MSG_DRAW:
				draw();
				break;
			case MSG_MOVE_LEFT:
				moveLeft();
				break;
			case MSG_MOVE_RIGHT:
				moveRight();
				break;
			}
			TimeElapsed.printElapsed();
			return false;
		}
	}

	void moveLeft() {
		int x = RECT_LEFT_TOP.left;

		while(mRectFocus.left > x) {
			mRectFocus.left -= FOCUS_MOVING_STEP;
			if(mRectFocus.left > x)
				draw();
		}
		mRectFocus.set(RECT_LEFT_TOP);
		draw();
	}

	void moveRight() {
		int x = RECT_RIGHT_TOP.left;
		while(mRectFocus.left < x) {
			mRectFocus.left += FOCUS_MOVING_STEP;
			if(mRectFocus.left < x)
				draw();
		}
		mRectFocus.set(RECT_RIGHT_TOP);
		draw();
	}

	void refresh() {
		mDrawHandler.removeMessages(MSG_DRAW);
		Message msg = mDrawHandler.obtainMessage(MSG_DRAW);
		mDrawHandler.sendMessage(msg);
	}

	void draw() {
		Canvas canvas = mHolder.lockCanvas();
		canvas.save();
		canvas.drawBitmap(mBitmapBackground, 0, 0, null);
		canvas.drawBitmap(mBitmapFocus, mRectFocus.left, mRectFocus.top, null);
		canvas.restore();
		mHolder.unlockCanvasAndPost(canvas);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode)
		{
		case KeyEvent.KEYCODE_DPAD_LEFT:
			mDrawHandler.removeMessages(MSG_MOVE_LEFT);
			mDrawHandler.sendEmptyMessage(MSG_MOVE_LEFT);
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			mDrawHandler.removeMessages(MSG_MOVE_RIGHT);
			mDrawHandler.sendEmptyMessage(MSG_MOVE_RIGHT);
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

    static class TimeElapsed {
        private static long startTime = 0;
        private static int msgId = 0;
        static long getTime() {
            Date d = new Date();
            return d.getTime();
        }
        static void start(int id) {
            startTime = getTime();
            msgId = id;
        }
        static void printElapsed() {
            long elapsedTime = getTime() - startTime;
            Log.i("MySurfaceView", "XXX "+"bHardwareAccelerated: "+bHardwareAccelerated+" "+category()+" "+elapsedTime);
        }
        static private String category() {
            switch(msgId) {
            case MSG_DRAW: return " DRAW";
            case MSG_MOVE_LEFT: return " LEFT";
            case MSG_MOVE_RIGHT: return "RIGHT";
            }
            return "";
        }
    }
}
