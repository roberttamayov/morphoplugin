package pt.wingman.entel.plugin.definitions;

public @interface FingerprintError {
    int UNEXPECTED = -1;
    int PERMISSION_DENIED = 1;
    int NO_DEVICE_FOUND = 2;
    int INVALID_CONNECTION = 3;
}