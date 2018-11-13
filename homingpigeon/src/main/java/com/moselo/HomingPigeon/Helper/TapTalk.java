package com.moselo.HomingPigeon.Helper;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.moselo.HomingPigeon.API.Api.TAPApiManager;
import com.moselo.HomingPigeon.API.View.TapDefaultDataView;
import com.moselo.HomingPigeon.BroadcastReceiver.TAPReplyBroadcastReceiver;
import com.moselo.HomingPigeon.BuildConfig;
import com.moselo.HomingPigeon.Interface.TapTalkTokenInterface;
import com.moselo.HomingPigeon.Manager.TAPChatManager;
import com.moselo.HomingPigeon.Manager.TAPConnectionManager;
import com.moselo.HomingPigeon.Manager.TAPDataManager;
import com.moselo.HomingPigeon.Manager.TAPNetworkStateManager;
import com.moselo.HomingPigeon.Manager.TAPNotificationManager;
import com.moselo.HomingPigeon.Manager.TAPOldDataManager;
import com.moselo.HomingPigeon.Model.HpMessageModel;
import com.moselo.HomingPigeon.Model.HpRoomModel;
import com.moselo.HomingPigeon.Model.ResponseModel.HpGetAccessTokenResponse;
import com.moselo.HomingPigeon.R;
import com.moselo.HomingPigeon.View.Activity.HpLoginActivity;
import com.moselo.HomingPigeon.View.Activity.HpRoomListActivity;
import com.moselo.HomingPigeon.ViewModel.HpRoomListViewModel;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.NoEncryption;

import static com.moselo.HomingPigeon.Const.TAPDefaultConstant.DatabaseType.MESSAGE_DB;
import static com.moselo.HomingPigeon.Const.TAPDefaultConstant.DatabaseType.MY_CONTACT_DB;
import static com.moselo.HomingPigeon.Const.TAPDefaultConstant.DatabaseType.SEARCH_DB;
import static com.moselo.HomingPigeon.Const.TAPDefaultConstant.HP_NOTIFICATION_CHANNEL;
import static com.moselo.HomingPigeon.Const.TAPDefaultConstant.K_ROOM;
import static com.moselo.HomingPigeon.Const.TAPDefaultConstant.Notification.K_REPLY_REQ_CODE;
import static com.moselo.HomingPigeon.Const.TAPDefaultConstant.Notification.K_TEXT_REPLY;

public class TapTalk {
    public static TapTalk tapTalk;
    public static boolean isForeground = true;
    private Thread.UncaughtExceptionHandler defaultUEH;
    private TapTalkTokenInterface hpTokenInterface;
    private static int clientAppIcon = R.drawable.hp_ic_launcher_background;
    private static String clientAppName = "";

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            TAPChatManager.getInstance().saveIncomingMessageAndDisconnect();
            defaultUEH.uncaughtException(thread, throwable);
        }
    };

    public static TapTalk init(Context context, TapTalkTokenInterface hpTokenInterface) {
        return tapTalk == null ? (tapTalk = new TapTalk(context, hpTokenInterface)) : tapTalk;
    }

    public TapTalk(final Context appContext, TapTalkTokenInterface hpTokenInterface) {
        //init Hawk for Preference
        //ini ngecek fungsinya kalau dev hawknya ga di encrypt sisanya hawknya di encrypt
        if (BuildConfig.BUILD_TYPE.equals("dev"))
            Hawk.init(appContext).setEncryption(new NoEncryption()).build();
        else Hawk.init(appContext).build();

        //ini buat bkin database bisa di akses (setiap tambah repo harus tambah ini)
        TAPDataManager.getInstance().initDatabaseManager(MESSAGE_DB, (Application) appContext);
        TAPDataManager.getInstance().initDatabaseManager(SEARCH_DB, (Application) appContext);
        TAPDataManager.getInstance().initDatabaseManager(MY_CONTACT_DB, (Application) appContext);
        //ini buat ambil context dr app utama karena library module ga bsa punya app context sndiri
        TapTalk.appContext = appContext;
        clientAppName = appContext.getResources().getString(R.string.app_name);

        if (TAPDataManager.getInstance().checkAccessTokenAvailable()) {
            TAPConnectionManager.getInstance().connect();
            Log.e("TAPOldDataManager", "TapTalk: ");
            TAPOldDataManager.getInstance().startAutoCleanProcess();
        }

        TAPDataManager.getInstance().updateSendingMessageToFailed();

        //init stetho tapi hanya untuk DEBUG State
        if (BuildConfig.DEBUG)
            Stetho.initialize(
                    Stetho.newInitializerBuilder(appContext)
                            .enableDumpapp(Stetho.defaultDumperPluginsProvider(appContext))
                            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(appContext))
                            .build()
            );

        this.hpTokenInterface = hpTokenInterface;

        AppVisibilityDetector.init((Application) appContext, new AppVisibilityDetector.AppVisibilityCallback() {
            @Override
            public void onAppGotoForeground() {
                TAPChatManager.getInstance().setFinishChatFlow(false);
                appContext.startService(new Intent(TapTalk.appContext, TapTalkEndAppService.class));
                TAPNetworkStateManager.getInstance().registerCallback(TapTalk.appContext);
                TAPChatManager.getInstance().triggerSaveNewMessage();
                defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
                isForeground = true;
            }

            @Override
            public void onAppGotoBackground() {
                HpRoomListViewModel.setShouldNotLoadFromAPI(false);
                TAPNetworkStateManager.getInstance().unregisterCallback(TapTalk.appContext);
                TAPChatManager.getInstance().updateMessageWhenEnterBackground();
                isForeground = false;
            }
        });
    }

    public static void saveAuthTicketAndGetAccessToken(String authTicket, TapDefaultDataView<HpGetAccessTokenResponse> view) {
        TAPDataManager.getInstance().saveAuthTicket(authTicket);
        TAPDataManager.getInstance().getAccessTokenFromApi(view);
    }

    public static void checkActiveUserToShowPage(Activity activity) {
        if (null != activity) {
            Intent intent;
            if (TAPDataManager.getInstance().checkAccessTokenAvailable()) {
                intent = new Intent(activity, HpRoomListActivity.class);
            } else {
                intent = new Intent(activity, HpLoginActivity.class);
            }
            activity.startActivity(intent);
            activity.finish();
        } else {
            throw new NullPointerException("The Activity that passed was null");
        }
    }

    // TODO: 15/10/18 saat integrasi harus di ilangin
    public static void refreshTokenExpired() {
        TAPApiManager.getInstance().setLogout(true);
        TAPChatManager.getInstance().disconnectAfterRefreshTokenExpired();
        TAPDataManager.getInstance().deleteAllPreference();
        TAPDataManager.getInstance().deleteAllFromDatabase();
        Intent intent = new Intent(appContext, HpLoginActivity.class);
        appContext.startActivity(intent);
    }

    public static void saveFirebaseToken(String newFirebaseToken) {
        if (!TAPDataManager.getInstance().checkFirebaseToken(newFirebaseToken)) {
            TAPDataManager.getInstance().saveFirebaseToken(newFirebaseToken);
        }
    }

    public static void saveAppInfo(int clientAppIcon, String clientAppName) {
        TapTalk.clientAppIcon = clientAppIcon;
        TapTalk.clientAppName = clientAppName;
    }

    public static int getClientAppIcon() {
        return clientAppIcon;
    }

    public static String getClientAppName() {
        return clientAppName;
    }

    //Builder buat setting isi dari Notification chat
    public static class NotificationBuilder {
        public Context context;
        public String chatSender = "", chatMessage = "";
        public HpMessageModel notificationMessage;
        public int smallIcon;
        boolean isNeedReply;
        HpRoomModel roomModel;
        NotificationCompat.Builder notificationBuilder;
        private Class aClass;

        public NotificationBuilder(Context context) {
            this.context = context;
        }

        public NotificationBuilder setNotificationMessage(HpMessageModel notificationMessage) {
            this.notificationMessage = notificationMessage;
            TAPNotificationManager.getInstance().addNotifMessageToMap(notificationMessage);
            setChatMessage(notificationMessage.getBody());
            setChatSender(notificationMessage.getUser().getName());
            return this;
        }

        private void setChatSender(String chatSender) {
            this.chatSender = chatSender;
        }

        private void setChatMessage(String chatMessage) {
            this.chatMessage = chatMessage;
        }

        public NotificationBuilder setSmallIcon(int smallIcon) {
            this.smallIcon = smallIcon;
            return this;
        }

        public NotificationBuilder setNeedReply(boolean needReply) {
            isNeedReply = needReply;
            return this;
        }

        public NotificationBuilder setOnClickAction(Class aClass) {
            this.roomModel = notificationMessage.getRoom();
            this.aClass = aClass;
            return this;
        }

        public Notification build() {
            this.notificationBuilder = TAPNotificationManager.getInstance().createNotificationBubble(this);
            addReply();
            if (null != roomModel && null != aClass) addPendingIntentWhenClicked();
            return this.notificationBuilder.build();
        }

        private void addReply() {
            if (isNeedReply && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                RemoteInput remoteInput = new RemoteInput.Builder(K_TEXT_REPLY)
                        .setLabel("Reply").build();
                Intent intent = new Intent(context, TAPReplyBroadcastReceiver.class);
                intent.setAction(K_TEXT_REPLY);
                PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context, K_REPLY_REQ_CODE,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Action action = new NotificationCompat.Action.Builder(smallIcon,
                        "Reply", replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setAllowGeneratedReplies(true)
                        .build();

                notificationBuilder.addAction(action);
            }
        }

        private void addPendingIntentWhenClicked() {
            Intent intent = new Intent(context, aClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(K_ROOM, roomModel);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_ONE_SHOT);
            notificationBuilder.setContentIntent(pendingIntent);
        }

        private void createNotificationChannel() {
            NotificationManager notificationManager = (NotificationManager) TapTalk.appContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && null == notificationManager.getNotificationChannel(HP_NOTIFICATION_CHANNEL)) {
                NotificationChannel notificationChannel = new NotificationChannel(HP_NOTIFICATION_CHANNEL, "Homing Pigeon Notifications", NotificationManager.IMPORTANCE_HIGH);

                // Configure the notification channel.
                notificationChannel.setDescription("TapTalk Notification");
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.parseColor("#2eccad"));
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        public void show() {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(TapTalk.appContext);
            createNotificationChannel();

            notificationManager.notify(notificationMessage.getRoom().getRoomID(), 0, build());

            if (1 < TAPNotificationManager.getInstance().getNotifMessagesMap().size()) {
                notificationManager.notify(0, TAPNotificationManager.getInstance().createSummaryNotificationBubble(context, aClass).build());
            }

        }
    }

    public static Context appContext;
}
