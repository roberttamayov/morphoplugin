package pt.wingman.entel.plugin;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.morpho.android.usb.USBManager;
import com.morpho.morphosmart.sdk.CallbackMask;
import com.morpho.morphosmart.sdk.CallbackMessage;
import com.morpho.morphosmart.sdk.CompressionAlgorithm;
import com.morpho.morphosmart.sdk.DetectionMode;
import com.morpho.morphosmart.sdk.ErrorCodes;
import com.morpho.morphosmart.sdk.IMsoSecu;
import com.morpho.morphosmart.sdk.LatentDetection;
import com.morpho.morphosmart.sdk.MorphoDevice;
import com.morpho.morphosmart.sdk.MorphoImage;
import com.morpho.morphosmart.sdk.SecuConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import morpho.msosecu.sdk.api.MsoSecu;
import pt.wingman.entel.plugin.definitions.FingerprintError;
import pt.wingman.entel.plugin.definitions.FingerprintMessageType;
import pt.wingman.entel.plugin.definitions.FingerprintStatus;

import com.digitalpersona.uareu.dpfj.CompressionImpl;
import com.digitalpersona.uareu.UareUException;

public class FingerprintManager {
    //region static
    private static final String USB_PERMISSION = "com.morpho.android.usb.USB_PERMISSION";
    private static FingerprintManager fingerprintManager;
    private static final int callbackCmd = ((((CallbackMask.MORPHO_CALLBACK_IMAGE_CMD.getValue() | CallbackMask.MORPHO_CALLBACK_ENROLLMENT_CMD.getValue()) | CallbackMask.MORPHO_CALLBACK_COMMAND_CMD.getValue()) | CallbackMask.MORPHO_CALLBACK_CODEQUALITY.getValue()) | CallbackMask.MORPHO_CALLBACK_DETECTQUALITY.getValue()) & (~CallbackMask.MORPHO_CALLBACK_ENROLLMENT_CMD.getValue());

    public static synchronized FingerprintManager getInstance() {
        if (fingerprintManager == null)
            fingerprintManager = new FingerprintManager();
        return fingerprintManager;
    }

    static {
        try {
            System.loadLibrary("MSO_Secu");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
    //endregion

    private Context context;
    private FingerprintManagerCallback fingerprintManagerCallback;
    private MorphoDevice morphoDevice;
    private FingerPrintUsbDeviceConnection fingerPrintUsbDeviceConnection;
    private SecuConfig secuConfig;
    private int compressionAlgorithmValue;
    private int compressionRate;
    private boolean isLatentDetection;

    public void initialize(Context newContext, FingerprintManagerCallback newFingerPrintManagerCallback, int newCompressionAlgorithmValue, int newCompressionRate, boolean newLatentDetection) {
        context = newContext;
        fingerprintManagerCallback = newFingerPrintManagerCallback;
        compressionAlgorithmValue = newCompressionAlgorithmValue;
        compressionRate = newCompressionRate;
        isLatentDetection = newLatentDetection;
        morphoDevice = new MorphoDevice();
        fingerPrintUsbDeviceConnection = new FingerPrintUsbDeviceConnection();
        secuConfig = new SecuConfig();
        USBManager.getInstance().initialize(context, context.getPackageName() + ".USB_ACTION", true);
        if (USBManager.getInstance().isDevicesHasPermission()) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STARTED);
            connectToMorphoDevice();
        } else {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onError(FingerprintError.PERMISSION_DENIED);
        }
    }
	
	public void initialize2(Context newContext, FingerprintManagerCallback newFingerPrintManagerCallback) {
        context = newContext;
		fingerprintManagerCallback = newFingerPrintManagerCallback;
        morphoDevice = new MorphoDevice();
        fingerPrintUsbDeviceConnection = new FingerPrintUsbDeviceConnection();
        secuConfig = new SecuConfig();
        USBManager.getInstance().initialize(context, context.getPackageName() + ".USB_ACTION", true);
        if (USBManager.getInstance().isDevicesHasPermission()) {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			HashMap<String, UsbDevice> usbDeviceHashMap = usbManager.getDeviceList();
			if (usbDeviceHashMap.isEmpty()) {
				fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.DISCONNECTED);
			} else {
				UsbDevice usbDevice = usbDeviceHashMap.values().iterator().next();
				if(usbDevice.getVendorId() != 8797) return;
				if (MorphoUtils.isSupported(usbDevice.getVendorId(), usbDevice.getProductId())) {
					if (usbManager.hasPermission(usbDevice)) {
						fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.CONNECTED);
					} else {
						usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(context, 0, new Intent(USB_PERMISSION), 0));
						fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.CONNECTED);
					}
				}
			}
        } else {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.DISCONNECTED);
        }
    }

    private void connectToMorphoDevice() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDeviceHashMap = usbManager.getDeviceList();
        if (usbDeviceHashMap.isEmpty()) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onError(FingerprintError.NO_DEVICE_FOUND);
        } else {
            UsbDevice usbDevice = usbDeviceHashMap.values().iterator().next();
			if(usbDevice.getVendorId() != 8797) return;
            if (MorphoUtils.isSupported(usbDevice.getVendorId(), usbDevice.getProductId())) {
                if (usbManager.hasPermission(usbDevice)) {
                    updateUsbDeviceConnection(usbManager, usbDevice);
                } else {
                    usbManager.requestPermission(usbDevice, PendingIntent.getBroadcast(context, 0, new Intent(USB_PERMISSION), 0));
                }
            }
        }
    }

    private void updateUsbDeviceConnection(UsbManager usbManager, UsbDevice usbDevice) {
        fingerPrintUsbDeviceConnection.updateData(usbDevice, usbManager.openDevice(usbDevice));
        if (fingerPrintUsbDeviceConnection.isValidConnection()) {
            readMorphoDevice(fingerPrintUsbDeviceConnection);
        } else {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onError(FingerprintError.INVALID_CONNECTION);
        }
    }

    private void readMorphoDevice(FingerPrintUsbDeviceConnection fingerPrintUsbDeviceConnection) {
        int morphoErrorCode;
        morphoErrorCode = this.morphoDevice.openUsbDeviceFD(fingerPrintUsbDeviceConnection.sensorBus, fingerPrintUsbDeviceConnection.sensorAddress, fingerPrintUsbDeviceConnection.sensorFileDescriptor, 0);
        if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onSDKError(morphoErrorCode, getSDKErrorMessage(morphoErrorCode));
            return;
        }

        morphoErrorCode = this.morphoDevice.getSecuConfig(secuConfig);
        if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
            fingerprintManagerCallback.onSDKError(morphoErrorCode, getSDKErrorMessage(morphoErrorCode));
            return;
        }

        IMsoSecu iMsoSecu = new MsoSecu();
        iMsoSecu.setOpenSSLPath("sdcard/Keys/");
        if (secuConfig.isModeOfferedSecurity()) {
            morphoErrorCode = this.morphoDevice.offeredSecuOpen();
            if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                fingerprintManagerCallback.onSDKError(morphoErrorCode, getSDKErrorMessage(morphoErrorCode));
                return;
            }
        }

        if (secuConfig.isModeTunneling()) {
            ArrayList<Byte> hostCertificate = new ArrayList<Byte>();
            iMsoSecu.getHostCertif(hostCertificate);
            morphoErrorCode = this.morphoDevice.tunnelingOpen(MorphoUtils.toByteArray(hostCertificate));
            if (morphoErrorCode != ErrorCodes.MORPHO_OK) {
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                fingerprintManagerCallback.onSDKError(morphoErrorCode, getSDKErrorMessage(morphoErrorCode));
                return;
            }
        }

        getFingerPrint(observer);
    }

    public void getFingerPrint(final Observer observer) {
        new Thread(new Runnable() {
            public void run() {
                int timeOut = 0;
                int acquisitionThreshold = 0;
                MorphoImage morphoImage = new MorphoImage();
                LatentDetection latentDetection = isLatentDetection ? LatentDetection.LATENT_DETECT_ENABLE : LatentDetection.LATENT_DETECT_DISABLE;
                com.morpho.morphosmart.sdk.CompressionAlgorithm compressionAlgorithm = MorphoUtils.getCompressionAlgorithm(compressionAlgorithmValue);
                int detectModeChoice = DetectionMode.MORPHO_ENROLL_DETECT_MODE.getValue();
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.SCANNING);
                int morphoErrorCode = morphoDevice.getImage(timeOut, acquisitionThreshold, compressionAlgorithm, compressionRate, detectModeChoice, latentDetection, morphoImage, callbackCmd, observer);
                if (morphoErrorCode == ErrorCodes.MORPHO_OK) {
					
					try {
							int width = morphoImage.getMorphoImageHeader().getNbColumn();
							int height = morphoImage.getMorphoImageHeader().getNbRow();
							byte[] bytes = morphoImage.getImage();
							byte[] rawCompress = processImage(bytes,width,height);
							
							fingerprintManagerCallback.onBitmapUpdate(width, height, encode(rawCompress));
							
						} catch (Exception e) {
							fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
							fingerprintManagerCallback.onError(FingerprintError.UNEXPECTED);
						}	

                    stop();
                } else {
                    fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                    fingerprintManagerCallback.onSDKError(morphoErrorCode, getSDKErrorMessage(morphoErrorCode));
                }
            }
        }).start();
    }

    private Observer observer = new Observer() {
        public synchronized void update(Observable observable, Object object) {
            try {
                CallbackMessage message = (CallbackMessage) object;
                switch (message.getMessageType()) {
                    case FingerprintMessageType.FINGER_POSITION:
                        fingerprintManagerCallback.onFingerStatusUpdate((Integer) message.getMessage());
                        break;
                    case FingerprintMessageType.BITMAP_UPDATE:
                        byte[] bytes = (byte[]) message.getMessage();
                        MorphoImage morphoImage = MorphoImage.getMorphoImageFromLive(bytes);
						
                        int width = morphoImage.getMorphoImageHeader().getNbColumn();
                        int height = morphoImage.getMorphoImageHeader().getNbRow();
						
						//byte[] rawCompress = processImage(bytes,width,height);
						//fingerprintManagerCallback.onBitmapUpdate(width, height, encode(rawCompress));
						
                        break;
                    case FingerprintMessageType.PERCENTAGE_UPDATE:
                        fingerprintManagerCallback.onPercentageUpdate((int) Integer.parseInt(message.getMessage().toString()));
                        break;
                }
            } catch (Exception e) {
                fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
                fingerprintManagerCallback.onError(FingerprintError.UNEXPECTED);
            }
        }
    };

    public void stop() {
        if (morphoDevice != null) {
            morphoDevice.cancelLiveAcquisition();
            closeDevice();
            fingerprintManagerCallback.onFingerprintStatusUpdate(FingerprintStatus.STOPED);
        }
    }

    private void closeDevice() {
        if (secuConfig.isModeOfferedSecurity()) {
            this.morphoDevice.offeredSecuClose();
        }
        if (secuConfig.isModeTunneling()) {
            this.morphoDevice.tunnelingClose();
        }
        this.morphoDevice.closeDevice();
    }
	
	private String encode(byte[] d) {
        if (d == null) {
            return null;
        }
        int idx;
        byte[] data = new byte[(d.length + 2)];
        System.arraycopy(d, 0, data, 0, d.length);
        byte[] dest = new byte[((data.length / 3) * 4)];
        int sidx = 0;
        int didx = 0;
        while (sidx < d.length) {
            dest[didx] = (byte) ((data[sidx] >>> 2) & 63);
            dest[didx + 1] = (byte) (((data[sidx + 1] >>> 4) & 15) | ((data[sidx] << 4) & 63));
            dest[didx + 2] = (byte) (((data[sidx + 2] >>> 6) & 3) | ((data[sidx + 1] << 2) & 63));
            dest[didx + 3] = (byte) (data[sidx + 2] & 63);
            sidx += 3;
            didx += 4;
        }
        for (idx = 0; idx < dest.length; idx++) {
            if (dest[idx] < (byte) 26) {
                dest[idx] = (byte) (dest[idx] + 65);
            } else if (dest[idx] < (byte) 52) {
                dest[idx] = (byte) ((dest[idx] + 97) - 26);
            } else if (dest[idx] < (byte) 62) {
                dest[idx] = (byte) ((dest[idx] + 48) - 52);
            } else if (dest[idx] < (byte) 63) {
                dest[idx] = (byte) 43;
            } else {
                dest[idx] = (byte) 47;
            }
        }
        for (idx = dest.length - 1; idx > (d.length * 4) / 3; idx--) {
            dest[idx] = (byte) 61;
        }
        return new String(dest);
    }
	
	public byte[] processImage(byte[] img, int width, int height){
            
            
            
             Bitmap bmWSQ = null;
             bmWSQ = getBitmapAlpha8FromRaw(img, width,
                           height);
 
             byte[] arrayT = null;
 
             Bitmap redimWSQ = overlay(bmWSQ);
             int numOfbytes = redimWSQ.getByteCount();
             ByteBuffer buffer = ByteBuffer.allocate(numOfbytes);
             redimWSQ.copyPixelsToBuffer(buffer);
             arrayT = buffer.array();
 
             int v1 = 1;
             for (int i = 0; i < arrayT.length; i++) {
                    if (i < 40448) { // 79
                           arrayT[i] = (byte) 255;
                    } else if (i >= 40448 && i <= 221696) {
 
                           if (v1 < 132) {
                                  arrayT[i] = (byte) 255;
                           } else if (v1 > 382) {
                                  arrayT[i] = (byte) 255;
                           }
                           if (v1 == 512) {
                                  v1 = 0;
                           }
                           v1++;
                    } else if (i > 221696) { // 433
                           arrayT[i] = (byte) 255;
                    }
 
             }
 
             CompressionImpl comp = new CompressionImpl();
             try {
                    comp.Start();
                    comp.SetWsqBitrate(500, 0);
					
                    byte[] rawCompress = comp.CompressRaw(arrayT, redimWSQ.getWidth(), redimWSQ.getHeight(), 500, 8,
                                  com.digitalpersona.uareu.Compression.CompressionAlgorithm.COMPRESSION_WSQ_NIST);
                    
                    comp.Finish();
                   
                    Log.i("Util", "getting WSQ...");
 
                    return rawCompress;
                   
             } catch (UareUException e) {
                    Log.e("Util", "UareUException..." + e);
                    return null;
             } catch (Exception e) {
                    Log.e("Util", "Exception..." + e);
                    return null;
             }
 
      
            
       }
	   
	   private Bitmap overlay(Bitmap bmp) {
             Bitmap bmOverlay = Bitmap.createBitmap(512, 512, Config.ALPHA_8);
             Canvas canvas = new Canvas(bmOverlay);
             canvas.drawBitmap(bmp, 512 / 2 - bmp.getWidth() / 2, 512 / 2 - bmp.getHeight() / 2, null);
             canvas.save();
             return bmOverlay;
       }
	   
	   private Bitmap getBitmapAlpha8FromRaw(byte[] Src, int width, int height)
	   { 
             byte [] Bits = new byte[Src.length];
             int i = 0;
             for(i=0;i<Src.length;i++)
             {
                    Bits[i] = Src[i];
             }
            
             Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
             bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
            
             return bitmap;
       }

    //region Error
    private String getSDKErrorMessage(int morphoErrorCode) {
        return ErrorCodes.getError(morphoErrorCode, morphoDevice.getInternalError());
    }
    //endregion

}