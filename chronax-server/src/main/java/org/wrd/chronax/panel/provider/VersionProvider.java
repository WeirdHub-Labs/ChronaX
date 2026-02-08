package org.wrd.chronax.panel.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.wrd.chronax.panel.PanelLinker;

public class VersionProvider implements ApiProvider {
    @Override
    public String id() {
        return "version";
    }

    @Override
    public JsonElement provide() {
        return new JsonPrimitive(PanelLinker.API_VERSION);
    }
}
