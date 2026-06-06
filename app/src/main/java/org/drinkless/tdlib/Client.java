package org.drinkless.tdlib;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Main class for interaction with the TDLib.
 */
public final class Client {
    static {
        try {
            System.loadLibrary("tdjni");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public interface ResultHandler {
        void onResult(TdApi.Object object);
    }

    public interface ExceptionHandler {
        void onException(Throwable e);
    }

    public interface LogMessageHandler {
        void onLogMessage(int verbosityLevel, String message);
    }

    public static class ExecutionException extends Exception {
        public final TdApi.Error error;
        ExecutionException(TdApi.Error error) {
            super(error.code + ": " + error.message);
            this.error = error;
        }
    }

    public void send(TdApi.Function query, ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
        long queryId = currentQueryId.incrementAndGet();
        if (resultHandler != null) {
            handlers.put(queryId, new Handler(resultHandler, exceptionHandler));
        }
        nativeClientSend(nativeClientId, queryId, query);
    }

    public void send(TdApi.Function query, ResultHandler resultHandler) {
        send(query, resultHandler, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TdApi.Object> T execute(TdApi.Function<T> query) throws ExecutionException {
        TdApi.Object object = nativeClientExecute(query);
        if (object instanceof TdApi.Error) {
            throw new ExecutionException((TdApi.Error) object);
        }
        return (T) object;
    }

    public static Client create(ResultHandler updateHandler, ExceptionHandler updateExceptionHandler, ExceptionHandler defaultExceptionHandler) {
        Client client = new Client(updateHandler, updateExceptionHandler, defaultExceptionHandler);
        synchronized (responseReceiver) {
            if (!responseReceiver.isRun) {
                responseReceiver.isRun = true;
                Thread receiverThread = new Thread(responseReceiver, "TDLib thread");
                receiverThread.setDaemon(true);
                receiverThread.start();
            }
        }
        return client;
    }

    public static void setLogMessageHandler(int maxVerbosityLevel, Client.LogMessageHandler logMessageHandler) {
        nativeClientSetLogMessageHandler(maxVerbosityLevel, logMessageHandler);
    }

    private static class ResponseReceiver implements Runnable {
        public boolean isRun = false;

        @Override
        public void run() {
            while (true) {
                int resultN = nativeClientReceive(clientIds, eventIds, events, 100000.0);
                for (int i = 0; i < resultN; i++) {
                    processResult(clientIds[i], eventIds[i], events[i]);
                    events[i] = null;
                }
            }
        }

        private void processResult(int clientId, long id, TdApi.Object object) {
            boolean isClosed = false;
            if (id == 0 && object instanceof TdApi.UpdateAuthorizationState) {
                TdApi.AuthorizationState authorizationState = ((TdApi.UpdateAuthorizationState) object).authorizationState;
                if (authorizationState instanceof TdApi.AuthorizationStateClosed) {
                    isClosed = true;
                }
            }

            Handler handler = id == 0 ? updateHandlers.get(clientId) : handlers.remove(id);
            if (handler != null) {
                try {
                    handler.resultHandler.onResult(object);
                } catch (Throwable cause) {
                    ExceptionHandler exceptionHandler = handler.exceptionHandler;
                    if (exceptionHandler == null) {
                        exceptionHandler = defaultExceptionHandlers.get(clientId);
                    }
                    if (exceptionHandler != null) {
                        try {
                            exceptionHandler.onException(cause);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
            if (isClosed) {
                updateHandlers.remove(clientId);
                defaultExceptionHandlers.remove(clientId);
                clientCount.decrementAndGet();
            }
        }

        private static final int MAX_EVENTS = 1000;
        private final int[] clientIds = new int[MAX_EVENTS];
        private final long[] eventIds = new long[MAX_EVENTS];
        private final TdApi.Object[] events = new TdApi.Object[MAX_EVENTS];
    }

    private final int nativeClientId;
    private static final ConcurrentHashMap<Integer, ExceptionHandler> defaultExceptionHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Handler> updateHandlers = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Handler> handlers = new ConcurrentHashMap<>();
    private static final AtomicLong currentQueryId = new AtomicLong();
    private static final AtomicLong clientCount = new AtomicLong();
    private static final ResponseReceiver responseReceiver = new ResponseReceiver();

    private static class Handler {
        final ResultHandler resultHandler;
        final ExceptionHandler exceptionHandler;

        Handler(ResultHandler resultHandler, ExceptionHandler exceptionHandler) {
            this.resultHandler = resultHandler;
            this.exceptionHandler = exceptionHandler;
        }
    }

    private Client(ResultHandler updateHandler, ExceptionHandler updateExceptionHandler, ExceptionHandler defaultExceptionHandler) {
        clientCount.incrementAndGet();
        nativeClientId = createNativeClient();
        if (updateHandler != null) {
            updateHandlers.put(nativeClientId, new Handler(updateHandler, updateExceptionHandler));
        }
        if (defaultExceptionHandler != null) {
            defaultExceptionHandlers.put(nativeClientId, defaultExceptionHandler);
        }
    }

    private static native int createNativeClient();
    private static native void nativeClientSend(int nativeClientId, long eventId, TdApi.Function function);
    private static native int nativeClientReceive(int[] clientIds, long[] eventIds, TdApi.Object[] events, double timeout);
    private static native TdApi.Object nativeClientExecute(TdApi.Function function);
    private static native void nativeClientSetLogMessageHandler(int maxVerbosityLevel, LogMessageHandler logMessageHandler);
}
