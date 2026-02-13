package org.dreeam.leaf.async.world;

import java.util.concurrent.CompletableFuture;

public record WorldReadRequest(
    ReadOperationType type,
    Payload payload,
    CompletableFuture<Object> future // Future to complete with the result
) {
    public WorldReadRequest(ReadOperationType type, Object[] params, CompletableFuture<Object> future) {
        this(type, new LegacyPayload(params), future);
    }

    public static WorldReadRequest unary(ReadOperationType type, Object first, CompletableFuture<Object> future) {
        return new WorldReadRequest(type, new UnaryPayload(first), future);
    }

    public static WorldReadRequest binary(ReadOperationType type, Object first, Object second, CompletableFuture<Object> future) {
        return new WorldReadRequest(type, new BinaryPayload(first, second), future);
    }

    public static WorldReadRequest ternary(ReadOperationType type, Object first, Object second, Object third, CompletableFuture<Object> future) {
        return new WorldReadRequest(type, new TernaryPayload(first, second, third), future);
    }

    public static WorldReadRequest quaternary(ReadOperationType type, Object first, Object second, Object third, Object fourth, CompletableFuture<Object> future) {
        return new WorldReadRequest(type, new QuaternaryPayload(first, second, third, fourth), future);
    }

    public static WorldReadRequest quinary(ReadOperationType type, Object first, Object second, Object third, Object fourth, Object fifth, CompletableFuture<Object> future) {
        return new WorldReadRequest(type, new QuinaryPayload(first, second, third, fourth, fifth), future);
    }

    public Object first() {
        return this.payload.first();
    }

    public Object second() {
        return this.payload.second();
    }

    public Object third() {
        return this.payload.third();
    }

    public Object fourth() {
        return this.payload.fourth();
    }

    public Object fifth() {
        return this.payload.fifth();
    }

    public sealed interface Payload permits LegacyPayload, UnaryPayload, BinaryPayload, TernaryPayload, QuaternaryPayload, QuinaryPayload {
        Object first();

        default Object second() {
            throw new IllegalStateException("No second parameter for this request payload");
        }

        default Object third() {
            throw new IllegalStateException("No third parameter for this request payload");
        }

        default Object fourth() {
            throw new IllegalStateException("No fourth parameter for this request payload");
        }

        default Object fifth() {
            throw new IllegalStateException("No fifth parameter for this request payload");
        }
    }

    public record LegacyPayload(Object[] params) implements Payload {
        @Override
        public Object first() {
            return this.params[0];
        }

        @Override
        public Object second() {
            return this.params[1];
        }

        @Override
        public Object third() {
            return this.params[2];
        }

        @Override
        public Object fourth() {
            return this.params[3];
        }

        @Override
        public Object fifth() {
            return this.params[4];
        }
    }

    public record UnaryPayload(Object first) implements Payload {
    }

    public record BinaryPayload(Object first, Object second) implements Payload {
    }

    public record TernaryPayload(Object first, Object second, Object third) implements Payload {
    }

    public record QuaternaryPayload(Object first, Object second, Object third, Object fourth) implements Payload {
    }

    public record QuinaryPayload(Object first, Object second, Object third, Object fourth, Object fifth) implements Payload {
    }
}
