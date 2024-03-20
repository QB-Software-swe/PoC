package it.qbsoftware.core.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import rs.ltt.jmap.mock.server.Update;

public class Updates {

    // SINGLETON

    final LinkedHashMap<String, Update> updates;

    public Updates() {
        updates = new LinkedHashMap<>();
    }

    public LinkedHashMap<String, Update> getUpdates() {
        return updates;
    }

    public Update getAccumulatedUpdateSince(final String oldVersion) {
        final ArrayList<Update> updates = new ArrayList<>();
        for (Map.Entry<String, Update> updateEntry : this.updates.entrySet()) {
            if (updateEntry.getKey().equals(oldVersion) || updates.size() > 0) {
                updates.add(updateEntry.getValue());
            }
        }
        if (updates.isEmpty()) {
            return null;
        }
        return Update.merge(updates);
    }
}
