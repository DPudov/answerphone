package com.dpudov.answerphone.fragments;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;

import com.dpudov.answerphone.R;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKApiGetMessagesResponse;
import com.vk.sdk.api.model.VKApiMessage;
import com.vk.sdk.api.model.VKList;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class MessagesService extends Service {
    NotificationManager nM;
    private int NOTIFICATION = R.string.serviceStarted;
    private int[] checkedUsers;
    private int[] userId;
    private int[] userIdCopy;
    private int[] userIdReturn;
    String message;

    public MessagesService() {
    }

    @Override
    public void onCreate() {
        nM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();
    }

    @Override
    public void onDestroy() {
        nM.cancel(NOTIFICATION);
    }

    private void showNotification() {
        CharSequence text = getString(R.string.serviceStarted);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_answerphone_64px)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(text)
                .build();
        nM.notify(NOTIFICATION, notification);
    }

    private void showNotificationNew(int i) {
        CharSequence text = Integer.toString(i);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_answerphone_64px)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(text)
                .build();
        nM.notify(NOTIFICATION, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        checkedUsers = bundle.getIntArray("userIds");

        //try {
        //   getAndSendMessages();
        // } catch (InterruptedException e) {
        //    e.printStackTrace();
        //    Toast.makeText(getApplicationContext(), "Произошла ошибка. Попробуйте позже", Toast.LENGTH_SHORT).show();
        // }


        return START_NOT_STICKY;
    }

    void getAndSendMessages() throws InterruptedException {
        //Запускаем поток, который проверяет новые сообщения. Если прилетает новое, читаем id отправителя. Затем шлём ему ответ.
        //Thread thread = new Thread(new Runnable() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    userId = getMsg();
                    sendTo(userId);
                    Thread.sleep(1800000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        //thread.start();
    }

    private int[] getMsg() {
        //TODO Ошибка тут. Исправляй
        final VKRequest getMsg = VKApi.messages().get(VKParameters.from());
        getMsg.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                VKApiGetMessagesResponse getMessagesResponse = (VKApiGetMessagesResponse) response.parsedModel;
                VKList<VKApiMessage> list = getMessagesResponse.items;
                // Формируем лист с id авторов сообщений без повторений
                LinkedHashSet<Integer> authors = new LinkedHashSet<>();
                for (VKApiMessage msg : list) {
                    authors.add(msg.user_id);
                }
                // конвертируем в массив
                userId = new int[authors.size()];
                userIdCopy = new int[checkedUsers.length];
                Iterator<Integer> iterator = authors.iterator();
                for (int i = 0; i < authors.size(); i++) {
                    userId[i] = iterator.next();

                }
                //сравниваем с выбранными друзьями
                int c = 0;
                for (int i = 0; i < userId.length; i++) {
                    for (int j = 0; i < userId.length; i++) {
                        if (userId[i] == checkedUsers[j]) {
                            userIdCopy[c] = userId[i];
                            c++;
                        }
                    }
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
            }
        });
        return userIdReturn;
    }

    public static boolean hasConnection(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifiInfo != null && wifiInfo.isConnected()) {
            return true;
        }
        wifiInfo = cm.getActiveNetworkInfo();
        return wifiInfo != null && wifiInfo.isConnected();
    }

    public void send(int userId) {
//метод для отправки сообщения user.
        message = getString(R.string.user_is_busy) + getString(R.string.defaultMsg);
        if (!(userId == 0)) {
            VKRequest requestSend = new VKRequest("messages.send", VKParameters.from(VKApiConst.USER_ID, userId, VKApiConst.MESSAGE, message));
            requestSend.executeWithListener(new VKRequest.VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                }

                @Override
                public void onError(VKError error) {
                    super.onError(error);
                }
            });
        }
    }

    public void sendTo(int[] userIds) {
        //метод для отправки сообщений нескольким юзерам
        for (int userId : userIds) {
            send(userId);
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}