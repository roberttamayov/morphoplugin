package pt.wingman.entel.plugin.definitions;

public @interface FingerprintStatus {
    int DISCONNECTED = -1;
	int CONNECTED = 0;
	int STARTED = 1;
    int SCANNING = 2;
    int STOPED = 3;
}