package com.mozilla.tv.notifications.tv;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.mozilla.tv.notifications.R;

public class TVDeviceAdapter extends BaseAdapter implements TVConn.Listener {

  private static LayoutInflater inflater;
  private TVDevice[] devices;
  private Activity context;
  private TVDevice selectedDevice;
  private boolean enabled;

  public TVDeviceAdapter(Activity ctx) {
    context = ctx;
    TVConn.get().addEventListener(this);
    devices = TVConn.get().getScannedDevices();
  }

  public void setSelectedDevice(TVDevice d) {
    selectedDevice = d;
    this.notifyDataSetChanged();
  }

  public TVDevice getSelectedDevice() {
    return selectedDevice;
  }

  public void setEnabled(boolean enabled) {
    if (this.enabled != enabled) {
      this.enabled = enabled;
      this.notifyDataSetChanged();
    }
  }

  @Override
  public int getCount() {
    return null == devices ? 0 : devices.length;
  }

  @Override
  public Object getItem(int arg0) {
    if (null == devices) {
      return null;
    } else if (arg0 < 0 || arg0 >= devices.length) {
      return null;
    } else {
      return devices[arg0];
    }
  }

  @Override
  public long getItemId(int arg0) {
    return arg0;
  }

  @Override
  public View getView(int arg0, View arg1, ViewGroup arg2) {
    View view = arg1;
    if (null == view) {
      if (null == inflater) {
        inflater = LayoutInflater.from(context);
      }
      view = inflater.inflate(R.layout.device_item, arg2, false);
    }

    TVDevice device = devices[arg0];
    ((TextView) view.findViewById(R.id.device_list_title)).setText(device.name);
    ((TextView) view.findViewById(R.id.device_status)).setText(device.state.toString());

    view.findViewById(R.id.device_list_title).setEnabled(enabled);
    view.findViewById(R.id.device_status).setEnabled(enabled);

    view.setBackgroundResource(device.equals(selectedDevice) ?
            R.drawable.listview_selected : 0);

    return view;
  }

  @Override
  public void deviceStateUpdate(TVDevice device) {
    context.runOnUiThread(new Runnable() {

      @Override
      public void run() {
        notifyDataSetChanged();
      }
    });
  }

  @Override
  public void deviceListUpdate(TVDevice[] devices) {
    this.devices = devices;
    context.runOnUiThread(new Runnable() {

      @Override
      public void run() {
        notifyDataSetChanged();
      }
    });
  }

}
