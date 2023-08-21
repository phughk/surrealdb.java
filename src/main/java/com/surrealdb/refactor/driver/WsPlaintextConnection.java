package com.surrealdb.refactor.driver;

import com.surrealdb.refactor.exception.SurrealDBUnimplementedException;
import com.surrealdb.refactor.types.Credentials;
import com.surrealdb.refactor.types.Param;
import com.surrealdb.refactor.types.surrealdb.Value;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WsPlaintextConnection {

    private static final String HANDLER_ID_SURREALDB_CLIENT = "srdb-client";

    private static final EventLoopGroup group = new NioEventLoopGroup();
    private static final int MAX_CONTENT_LENGTH = 65536;

    public static UnauthenticatedSurrealDB<BidirectionalSurrealDB> connect(URI uri) {
        Channel channel;
        try {
            System.out.printf("Connecting to %s\n", uri);
            channel = bootstrapProtocol(uri).connect(uri.getHost(), uri.getPort()).sync().channel();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new UnauthenticatedSurrealDB<BidirectionalSurrealDB>() {
            @Override
            public UnusedSurrealDB<BidirectionalSurrealDB> authenticate(Credentials credentials) {
                SurrealDBWebsocketClientProtocolHandler srdbHandler =
                        (SurrealDBWebsocketClientProtocolHandler)
                                channel.pipeline().get(HANDLER_ID_SURREALDB_CLIENT);
                Object result;
                try {
                    result = srdbHandler.signin(credentials).get(2, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
                System.out.printf("Successfully signed in: %s", result);
                BidirectionalSurrealDB surrealdb =
                        new BidirectionalSurrealDB() {

                            @Override
                            public List<Value> query(String query, List<Param> params) {
                                throw new SurrealDBUnimplementedException(
                                        "https://github.com/surrealdb/surrealdb.java/issues/62",
                                        "Plaintext websocket connections are not supported yet");
                            }
                        };
                return new UnusedSurrealDB<>() {
                    @Override
                    public BidirectionalSurrealDB use() {
                        try {
                            Object use = srdbHandler.use(null, null).get(2, TimeUnit.SECONDS);
                            return surrealdb;
                        } catch (InterruptedException | ExecutionException | TimeoutException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public BidirectionalSurrealDB use(String namespace) {
                        try {
                            Object use = srdbHandler.use(namespace, null).get(2, TimeUnit.SECONDS);
                            return surrealdb;
                        } catch (InterruptedException | ExecutionException | TimeoutException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public BidirectionalSurrealDB use(String namespace, String database) {
                        try {
                            Object use =
                                    srdbHandler.use(namespace, database).get(2, TimeUnit.SECONDS);
                            return surrealdb;
                        } catch (InterruptedException | ExecutionException | TimeoutException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };
    }

    private static Bootstrap bootstrapProtocol(URI uri) {
        return new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(
                        new ChannelInitializer<>() {
                            @Override
                            protected void initChannel(Channel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpClientCodec())
                                        .addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                                        .addLast(
                                                new WebSocketClientProtocolHandler(
                                                        WebSocketClientHandshakerFactory
                                                                .newHandshaker(
                                                                        uri,
                                                                        WebSocketVersion.V13,
                                                                        null,
                                                                        false,
                                                                        null)))
                                        .addLast(
                                                HANDLER_ID_SURREALDB_CLIENT,
                                                new SurrealDBWebsocketClientProtocolHandler());
                            }
                        });
    }
}
