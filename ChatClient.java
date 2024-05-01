import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Scanner;

public class ChatClient {
    // Implementation of the API with a basic chat client with conversation context support
    private static final String SYSTEM_PROMPT = "You are a helpful assistant.";
    private static JSONArray messages = new JSONArray();
    public static final String API_URL = "http://localhost:8080/https://odjzvygjmpogtophihqs.functions.supabase.co/connect-llm";
    public static final String REFERER_URL = "https://app.omnigpt.co/";
    public static final String BEARER_TOKEN = "USER_PROVIDED";
    private static final String MODEL_STRING = "claude_3_haiku";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String bearerToken = BEARER_TOKEN;
        if (BEARER_TOKEN.equals("USER_PROVIDED")) {
            System.out.println("Enter Bearer token:");
            bearerToken = scanner.nextLine();
        }

        messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));

        OmniAPI apiClient = new OmniAPI(bearerToken);

        while (true) {
            System.out.println("### Enter your message:");
            String userInput = scanner.nextLine();
            String response = apiClient.sendMessage(userInput, MODEL_STRING, REFERER_URL, API_URL);
            messages.put(new JSONObject().put("role", "user").put("content", userInput));
            messages.put(new JSONObject().put("role", "assistant").put("content", response));
            // System.out.println("\nCurrent Conversation History:\n" + messages.toString(2));
            System.out.println(STR."### Response:\n\{response}");
        }
    }
}
