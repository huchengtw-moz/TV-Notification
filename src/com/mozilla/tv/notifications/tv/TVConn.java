package com.mozilla.tv.notifications.tv;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.util.Log;

public class TVConn {

  /**
   * all functions in this Listener will be called in unknown thread. DO NOT
   * UPDATE UI while listener is called. Please do it at UI thread.
   * 
   * @author hchu
   * 
   */
  public interface Listener extends EventListener {
    public void deviceStateUpdate(TVDevice device);

    public void deviceListUpdate(TVDevice[] devices);
  }

  private static final String TAG = "Connections";
  private static final int SCAN_PORT = 50624;
  private static final int UPDATE_PORT = 50625;
  private static final byte ADDRESS[] = { (byte) 224, 0, 0, 115 };
  private static final int BROADCAST_INTERVAL = 3000;
  private static final int DEVICE_TTL = 6000;
  private static final String REMOTE_URL = "http://people.mozilla.org/~schien/presentation-demo/secondary.html";

  private static TVConn instance;

  /**
   * get singleton of this class
   * 
   * @return
   */
  public static TVConn get() {
    synchronized (TVConn.class) {
      if (null == instance) {
        instance = new TVConn();
      }
    }
    return instance;
  }

  private DatagramSocket udpSocket = null;
  private Thread udpThread;
  private int udpPort;
  private TVDevice connectingDevice;
  private TVDevice connectedDevice;
  private boolean selfBroadcasting;
  private Vector<TVDevice> deviceList = new Vector<TVDevice>();
  private Vector<String> pendingQueue = new Vector<String>();
  private ArrayList<Listener> listenerList = new ArrayList<Listener>();
  private boolean autoConnect = false;

  // functions for event listeners
  public void addEventListener(Listener l) {
    listenerList.add(l);
  }

  public void removeEventListener(Listener l) {
    listenerList.remove(l);
  }

  private void fireStateUpate(TVDevice device) {
    for (Listener l : listenerList) {
      try {
        l.deviceStateUpdate(device);
      } catch (Exception ex) {
      }
    }
  }

  private void fireListUpate(TVDevice[] devices) {
    for (Listener l : listenerList) {
      try {
        l.deviceListUpdate(devices);
      } catch (Exception ex) {
      }
    }
  }

  public boolean isRunning() {
    return selfBroadcasting;
  }

  // main functions
  public void start() {
    if (selfBroadcasting) {
      return;
    }
    selfBroadcasting = true;

    udpThread = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          udpSocket = new DatagramSocket();
          udpSocket.setSoTimeout(1000);
          udpPort = udpSocket.getLocalPort();
          Log.i(TAG, "open UDP socket at " + udpPort);
          startBroadcasting();
          startReceiving();

          byte[] buf = new byte[1024];
          DatagramPacket packet = new DatagramPacket(buf, buf.length);
          while (selfBroadcasting) {
            try {
              udpSocket.receive(packet);
              handleTask(packet);
            } catch (SocketTimeoutException ex) {
            }
          }
        } catch (SocketException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (null != udpSocket) {
            udpSocket.close();
            udpSocket = null;
          }
        }
      }

    });
    udpThread.start();
  }

  public void stop() {
    if (!selfBroadcasting) {
      return;
    }
    selfBroadcasting = false;
    try {
      udpThread.join(10 * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    synchronized (TVConn.class) {
      connectingDevice = null;
      connectedDevice = null;
    }

    deviceList.clear();
    fireListUpate(new TVDevice[0]);
  }

  public boolean isConnected() {
    return null != connectedDevice;
  }

  public boolean isAutoConnect() {
    return autoConnect;
  }

  public void setAutoConnect(boolean autoConnect) {
    this.autoConnect = autoConnect;
  }

  private DatagramPacket createDataPacket(String msg) throws JSONException, UnsupportedEncodingException {
    JSONObject jsObj = new JSONObject();
    jsObj.put("type", "ondata");
    synchronized (TVConn.class) {
      jsObj.put("channelId", connectedDevice.remoteChannelId);
      jsObj.put("message", msg);
      String data = jsObj.toString();
      final byte[] dataBytes = data.getBytes("UTF-8");
      // Log.i should put before udpSocket.send. But we need to enter critical
      // section to use connectedDevice variable. I don't want to create another
      // one there. So, I print the log here.
      Log.i(TAG, "sending message to " + connectedDevice.name);
      return new DatagramPacket(dataBytes, dataBytes.length,
              connectedDevice.remoteAddress, connectedDevice.remotePort);
    }
  }

  public void send(final String data) {
    // if start() hasn't called, the udpSocket will be null.
    if (null == udpSocket) {
      return;
    } else if (!isConnected()) {
      if (deviceList.size() > 0 && isAutoConnect()) {
        pendingQueue.add(data);
        connectToDevice(deviceList.firstElement());
      }
      return;
    }

    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          DatagramPacket packet = createDataPacket(data);
          udpSocket.send(packet);
        } catch (IOException e) {
          e.printStackTrace();
        } catch (JSONException e) {
          e.printStackTrace();
        }

      }
    });
    t.start();
  }

  private void flashQueue() {
    while (pendingQueue.size() > 0) {
      String data = pendingQueue.firstElement();
      pendingQueue.remove(data);
      send(data);
    }
  }

  private DatagramPacket createRequestSessionPacket(TVDevice device)
          throws UnsupportedEncodingException {

    String data = "{\"type\": \"requestSession\"," +
            "\"offer\": " + device.localChannelId + "," +
            "\"url\": \"" + REMOTE_URL + "\"}";
    byte[] dataBytes = data.getBytes("UTF-8");
    return new DatagramPacket(dataBytes, dataBytes.length, device.remoteAddress,
            device.remotePort);
  }

  public void connectToDevice(final TVDevice device) {
    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {
        Log.i(TAG, "try to connect to device: " + device.name);
        long channelId = System.currentTimeMillis();
        device.localChannelId = channelId;
        if (null != connectingDevice) {
          connectingDevice.state = TVDevice.State.SCANNED;
          fireStateUpate(connectingDevice);
        }
        synchronized (TVConn.class) {
          connectingDevice = device;
          connectingDevice.state = TVDevice.State.CONNECTING;
        }
        fireStateUpate(connectingDevice);
        try {
          DatagramPacket req = createRequestSessionPacket(device);
          udpSocket.send(req);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    t.start();
  }

  private void handleTask(DatagramPacket packet) {
    try {
      String data = new String(packet.getData());
      JSONObject obj = new JSONObject(data);
      if (!obj.has("type")) {
        Log.e(TAG, "no type found at message: " + data);
        return;
      }
      if ("requestSession:Answer".equals(obj.get("type"))) {
        if (null == connectingDevice) {
          Log.e(TAG, "no connecting device, it may be removed from list");
          return;
        }
        if (!packet.getAddress().equals(connectingDevice.remoteAddress)) {
          Log.e(TAG, "the replied answer address is different than connecting" +
                  "device.");
          return;
        }
        Log.i(TAG, "answer got from device: " + connectingDevice.name);
        synchronized (TVConn.class) {
          connectedDevice = connectingDevice;
          connectedDevice.state = TVDevice.State.CONNECTED;
          fireStateUpate(connectedDevice);
          connectedDevice.remoteChannelId = obj.getLong("answer");
          connectingDevice = null;
        }
        flashQueue();
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  private void cleanDeviceList() {
    boolean someoneRemoved = false;
    TVDevice[] devices = deviceList.toArray(new TVDevice[0]);
    for (TVDevice d : devices) {
      if (System.currentTimeMillis() - d.lastPinged > DEVICE_TTL) {
        synchronized (TVConn.class) {
          if (d.equals(connectingDevice)) {
            connectingDevice.state = TVDevice.State.SCANNED;
            connectingDevice = null;
          } else if (d.equals(connectedDevice)) {
            connectedDevice.state = TVDevice.State.SCANNED;
            connectedDevice = null;
          }
        }
        deviceList.remove(d);
        someoneRemoved = true;
        Log.i(TAG, "Device " + d.name + " removed from scanned list: " +
                d.lastPinged);
      }
    }
    if (someoneRemoved) {
      fireListUpate(deviceList.toArray(new TVDevice[0]));
    }
  }

  private void handleDeviceList(DatagramPacket packet) throws JSONException {
    String data = new String(packet.getData());
    JSONObject obj = new JSONObject(data);
    if (!obj.has("device")) {
      Log.e(TAG, "no device found at message: " + data);
      return;
    }
    TVDevice tv = new TVDevice();
    tv.name = obj.getString("device");
    tv.remoteAddress = packet.getAddress();
    tv.remotePort = obj.getJSONObject("services").getJSONObject("presentation").getInt("port");
    int idx = deviceList.indexOf(tv);
    if (idx > -1) {
      // update last pinged if this device is in list.
      deviceList.get(idx).lastPinged = System.currentTimeMillis();
      // Log.d(TAG, "Device: " + tv.name + " TTL updated: " +
      // System.currentTimeMillis());
    } else {
      tv.lastPinged = System.currentTimeMillis();
      deviceList.add(tv);
      fireListUpate(deviceList.toArray(new TVDevice[0]));
      Log.i(TAG, "Device: " + tv.name + " scanned.");
    }
  }

  public TVDevice[] getScannedDevices() {
    return deviceList.toArray(new TVDevice[0]);
  }

  private void startBroadcasting() {
    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {
        MulticastSocket scanSocket = null;
        try {
          InetAddress groupAddr = InetAddress.getByAddress(ADDRESS);
          scanSocket = new MulticastSocket(SCAN_PORT);
          scanSocket.setSoTimeout(1000);
          scanSocket.joinGroup(groupAddr);

          String data = "{\"device\": \"" + Build.MODEL + "\"," +
                  "\"services\":{" +
                  "\"presentation\":{" +
                  "\"port\": " + udpPort +
                  "}}}";
          byte[] dataBytes = data.getBytes();
          DatagramPacket scanPacket = new DatagramPacket(dataBytes,
                  dataBytes.length, groupAddr, SCAN_PORT);
          Log.i(TAG, "broadcasting start");
          while (selfBroadcasting) {
            try {
              scanSocket.send(scanPacket);
            } catch (SocketTimeoutException ex) {
            }
            cleanDeviceList();
            try {
              Thread.sleep(BROADCAST_INTERVAL);
            } catch (InterruptedException e) {
            }
          }
        } catch (SocketException e) {
          e.printStackTrace();
        } catch (UnknownHostException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (null != scanSocket) {
            scanSocket.close();
            scanSocket = null;
          }
        }
      }
    });
    t.start();
  }

  private void startReceiving() {

    Thread t = new Thread(new Runnable() {

      @Override
      public void run() {
        MulticastSocket updateSocket = null;
        try {
          InetAddress groupAddr = InetAddress.getByAddress(ADDRESS);
          updateSocket = new MulticastSocket(UPDATE_PORT);
          updateSocket.setSoTimeout(250);
          updateSocket.joinGroup(groupAddr);
          byte[] buffer = new byte[1024];
          DatagramPacket devicePacket = new DatagramPacket(buffer, buffer.length);
          while (selfBroadcasting) {
            try {
              updateSocket.receive(devicePacket);
              handleDeviceList(devicePacket);
            } catch (SocketTimeoutException ex) {
              // we don't care about timeout exception.
            } catch (JSONException e) {
              e.printStackTrace();
              Log.e(TAG, "error while parsing packet", e);
            }
          }
        } catch (SocketException e) {
          e.printStackTrace();
        } catch (UnknownHostException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        } finally {
          if (null != updateSocket) {
            updateSocket.close();
            updateSocket = null;
          }
        }
      }
    });
    t.start();
  }
}
