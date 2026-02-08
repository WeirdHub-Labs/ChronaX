package org.wrd.chronax.panel.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

public class MSPTProvider implements ApiProvider {
    @Override
    public String id() {
        return "mspt";
    }

    @Override
    public JsonElement provide() {
        MinecraftServer.TickTimes tickTimes = MinecraftServer.getServer().tickTimes5s;
        JsonObject json = new JsonObject();
        json.addProperty("mean", tickTimes.getAverage());
        long max = 0L;
        for (long t : tickTimes.getTimes()) {
            if (t > max) max = t;
        }
        json.addProperty("max", max * 1.0E-6D);
        return json;
    }
}
