package com.walixiwa.dlnaserver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;


import com.walixiwa.dlnaserver.binder.DlnaItemBinder;
import com.walixiwa.dlnaserver.service.MyUpnpService;
import com.walixiwa.dlnaserver.tools.DlnaCtrlUtil;
import com.walixiwa.model.DeviceModel;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;
import org.teleal.cling.support.avtransport.callback.Play;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import me.drakeet.multitype.MultiTypeAdapter;


public class DlnaDialog extends AlertDialog.Builder {

    private AndroidUpnpService upnpService;
    private RegistryListener registryListener;
    private ServiceConnection serviceConnection;
    private MultiTypeAdapter adapter = new MultiTypeAdapter();

    private Context context;
    private String url;

    private List<Object> items = new ArrayList<>();

    private LinearLayout ll_find;
    private ProgressBar loading;
    private TextView tv_info;
    private Handler handler = new Handler(Looper.getMainLooper());

    public DlnaDialog(@NonNull final Context context) {
        super(context, R.style.MyAlertDialogStyle);
        this.context = context;
        View view = View.inflate(context, R.layout.dlna_server_dialog, null);
        this.setView(view);

        DlnaItemBinder binder = new DlnaItemBinder();
        adapter.register(DeviceModel.class, binder);
        adapter.setItems(items);
        RecyclerView recyclerView = view.findViewById(R.id.rv_dlna);
        recyclerView.setAdapter(adapter);

        ll_find = view.findViewById(R.id.ll_find);
        loading = ll_find.findViewById(R.id.loading);
        tv_info = ll_find.findViewById(R.id.tv_info);

        binder.setOnItemClickListener(new DlnaItemBinder.OnItemClickListener() {
            @Override
            public void onItemClick(DeviceModel display) {
                //ll_find.setVisibility(View.VISIBLE);
                //tv_info.setText("正在投送到设备...");
                Toast.makeText(context, "正在投送到设备...", Toast.LENGTH_SHORT).show();
                DlnaCtrlUtil.startUrl(display, upnpService, url.replace("127.0.0.1", getLANAddress()), new DlnaCtrlUtil.OnPlayCallBack() {
                    @Override
                    public void onPlayCallBack(final boolean result) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //ll_find.setVisibility(View.GONE);
                                //tv_info.setText("已投送到设备...");
                                Toast.makeText(context, "已投送到设备", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });

        this.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                DlnaDialog.this.onDestroy();
            }
        });

        bindListener();
        context.bindService(new Intent(context, MyUpnpService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }



    public DlnaDialog setUrl(String url) {
        this.url = url;
        return this;
    }

    private void bindListener() {
        registryListener = new BrowseRegistryListener();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onBindingDied(ComponentName name) {
                Toast.makeText(context, "Dlna服务已被回收", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNullBinding(ComponentName name) {
                Toast.makeText(context, "Dlna服务绑定失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                upnpService = (AndroidUpnpService) service;
                for (Device device : upnpService.getRegistry().getDevices()) {
                    ((BrowseRegistryListener) registryListener).deviceAdded(device);
                }
                upnpService.getRegistry().addListener(registryListener);
                upnpService.getControlPoint().search();
            }

            public void onServiceDisconnected(ComponentName className) {
                upnpService = null;
            }

        };
    }

    private void onDestroy() {
        if (upnpService != null) {
            upnpService.getRegistry().removeListener(registryListener);
        }
        if (serviceConnection != null) {
            context.unbindService(serviceConnection);
        }
    }

    private class BrowseRegistryListener extends DefaultRegistryListener {

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, final RemoteDevice device, final Exception ex) {
            deviceRemoved(device);
        }

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            deviceRemoved(device);
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            deviceAdded(device);
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            deviceRemoved(device);
        }

        @Override
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
            super.remoteDeviceUpdated(registry, device);
        }

        void deviceAdded(final Device device) {

            handler.post(new Runnable() {
                @Override
                public void run() {
                    DeviceModel display = new DeviceModel(device);
                    if (items.contains(display)) {
                        items.remove(display);
                        items.add(display);
                    } else {
                        items.add(display);
                    }
                    adapter.notifyDataSetChanged();

                    //tv_info.setText("实时发现局域网设备...");
                    //ll_find.setVisibility(items.size() == 0 ? View.VISIBLE : View.GONE);
                }
            });
        }

        void deviceRemoved(final Device device) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    items.remove(new DeviceModel(device));
                }
            });
        }
    }


    /**
     * 获取本机局域网IP
     *
     * @return
     */
    public static String getLANAddress() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("kalshen", "SocketException");
            e.printStackTrace();
        }
        return hostIp;
    }

}
