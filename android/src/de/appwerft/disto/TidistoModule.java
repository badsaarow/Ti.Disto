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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
import ch.leica.sdk.Types;
import ch.leica.sdk.Listeners.ErrorListener;

@Kroll.module(name = "Tidisto", id = "de.appwerft.disto", propertyAccessors = { "onScanResult" })
public class TidistoModule extends KrollModule implements
		DeviceManager.FoundAvailableDeviceListener, Device.ConnectionListener,
		ErrorListener {

	@Kroll.constant
	public static final int DEVICE_TYPE_BLE = Types.DeviceType.Ble.ordinal();
	@Kroll.constant
	public static final int DEVICE_TYPE_DISTO = Types.DeviceType.Disto
			.ordinal();
	@Kroll.constant
	public static final int DEVICE_TYPE_YETI = Types.DeviceType.Yeti.ordinal();

	@Kroll.constant
	public static final int DEVICE_CONNECTION_STATE_CONNECTED = Device.ConnectionState.connected
			.ordinal();
	@Kroll.constant
	public static final int DEVICE_CONNECTION_STATE_DISCONNECTED = Device.ConnectionState.disconnected
			.ordinal();
	@Kroll.constant
	public static final int DEVICE_STATE_NORMAL = Device.DeviceState.normal
			.ordinal();
	@Kroll.constant
	public static final int DEVICE_STATE_UPDATE = Device.DeviceState.update
			.ordinal();
	@Kroll.constant
	public static final int CONNECTION_TYPE_WIFI_AP = Types.ConnectionType.wifiAP
			.ordinal();
	@Kroll.constant
	public static final int CONNECTION_TYPE_WIFI_HOTSPOT = Types.ConnectionType.wifiHotspot
			.ordinal();

	@Kroll.constant
	public static final int WIFI = 1;
	@Kroll.constant
	public static final int BLE = 2;
	@Kroll.constant
	public static final int BLUETOOTH = 2;
	List<Device> availableDevices = new ArrayList<>();
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
	// needed for connection timeout
	Timer connectionTimeoutTimer;
	TimerTask connectionTimeoutTask;
	// to do infinite rounds of finding devices
	Timer findDevicesTimer;
	boolean activityStopped = true;
	// to handle user cancel connection attempt
	Map<Device, Boolean> connectionAttempts = new HashMap<>();
	Device currentConnectionAttemptToDevice = null;
	public static boolean DEBUG = false;

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
	public KrollDict getConnectedDevices() {
		KrollDict res = new KrollDict();
		List<DeviceProxy> deviceArray = new ArrayList<DeviceProxy>();
		List<Device> devices = deviceManager.getConnectedDevices();
		for (Device device : devices) {
			deviceArray.add(new DeviceProxy(device));
		}
		res.put("devices", deviceArray.toArray(new DeviceProxy[devices.size()]));
		return res;
	}

	@Kroll.method
	public boolean isBluetoothAvailable() {
		return deviceManager.checkBluetoothAvailibilty();
	}

	@Kroll.method
	public TidistoModule enableBLE() {
		if (isBluetoothAvailable() == false)
			deviceManager.enableBLE();
		return this;
	}

	@Kroll.method
	public TidistoModule setTimeout(int timeout) {
		return this;
	}

	@Kroll.method
	public TidistoModule enableDebugging() {
		DEBUG = true;
		return this;

	}

	private TidistoModule init() {
		boolean[] modi = { false, false, false, false };
		if (DEBUG) Log.i(LCAT, "====== START leica ========");

		if (LeicaSdk.isInit == false) {
			LeicaSdk.InitObject initObject = new LeicaSdk.InitObject(
					"commands.json");
			try {
				LeicaSdk.init(ctx, initObject);
				LeicaSdk.setMethodCalledLog(false);
				LeicaSdk.setScanConfig(modi[0], modi[1], modi[2], modi[3]);
				LeicaSdk.setLicenses(keys);
				if (DEBUG)  Log.d(LCAT, keys.toString());
				

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
			if (DEBUG)  Log.d(LCAT, "was always initalized.");

		if (DEBUG) Log.i(LCAT, "deviceManager created");
		deviceManager.setFoundAvailableDeviceListener(this);
		deviceManager.setErrorListener(this);
		KrollDict res = new KrollDict();
		if (DEBUG) Log.i(LCAT, "listeners added");
		res.put("BluetoothAvailibilty",
				deviceManager.checkBluetoothAvailibilty());
		res.put("WiFiAvailibilty", deviceManager.checkWifiAvailibilty());
		dispatchMessage(res);
		findAvailableDevices();
		return this;
	}

	@Kroll.method
	public void findAvailableDevices() {
		init();
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
	public TidistoModule addLicence(String key) {
		keys.add(key);
		return this;
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

	/**
	 * called when a valid Leica device is found
	 *
	 * @param device
	 *            the device
	 */
	@Override
	public void onAvailableDeviceFound(final Device device) {

		final String METHODTAG = ".onAvailableDeviceFound";
		synchronized (availableDevices) {

			// in rare cases it can happen, that a device is found twice. so
			// here is a double check.
			for (Device availableDevice : availableDevices) {
				if (availableDevice.getDeviceID().equalsIgnoreCase(
						device.getDeviceID())) {
					return;
				}
			}
			KrollDict res = new KrollDict();
			res.put("device", new DeviceProxy(device));
			if (device == null) {
				Log.i(METHODTAG, "device not found");
				return;
			}
			availableDevices.add(device);
		}

		// updateList();

		// uiHelper.setLog(this, log, "DeviceId found: " + device.getDeviceID()
		// + ", deviceName: " + device.getDeviceName());
		// new Thread
		// Log.i(CLASSTAG, METHODTAG + "DeviceId found: " + device.getDeviceID()
		// + ", deviceName: " + device.getDeviceName());

		// Call this to avoid interference in Bluetooth operations

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
				granted = false;
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
			if (!hasPermission("android.permission.ACCESS_FINE_LOCATION"))
				granted = false;
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
