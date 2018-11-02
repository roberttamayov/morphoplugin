package pt.wingman.entel.plugin;

import android.util.Pair;

import com.morpho.morphosmart.sdk.CompressionAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MorphoUtils {
    public static boolean isSupported(int vid, int pid) {
        for (Pair<Integer, Integer> supportedAttribs : getSupportedDevices().keySet()) {
            if (supportedAttribs.first == vid && supportedAttribs.second == pid) {
                return true;
            }
        }
        return false;
    }

    private static Map<Pair<Integer, Integer>, String> getSupportedDevices() {
        Map<Pair<Integer, Integer>, String> supportedDevices = new HashMap<Pair<Integer, Integer>, String>();
        supportedDevices.put(Pair.create(1947, 35), "MSO100");
        supportedDevices.put(Pair.create(1947, 36), "MSO300");
        supportedDevices.put(Pair.create(1947, 38), "MSO350");
        supportedDevices.put(Pair.create(1947, 71), "CBM");
        supportedDevices.put(Pair.create(1947, 82), "MSO1350");
        supportedDevices.put(Pair.create(8797, 1), "MSO FVP");
        supportedDevices.put(Pair.create(8797, 2), "MSO FVP_C");
        supportedDevices.put(Pair.create(8797, 3), "MSO FVP_CL");
        supportedDevices.put(Pair.create(8797, 7), "MEPUSB");
        supportedDevices.put(Pair.create(8797, 8), "CBM-E3");
        supportedDevices.put(Pair.create(8797, 9), "CBM-V3");
        supportedDevices.put(Pair.create(8797, 10), "MSO1300-E3");
        supportedDevices.put(Pair.create(8797, 11), "MSO1300-V3");
        supportedDevices.put(Pair.create(8797, 12), "MSO1350-E3");
        supportedDevices.put(Pair.create(8797, 13), "MSO1350-V3");
        supportedDevices.put(Pair.create(8797, 14), "MA SIGMA");
        return supportedDevices;
    }

    public static byte[] toByteArray(ArrayList<Byte> array) {
        return toPrimitives(array.toArray(new Byte[array.size()]));
    }

    public static byte[] toPrimitives(Byte[] array) {
        byte[] b = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            b[i] = array[i];
        }
        return b;
    }

    public static CompressionAlgorithm getCompressionAlgorithm(int id) {
        CompressionAlgorithm[] compressionAlgorithms = CompressionAlgorithm.values();

        for(int i = 0; i < compressionAlgorithms.length; ++i) {
            if (compressionAlgorithms[i].getCode() == id) {
                return compressionAlgorithms[i];
            }
        }

        return CompressionAlgorithm.MORPHO_NO_COMPRESS;
    }
}
