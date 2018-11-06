package pt.wingman.entel.plugin;

public interface FingerprintManagerCallback {
    void onFingerStatusUpdate(int fingerStatus);

    void onBitmapUpdate(int width, int height, String base64String, String imageData);

    void onPercentageUpdate(int percentage);

    void onFingerprintStatusUpdate(int fingerprintStatus);

    void onError(int errorCode);

    void onSDKError(int sdkErrorCode, String errorMessage);
}
