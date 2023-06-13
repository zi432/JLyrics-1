import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// based on https://github.com/PedroHLC/ViewLyricsOpenSearcher
public class LRCFinder {

    private static final String API_URL 		  = "http://search.crintsoft.com/searchlyrics.htm";
    private static final String CLIENT_USER_AGENT = "MiniLyrics";
    private static final String CLIENT_TAG 		  = "client=\"ViewLyricsOpenSearcher\"";
    private static final String XML_HEADER 	   	  = "<?xml version='1.0' encoding='utf-8' ?><searchV1 artist=\"%s\" title=\"%s\" OnlyMatched=\"1\" %s/>";
    private static final String SEARCH_QUERY_PAGE = " RequestPage='%d'";

    private static final byte[] MAGIC_KEY = "Mlv1clt4.0".getBytes();

    public static String findSyncLyrics(String artist, String title) throws IOException, NoSuchAlgorithmException {
        return searchQuery(
                String.format(XML_HEADER, artist, title, CLIENT_TAG +
                        String.format(SEARCH_QUERY_PAGE, 0))
        );
    }

    private static String searchQuery(String searchQuery) throws IOException, NoSuchAlgorithmException {

        BufferedReader reader;

        HttpURLConnection urlConnection = (HttpURLConnection) new URL(API_URL).openConnection();
        urlConnection.setConnectTimeout(10 * 1000);
        urlConnection.setRequestProperty("User-Agent", CLIENT_USER_AGENT);
        urlConnection.setRequestProperty("Expect", "100-Continue");
        urlConnection.setDoOutput(true);

        OutputStream out = urlConnection.getOutputStream();
        out.write(assembleQuery(searchQuery.getBytes(StandardCharsets.UTF_8)));
        out.close();

        if (urlConnection.getResponseCode() != 200) {
            System.err.println("An error occurred: " + urlConnection.getResponseCode());
            return "";
        }

        reader = new BufferedReader(
                new InputStreamReader(urlConnection.getInputStream(), "ISO_8859_1")
        );

        StringBuilder builder = new StringBuilder();
        char[] buffer 		  = new char[8192];

        int read;
        while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
            builder.append(buffer, 0, read);
        }
        reader.close();

        String lyrics = getLyricsURL(decryptResultXML(builder.toString()));
        if (lyrics.isEmpty()) return lyrics;

        URL lyricsURL = new URL(lyrics);
        reader = new BufferedReader(new InputStreamReader(lyricsURL.openStream()));

        String inputLine;
        StringBuilder sb = new StringBuilder();

        while ((inputLine = reader.readLine()) != null)
            sb.append(inputLine).append("\n");

        reader.close();
        return sb.toString();
    }

    private static byte[] assembleQuery(byte[] value) throws NoSuchAlgorithmException, IOException {

        byte[] pog = new byte[value.length + MAGIC_KEY.length];
        System.arraycopy(value, 0, pog, 0, value.length);
        System.arraycopy(MAGIC_KEY, 0, pog, value.length, MAGIC_KEY.length);

        byte[] pog_md5 = MessageDigest.getInstance("MD5").digest(pog);

        int j = 0;
        for (byte b : value) {
            j += b;
        }

        int k = (byte)(j / value.length);
        for (int m = 0; m < value.length; m++)
            value[m] = (byte) (k ^ value[m]);

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        result.write(0x02);
        result.write(k);
        result.write(0x04);
        result.write(0x00);
        result.write(0x00);
        result.write(0x00);

        result.write(pog_md5);
        result.write(value);
        result.close();

        return result.toByteArray();
    }

    private static String decryptResultXML(String value){

        char key = value.charAt(1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (int i = 22; i < value.length(); i++) {
            out.write((byte) (value.charAt(i) ^ key));
        }

        return out.toString();
    }

    private static String getLyricsURL(String result) {

        String[] parts  = result.split("\u0000");

        String url  = "";
        String link = "";

        for (int i = 0; i < parts.length; i++) {

            String s = parts[i];
            if (s.equals("server_url")) {
                url = parts[++i];
            }
            if (s.equals("link")) {
                link = parts[++i]; break;
            }
        }

        if (url.isEmpty() || link.isEmpty()) return "";
        return url + link;
    }
}
