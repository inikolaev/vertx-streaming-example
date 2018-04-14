package io.github.inikolaev;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;

import java.util.function.Consumer;

class BlockingService {
    private final Vertx vertx;

    public static BlockingService create(final Vertx vertx) {
        return new BlockingService(vertx);
    }

    BlockingService(final Vertx vertx) {
        this.vertx = vertx;
    }

    public void generate(final int n, final Consumer<Integer> consumer, Handler<AsyncResult<Object>> handler) {
        vertx.executeBlocking((future) -> {
            for (int i = 0; i < n; i++) {
                System.out.println("Processing " + i);

                final int k = i;
                vertx.runOnContext((v) -> consumer.accept(k));

                try {
                    Thread.sleep(100l);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            future.complete();
        }, handler);
    }
}

public class Main {
    public static void main(String[] args) {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new AbstractVerticle() {
            @Override
            public void start() throws Exception {
                System.out.println(Thread.currentThread().getName());

                HttpServerOptions options = new HttpServerOptions().setCompressionSupported(true);

                vertx.createHttpServer(options).requestHandler((request) -> {
                    StringBuilder buffer = new StringBuilder();

                    request.response().setChunked(true);
                    request.response().putHeader("Content-Disposition", "attachment; filename=\"test.txt\"");

                    final Consumer<Integer> consumer = n -> {
                        buffer.append(n).append("\r\n");

                        if (buffer.length() >= 10) {
                            request.response().write(buffer.toString());
                            buffer.setLength(0);
                        }
                    };

                    final BlockingService blockingService = BlockingService.create(vertx);
                    blockingService.generate(100, consumer, (ar) -> {
                        request.response().write(buffer.toString());
                        request.response().end();
                    });
               }).listen(9090);
           }
       });
    }
}
