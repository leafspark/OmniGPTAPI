import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class OmniAPI {

    private final HttpClient client;
    static String bearerToken = "";

    public OmniAPI(String bearerToken) {
        this.client = HttpClient.newHttpClient();
        this.bearerToken = bearerToken;
    }

    public String sendMessage(JSONArray conversationHistory, String MODEL_STRING, String REFERER_URL, String API_URL) {
        StringBuilder partialResponse = new StringBuilder();

        JSONObject messageJson = new JSONObject();
        messageJson.put("messages", conversationHistory);
        messageJson.put("gptModel", MODEL_STRING);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + bearerToken)
                .header("Content-Type", "application/json")
                .header("Referer", REFERER_URL)
                .header("Origin", "http://localhost:3000")
                .header("X-Requested-With", "XMLHttpRequest")
                .POST(HttpRequest.BodyPublishers.ofString(messageJson.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    response.body().forEach(line -> {
                        if (line.startsWith("data: [DONE]")) {
                            return;
                        }

                        if (line.startsWith("data:")) {
                            String jsonPart = line.substring(5).trim(); // Remove "data:" prefix and trim
                            if (!jsonPart.isEmpty()) {
                                try {
                                    JSONObject responseData = new JSONObject(jsonPart);
                                    if (responseData.has("choices")) {
                                        JSONArray choices = responseData.getJSONArray("choices");
                                        for (int i = 0; i < choices.length(); i++) {
                                            JSONObject choice = choices.getJSONObject(i);
                                            if (choice.has("delta")) {
                                                JSONObject delta = choice.getJSONObject("delta");
                                                String content = delta.getString("content");
                                                partialResponse.append(content);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    System.out.println("\nError parsing JSON: " + e.getMessage());
                                }
                            }
                        }
                    });
                })
                .join(); // Wait for completion of the task
        return partialResponse.toString();
    }
}
