package pt.wingman.entel.plugin;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

public class FingerPrintUsbDeviceConnection {
    private UsbDeviceConnection usbDeviceConnection = null;
    public String serial = null;
    public int sensorFileDescriptor = -1;
    public int sensorBus = -1;
    public int sensorAddress = -1;

    public void updateData(UsbDevice usbDevice, UsbDeviceConnection newUsbDeviceConnection) {
        if (newUsbDeviceConnection == null) {
            usbDeviceConnection = null;
            serial = null;
            sensorFileDescriptor = -1;
            sensorBus = -1;
            sensorAddress = -1;
        } else {
            usbDeviceConnection = newUsbDeviceConnection;
            serial = newUsbDeviceConnection.getSerial();
            sensorFileDescriptor = newUsbDeviceConnection.getFileDescriptor();
            String[] strings = usbDevice.getDeviceName().split("/");
            if (strings.length >= 5) {
                this.sensorBus = Integer.parseInt(strings[4]);
                this.sensorAddress = Integer.parseInt(strings[5]);
            }
        }
    }

    public boolean isValidConnection() {
        return usbDeviceConnection != null && serial != null && sensorFileDescriptor > 0 && sensorBus > 0 && sensorAddress > 0;
    }
}
