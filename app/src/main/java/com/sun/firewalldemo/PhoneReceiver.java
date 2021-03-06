package com.sun.firewalldemo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;
import com.sun.firewalldemo.blacklist.BlackListBean;
import com.sun.firewalldemo.blacklist.BlackListDBTable;
import com.sun.firewalldemo.blacklist.BlackListDao;
import com.sun.firewalldemo.phonelog.PhoneLogDao;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by S on 2016/5/16.
 */
public class PhoneReceiver extends BroadcastReceiver {
    private PhoneStateListener listener;

    @Override
    public void onReceive(final Context context, Intent intent) {
        final BlackListDao dao = new BlackListDao(context);
        final PhoneLogDao phoneLogDao = new PhoneLogDao(context);

        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            //去电电话
            System.out.println("去电电话");
        } else {
            //来电电话
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            //设置监听器
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

            /*String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                //state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING))
                System.out.println("来电电话");

            }*/
        }

        listener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);
                switch (state) {
                    //手机空闲
                    case TelephonyManager.CALL_STATE_IDLE:
                        System.out.println("TelephonyManager.CALL_STATE_IDLE:");
                        break;
                    //电话被挂起
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        System.out.println("TelephonyManager.CALL_STATE_OFFHOOK");
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        System.out.println("TelephonyManager.CALL_STATE_RINGING " + incomingNumber);
                        /*if (incomingNumber.equals("13117875310")) {
                            System.out.println("incomingNumber.equals(\"13117875310\") " + incomingNumber);
                            stopCall();
                            return;
                        }*/

                        int mode = dao.getMode(incomingNumber);
                        if ((mode == BlackListDBTable.ALL) || (mode == BlackListDBTable.TEL)) {
                            stopCall();
                            List<BlackListBean> blackListBeen = dao.getAllDatas();
                            BlackListBean bean = new BlackListBean();

                            for (int i = 0; i < blackListBeen.size(); i++) {
                                if (bean.equals(blackListBeen.get(i))) {
                                    String name = bean.getName();
                                    Date date = new Date();
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm");
                                    String time = dateFormat.format(date);
                                    System.out.println("name " + name + " phone " + incomingNumber + " time " + time);
                                    phoneLogDao.add(name, incomingNumber, time);
                                    return;
                                }
                            }
                        }
                        break;

                }
            }

            private void stopCall() {
                try {
                    Class clazz = Class.forName("android.os.ServiceManager");
                    Method method = clazz.getDeclaredMethod("getService", String.class);
                    IBinder binder = (IBinder) method.invoke(null, Context.TELEPHONY_SERVICE);
                    ITelephony telephony = ITelephony.Stub.asInterface(binder);
                    telephony.endCall();

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        };
    }
}
