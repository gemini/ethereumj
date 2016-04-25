package org.ethereum.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The class intended to serve as an 'Event Bus' where all EthereumJ events are
 * dispatched asynchronously from component to component or from components to
 * the user event handlers.
 *
 * This made for decoupling different components which are intended to work
 * asynchronously and to avoid complex synchronisation and deadlocks between them
 *
 * Created by Anton Nashatyrev on 29.12.2015.
 */
@Component
public class EventDispatchThread {
    private static final Logger logger = LoggerFactory.getLogger("blockchain");

    private final ExecutorService executor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("ethereumj-event-dispatch-thread")
            .build());

    private static EventDispatchThread eventDispatchThread;

    /**
     * Returns the default instance for initialization of Autowired instances
     * to be used in tests
     */
    public static EventDispatchThread getDefault() {
        if (eventDispatchThread == null) {
            eventDispatchThread = new EventDispatchThread() {
                @Override
                public void invokeLater(Runnable r) {
                    r.run();
                }
            };
        }
        return eventDispatchThread;
    }

    public void invokeLater(final Runnable r) {
        if (executor.isShutdown()) return;
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Exception e) {
                    logger.error("EDT task exception", e);
                }
            }
        });
    }


    /**
     * Shut down the event dispatch executor
     *
     * The event dispatch thread is a single daemonic thread,
     * so it will not halt JVM exit, and thus does not need
     * to be explicitly shutdown. However, if one wishes to
     * explicitly clean up the thread earlier, then this
     * method will safely clean up this resource.
     */
    public void shutdown() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.warn("shutdown: executor interrupted: {}", e.getMessage());
        }
    }
}
