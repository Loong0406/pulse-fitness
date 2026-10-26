package com.pulse.server;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Bounds upstream bytes before allocating a complete provider response. */
final class LimitedBodyHandler implements HttpResponse.BodyHandler<byte[]> {
    private final int limit;

    LimitedBodyHandler(int limit) {
        this.limit = limit;
    }

    public HttpResponse.BodySubscriber<byte[]> apply(HttpResponse.ResponseInfo info) {
        return new Subscriber(limit);
    }

    private static final class Subscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        Subscriber(int limit) {
            this.limit = limit;
        }

        public CompletionStage<byte[]> getBody() {
            return result;
        }

        public void onSubscribe(Flow.Subscription s) {
            subscription = s;
            s.request(1);
        }

        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (bytes.size() + buffer.remaining() > limit) {
                    subscription.cancel();
                    result.completeExceptionally(new IllegalArgumentException("模型返回数据过大"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.write(chunk, 0, chunk.length);
            }
            subscription.request(1);
        }

        public void onError(Throwable error) {
            result.completeExceptionally(error);
        }

        public void onComplete() {
            result.complete(bytes.toByteArray());
        }
    }
}
