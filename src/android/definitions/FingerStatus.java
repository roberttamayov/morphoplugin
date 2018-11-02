package pt.wingman.entel.plugin.definitions;

public @interface FingerStatus {
    int UNEXPECTED = -1;
    int NO_FINGER = 0;
    int MOVE_UP = 1;
    int MOVE_DOWN = 2;
    int MOVE_LEFT = 3;
    int MOVE_RIGHT = 4;
    int PRESS_HARDER = 5;
    int MOVE_LATENT = 6;
    int REMOVE_FINGER = 7;
    int FINGER_OK = 8;
}