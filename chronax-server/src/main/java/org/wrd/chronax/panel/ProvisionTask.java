package org.wrd.chronax.panel;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.wrd.chronax.panel.provider.ApiProvider;
import org.wrd.chronax.util.StringUtil;

import net.minecraft.server.MinecraftServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProvisionTask extends TimerTask {
    private static final int CONNECTION_TIMEOUT_MILLIS = 5000;
    private static final long FAST_RETRY_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private static final long SLOW_RETRY_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(1);

    private final Logger logger;
    private final PanelLinker linker;
    private final List<ApiProvider> providers;
    private long firstFailureAtMillis = -1L;
    private long nextAttemptAtMillis = 0L;
    private boolean slowRetryModeAnnounced;

    public ProvisionTask(Logger logger, PanelLinker linker, List<ApiProvider> providers) {
        this.logger = logger;
        this.linker = linker;
        this.providers = providers;
    }

    @Override
    public void run() {
        final long now = System.currentTimeMillis();
        if (now < nextAttemptAtMillis) {
            return;
        }

        final ProvisionResult result = provisionOnce();
        if (result.isSuccess()) {
            onSuccess();
            return;
        }

        onFailure(now, result.reason());
    }

    private ProvisionResult provisionOnce() {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String body = getBody();
        String signature = getSignature(timestamp, body);
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) linker.apiURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
            connection.setReadTimeout(CONNECTION_TIMEOUT_MILLIS);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Server-Id", linker.info().serverId());
            connection.setRequestProperty("X-Timestamp", timestamp);
            connection.setRequestProperty("X-Signature", signature);

            try(OutputStream os = connection.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                return ProvisionResult.ok();
            }

            final String responseBody = readResponseBody(connection);
            if (responseBody.isBlank()) {
                return ProvisionResult.fail("panel returned HTTP " + responseCode + " without response body");
            }
            return ProvisionResult.fail("panel returned HTTP " + responseCode + " (" + responseBody + ")");
        } catch (IOException e) {
            return ProvisionResult.fail("panel did not respond (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponseBody(HttpURLConnection connection) {
        final InputStream stream = connection.getErrorStream();
        if (stream == null) {
            return "";
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            final StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        } catch (IOException ignored) {
            return "";
        }
    }

    private void onSuccess() {
        if (firstFailureAtMillis != -1L) {
            logger.info("Panel response restored. Returning to normal 5-second provisioning interval.");
        }

        firstFailureAtMillis = -1L;
        nextAttemptAtMillis = 0L;
        slowRetryModeAnnounced = false;
    }

    private void onFailure(long now, String reason) {
        if (firstFailureAtMillis == -1L) {
            firstFailureAtMillis = now;
        }

        final long elapsed = now - firstFailureAtMillis;
        if (elapsed < FAST_RETRY_WINDOW_MILLIS) {
            nextAttemptAtMillis = now + PanelLinker.PROVISION_INTERVAL;
            logger.warn("Panel request failed: {}. Retrying every 5 seconds for up to 1 minute.", reason);
            return;
        }

        nextAttemptAtMillis = now + SLOW_RETRY_INTERVAL_MILLIS;
        if (!slowRetryModeAnnounced) {
            slowRetryModeAnnounced = true;
            logger.warn("Panel request has failed for over 1 minute: {}. Switching to hourly retry (1 attempt per hour).", reason);
            return;
        }

        logger.warn("Panel request failed: {}. Next retry will be attempted in 1 hour.", reason);
    }

    private String getBody() {
        CompletableFuture<String> future = new CompletableFuture<>();
        MinecraftServer.getServer().execute(() -> {
            try {
                JsonObject bodyJson = new JsonObject();
                providers.forEach((provider) -> bodyJson.add(provider.id(), provider.provide()));
                future.complete(bodyJson.toString());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Failed to collect panel data from main thread", e);
            return "{}";
        }
    }

    private String getSignature(String timestamp, String body) {
        String signTarget = timestamp + "." + body;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(linker.info().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return StringUtil.bytesToHex(mac.doFinal(signTarget.getBytes(StandardCharsets.UTF_8)));
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private record ProvisionResult(boolean isSuccess, String reason) {
        private static ProvisionResult ok() {
            return new ProvisionResult(true, "");
        }

        private static ProvisionResult fail(String reason) {
            return new ProvisionResult(false, reason);
        }
    }
}
