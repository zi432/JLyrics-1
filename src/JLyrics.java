import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class JLyrics {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        if (args.length < 2) System.out.println("Usage: JLyrics <artist_name> <song_name> [lyrics_file]");
        String syncLyrics = LRCFinder.findSyncLyrics(args[0], args[1]);

        if (args.length > 2) {

            FileWriter writer = new FileWriter(args[2]);
            writer.write(syncLyrics);
            writer.close();
            return;
        }
        System.out.println(syncLyrics);
    }
}
