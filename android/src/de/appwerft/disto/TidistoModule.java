/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2017 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package de.appwerft.disto;

import java.io.IOException;
import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.json.JSONException;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.Device.ConnectionState;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.Listeners.ErrorListener;
import ch.leica.sdk.Devices.Device;
import ch.leica.sdk.Devices.DeviceManager;
import ch.leica.sdk.ErrorHandling.ErrorObject;
import ch.leica.sdk.ErrorHandling.IllegalArgumentCheckedException;
import ch.leica.sdk.ErrorHandling.PermissionException;
import ch.leica.sdk.LeicaSdk;
import ch.leica.sdk.Listeners.ErrorListener;

@Kroll.module(name = "Tidisto", id = "de.appwerft.disto")
public class TidistoModule extends KrollModule implements
		DeviceManager.FoundAvailableDeviceListener, Device.ConnectionListener,
		ErrorListener {

	// Standard Debugging variables
	public static final String LCAT = "TiDisto";

	private ArrayList<String> keys = new ArrayList<>();

	private KrollFunction Callback;
	boolean findDevicesRunning = false;
	/**
	 * Current selected device
	 */
	Device currentDevice;
	Context ctx;
	DeviceManager deviceManager;
	@Kroll.constant
	public static final int distoWifi = 1;
	@Kroll.constant
	public static final int distoBle = 2;
	@Kroll.constant
	public static final int yeti = 4;
	@Kroll.constant
	public static final int disto3DD = 8;

	public TidistoModule() {
		super();
		ctx = TiApplication.getInstance().getApplicationContext();
		deviceManager = DeviceManager.getInstance(ctx);
	}

	@Kroll.method
	public String getVersion() {
		return LeicaSdk.getVersion();
	}

	@Kroll.method
	public boolean isBluetoothAvailable() {
		return deviceManager.checkBluetoothAvailibilty();
	}

	@Kroll.method
	public void enableBLE() {
		deviceManager.enableBLE();
	}

	@Kroll.method
	public void init() {
		Log.i(LCAT, "====== START leica ========");

		if (LeicaSdk.isInit == false) {
			LeicaSdk.InitObject initObject = new LeicaSdk.InitObject(
					"commands.json");
			try {
				LeicaSdk.init(ctx, initObject);
				LeicaSdk.setMethodCalledLog(false);

				LeicaSdk.setScanConfig(false, true, false, false);
				LeicaSdk.setLicenses(keys);
				Log.d(LCAT, keys.toString());
				Log.d(LCAT, "Interface started >>>>>>>>>>>");

			} catch (JSONException e) {
				Log.e(LCAT,
						"Error in the structure of the JSON File, closing the application");
				Log.d(LCAT, e.getMessage());

			} catch (IllegalArgumentCheckedException e) {
				Log.e(LCAT,
						"Error in the data of the JSON File, closing the application");
				Log.d(LCAT, e.getMessage());

			} catch (IOException e) {
				Log.d(LCAT, e.getMessage());

			}

		} else
			Log.d(LCAT, "was always initalized.");

		Log.i(LCAT, "deviceManager created");
		deviceManager.setFoundAvailableDeviceListener(this);
		deviceManager.setErrorListener(this);
		KrollDict res = new KrollDict();
		Log.i(LCAT, "listeners added");
		res.put("BluetoothAvailibilty",
				deviceManager.checkBluetoothAvailibilty());
		res.put("WiFiAvailibilty", deviceManager.checkWifiAvailibilty());
		dispatchMessage(res);
		findAvailableDevices();

	}

	@Kroll.method
	public void findAvailableDevices() {

		findDevicesRunning = true;

		// Verify and enable Wifi and Bluetooth, according to what the user
		// allowed
		verifyPermissions();

		deviceManager.setErrorListener(this);
		deviceManager.setFoundAvailableDeviceListener(this);

		try {
			deviceManager.findAvailableDevices(TiApplication
					.getAppCurrentActivity().getApplicationContext());
		} catch (PermissionException e) {
			if (LeicaSdk.ERROR) {
				Log.e(LCAT, "Wissing permission: " + e.getMessage());
			}
		}

	}

	@Kroll.method
	public void addLicences(String key) {
		keys.add(key);
	};

	@Kroll.method
	public void stopFindingDevices() {
		Log.i(LCAT,
				" Stop find Devices Task and set BroadcastReceivers to Null");
		findDevicesRunning = false;
		deviceManager.stopFindingDevices();
	}

	@Override
	public void onError(ErrorObject arg0, Device arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnectionStateChanged(Device arg0, ConnectionState arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAvailableDeviceFound(final Device device) {

		final String METHODTAG = ".onAvailableDeviceFound";
		// stopFindingDevices();

		// uiHelper.setLog(this, log, "DeviceId found: " + device.getDeviceID()
		// + ", deviceName: " + device.getDeviceName());
		// new Thread
		// Log.i(CLASSTAG, METHODTAG + "DeviceId found: " + device.getDeviceID()
		// + ", deviceName: " + device.getDeviceName());

		// Call this to avoid interference in Bluetooth operations

		KrollDict res = new KrollDict();
		res.put("device", new DeviceProxy(device));

		currentDevice = device;

	}

	private boolean hasPermission(String permission) {
		if (Build.VERSION.SDK_INT >= 23) {
			Activity currentActivity = TiApplication.getInstance()
					.getCurrentActivity();
			if (currentActivity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
				return false;
			}
		}
		return true;
	}

	public boolean verifyPermissions() {
		Log.d(LCAT, "Starting verifyPermissions()");
		boolean granted = false;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!hasPermission("android.permission.ACCESS_FINE_LOCATION")
					&& !hasPermission("android.permission.ACCESS_COARSE_LOCATION"))
				granted = false;
			Log.i(LCAT, "ACCESS_FINE_LOCATION="
					+ hasPermission("android.permission.ACCESS_FINE_LOCATION"));
			Log.i(LCAT,
					"ACCESS_COARSE_LOCATION="
							+ hasPermission("android.permission.ACCESS_COARSE_LOCATION"));

			LocationManager locationManager = (LocationManager) ctx
					.getSystemService(Context.LOCATION_SERVICE);
			boolean network_enabled = false;
			try {
				network_enabled = locationManager
						.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			} catch (Exception e) {
				granted=false;
				Log.e(LCAT + "NETWORK PROVIDER, network not enabled",
						e.getMessage());
			}
			if (network_enabled) {
				// LeicaSdk.scanConfig.setWifiAdapterOn(true);
				LeicaSdk.scanConfig.setBleAdapterOn(ctx.getPackageManager()
						.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
			}
			Log.i(LCAT,
					"Permissions: WIFI: "
							+ LeicaSdk.scanConfig.isWifiAdapterOn() + ", BLE: "
							+ LeicaSdk.scanConfig.isBleAdapterOn());
			if (!hasPermission("android.permission.ACCESS_FINE_LOCATION")) granted=false;
		}
		return granted;
	}

	private void dispatchMessage(KrollDict dict) {
		Log.i(LCAT, dict.toString());
		if (Callback != null) {
			Callback.call(getKrollObject(), dict);
		}
		KrollFunction onTest = (KrollFunction) getProperty("onTest");
		if (onTest != null) {
			onTest.call(getKrollObject(), new Object[] { dict });
		}
		if (hasListeners("availableDeviceFound"))
			fireEvent("availableDeviceFound", dict);
	}

}
