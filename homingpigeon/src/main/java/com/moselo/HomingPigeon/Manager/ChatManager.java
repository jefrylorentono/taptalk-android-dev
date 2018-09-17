package com.moselo.HomingPigeon.Manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.moselo.HomingPigeon.Data.Message.MessageEntity;
import com.moselo.HomingPigeon.Helper.DefaultConstant;
import com.moselo.HomingPigeon.Helper.HomingPigeon;
import com.moselo.HomingPigeon.Helper.Utils;
import com.moselo.HomingPigeon.Listener.HomingPigeonChatListener;
import com.moselo.HomingPigeon.Listener.HomingPigeonSocketListener;
import com.moselo.HomingPigeon.Model.EmitModel;
import com.moselo.HomingPigeon.Model.MessageModel;
import com.moselo.HomingPigeon.Model.RoomModel;
import com.moselo.HomingPigeon.Model.UserModel;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kEventOpenRoom;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketAuthentication;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketCloseRoom;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketDeleteMessage;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketNewMessage;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketOpenMessage;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketStartTyping;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketStopTyping;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketUpdateMessage;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketUserOffline;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.ConnectionEvent.kSocketUserOnline;
import static com.moselo.HomingPigeon.Helper.DefaultConstant.K_USER;

public class ChatManager {

    private final String TAG = ChatManager.class.getSimpleName();
    private static ChatManager instance;
    private List<HomingPigeonChatListener> chatListeners;
    private Map<String, MessageModel> pendingMessages, waitingResponses, incomingMessages;
    private Map<String, String> messageDrafts;
    private String activeRoom;
    private UserModel activeUser;
    private boolean isCheckPendingArraySequenceActive = false;
    private boolean isPendingMessageExist = false;
    private boolean isFileUploadExist = false;
    private final Integer CHARACTER_LIMIT = 1000;
    private int pendingRetryAttempt = 0;
    private int maxRetryAttempt = 10;
    private int pendingRetryInterval = 60 * 1000;
    private ScheduledExecutorService scheduler;

    private HomingPigeonSocketListener socketListener = new HomingPigeonSocketListener() {
        @Override
        public void onSocketConnected() {
            Log.e(TAG, "onSocketConnected: ");
            checkAndSendPendingMessages();
        }

        @Override
        public void onSocketDisconnected() {
            if (HomingPigeon.isForeground && NetworkStateManager.getInstance().hasNetworkConnection(HomingPigeon.appContext)
                    && ConnectionManager.ConnectionStatus.DISCONNECTED == ConnectionManager.getInstance().getConnectionStatus())
                ConnectionManager.getInstance().reconnect();
        }

        @Override
        public void onSocketConnecting() {

        }

        @Override
        public void onSocketError() {
            ConnectionManager.getInstance().reconnect();
        }

        @Override
        public void onReceiveNewEmit(String eventName, String emitData) {
            switch (eventName) {
                case kEventOpenRoom:
                    break;
                case kSocketCloseRoom:
                    break;
                case kSocketNewMessage:
                    EmitModel<MessageModel> messageEmit = Utils.getInstance()
                            .fromJSON(new TypeReference<EmitModel<MessageModel>>() {
                            }, emitData);
                    try {
                        Log.e(TAG, "onReceiveNewEmit: "+Utils.getInstance().toJsonString(MessageModel.BuilderDecrypt(messageEmit.getData())) );
                        receiveMessageFromSocket(MessageModel.BuilderDecrypt(messageEmit.getData()), eventName);
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    }
                    break;
                case kSocketUpdateMessage:
                    EmitModel<MessageModel> messageUpdateEmit = Utils.getInstance()
                            .fromJSON(new TypeReference<EmitModel<MessageModel>>() {
                            }, emitData);
                    try {
                        receiveMessageFromSocket(MessageModel.BuilderDecrypt(messageUpdateEmit.getData()), eventName);
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    }
                    break;
                case kSocketDeleteMessage:
                    EmitModel<MessageModel> messageDeleteEmit = Utils.getInstance()
                            .fromJSON(new TypeReference<EmitModel<MessageModel>>() {
                            }, emitData);
                    try {
                        receiveMessageFromSocket(MessageModel.BuilderDecrypt(messageDeleteEmit.getData()), eventName);
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                    }
                    break;
                case kSocketOpenMessage:
                    break;
                case kSocketStartTyping:
                    break;
                case kSocketStopTyping:
                    break;
                case kSocketAuthentication:
                    break;
                case kSocketUserOnline:
                    break;
                case kSocketUserOffline:
                    break;
            }
        }
    };

    public static ChatManager getInstance() {
        return instance == null ? (instance = new ChatManager()) : instance;
    }

    public ChatManager() {
        ConnectionManager.getInstance().addSocketListener(socketListener);
        setActiveUser(Utils.getInstance().fromJSON(new TypeReference<UserModel>() {
                                                   },
                PreferenceManager.getDefaultSharedPreferences(HomingPigeon.appContext)
                        .getString(K_USER, null)));
        chatListeners = new ArrayList<>();
        pendingMessages = new LinkedHashMap<>();
        waitingResponses = new LinkedHashMap<>();
        incomingMessages = new LinkedHashMap<>();
        messageDrafts = new HashMap<>();
    }

    public void addChatListener(HomingPigeonChatListener chatListener) {
        chatListeners.add(chatListener);
    }

    public void removeChatListener(HomingPigeonChatListener chatListener) {
        chatListeners.remove(chatListener);
    }

    public void removeChatListenerAt(int index) {
        chatListeners.remove(index);
    }

    public void clearChatListener() {
        chatListeners.clear();
    }

    public String getActiveRoom() {
        return activeRoom;
    }

    public void setActiveRoom(String roomId) {
        this.activeRoom = roomId;
    }

    public UserModel getActiveUser() {
        return activeUser;
    }

    public void setActiveUser(UserModel user) {
        this.activeUser = user;
    }

    public void saveActiveUser(Context context, UserModel user) {
        this.activeUser = user;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(K_USER, Utils.getInstance().toJsonString(user)).apply();
    }

    public Map<String, MessageModel> getMessageQueueInActiveRoom() {
        Map<String, MessageModel> roomQueue = new LinkedHashMap<>();
        for (Map.Entry<String, MessageModel> entry : pendingMessages.entrySet()) {
            if (entry.getValue().getRoom().getRoomID().equals(activeRoom)) {
                roomQueue.put(entry.getKey(), entry.getValue());
            }
        }
        return roomQueue;
    }

    /**
     * generate room ID
     */
    public String arrangeRoomId(String userId, String friendId) {
        int myId = (null != userId && !"null".equals(userId)) ? Integer.parseInt(userId) : 0;
        int fId = (null != friendId && !"null".equals(friendId)) ? Integer.parseInt(friendId) : 0;
        return myId < fId ? myId + "-" + fId : fId + "-" + myId;
    }

    /**
     * convert MessageEntity to MessageModel
     */
    public MessageModel convertToModel(MessageEntity entity) {
        return new MessageModel(
                entity.getMessageID(),
                entity.getLocalID(),
                entity.getMessage(),
                new RoomModel(entity.getRoomID(), entity.getRoomType()),
                entity.getType(),
                entity.getCreated(),
                Utils.getInstance().fromJSON(new TypeReference<UserModel>() {
                }, entity.getUser()),
                entity.getRecipientID(),
                entity.getDeleted(),
                entity.getIsSending(),
                entity.getIsFailedSend());
    }

    /**
     * convert MessageModel to MessageEntity
     */
    public MessageEntity convertToEntity(MessageModel model) {
        return new MessageEntity(
                model.getMessageID(),
                model.getLocalID(),
                model.getRoom().getRoomID(),
                model.getRoom().getRoomType(),
                model.getType(),
                model.getMessage(),
                model.getCreated(),
                Utils.getInstance().toJsonString(model.getUser()),
                model.getRecipientID(),
                Utils.getInstance().toJsonString(model.getDeliveredTo()),
                Utils.getInstance().toJsonString(model.getSeenBy()),
                model.getIsDeleted(),
                model.getIsSending(),
                model.getIsFailedSend(),
                model.getUpdated());
    }

    /**
     * sending text messages
     */
    public void sendTextMessage(String textMessage) {
        Integer startIndex;
        if (textMessage.length() > CHARACTER_LIMIT) {
            // Message exceeds character limit
            List<MessageEntity> messageEntities = new ArrayList<>();
            Integer length = textMessage.length();
            for (startIndex = 0; startIndex < length; startIndex += CHARACTER_LIMIT) {
                String substr = Utils.getInstance().mySubString(textMessage, startIndex, CHARACTER_LIMIT);
                MessageModel messageModel = buildTextMessage(substr);

                // Add entity to list
                messageEntities.add(ChatManager.getInstance().convertToEntity(messageModel));

                // Send truncated message
                sendMessage(messageModel);
            }
            // Insert list to database
//            DataManager.getInstance().insertToDatabase(messageEntities);
        } else {
            MessageModel messageModel = buildTextMessage(textMessage);

            // Insert new message to database
//            DataManager.getInstance().insertToDatabase(ChatManager.getInstance().convertToEntity(messageModel));

            // Send message
            sendMessage(messageModel);
        }
        // Run queue after list is updated
        //checkAndSendPendingMessages();
    }

    private MessageModel buildTextMessage(String message) {
        // Create new MessageModel based on text
        MessageModel messageModel = MessageModel.Builder(
                message,
                new RoomModel(activeRoom, 1),
                DefaultConstant.MessageType.TYPE_TEXT,
                System.currentTimeMillis(),
                activeUser, DataManager.getInstance().getRecipientID(HomingPigeon.appContext));

        // Add encrypted message to queue
        //try {
        //    JSONObject messageObject = new JSONObject();
        //    messageObject.put(MESSAGE, MessageModel.BuilderEncrypt(messageModel));
        //    messageObject.put(DefaultConstant.MessageQueue.NUM_OF_ATTEMPT, 0);
        //    pendingMessages.put(messageModel.getLocalID(), messageObject);
        //} catch (GeneralSecurityException | JSONException e) {
        //    e.printStackTrace();
        //}

        return messageModel;
    }

    /**
     * save text to draft
     */
    public void saveMessageToDraft(String message) {
        messageDrafts.put(activeRoom, message);
    }

    public String getMessageFromDraft() {
        return messageDrafts.get(activeRoom);
    }

    /**
     * send pending messages from queue
     */
    public void checkAndSendPendingMessages() {
        if (!pendingMessages.isEmpty()) {
            MessageModel message = pendingMessages.entrySet().iterator().next().getValue();
            runSendMessageSequence(message);
            pendingMessages.remove(message.getLocalID());
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    checkAndSendPendingMessages();
                }
            }, 50);
        }
    }

    /**
     * sending Message to server
     */
    private void sendMessage(MessageModel messageModel) {
        // Call listener
        if (null != chatListeners && !chatListeners.isEmpty()) {
            for (HomingPigeonChatListener chatListener : chatListeners)
                chatListener.onSendTextMessage(messageModel);
        }

        runSendMessageSequence(messageModel);
    }

    private void runSendMessageSequence(MessageModel messageModel) {
        if (ConnectionManager.getInstance().getConnectionStatus() == ConnectionManager.ConnectionStatus.CONNECTED) {
            waitingResponses.put(messageModel.getLocalID(), messageModel);

            // Send message if socket is connected
            try {
                sendEmit(kSocketNewMessage, MessageModel.BuilderEncrypt(messageModel));
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        } else {
            // Add message to queue if socket is not connected
            pendingMessages.put(messageModel.getLocalID(), messageModel);
        }
    }

    /**
     * Sending Emit to Server
     */
    private void sendEmit(String eventName, MessageModel messageModel) {
        EmitModel<MessageModel> emitModel;
        emitModel = new EmitModel<>(eventName, messageModel);
        ConnectionManager.getInstance().send(Utils.getInstance().toJsonString(emitModel));
    }

    /**
     * update pending status when app enters background and close socket
     */
    public void updateMessageWhenEnterBackground() {
        //saveWaitingMessageToDatabase();
        setPendingRetryAttempt(0);
        isCheckPendingArraySequenceActive = false;
        if (null != scheduler && !scheduler.isShutdown())
            scheduler.shutdown();
        saveUnsentMessage();
        checkPendingBackgroundTask();
    }

    public void insertToDatabase(Map<String, MessageModel> hashMap) {
        List<MessageEntity> messages = new ArrayList<>();
        for (Map.Entry<String, MessageModel> message : hashMap.entrySet()) {
            messages.add(convertToEntity(message.getValue()));
        }
        DataManager.getInstance().insertToDatabase(messages);
    }

    private void checkPendingBackgroundTask() {
        // TODO: 05/09/18 nnti cek file manager upload queue juga
        isPendingMessageExist = false;
        isFileUploadExist = false;

        if (0 < pendingMessages.size())
            isPendingMessageExist = true;

        if (isCheckPendingArraySequenceActive)
            return;

        // TODO: 13/09/18 check file progress upload

        if ((isPendingMessageExist || isFileUploadExist) && maxRetryAttempt > pendingRetryAttempt) {
            Log.e(TAG, "checkPendingBackgroundTask: "+pendingRetryAttempt );
            isCheckPendingArraySequenceActive = true;
            pendingRetryAttempt++;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.e(TAG, "run: " );
                    checkPendingBackgroundTask();
                }
            }, pendingRetryInterval);
            isCheckPendingArraySequenceActive = false;
        } else saveIncomingMessageAndDisconnect();
    }

    public void saveIncomingMessageAndDisconnect() {
        saveUnsentMessage();
        if (null != scheduler && !scheduler.isShutdown())
            scheduler.shutdown();
        ConnectionManager.getInstance().close();
    }

    public void deleteActiveRoom() {
        activeRoom = "";
    }

    /**
     * this is when receive new message from socket
     *
     * @param newMessage
     */
    private void receiveMessageFromSocket(MessageModel newMessage, String eventName) {
        // TODO: 4 September 2018 TEMP
        newMessage.setIsSending(0);

        // Remove from waiting response hashmap
        if (kSocketNewMessage.equals(eventName))
            waitingResponses.remove(newMessage.getLocalID());

        // Insert decrypted message to database
        incomingMessages.put(newMessage.getLocalID(), newMessage);

        // Receive message in active room
        if (null != chatListeners && !chatListeners.isEmpty() && newMessage.getRoom().getRoomID().equals(activeRoom)) {
            for (HomingPigeonChatListener chatListener : chatListeners) {
                if (kSocketNewMessage.equals(eventName))
                    chatListener.onReceiveMessageInActiveRoom(newMessage);
                else if (kSocketUpdateMessage.equals(eventName))
                    chatListener.onUpdateMessageInActiveRoom(newMessage);
                else if (kSocketDeleteMessage.equals(eventName))
                    chatListener.onDeleteMessageInActiveRoom(newMessage);
            }
        }
        // Receive message outside active room
        else if (null != chatListeners && !chatListeners.isEmpty() && !newMessage.getRoom().getRoomID().equals(activeRoom)) {
            for (HomingPigeonChatListener chatListener : chatListeners) {
                if (kSocketNewMessage.equals(eventName))
                    chatListener.onReceiveMessageInOtherRoom(newMessage);
                else if (kSocketUpdateMessage.equals(eventName))
                    chatListener.onUpdateMessageInOtherRoom(newMessage);
                else if (kSocketDeleteMessage.equals(eventName))
                    chatListener.onDeleteMessageInOtherRoom(newMessage);
            }
        }
    }

    public void saveNewMessageToDatabase() {
        if (0 == incomingMessages.size())
            return;

        insertToDatabase(incomingMessages);
        incomingMessages.clear();
    }

    public void savePendingMessageToDatabase() {
        if (0 < pendingMessages.size()) {
            insertToDatabase(pendingMessages);
            //pendingMessages.clear();
        }
    }

    public void saveWaitingMessageToDatabase() {
        if (0 == waitingResponses.size())
            return;

        insertToDatabase(waitingResponses);
        waitingResponses.clear();
    }

    public void triggerSaveNewMessage() {
        if (null == scheduler || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        } else {
            scheduler.shutdown();
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                saveNewMessageToDatabase();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void saveUnsentMessage() {
        new Thread(() -> saveNewMessageToDatabase()).start();

        new Thread(() -> savePendingMessageToDatabase()).start();

        new Thread(() -> saveWaitingMessageToDatabase()).start();
    }

    public void setPendingRetryAttempt(int counter) {
        pendingRetryAttempt = counter;
    }
}
