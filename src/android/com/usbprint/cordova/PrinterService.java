package com.usbprint.cordova;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;

import com.zj.usbsdk.*;

public class PrinterService extends CordovaPlugin {

    private UsbController usbController;
    private static final String TAG = "USBPrint";
    protected static final String ACTION_USB_PERMISSION = "com.zj.usbconn.USB";
    private CallbackContext callbackContext;

    protected void pluginInitialize() {
        Log.i(TAG, "Initializing Printer Service");
        usbController = new UsbController(cordova.getActivity(), mHandler);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Received message of permission request :" + msg.what);
            Toast.makeText(cordova.getActivity().getApplicationContext(),
                    "Received message of permission request " + msg.what, Toast.LENGTH_SHORT).show();
            switch (msg.what) {
            case UsbController.USB_CONNECTED:
                Log.i(TAG, "Got permission for the USB device");
                if (callbackContext != null) {
                    callbackContext.success("Connected");
                    this.callbackContext = null;
                }
                break;
            default:
                if (callbackContext != null) {
                    callbackContext.error("Permission denied");
                    this.callbackContext = null;
                }
                break;
            }
        }
    };

    private Context getApplicationContext() {
        return cordova.getActivity().getApplicationContext();
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getConnectedPrinters")) {
            // orderid, cust_id, email, phone, txn_amt
            getConnectedPrinters(callbackContext);
            return true;
        } else if (action.equals("connect")) {
            // orderid, cust_id, email, phone, txn_amt
            String printer_name = args.getString(0);
            connect(printer_name, callbackContext);
            return true;
        } else if (action.equals("print")) {
            // orderid, cust_id, email, phone, txn_amt
            String printer_name = args.getString(0);
            String msg = args.getString(1);
            print(printer_name, msg, callbackContext);
            return true;
        } else if (action.equals("isPaperAvailable")) {
            // orderid, cust_id, email, phone, txn_amt
            String printer_name = args.getString(0);
            isPaperAvailable(printer_name, callbackContext);
            return true;
        } else if (action.equals("cutPaper")) {
            // orderid, cust_id, email, phone, txn_amt
            String printer_name = args.getString(0);
            cutPaper(printer_name, callbackContext);
            return true;
        }
        return false;
    }

    private void getConnectedPrinters(final CallbackContext callbackContext) {
        JSONArray printers = new JSONArray();
        JSONObject jsonObj = new JSONObject();
        Log.i(TAG, String.format("Found: %s Devices ", usbController.getUsbList().size()));
        List<UsbDevice> lstPrinters = new ArrayList(usbController.getUsbList().values());
        for (UsbDevice usbDevice : lstPrinters) {
            if (UsbConstants.USB_CLASS_PRINTER == usbDevice.getInterface(0).getInterfaceClass()) {
                try {
                    JSONObject printerObj = new JSONObject();
                    printerObj.put("printername", usbDevice.getVendorId() + "_" + usbDevice.getDeviceId());
                    printerObj.put("productName", usbDevice.getProductName());
                    printerObj.put("manufacturerName", usbDevice.getManufacturerName());
                    printerObj.put("deviceId", usbDevice.getDeviceId());
                    printerObj.put("deviceName", usbDevice.getDeviceName());
                    printerObj.put("serialNumber", usbDevice.getSerialNumber());
                    printerObj.put("vendorId", usbDevice.getVendorId());
                    printerObj.put("protocol", usbDevice.getDeviceProtocol());
                    printerObj.put("deviceClass",
                            usbDevice.getDeviceClass() + "_" + translateDeviceClass(usbDevice.getDeviceClass()));
                    printerObj.put("deviceSubClass", usbDevice.getDeviceSubclass());
                    printers.put(printerObj);
                } catch (JSONException err) {
                    Log.e(TAG, "Exception in parsing to JSON object");
                }
            }
        }
        if (printers.length() <= 0) {
            Log.i(TAG, "No Printers identified");
        }
        callbackContext.success(printers);
    }

    private void connect(String printer_name, final CallbackContext callbackContext) {
        UsbDevice device = getDevice(printer_name);
        if (device != null) {
            Log.i(TAG, "Requesting permission for the device " + device.getDeviceId());
            this.callbackContext = callbackContext;
            usbController.getPermission(device);
        } else {
            callbackContext.error("No Printer of specified name is connected to check");
        }
    }

    private void isPaperAvailable(String printer_name, final CallbackContext callbackContext) {
        UsbDevice device = getDevice(printer_name);
        if (device != null) {
            callbackContext.success(String.valueOf(isPaperAvailable(device)));
        } else {
            callbackContext.error("No Printer of specified name is connected to check");
        }
    }

    private boolean isPaperAvailable(UsbDevice device) {
        byte isHasPaper = usbController.revByte(device);
        if (isHasPaper == 0x38) {
            return false;
        } else {
            return true;
        }
    }

    private void cutPaper(String printer_name, final CallbackContext callbackContext) {
        UsbDevice device = getDevice(printer_name);
        if (device != null) {
            callbackContext.success(String.valueOf(cutPaper(device)));
        } else {
            callbackContext.error("No Printer of specified name is connected to check");
        }
    }

    private boolean cutPaper(UsbDevice device) {
        if (isPaperAvailable(device)) {
            usbController.cutPaper(device, 0);
            return true;
        } else {
            return false;
        }
    }

    private void print(String printer_name, String msg, final CallbackContext callbackContext) {
        UsbDevice device = getDevice(printer_name);
        if (device != null) {
            if (isPaperAvailable(device)) {
                usbController.sendMsg(msg, "GBK", device);
                callbackContext.success("Printed");
            } else {
                callbackContext.error("No Printer of specified name is connected to check");
            }
        } else {
            callbackContext.error("No Printer of specified name is connected to check");
        }
    }

    private UsbDevice getDevice(String printer_name) {
        Log.i(TAG, String.format("Found: %s Devices ", usbController.getUsbList().size()));
        String[] parts = printer_name.split("_");
        if (parts.length == 2) {
            List<UsbDevice> lstPrinters = new ArrayList(usbController.getUsbList().values());
            for (UsbDevice usbDevice : lstPrinters) {
                if (usbDevice.getVendorId() == Integer.valueOf(parts[0])
                        && usbDevice.getDeviceId() == Integer.parseInt(parts[1])) {
                    return usbDevice;
                }
            }
        }
        return null;
    }

    private String translateDeviceClass(int deviceClass) {
        switch (deviceClass) {
        case UsbConstants.USB_CLASS_APP_SPEC:
            return "Application specific USB class";
        case UsbConstants.USB_CLASS_AUDIO:
            return "USB class for audio devices";
        case UsbConstants.USB_CLASS_CDC_DATA:
            return "USB class for CDC devices (communications device class)";
        case UsbConstants.USB_CLASS_COMM:
            return "USB class for communication devices";
        case UsbConstants.USB_CLASS_CONTENT_SEC:
            return "USB class for content security devices";
        case UsbConstants.USB_CLASS_CSCID:
            return "USB class for content smart card devices";
        case UsbConstants.USB_CLASS_HID:
            return "USB class for human interface devices (for example, mice and keyboards)";
        case UsbConstants.USB_CLASS_HUB:
            return "USB class for USB hubs";
        case UsbConstants.USB_CLASS_MASS_STORAGE:
            return "USB class for mass storage devices";
        case UsbConstants.USB_CLASS_MISC:
            return "USB class for wireless miscellaneous devices";
        case UsbConstants.USB_CLASS_PER_INTERFACE:
            return "USB class indicating that the class is determined on a per-interface basis";
        case UsbConstants.USB_CLASS_PHYSICA:
            return "USB class for physical devices";
        case UsbConstants.USB_CLASS_PRINTER:
            return "USB class for printers";
        case UsbConstants.USB_CLASS_STILL_IMAGE:
            return "USB class for still image devices (digital cameras)";
        case UsbConstants.USB_CLASS_VENDOR_SPEC:
            return "Vendor specific USB class";
        case UsbConstants.USB_CLASS_VIDEO:
            return "USB class for video devices";
        case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
            return "USB class for wireless controller devices";
        default:
            return "Unknown USB class!";
        }
    }

}