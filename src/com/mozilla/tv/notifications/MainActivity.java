package com.mozilla.tv.notifications;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import com.mozilla.tv.notifications.tv.TVConn;
import com.mozilla.tv.notifications.tv.TVConnService;
import com.mozilla.tv.notifications.tv.TVDevice;
import com.mozilla.tv.notifications.tv.TVDeviceAdapter;
import com.mozilla.tv.notifications.utils.IntentUtils;

public class MainActivity extends Activity {

  private TextView deviceListTitle;
  private ListView deviceList;
  private CheckBox autoConnect;
  private Button connectButton;
  private boolean tvServiceRunning;
  private TVDeviceAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    tvServiceRunning = TVConnService.hasServiceRunning;

    initView();
    initListeners();
    updateDeviceListState();
    udpateButtonState();
    TVConn.get().setAutoConnect(autoConnect.isChecked());
  }

  private void initView() {
    deviceList = (ListView) this.findViewById(R.id.device_list);
    autoConnect = (CheckBox) this.findViewById(R.id.auto_first);
    connectButton = (Button) this.findViewById(R.id.connect_button);
    deviceListTitle = (TextView) this.findViewById(R.id.device_list_title);
    initAdapter();
  }

  private void initAdapter() {
    adapter = new TVDeviceAdapter(this);
    deviceList.setAdapter(adapter);
  }

  private void initListeners() {
    this.findViewById(R.id.start_button).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        IntentUtils.startTVConnService(MainActivity.this);
        tvServiceRunning = true;
        updateDeviceListState();
        udpateButtonState();
      }
    });
    this.findViewById(R.id.stop_button).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View arg0) {
        IntentUtils.stopTVConnService(MainActivity.this);
        tvServiceRunning = false;
        adapter.setSelectedDevice(null);
        updateDeviceListState();
        udpateButtonState();
      }
    });
    connectButton.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View arg0) {
        TVDevice device = adapter.getSelectedDevice();
        if (device == null) {
          return;
        }

        if (device.equals(TVConn.get().getConnectedDevice())) {
          TVConn.get().disconnect(IntentUtils.REMOTE_URL);
        } else if (TVConn.get().isConnected()) {
          TVConn.get().disconnect(IntentUtils.REMOTE_URL);
          TVConn.get().connectToDevice(device, IntentUtils.REMOTE_URL);
        } else {
          TVConn.get().connectToDevice(device, IntentUtils.REMOTE_URL);
        }
      }
    });
    autoConnect.setOnCheckedChangeListener(new OnCheckedChangeListener() {

      @Override
      public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
        TVConn.get().setAutoConnect(arg1);
        updateDeviceListState();
      }
    });
    deviceList.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        adapter.setSelectedDevice((TVDevice) adapter.getItem(arg2));
      }
    });
  }

  private void updateDeviceListState() {
    boolean enabled = tvServiceRunning && !autoConnect.isChecked();
    deviceList.setEnabled(enabled);
    deviceListTitle.setEnabled(enabled);
    adapter.setEnabled(enabled);
    connectButton.setEnabled(tvServiceRunning && !autoConnect.isChecked());
  }

  private void udpateButtonState() {
    this.findViewById(R.id.start_button).setEnabled(!tvServiceRunning);
    this.findViewById(R.id.stop_button).setEnabled(tvServiceRunning);
    autoConnect.setEnabled(tvServiceRunning);
  }

}
