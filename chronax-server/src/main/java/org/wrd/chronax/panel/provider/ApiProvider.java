package org.wrd.chronax.panel.provider;

import com.google.gson.JsonElement;

public interface ApiProvider {
    String id();

    JsonElement provide();
}
