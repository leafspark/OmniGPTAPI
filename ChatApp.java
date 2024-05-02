import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class ChatApp {

    private JTextField txtMessage;
    private JList<Message> historyList;
    private DefaultListModel<Message> listModel;
    private JButton btnSend;
    private JComboBox<String> modelComboBox;
    private OmniAPI api;
    private JSONArray conversationHistory;
    private String SYSTEM_PROMPT = "You are a helpful assistant. You are a helpful assistant. You have the ability to search the web by using the string <tool><{webSearch, query}>. The response will be provided to you in JSON format. You are able to access a Python interpreter, with the command <tool><{pythonExec, code}> (Python 2.7). Please one line your Python code. The program output will be provided to you as text. Please remember this enviorment is not sandboxed; use caution when doing operations on the file system. You are able to access an image generator using the function <tool><{imageGen, prompt}>. The response will be provided to you as a link, which you can embed using Markdown for the user.";
    private String ASSISTANT_PREFILL = "";
    private String bearerToken, refererUrl, apiUrl;
    private boolean useConversationHistory = true;
    private final Map<String, String> modelMap;
    private static final String API_KEY = "USER_PROVIDED";
    private static final String SEARCH_ENGINE_ID = "USER_PROVIDED";

    public ChatApp() {
        // Load settings from file
        loadSettings();

        api = new OmniAPI(bearerToken);
        conversationHistory = new JSONArray();

        // System prompt message at the start
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", SYSTEM_PROMPT);
        conversationHistory.put(system);

        // Map of model strings to display names
        modelMap = new HashMap<>();
        modelMap.put("claude2.0", "Claude 2");
        modelMap.put("claude2.1", "Claude 2.1");
        modelMap.put("claude_3_haiku", "Claude 3 Haiku");
        modelMap.put("claude_3_sonnet", "Claude 3 Sonnet");
        modelMap.put("claude_3_opus", "Claude 3 Opus");
        modelMap.put("gpt-3.5-turbo", "GPT-3.5-Turbo");
        modelMap.put("gpt-3.5-turbo-16k", "GPT-3.5-Turbo-16k");
        modelMap.put("gpt-4-turbo", "GPT-4-Turbo");
        modelMap.put("gpt-4", "GPT-4");
        modelMap.put("gemini-pro-1.5", "Gemini Pro 1.5");
        modelMap.put("mistral-8x22b-instruct", "Mistral 8x22b Instruct");
        modelMap.put("llama-3-70b-instruct", "Llama 3 70b Instruct");
        modelMap.put("llama-3-8b-instruct", "Llama 3 8b Instruct");
        modelMap.put("llama-2-70b-chat-hf", "Llama 2 70b Chat");
        modelMap.put("perplexity-70b-online", "Perplexity 70b Online");
        modelMap.put("perplexity", "Perplexity 7b Online");
    }

    private void loadSettings() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(STR."\{System.getProperty("user.home")}/Desktop/OmniApp/config.json")));
            JSONObject config = new JSONObject(content);
            bearerToken = config.getString("bearerToken");
            refererUrl = config.getString("refererUrl");
            apiUrl = config.getString("apiUrl");
            SYSTEM_PROMPT = config.getString("systemPrompt");
            useConversationHistory = config.getBoolean("useConversationHistory");
            ASSISTANT_PREFILL = config.getString("assistantPrefill");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSettings(String bearerToken, String refererUrl, String apiUrl, String systemPrompt, boolean useConversationHistory, String assistantPrefill) {
        try {
            JSONObject config = new JSONObject();
            config.put("bearerToken", bearerToken);
            config.put("refererUrl", refererUrl);
            config.put("apiUrl", apiUrl);
            config.put("systemPrompt", systemPrompt);
            config.put("useConversationHistory", useConversationHistory);
            config.put("assistantPrefill", assistantPrefill);

            File directory = new File(STR."\{System.getProperty("user.home")}/Desktop/OmniApp");
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Config directory created");
                }
            }

            FileWriter file = new FileWriter(STR."\{directory}/config.json");
            file.write(config.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportChatHistory() {
        try {
            FileWriter file = new FileWriter(STR."\{System.getProperty("user.home")}/Desktop/OmniApp/chat_history.json");
            file.write(conversationHistory.toString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadChatHistory() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(STR."\{System.getProperty("user.home")}/Desktop/OmniApp/chat_history.json")));
            conversationHistory = new JSONArray(content);
            listModel.clear();
            for (int i = 0; i < conversationHistory.length(); i++) {
                JSONObject messageJson = conversationHistory.getJSONObject(i);
                if (Objects.equals(messageJson.getString("role"), "system")) continue;
                Message message = new Message(messageJson.getString("role"), messageJson.getString("content"));
                listModel.addElement(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createAndShowGUI() {
        // Create and set up the window.
        JFrame frame = new JFrame("OmniChat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.setSize(500, 500);  // Set the size of the window

        // Load the favicon
        try {
            Image favicon = Toolkit.getDefaultToolkit().getImage("favicon.ico");
            frame.setIconImage(favicon);
        } catch (Exception e) {
            System.out.println(STR."Error loading favicon: \{e.getMessage()}");
        }

        // Create model selection dropdown
        String[] modelStrings = modelMap.values().toArray(new String[0]);
        modelComboBox = new JComboBox<>(modelStrings);
        String MODEL_STRING = "claude_3_haiku";
        modelComboBox.setSelectedItem(modelMap.get(MODEL_STRING));

        // Create UI elements
        listModel = new DefaultListModel<>();
        historyList = new JList<>(listModel);
        historyList.setCellRenderer(new MessageCellRenderer());
        JScrollPane scrollPane = new JScrollPane(historyList);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        txtMessage = new JTextField();
        txtMessage.addActionListener(_ -> btnSend.doClick());

        btnSend = new JButton("Send");

        // Create a new panel for EAST side elements
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.add(btnSend, BorderLayout.CENTER);
        eastPanel.add(modelComboBox, BorderLayout.EAST);

        // Add elements to panel and frame
        panel.add(txtMessage, BorderLayout.CENTER);
        panel.add(eastPanel, BorderLayout.EAST);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);

        // Add a settings menu
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(_ -> {
            JTextField tokenField = new JTextField(10);
            tokenField.setText(bearerToken);
            JTextField refererField = new JTextField(10);
            refererField.setText(refererUrl);
            JTextField apiField = new JTextField(10);
            apiField.setText(apiUrl);
            JTextField promptField = new JTextField(10);
            promptField.setText(SYSTEM_PROMPT);
            JTextField assistantPrefillField = new JTextField(10); // Add this field
            assistantPrefillField.setText(ASSISTANT_PREFILL); // Add this line to set the initial value
            JCheckBox historyCheckBox = new JCheckBox("Use Conversation History", useConversationHistory);

            JPanel myPanel = new JPanel();
            myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
            myPanel.add(new JLabel("Bearer Token:"));
            myPanel.add(tokenField);
            myPanel.add(new JLabel("Referer URL:"));
            myPanel.add(refererField);
            myPanel.add(new JLabel("API URL:"));
            myPanel.add(apiField);
            myPanel.add(new JLabel("System Prompt:"));
            myPanel.add(promptField);
            myPanel.add(new JLabel("Assistant Prefill:")); // Add this line
            myPanel.add(assistantPrefillField); // Add this line
            myPanel.add(historyCheckBox);

            int result = JOptionPane.showConfirmDialog(null, myPanel, "Settings", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                saveSettings(tokenField.getText(), refererField.getText(), apiField.getText(), promptField.getText(), historyCheckBox.isSelected(), assistantPrefillField.getText()); // Update this line to save the assistant prefill
                loadSettings(); // Update this line to reload the settings after they're saved
                api = new OmniAPI(bearerToken);
            }
        });
        JMenuItem exportItem = new JMenuItem("Export as JSON");
        exportItem.addActionListener(_ -> exportChatHistory());
        JMenuItem loadItem = new JMenuItem("Load from JSON");
        loadItem.addActionListener(_ -> loadChatHistory());
        menu.add(settingsItem);
        menu.add(exportItem);
        menu.add(loadItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        // Add a listener for the send button
        btnSend.addActionListener(_ -> {
            String message = txtMessage.getText().trim();
            String modelString = getKeyFromValue(modelMap, (String) modelComboBox.getSelectedItem());

            if (!message.isEmpty()) {
                // Add user message to conversation history
                Message userMessage = new Message("user", message);
                listModel.addElement(userMessage);
                conversationHistory.put(userMessage.toJSON());
                txtMessage.setText("");

                // Send message and get response
                new Thread(() -> {
                    String response;
                    if (!ASSISTANT_PREFILL.isEmpty()) {
                        // Add prefill to JSON
                        conversationHistory.put(new Message("assistant", ASSISTANT_PREFILL).toJSON());
                        // Get response
                        response = api.sendMessage(conversationHistory, modelString, refererUrl, apiUrl, "<tool><\\{([^,]+),([^>]+)}>", false);
                        // Remove prefill
                        conversationHistory.remove(conversationHistory.length() - 1);
                        // Tool call parameters
                        String[] toolCall;
                        // Check for tool call
                        if (response.contains("#!#!#")) {
                            // If tool call detected, split it and get name (0), parameters (1), and partial response (2)
                            toolCall = response.split("#!#!#");
                            // Get return value of tool with params
                            String toolResponse = null;
                            try {
                                toolResponse = callFunction(toolCall[0], toolCall[1]);
                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            // Place the assistant response into JSON conversation history, but remove tool call
                            conversationHistory.put(new Message("assistant", toolCall[2].replace(STR."<tool><{\{toolCall[0]}\{toolCall[1]}}>", "")).toJSON()); // Partial assistant message
                            // Place the response of the tool call into conversationHistory, but as a function role
                            conversationHistory.put(new Message("function", toolResponse).toJSON());
                            // Add assistant message into list to maintain sync
                            listModel.addElement(new Message("assistant", toolCall[2].replace(STR."<tool><{\{toolCall[0]}\{toolCall[1]}}>", "")));
                            listModel.addElement(new Message("function", STR."Ran \{toolCall[0]}"));
                            // Add prefill to JSON
                            conversationHistory.put(new Message("assistant", ASSISTANT_PREFILL).toJSON());
                            // Assistant generates response based on tool call
                            response = api.sendMessage(conversationHistory, modelString, refererUrl, apiUrl, "<tool><\\{([^,]+),([^>]+)}>", true);
                            // Remove prefill
                            conversationHistory.remove(conversationHistory.length() - 1);
                        }
                        // Add Assistant prefill when adding to listElement
                        Message assistantMessage = new Message("assistant", ASSISTANT_PREFILL + response);
                        // Add into assistant tool response into listModel
                        listModel.addElement(assistantMessage);
                        // Place assistant tool response message into JSON, now we have User => Assistant => Function => Assistant => User next
                        conversationHistory.put(conversationHistory.length(), assistantMessage.toJSON());
                    } else {
                        // Get response
                        response = api.sendMessage(conversationHistory, modelString, refererUrl, apiUrl, "<tool><\\{([^,]+),([^>]+)}>", false);
                        // Tool call parameters
                        String[] toolCall;
                        // Check for tool call
                        if (response.contains("#!#!#")) {
                            // If tool call detected, split it and get name (0), parameters (1), and partial response (2)
                            toolCall = response.split("#!#!#");
                            // Get return value of tool with params
                            String toolResponse = null;
                            try {
                                toolResponse = callFunction(toolCall[0], toolCall[1]);
                            } catch (IOException | InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            // Place the assistant response into JSON conversation history, but remove tool call
                            conversationHistory.put(new Message("assistant", toolCall[2].replace(STR."<tool><{\{toolCall[0]}\{toolCall[1]}}>", "")).toJSON()); // Partial assistant message
                            // Place the response of the tool call into conversationHistory, but as a function role
                            conversationHistory.put(new Message("function", toolResponse).toJSON());
                            // Add assistant message into list to maintain sync
                            listModel.addElement(new Message("assistant", toolCall[2].replace(STR."<tool><{\{toolCall[0]}\{toolCall[1]}}>", "")));
                            listModel.addElement(new Message("function", STR."Ran \{toolCall[0]}"));
                            // Assistant generates response based on tool call
                            response = api.sendMessage(conversationHistory, modelString, refererUrl, apiUrl, "none", true);
                            System.out.println(response);
                        }
                        // Add refined assistant into Message object
                        Message assistantMessage = new Message("assistant", response);
                        // Sync with listModel
                        listModel.addElement(assistantMessage);
                        // Add refined assistant message into conversation history
                        conversationHistory.put(conversationHistory.length(), assistantMessage.toJSON());
                    }
                }).start();
            }
        });

        // Add edit and delete buttons under each message
        historyList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = historyList.locationToIndex(e.getPoint());
                    historyList.setSelectedIndex(index);

                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem editItem = new JMenuItem("Edit");
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    popupMenu.add(editItem);
                    popupMenu.add(deleteItem);

                    editItem.addActionListener(_ -> {
                        // Implement edit functionality
                        int selectedIndex = historyList.getSelectedIndex();
                        if (selectedIndex != -1) {
                            String newContent = JOptionPane.showInputDialog(frame, "Edit message:", listModel.getElementAt(selectedIndex).getContent());
                            if (newContent != null && !newContent.isEmpty()) {
                                // Update the message content in the listModel
                                listModel.getElementAt(selectedIndex).setContent(newContent);
                                // Update the corresponding message in the conversationHistory array
                                conversationHistory.getJSONObject(selectedIndex + 1).put("content", newContent);
                            }
                        }
                    });

                    deleteItem.addActionListener(_ -> {
                        // Implement delete functionality
                        int selectedIndex = historyList.getSelectedIndex();
                        if (selectedIndex != -1) {
                            Message selectedMessage = listModel.getElementAt(selectedIndex);
                            if (selectedMessage.getRole().equals("assistant")) {
                                // If the model is not Gemini Pro 1.5, only remove the assistant message
                                listModel.remove(selectedIndex); // Remove the assistant message
                                conversationHistory.remove(selectedIndex + 1); // Remove the assistant message from the conversationHistory array
                            }
                            if (selectedMessage.getRole().equals("user")) {
                                // If the model is not Gemini Pro 1.5, only remove the assistant message
                                listModel.remove(selectedIndex); // Remove the assistant message
                                conversationHistory.remove(selectedIndex + 1); // Remove the assistant message from the conversationHistory array
                            }
                        }
                    });

                    popupMenu.show(historyList, e.getX(), e.getY());
                }
            }
        });

        // Display the window.
        frame.setVisible(true);
    }

    private String getKeyFromValue(Map<String, String> map, String value) {
        for (String key : map.keySet()) {
            if (map.get(key).equals(value)) {
                return key;
            }
        }
        return null;
    }

    public String callFunction(String name, String parameters) throws IOException, InterruptedException {
        if (name.equals("webSearch")) {
            try {
                String encodedQuery = URLEncoder.encode(parameters, StandardCharsets.UTF_8);
                String url = STR."https://www.googleapis.com/customsearch/v1?key=\{API_KEY}&cx=\{SEARCH_ENGINE_ID}&q=\{encodedQuery}";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return response.body();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (name.equals("pythonExec")) {
            return "Not implemented yet";
        }
        if (name.equals("imageGen")) {
            HttpClient client = HttpClient.newHttpClient();

            String apiKey = "USER_PROVIDED";
            String authorization = "Bearer " + bearerToken;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.omnigpt.co/functions/v1/image-generation"))
                    .header("accept", "*/*")
                    .header("accept-language", "en-US,en;q=0.9")
                    .header("apikey", apiKey)
                    .header("authorization", authorization)
                    .header("cache-control", "no-cache")
                    .header("content-type", "application/json")
                    .header("pragma", "no-cache")
                    .header("priority", "u=1, i")
                    .header("sec-ch-ua", "\"Not-A.Brand\";v=\"99\", \"Chromium\";v=\"124\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"Windows\"")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-site")
                    .header("x-client-info", "supabase-ssr/0.1.0")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"prompt\":\"" + parameters + "\",\"model\":\"dall-e-3\"}"))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return String.valueOf(response);
        }
        return "";
    }

    public static String parseImageURL(String jsonResponse) {
        System.out.println(jsonResponse);
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        if (!dataArray.isEmpty()) {
            JSONObject dataObject = dataArray.getJSONObject(0);
            return dataObject.getString("url");
        }

        return null; // Return null if no image URL found
    }

    public String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static class Message {
        private final String role;
        private String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("role", role);
            json.put("content", content);
            return json;
        }
    }

    private class MessageCellRenderer extends JPanel implements ListCellRenderer<Message> {
        private final JLabel label;

        public MessageCellRenderer() {
            setLayout(new BorderLayout());
            label = new JLabel();
            add(label, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends Message> list, Message message, int index, boolean isSelected, boolean cellHasFocus) {
            // Parse Markdown content to HTML
            String htmlContent = parseMarkdownToHTML(message.getContent());

            // Set HTML content with line breaks for newline characters
            label.setText(STR."<html><body style='width: 300px'>\{capitalize(message.getRole())}: \{htmlContent}</body></html>");

            // Set background and foreground colors
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            // Set other properties
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }

        private String parseMarkdownToHTML(String markdownContent) {
            // Create a CommonMark parser
            Parser parser = Parser.builder().build();

            // Parse Markdown content
            Node document = parser.parse(markdownContent);

            // Render HTML from the parsed Markdown content
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            return renderer.render(document);
        }
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new ChatApp().createAndShowGUI());
    }
}
