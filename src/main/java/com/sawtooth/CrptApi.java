package com.sawtooth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CrptApi extends Thread {
    private final HttpClient httpClient;
    private final TimeUnit timeUnit;
    private final long timeLimit;
    private final int requestLimit;
    private final ArrayBlockingQueue<Request> requestsQueue;
    private int requestsPerTimeCount;
    private long lastCounterResetTime;


    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        httpClient = HttpClient.newHttpClient();
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        requestsPerTimeCount = 0;
        timeLimit = 1;
        requestsQueue = new ArrayBlockingQueue<>(50);
    }

    public CrptApi(TimeUnit timeUnit, int requestLimit, long timeLimit, int capacity) {
        httpClient = HttpClient.newHttpClient();
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.timeLimit = timeLimit;
        requestsPerTimeCount = 0;
        requestsQueue = new ArrayBlockingQueue<>(capacity);
    }

    private void sendRequest(Request request) {
        try {
            request.getCallback().accept(httpClient.send(request.getRequest(), HttpResponse.BodyHandlers.ofString()));
        }
        catch (IOException | InterruptedException exception) {
            request.getCallback().accept(null);
        }
    }

    private long getTimeLimitDelta() {
        return System.currentTimeMillis() - lastCounterResetTime;
    }

    private void reset() {
        lastCounterResetTime = System.currentTimeMillis();
        requestsPerTimeCount = 0;
    }

    @Override
    public void run() {
        long timeLimitDelta;

        lastCounterResetTime = System.currentTimeMillis();
        while (!isInterrupted()) {
            try {
                timeLimitDelta = getTimeLimitDelta();
                if (timeLimitDelta > timeUnit.toMillis(timeLimit))
                    reset();
                if (requestsPerTimeCount >= requestLimit) {
                    timeUnit.sleep(timeUnit.convert(timeUnit.toMillis(timeLimit) - timeLimitDelta, TimeUnit.MILLISECONDS));
                    reset();
                }
                else {
                    sendRequest(requestsQueue.take());
                    requestsPerTimeCount++;
                }
            }
            catch (InterruptedException exception) {
                interrupt();
            }
        }
    }

    public void createDocument(Document document, Consumer<HttpResponse<String>> callback) throws JsonProcessingException, InterruptedException {
        ObjectMapper mapper = new ObjectMapper();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(document)))
                .build();
        requestsQueue.put(new Request(request, callback));
    }



    public class Request {
        private final Consumer<HttpResponse<String>> callback;
        private final HttpRequest request;

        public Request(HttpRequest request, Consumer<HttpResponse<String>> callback) {
            this.request = request;
            this.callback = callback;
        }

        public Consumer<HttpResponse<String>> getCallback() { return callback; }

        public HttpRequest getRequest() { return request; }
    }

    public class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public Date production_date;
        public String production_type;
        public List<Product> products;
        public Date reg_date;
        public String reg_number;

        public class Description {
            public String participantInn;
        }

        public class Product {
            public String certificate_document;
            public Date certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public Date production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }
}
