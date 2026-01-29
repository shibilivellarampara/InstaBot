package com.bot.insta;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.*;
import java.util.List;
import java.time.*;
import java.time.temporal.ChronoUnit;

public class InstagramUsernameExtractor {

    /**
     * Extract Instagram usernames from the JSON file.
     */
    public static List<String> extractUsernames(String jsonPath) throws Exception {
        System.out.println("📥 Reading JSON file from: " + jsonPath);

        JSONParser parser = new JSONParser();
        Object parsed = parser.parse(new FileReader(jsonPath));

        List<String> usernames = new ArrayList<>();

        if (parsed instanceof JSONArray) {
            System.out.println("🔍 Detected JSON array format (likely followers.json)");
            JSONArray rootArray = (JSONArray) parsed;
            for (Object item : rootArray) {
                if (item instanceof JSONObject) {
                    extractUsernameFromUserObject((JSONObject) item, usernames);
                }
            }
        } else if (parsed instanceof JSONObject) {
            JSONObject rootObj = (JSONObject) parsed;

            JSONArray followersArray = (JSONArray) rootObj.get("relationships_followers");
            JSONArray followingArray = (JSONArray) rootObj.get("relationships_following");
            JSONArray pendingRequestsArray = (JSONArray) rootObj.get("relationships_follow_requests_sent");


            if (followersArray != null) {
                System.out.println("🔍 Detected 'relationships_followers' key.");
                for (Object item : followersArray) {
                    if (item instanceof JSONObject) {
                        extractUsernameFromUserObject((JSONObject) item, usernames);
                    }
                }
            }

            if (followingArray != null) {
                System.out.println("🔍 Detected 'relationships_following' key.");
                for (Object item : followingArray) {
                    if (item instanceof JSONObject) {
                        extractUsernameFromUserObject((JSONObject) item, usernames);
                    }
                }
            }
            if (pendingRequestsArray != null) {
                System.out.println("🔍 Detected 'relationships_follow_requests_sent' key.");
                for (Object item : pendingRequestsArray) {
                    if (item instanceof JSONObject) {
                        extractUsernameFromUserObject((JSONObject) item, usernames);
                    }
                }
            }

            if (followersArray == null && followingArray == null) {
                System.err.println("⚠️ Unsupported File. Please upload only 'followers.json' or 'following.json' exported from Instagram.");
                throw new Exception("Unsupported File. Only 'followers' or 'following' are supported.");
            }
        } else {
            System.err.println("⚠️ Unsupported file structure. Only 'followers' or 'following' are supported.");
            throw new Exception("Unsupported root structure in JSON.");
        }

        System.out.println("✅ Extracted " + usernames.size() + " usernames.");
        return usernames;
    }

    /**
     * Extract usernames safely from each user object.
     */
    private static void extractUsernameFromUserObject(JSONObject userObj, List<String> usernames) {
        JSONArray stringListData = (JSONArray) userObj.get("string_list_data");

        if (stringListData != null && !stringListData.isEmpty()) {
            Object firstData = stringListData.get(0);
            if (firstData instanceof JSONObject) {
                JSONObject data = (JSONObject) firstData;
                Object rawValue = data.get("value");
                if (rawValue != null) {
                    String username = rawValue.toString().trim();
                    if (!username.isEmpty()) {
                        usernames.add(username);
                    }
                }
            }
        }
    }

    /**
     * Create a PDF report of recently unfollowed profiles from JSON.
     */
    public static void createRecentlyUnfollowedPdf(File jsonFile, String outputPath) throws Exception {
        System.out.println("📥 Reading recently_unfollowed_profiles JSON from: " + jsonFile.getAbsolutePath());

        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(new FileReader(jsonFile));

        // Get the array of unfollowed users
        JSONArray unfollowedArray = (JSONArray) root.get("relationships_unfollowed_users");

        List<String> lines = new ArrayList<>();

        if (unfollowedArray != null) {
            for (Object entryObj : unfollowedArray) {
                if (!(entryObj instanceof JSONObject)) continue;
                JSONObject entry = (JSONObject) entryObj;

                JSONArray stringListData = (JSONArray) entry.get("string_list_data");
                if (stringListData != null && !stringListData.isEmpty()) {
                    JSONObject data = (JSONObject) stringListData.get(0);

                    String username = (String) data.get("value");
                    String link = (String) data.get("href");
                    // timestamp might be Long or Number
                    Number timestampNum = (Number) data.get("timestamp");
                    long timestamp = timestampNum != null ? timestampNum.longValue() : 0L;


                    String date="";

// Current time in the same zone
                    ZonedDateTime now = ZonedDateTime.now(java.time.ZoneId.systemDefault());
                    ZonedDateTime eventTime = Instant.ofEpochSecond(timestamp).atZone(java.time.ZoneId.systemDefault());

                    long daysBetween = ChronoUnit.DAYS.between(eventTime.toLocalDate(), now.toLocalDate());

                    if (daysBetween == 0) {
                        date = "Today";
                    } else if (daysBetween == 1) {
                        date = "1 day ago";
                    } else if (daysBetween < 7) {
                        date = daysBetween + " days ago";
                    } else if (daysBetween < 30) {
                        long weeks = daysBetween / 7;
                        date = weeks + (weeks == 1 ? " week ago" : " weeks ago");
                    } else if (daysBetween < 365) {
                        // For older than a month, just show the exact date (optional)
                        long weeks = daysBetween / 30;
                        date = daysBetween + " month ago";
                    }

                    // Format line for PDF: username - link - date
                    lines.add(String.format("%s - [%s](%s) - Unfollowed %s", username, username, link, date));
                }
            }
        } else {
            System.err.println("⚠️ No 'relationships_unfollowed_users' found in JSON.");
            throw new Exception("Missing 'relationships_unfollowed_users' in JSON.");
        }

        // Reuse your existing method to generate PDF
        createStyledPdf(lines, outputPath,"Recently Unfollowed Profiles");
    }

    public static void createPendingReq(File jsonFile, String outputPath) throws Exception {
        System.out.println("📥 Reading pending follow requests JSON from: " + jsonFile.getAbsolutePath());

        JSONParser parser = new JSONParser();
        JSONObject root = (JSONObject) parser.parse(new FileReader(jsonFile));

        // Get the array of unfollowed users
        JSONArray unfollowedArray = (JSONArray) root.get("relationships_follow_requests_sent");

        List<String> lines = new ArrayList<>();

        if (unfollowedArray != null) {
            for (Object entryObj : unfollowedArray) {
                if (!(entryObj instanceof JSONObject)) continue;
                JSONObject entry = (JSONObject) entryObj;

                JSONArray stringListData = (JSONArray) entry.get("string_list_data");
                if (stringListData != null && !stringListData.isEmpty()) {
                    JSONObject data = (JSONObject) stringListData.get(0);

                    String username = (String) data.get("value");
                    String link = (String) data.get("href");
                    // Format line for PDF: username - link
                    lines.add(String.format("%s - [%s](%s) - ", username, username, link));
                }
            }
        } else {
            System.err.println("⚠️ No 'relationships_unfollowed_users' found in JSON.");
            throw new Exception("Missing 'relationships_unfollowed_users' in JSON.");
        }

        // Reuse your existing method to generate PDF
        createStyledPdf(lines, outputPath, "Pending Follow Requests");
    }

    /**
     * Create a styled and grouped PDF listing usernames with clickable links.
     */
    public static void createStyledPdf(List<String> usernames, String outputPath, String heading) throws Exception {
        System.out.println("📄 Starting PDF generation to: " + outputPath);

        // Normalize and sort usernames
        usernames.sort(Comparator.comparing(s -> s.replaceAll("[_.]", "").toLowerCase()));

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Font groupFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
        Font serialFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
        Font linkFont = FontFactory.getFont(FontFactory.HELVETICA, 12,  BaseColor.DARK_GRAY);
        Font extraFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);

        document.add(new Paragraph(heading, titleFont));
        document.add(Chunk.NEWLINE);

        char currentGroup = 0;
        int serial = 1;

        for (String usernameLine : usernames) {
            if (usernameLine.trim().isEmpty()) continue;

            // Extract username and link
            String username = usernameLine.split(" - ")[0];
            String normalized = username.replaceAll("[_.]", "").toLowerCase();
            if (normalized.isEmpty()) continue;

            char firstChar = Character.toUpperCase(normalized.charAt(0));

            if (firstChar != currentGroup) {
                currentGroup = firstChar;
                document.add(new Paragraph("\n" + currentGroup, groupFont));
                document.add(Chunk.NEWLINE);
            }

            Paragraph entry = new Paragraph();
            entry.add(new Chunk(serial + ". ", serialFont));

            // Extract link from markdown-style link: [username](https://instagram.com/username)
            String link = null;
            int startLink = usernameLine.indexOf('(');
            int endLink = usernameLine.indexOf(')');
            if (startLink != -1 && endLink != -1 && endLink > startLink) {
                link = usernameLine.substring(startLink + 1, endLink);
            } else {
                // Fallback to standard link
                link = "https://instagram.com/" + username;
            }

            Anchor anchor = new Anchor(username, linkFont);
            anchor.setReference(link);
            entry.add(anchor);

            // Add trailing text like "Unfollowed At: ..." if present
            int extraStart = usernameLine.indexOf(") - ");
            if (extraStart != -1 && extraStart + 4 < usernameLine.length()) {
                String extra = usernameLine.substring(extraStart + 4);
                entry.add(new Chunk(" - " + extra, extraFont));
            }

            document.add(entry);
            serial++;
        }

        document.close();
        System.out.println("✅ PDF generation complete. " + (serial - 1) + " usernames added.");
    }
}
