package hw5.ockel.id1212.kth.se.hw5.net;

import android.net.NetworkInfo;

public interface Callback {
    void messageSent();
    void messageReceived(String message);
    void notifyConnected();
    void notifyDisconnected();
}
