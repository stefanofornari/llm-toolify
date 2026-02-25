package ste.ai.toolify.log;

import static org.assertj.core.api.BDDAssertions.then;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static ste.ai.toolify.log.HTTPLogParser.KEY_BODY;
import static ste.ai.toolify.log.HTTPLogParser.KEY_HEADERS;
import static ste.ai.toolify.log.HTTPLogParser.KEY_STATUS;
import static ste.ai.toolify.log.HTTPLogParser.KEY_TYPE;

/**
 * Tests for HTTPLogParser
 */
public class HTTPLogParserTest {

    private final String LOG1 =
        """
        HTTP request:
        - method: POST
        - url: https://api.perplexity.ai/chat/completions
        - headers: [Authorization: Beare...Cg], [User-Agent: langchain4j-openai], [Content-Type: application/json]
        - body: {
          "model" : "sonar-pro",
          "messages" : [ {
            "role" : "system",
            "content" : "You are a code assistant"
          }, {
            "role" : "user",
            "content" : "hello"
          } ],
          "stream" : false
        }
        """;
    private final String LOG2 = "HTTP response:\n- status code: 404\n- headers: [:status: 200],[h1: v1] , [h2: v21, v22]\n- body: {}";
    //private final String LOG2 = "HTTP response:\n- status code: 404\n- headers:  [:status: 200], [access-control-allow-origin: *], [alt-svc: h3=\":443\"; ma=86400], [cf-cache-status: DYNAMIC], [cf-ray: 9cfb3374dabcbb1a-MXP], [content-length: 9866], [content-type: application/json], [date: Wed, 18 Feb 2026 05:42:35 GMT], [mistral-correlation-id: 019c6f45-6523-7b29-a0d1-2e9d64650277], [server: cloudflare], [set-cookie: [__cf_bm=qIvV.TgfMeGARGjfwGZsZ9NcFpnjODETpNFK4i6aoAo-1771393355-1.0.1.1-O4paiezSW_2o2DlRNHS2Uozub._3mjalGALXdmJ8xgsHDn2zubTwd10WtUWQZYp2EtGp3izMAhrzMOKWdQGAg_txjrn3BDClkZWOnmtqUEE; path=/; expires=Wed, 18-Feb-26 06:12:35 GMT; domain=.mistral.ai; HttpOnly; Secure; SameSite=None, _cfuvid=Ko6n5YWEWyYwXEi79Kg98aRhXzjtqo1InrTB9CkDz4I-1771393355057-0.0.1.1-604800000; path=/; domain=.mistral.ai; HttpOnly; Secure; SameSite=None]], [strict-transport-security: max-age=15552000; includeSubDomains; preload], [x-content-type-options: nosniff], [x-envoy-upstream-service-time: 10229], [x-kong-proxy-latency: 8], [x-kong-request-id: 019c6f45-6523-7b29-a0d1-2e9d64650277], [x-kong-upstream-latency: 10229], [x-ratelimit-limit-req-minute: 60], [x-ratelimit-limit-tokens-minute: 50000], [x-ratelimit-limit-tokens-month: 4000000], [x-ratelimit-remaining-req-minute: 59], [x-ratelimit-remaining-tokens-minute: 47196], [x-ratelimit-remaining-tokens-month: 3950246], [x-ratelimit-tokens-query-cost: 2804]\n- body: {}";
    private final String LOG3 = "HTTP request:\n- method: POST\n- url: https://api.perplexity.ai/chat/completions\n- headers: [content-type: application/json]\n- body: {\"id\":\"b19c37da-e5ae-44de-b463-d8d01ca7aacb\"}";

    @Test
    void json_parses_captures_all_fields() {
        final HTTPLogParser parser = new HTTPLogParser();

        JSONObject json = parser.json(LOG1);
        then(json.getString(KEY_TYPE)).isEqualTo("request");

        JSONObject bodyJson = json.getJSONObject(KEY_BODY);
        then(bodyJson.getString("model")).isEqualTo("sonar-pro");
        then(bodyJson.getJSONArray("messages").length()).isEqualTo(2);
        then(bodyJson.getBoolean("stream")).isFalse();

        JSONObject headersJson = json.getJSONObject(KEY_HEADERS);
        then(headersJson.length()).isEqualTo(3);
        then(headersJson.getString("Authorization")).isEqualTo("Beare...Cg");
        then(headersJson.getString("User-Agent")).isEqualTo("langchain4j-openai");
        then(headersJson.getString("Content-Type")).isEqualTo("application/json");

        json = parser.json(LOG2);
        then(json.getString(KEY_TYPE)).isEqualTo("response");
        bodyJson = json.getJSONObject(KEY_BODY);
        then(bodyJson.length()).isZero();

        headersJson = json.getJSONObject(KEY_HEADERS);
        then(headersJson.length()).isEqualTo(3);
        then(headersJson.getString("status")).isEqualTo("200");
        then(headersJson.getString("h1")).isEqualTo("v1");
        then(headersJson.getString("h2")).isEqualTo("v21, v22");

        json = parser.json(LOG3);
        then(json.getString(KEY_TYPE)).isEqualTo("request");
        bodyJson = json.getJSONObject(KEY_BODY);
        then(bodyJson.getString("id")).isEqualTo("b19c37da-e5ae-44de-b463-d8d01ca7aacb");

        headersJson = json.getJSONObject(KEY_HEADERS);
        then(headersJson.length()).isEqualTo(1);
        then(headersJson.getString("content-type")).isEqualTo("application/json");
    }


    @Test
    void does_not_extract_status_code_from_invalid_log() {
        HTTPLogParser parser = new HTTPLogParser();
        String log = "first line:\n- no status code present";
        then(parser.json(log).has(KEY_STATUS)).isFalse();
        then(parser.json(log).has(KEY_TYPE)).isFalse();
        then(parser.json(log).has(KEY_HEADERS)).isFalse();
        then(parser.json(log).has(KEY_BODY)).isFalse();
    }

}