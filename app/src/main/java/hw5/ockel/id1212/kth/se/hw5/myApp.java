package hw5.ockel.id1212.kth.se.hw5;

import android.app.Application;
import android.os.AsyncTask;

import hw5.ockel.id1212.kth.se.hw5.net.ServerHandler;

public class myApp extends Application {
    private ServerHandler sh;

    public ServerHandler getServerHandler(){
        return sh;
    }

    public void setServerHandler(ServerHandler servHand){
        this.sh = servHand;
    }

}
