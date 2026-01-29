package com.bot.insta;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

public class BotLauncher {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new InstagramBot("7999603144:AAFchSWgbgyWH1TLDV4OnpgMqiUM__04Tr8"));

            System.out.println("Telegram bot started successfully.");

            // Start an HTTP server on port 8080 for Google Cloud Run
            HttpServer server = HttpServer.create(new InetSocketAddress(8090), 0);
            server.createContext("/", exchange -> {
                String response = "Bot is running";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("HTTP server started on port 8080.");

        } catch (TelegramApiException | IOException e) {
            e.printStackTrace();
        }
    }
}
