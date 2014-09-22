package com.mozilla.tv.notifications.tv;

import java.net.InetAddress;

public class TVDevice {
  public enum State {
    SCANNED, CONNECTING, CONNECTED
  }

  public String name;
  public InetAddress remoteAddress;
  public int remotePort;
  public long lastPinged;
  public long localChannelId;
  public long remoteChannelId;
  public State state = State.SCANNED;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((remoteAddress == null) ? 0 : remoteAddress.hashCode());
    result = prime * result + remotePort;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TVDevice other = (TVDevice) obj;
    if (remoteAddress == null) {
      if (other.remoteAddress != null)
        return false;
    } else if (!remoteAddress.equals(other.remoteAddress))
      return false;
    if (remotePort != other.remotePort)
      return false;
    return true;
  }

}
