package com.bot.insta;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InstagramBot extends TelegramLongPollingBot {

    private final String botToken;

    public InstagramBot(String botToken) {
        this.botToken = botToken;
        System.out.println("✅ InstagramBot initialized with token.");
    }

    @Override
    public String getBotUsername() {
        return "NonFollowersFinderBot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    private final Map<Long, File> followersMap = new HashMap<>();
    private final Map<Long, File> followingMap = new HashMap<>();
    private final Map<Long, File> RecentlyUnfollowedMap = new HashMap<>();
    private final Map<Long, File> pendingReqMap = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long userId = message.getChatId();

            if (message.hasDocument()) {
                Document document = message.getDocument();
                String fileName = document.getFileName().toLowerCase();

                try {
                    File downloaded = downloadFile(document);
                    if (fileName.endsWith(".zip")) {
                        sendText(userId, "📂 Zip file received. Extracting and processing...");
                        handleZipFile(userId, downloaded);
                    } else {
                        sendText(userId, "⚠️ Please send the full ZIP file downloaded from Instagram.");
                    }
                } catch (Exception e) {
                    sendText(userId, "❌ Error: " + e.getMessage());
                    e.printStackTrace();
                }


            } else if (message.hasText()) {
                String text = message.getText().trim().toLowerCase();

                if (text.equals("/help")) {
                    sendText(userId, "🤖 *NonFollowersFinderBot*\n\n" +
                            "This bot helps you analyze your Instagram followers and following data.\n\n" +
                            "📦 *Features:*\n" +
                            "- Generate a report of accounts you follow but who don't follow you back.\n" +
                            "- Create a report of recently unfollowed accounts.\n\n" +
                            "📂 *How to Use:*\n" +
                            "1. Download your Instagram data as a ZIP file.\n" +
                            "2. Upload the ZIP file to this bot.\n" +
                            "3. Receive a detailed PDF report.\n\n" +
                            "⚠️ *Note:* Do not unfollow more than 200 accounts in 24 hours to avoid Instagram restrictions.\n\n" +
                            "Use /how_to_download for instructions on downloading your Instagram data.\n\n" +
                            "📧 *Contact:* If any help or query, you can contact Developer @shibili or on Instagram: [shibliee](https://www.instagram.com/shibliee).");
                } else if (text.contains("hi") || text.contains("hello")) {
                    sendText(userId, "👋 Hi! Please upload the Instagram ZIP file to get started.");
                }

                else if (text.equals("/how_to_download_video")) {
                    SendMessage msg = new SendMessage();
                    msg.setChatId(userId.toString());
                    msg.setText("▶️ *Watch this short video on how to download your Instagram ZIP file:*\n\n" +
                            "[Click to Watch](https://youtube.com/shorts/rQY8vDRVDdA?si=-8yGnI4fcHdn8Q1s)");
                    msg.setParseMode("Markdown");
                    try {
                        execute(msg);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
                else if (text.equals("/start")) {
                    sendText(userId, "🤖 *NonFollowersFinderBot*\n\n" +
                            "Welcome! Please upload your Instagram ZIP file to get started.\n\n" +
                            "Use /help for instructions on how to use this bot.");
                } else if (text.equals("/how_to_download") || text.equals("/howtodownload")) {
                    sendText(userId, "📂 *How to Download Your Instagram Data*\n\n" +
                            "1️⃣ Go to *Instagram* and log in to your account.\n" +
                            "2️⃣ Click your profile photo → *Settings* → *Accounts Center* (bottom left).\n" +
                            "3️⃣ In *Accounts Center*, go to *Your information and permissions* → *Download your information* → *Download or transfer information*.\n" +
                            "4️⃣ Select your Instagram account and choose the data you want (recommended: *Followers and Following* only).\n" +
                            "5️⃣ Select the time range (recommended: *All time*), format (*JSON*), and media quality (*Medium*).\n" +
                            "6️⃣ Click *Create files*. Meta will prepare your data and notify you via email.\n" +
                            "7️⃣ Once ready, download the ZIP file from the provided link.\n\n" +
                            "📧 *Contact:* If you need help or suggestions, you can contact the developer @shibili.");
                } else if (text.equals("/non_followers_report") && followersMap.containsKey(userId) && followingMap.containsKey(userId)) {
                    try {
                        generateAndSendPdf(userId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendText(userId, "❌ Failed to generate Non-Followers Report.");
                    }
                } else if (text.equals("/Recently_Unfollowed_Report") || text.equalsIgnoreCase("/RecentlyUnfollowedReport")) {
                    File unfollowedFile = RecentlyUnfollowedMap.get(userId);
                    File pdf = new File(System.getProperty("java.io.tmpdir"), "RecentlyUnfollowedReport.pdf");

                    try {
                        InstagramUsernameExtractor.createRecentlyUnfollowedPdf(unfollowedFile, pdf.getAbsolutePath());
                        SendDocument sendDoc = new SendDocument();
                        sendDoc.setChatId(userId.toString());
                        sendDoc.setDocument(new InputFile(pdf));
                        sendDoc.setCaption("Recently Unfollowed Report");
                        execute(sendDoc);
                        sendText(userId, "📤 Here is your Recently Unfollowed Report!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendText(userId, "❌ Failed to generate Recently Unfollowed Report. Please share the zip file first");
                    }

                   // unfollowedMap.remove(userId);

                } else if (text.equals("/pending_follow_requests") || text.equalsIgnoreCase("/pendingfollowrequests")) {
                    File pendingReqFile = pendingReqMap.get(userId);

                    File pdf = new File(System.getProperty("java.io.tmpdir"), "PendingFollowRequestReport.pdf");

                    try {
                        InstagramUsernameExtractor.createPendingReq(pendingReqFile, pdf.getAbsolutePath());
                        SendDocument sendDoc = new SendDocument();
                        sendDoc.setChatId(userId.toString());
                        sendDoc.setDocument(new InputFile(pdf));
                        sendDoc.setCaption("Recently Unfollowed Report");
                        execute(sendDoc);
                        sendText(userId, "📤 Here is your Recently Unfollowed Report!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendText(userId, "❌ Failed to generate Recently Unfollowed Report. Please share the zip file first");
                    }

                  //  unfollowedMap.remove(userId);

                } else {
                    sendText(userId, "📦 Please send the Instagram ZIP file to continue.");
                }
            }
        }
    }

    private File downloadFile(Document document) throws Exception {
        GetFile getFileRequest = new GetFile(document.getFileId());
        org.telegram.telegrambots.meta.api.objects.File file = execute(getFileRequest);

        File downloaded = File.createTempFile("insta_", ".json");
        try (
                BufferedInputStream in = new BufferedInputStream(
                        new URL(file.getFileUrl(getBotToken())).openStream());
                FileOutputStream out = new FileOutputStream(downloaded)
        ) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
        return downloaded;
    }

    private Map<String, File> unzipInstagramData(File zipFile) throws IOException {
        Map<String, File> extracted = new HashMap<>();
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "instaZip_" + System.currentTimeMillis());
        tempDir.mkdirs();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace("\\", "/").toLowerCase();

                File out = null;
                if (name.endsWith("followers_1.json")) {
                    out = new File(tempDir, "followers.json");
                    extracted.put("followers", out);
                } else if (name.endsWith("following.json")) {
                    out = new File(tempDir, "following.json");
                    extracted.put("following", out);
                } else if (name.endsWith("recently_unfollowed_profiles.json")) {
                    out = new File(tempDir, "recently_unfollowed_profiles.json");
                    extracted.put("unfollowed", out);
                } else if (name.endsWith("pending_follow_requests.json")) {
                    out = new File(tempDir, "pending_follow_requests.json");
                    extracted.put("pendingReq", out);
                }

                if (out != null) {
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        return extracted;
    }

    private void handleZipFile(Long userId, File zipFile) throws Exception {
        Map<String, File> extractedFiles = unzipInstagramData(zipFile);

        File followersFile = extractedFiles.get("followers");
        File followingFile = extractedFiles.get("following");
        File unfollowedFile = extractedFiles.get("unfollowed");
        File pendingReqFile = extractedFiles.get("pendingReq");


        if (followersFile == null || followingFile == null) {
            throw new Exception("Required files not found in ZIP. Make sure your ZIP contains followers_1.json and following.json");
        }
        pendingReqMap.put(userId, pendingReqFile);
        followersMap.put(userId, followersFile);
        followingMap.put(userId, followingFile);
        if (pendingReqFile != null) {
            pendingReqMap.put(userId, pendingReqFile);
        }
        if (unfollowedFile != null) {
            RecentlyUnfollowedMap.put(userId, unfollowedFile);
        }

        sendText(userId, "✅ Extracted files. Generating Non-Followers Report...");
        generateAndSendPdf(userId);

        followersMap.remove(userId);
        followingMap.remove(userId);
    }

    private void generateAndSendPdf(Long chatId) throws Exception {
        File followersFile = followersMap.get(chatId);
        File followingFile = followingMap.get(chatId);

        List<String> followers = InstagramUsernameExtractor.extractUsernames(followersFile.getPath());
        List<String> following = InstagramUsernameExtractor.extractUsernames(followingFile.getPath());


        Set<String> nonFollowers = new HashSet<>(following);
        nonFollowers.removeAll(followers);

        File pdf = new File(System.getProperty("java.io.tmpdir"), "NonFollowersReport.pdf");
        InstagramUsernameExtractor.createStyledPdf(new ArrayList<>(nonFollowers), pdf.getAbsolutePath(),"Non-Followers Report");

        SendDocument sendDoc = new SendDocument();
        sendDoc.setChatId(chatId.toString());
        sendDoc.setDocument(new InputFile(pdf));
        sendDoc.setCaption("Non-Followers Report");
        execute(sendDoc);

        SendMessage summary = new SendMessage();
        summary.setChatId(chatId.toString());
        summary.setText(String.format(
                "\uD83D\uDCCA *Instagram Report Summary*\n\n" +
                        "\uD83D\uDC65 *Followers:* %d\n" +
                        "\u2795 *Following:* %d\n" +
                        "\u274C *Non-Followers:* %d\n\n" +
                        "\uD83D\uDCC4 Report sent as PDF.",
                followers.size(), following.size(), nonFollowers.size()));
        summary.setParseMode("Markdown");
        execute(summary);

        SendMessage disclaimer = new SendMessage();
        disclaimer.setChatId(chatId.toString());
        disclaimer.setText("⚠️ *Important Note:*\n" +
                "Do not unfollow more than 200 accounts in a 24-hour period.\n" +
                "Doing so may result in a temporary ban or shadowban on your Instagram account.");
        disclaimer.setParseMode("Markdown");
        execute(disclaimer);

        SendMessage ask = new SendMessage();
        ask.setChatId(chatId.toString());
        ask.setText("❓ *What would you like to do?*\n\n" +
                    "- Generate a *Recently Unfollowed Report*: /Recently_Unfollowed_Report\n\n" +
                    "- View *Pending Follow Requests*: /pendingfollowrequests\n\n" +
                    "Reply with the corresponding command to proceed.");//   ask.setReplyMarkup(new ForceReplyKeyboard()); // 👈 Optional, prompts for reply
        ask.setParseMode("Markdown");
        execute(ask);
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown"); // Enables Markdown formatting
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
