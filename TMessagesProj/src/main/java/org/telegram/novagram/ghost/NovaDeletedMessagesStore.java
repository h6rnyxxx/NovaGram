package org.telegram.novagram.ghost;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks messages deleted remotely by other participants while Anti-Delete is enabled,
 * so NovaGram preserves them locally and labels them in chat bubbles.
 */
public class NovaDeletedMessagesStore {

    private static final String PREFS_NAME = "nova_deleted_msgs";
    private static final String KEY_IDS = "deleted_keys";
    private static final int MAX_ENTRIES = 4000;

    private static volatile NovaDeletedMessagesStore instance;

    public static NovaDeletedMessagesStore getInstance() {
        if (instance == null) {
            synchronized (NovaDeletedMessagesStore.class) {
                if (instance == null) {
                    instance = new NovaDeletedMessagesStore();
                }
            }
        }
        return instance;
    }

    private final LinkedHashSet<String> deletedKeys = new LinkedHashSet<>();
    private boolean loaded = false;
    private Runnable saveRunnable;

    private void ensureLoaded() {
        if (loaded || ApplicationLoader.applicationContext == null) {
            return;
        }
        synchronized (deletedKeys) {
            if (loaded || ApplicationLoader.applicationContext == null) {
                return;
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> saved = prefs.getStringSet(KEY_IDS, null);
            if (saved != null) {
                deletedKeys.addAll(saved);
            }
            loaded = true;
        }
    }

    public void markDeleted(long dialogId, List<Integer> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) {
            return;
        }
        ensureLoaded();
        boolean changed = false;
        synchronized (deletedKeys) {
            for (int i = 0; i < msgIds.size(); i++) {
                Integer id = msgIds.get(i);
                if (id == null || id <= 0) continue;
                String key = makeKey(dialogId, id);
                if (deletedKeys.add(key)) {
                    changed = true;
                }
            }
            while (deletedKeys.size() > MAX_ENTRIES) {
                String first = deletedKeys.iterator().next();
                deletedKeys.remove(first);
            }
        }
        if (changed) {
            scheduleSave();
        }
    }

    public boolean isDeleted(MessageObject msg) {
        if (msg == null) return false;
        return isDeleted(msg.getDialogId(), msg.getId());
    }

    public boolean isDeleted(long dialogId, int msgId) {
        if (msgId <= 0) return false;
        ensureLoaded();
        synchronized (deletedKeys) {
            if (deletedKeys.contains(makeKey(dialogId, msgId))) {
                return true;
            }
            // Non-channel updates report channel/dialogId == 0 for PM/basic group messages
            return dialogId > 0 && deletedKeys.contains(makeKey(0L, msgId));
        }
    }

    private static String makeKey(long dialogId, int msgId) {
        long normalized = dialogId > 0 ? 0L : dialogId;
        return normalized + "_" + msgId;
    }

    private void scheduleSave() {
        if (saveRunnable == null) {
            saveRunnable = () -> {
                if (ApplicationLoader.applicationContext == null) return;
                Set<String> snapshot;
                synchronized (deletedKeys) {
                    snapshot = new HashSet<>(deletedKeys);
                }
                ApplicationLoader.applicationContext
                        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putStringSet(KEY_IDS, snapshot)
                        .apply();
            };
        }
        AndroidUtilities.cancelRunOnUIThread(saveRunnable);
        AndroidUtilities.runOnUIThread(saveRunnable, 800);
    }

    public void clearAll() {
        synchronized (deletedKeys) {
            deletedKeys.clear();
            loaded = true;
        }
        if (ApplicationLoader.applicationContext != null) {
            ApplicationLoader.applicationContext
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(KEY_IDS)
                    .apply();
        }
    }
}
