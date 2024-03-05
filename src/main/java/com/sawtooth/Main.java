package com.sawtooth;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 2, 10, 5);
        CrptApi.Document document = api.new Document();

        try {
            api.start();
            api.createDocument(document, Main::ResponseHandler);
            api.createDocument(document, Main::ResponseHandler);
            api.createDocument(document, Main::ResponseHandler);
            api.createDocument(document, Main::ResponseHandler);
            api.createDocument(document, Main::ResponseHandler);
            api.createDocument(document, Main::ResponseHandler);
            Thread.sleep(20000);
            api.createDocument(document, Main::ResponseHandler);
            api.createDocument(document, Main::ResponseHandler);
        }
        catch (JsonProcessingException exception) {
            System.out.println("Ошибка сериализации документа");
        }
        catch (InterruptedException exception) {
            System.out.println("Ошибка добавления документа");
        }
    }

    public static void ResponseHandler(HttpResponse<String> response) {
        System.out.println(response.body());
    }
}