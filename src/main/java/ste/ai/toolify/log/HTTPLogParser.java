package ste.ai.toolify.log;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONObject;

/**
 * Parses HTTP logs into a structured JSON format.
 */
public class HTTPLogParser {

    private static final Pattern PATTERN_STATUS = Pattern.compile("- status code: (\\d+)");
    private static final Pattern PATTERN_HEADERS = Pattern.compile("\\[:?([^\\]]*)\\]");

    public static final String KEY_BODY = "body";
    public static final String KEY_HEADERS = "headers";
    public static final String KEY_STATUS = "status";
    public static final String KEY_TYPE = "type";

    /**
     * Parses the given log string and identifies if it is a response.
     *
     * @param log the log string to analyze
     * @return true if it is a response, otherwise false
     */
    public JSONObject json(String log) {
        final JSONObject ret = new JSONObject();

        final Scanner scanner = new Scanner(log);
        String line = scanner.nextLine();

        if (line.startsWith("HTTP request")) {
            ret.put(KEY_TYPE, "request");
            scanner.nextLine();   // method
            scanner.nextLine();   // url
            line = scanner.nextLine();
        } else if (line.startsWith("HTTP response")) {
            ret.put(KEY_TYPE, "response"); line = scanner.nextLine();
        }

        final Matcher statusMatcher = PATTERN_STATUS.matcher(line);

        if (statusMatcher.find()) {
            try {
                ret.put(KEY_STATUS, Integer.parseInt(statusMatcher.group(1)));
            } catch (NumberFormatException x) {
                // nothing to do, just ignore it
            }
            line = scanner.nextLine();
        }

        final JSONObject headers = new JSONObject();
        if (line.startsWith("- headers:")) {
            final Matcher headerMatcher = PATTERN_HEADERS.matcher(line.substring(11));
            while(headerMatcher.find()) {
                final String headerLine = headerMatcher.group(1).trim();
                int pos = headerLine.indexOf(':');
                if (pos < 0) {
                    headers.put(headerLine, "");
                } else {
                    headers.put(
                        headerLine.substring(0, pos).trim(),
                        headerLine.substring(pos+1).trim()
                    );
                }
            }
            ret.put(KEY_HEADERS, headers);
            line = scanner.nextLine();
        }

        if (line.startsWith("- body:")) {
            final StringBuilder sb = new StringBuilder(line.substring(8));

            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }

            ret.put(KEY_BODY, new JSONObject(sb.toString()));
        }

        return ret;
    }

}