package dev.animedespair.lookupbubble;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Public search links. Never scrapes or logs in to Tinder or Facebook. */
public final class SearchUrls {
    private SearchUrls() {}

    static String normalized(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ");
    }

    public static String facebook(String name, String city) {
        String query = normalized(name);
        if (query.isEmpty()) throw new IllegalArgumentException("Enter a name first");
        String place = normalized(city);
        if (!place.isEmpty()) query += " " + place;
        return "https://www.facebook.com/search/people/?q=" + encode(query);
    }

    public static String google(String name, String city) {
        String person = normalized(name);
        if (person.isEmpty()) throw new IllegalArgumentException("Enter a name first");
        String query = "site:facebook.com \"" + person + "\"";
        if (!normalized(city).isEmpty()) query += " \"" + normalized(city) + "\"";
        return "https://www.google.com/search?q=" + encode(query);
    }

    private static String encode(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }
}
