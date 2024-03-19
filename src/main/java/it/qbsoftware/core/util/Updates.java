package it.qbsoftware.core.util;

import java.util.LinkedHashMap;

import rs.ltt.jmap.mock.server.Update;

public class Updates {
    final LinkedHashMap<String, Update> updates; 

    public Updates(){
        updates = new LinkedHashMap<>();
    }

    public LinkedHashMap<String, Update> getUpdates(){
        return updates;
    }
}
