package de.tosa.camera;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
	/** Called when the activity is first created. */
	private final String TAG = "Preview";

	SurfaceView mSurfaceView;
	SurfaceHolder mHolder;
	Size mPreviewSize;

	OrientationEventListener orientationListener;
	Camera cam;
	SurfaceView view;

	private int numberOfCameras;

	private int defaultCameraId;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.preview_activity);

		// Find the total number of cameras available
		numberOfCameras = Camera.getNumberOfCameras();

		// Find the ID of the default camera
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				defaultCameraId = i;
			}
		}

		mSurfaceView = (SurfaceView) findViewById(R.id.previewView);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = mSurfaceView.getHolder();
		mHolder.addCallback(this);
		
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

	}
	
	
	private Camera.PreviewCallback mPrevCallback = new Camera.PreviewCallback() 
	{
	        private Bitmap currentprev;

			public void onPreviewFrame( byte[] data, Camera Cam ) {
	                Log.d("CombineTestActivity", "Preview started");
	                Log.d("CombineTestActivity", "Data length = " 
	                        + data.length );
	                currentprev = BitmapFactory.decodeByteArray( data, 0, 
	                        data.length );

	               if( currentprev == null )
	                   Log.d(this.getClass().getSimpleName(), "currentprev is null" );

	                Log.d(this.getClass().getSimpleName(), "Preview Finished" );

	        }
	};
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		cam = Camera.open(defaultCameraId);
		Parameters parameters = cam.getParameters();
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(defaultCameraId, info);
		int rotation = this.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		cam.setDisplayOrientation(result);
		cam.setParameters(parameters);
		cam.setPreviewCallback(mPrevCallback);

	}

	@Override
	protected void onPause() {
		super.onPause();

		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (cam != null) {
			cam.setPreviewCallback(null);
			cam.release();
			cam = null;
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		try {
			if (cam != null) {
				cam.setPreviewDisplay(holder);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		if (cam != null) {
			cam.stopPreview();
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Parameters parameters = cam.getParameters();
		// parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
		cam.setParameters(parameters);
		cam.startPreview();
	}

}