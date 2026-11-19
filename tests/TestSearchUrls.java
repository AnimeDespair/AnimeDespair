import dev.animedespair.lookupbubble.SearchUrls;

public class TestSearchUrls {
    public static void main(String[] args) {
        String url = SearchUrls.facebook("  Alex  ", "  Sioux City  ");
        if (!url.contains("Alex+Sioux+City")) throw new AssertionError(url);
        String google = SearchUrls.google("Alex", "Iowa");
        if (!google.contains("site%3Afacebook.com")) throw new AssertionError(google);
        boolean threw = false;
        try { SearchUrls.facebook("  ", "Iowa"); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) throw new AssertionError("Missing name was allowed");
        System.out.println("Public search URL tests passed.");
    }
}
