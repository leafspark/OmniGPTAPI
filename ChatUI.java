import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
import org.json.JSONArray;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class ChatUI {
    private JFrame frame;
    private JEditorPane textPane;
    private JTextField textField;
    private JComboBox<String> comboBox, roleBox;
    private OmniAPI apiClient;
    private static final String[] MODELS = {
            "claude_3_haiku", "claude_3_opus", "claude_3_sonnet", "gpt-3.5-turbo-16k",
            "gpt-3.5-turbo", "llama-2-70b-chat-hf", "gpt-4", "gpt-4-turbo",
            "gemini-pro-1.5", "mistral-8x22b-instruct", "llama-3-70b-instruct",
            "llama-3-8b-instruct", "claude2.1", "perplexity-70b-online", "perplexity"
    };
    private static final String[] ROLES = {"User", "Assistant"};
    private JSONArray conversationHistory = new JSONArray();
    private static final Path CONVERSATION_DIR = Paths.get(System.getProperty("user.home"), "Desktop", "OmniClient");
    private String SYSTEM_PROMPT = "You are a helpful assistant.";
    private JMenuItem settingsMenuItem;
    private String MODEL_STRING = "";

    public ChatUI() {
        frame = new JFrame("Chat Client");
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        settingsMenuItem = new JMenuItem("Configure");
        settingsMenuItem.addActionListener(e -> showSettingsDialog());
        settingsMenu.add(settingsMenuItem);
        menuBar.add(settingsMenu);
        frame.setJMenuBar(menuBar);
        textPane = new JEditorPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textPane);

        textField = new JTextField();
        comboBox = new JComboBox<>(MODELS);
        roleBox = new JComboBox<>(ROLES);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textField, BorderLayout.CENTER);
        panel.add(comboBox, BorderLayout.EAST);
        panel.add(roleBox, BorderLayout.WEST);
        JButton regenerateButton = new JButton("Regenerate Response");
        regenerateButton.addActionListener(e -> regenerateResponse());
        panel.add(regenerateButton, BorderLayout.NORTH);
        JButton editLastButton = new JButton("Edit Last");
        editLastButton.addActionListener(e -> editLastMessage());
        panel.add(editLastButton, BorderLayout.NORTH);

        JButton deleteLastButton = new JButton("Delete Last");
        deleteLastButton.addActionListener(e -> deleteLastMessage());
        panel.add(deleteLastButton, BorderLayout.NORTH);
        JButton replyWithModelButton = new JButton("Reply with New Model");
        replyWithModelButton.addActionListener(e -> replyWithNewModel());
        panel.add(replyWithModelButton, BorderLayout.NORTH);


        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);

        apiClient = new OmniAPI("none");
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "assistant");
        systemMessage.put("content", SYSTEM_PROMPT);
        conversationHistory.put(systemMessage);

        textField.addActionListener(e -> {
            String role = (String) roleBox.getSelectedItem();
            String content = textField.getText();
            if (role != null && !content.isEmpty()) {
                appendText(role, content);
                textField.setText("");

                // Create a JSON object for the current message and add it to the history
                JSONObject message = new JSONObject();
                message.put("role", role.toLowerCase());
                message.put("content", content);
                conversationHistory.put(message);

                if (role.equals("User")) {
                    SwingWorker<String, Void> worker = new SwingWorker<>() {
                        @Override
                        protected String doInBackground() {
                            // Send the entire conversation history
                            if (!MODEL_STRING.isEmpty()) {
                                return apiClient.sendMessage(conversationHistory, MODEL_STRING, "https://app.omnigpt.co/", "http://localhost:8080/https://odjzvygjmpogtophihqs.functions.supabase.co/connect-llm");
                            }
                            return apiClient.sendMessage(conversationHistory, (String) comboBox.getSelectedItem(), "https://app.omnigpt.co/", "http://localhost:8080/https://odjzvygjmpogtophihqs.functions.supabase.co/connect-llm");
                        }

                        @Override
                        protected void done() {
                            try {
                                String response = get();
                                appendText("Assistant", response);
                                textPane.repaint();
                                // Add assistant's response to the conversation history
                                JSONObject assistantMessage = new JSONObject();
                                assistantMessage.put("role", "assistant");
                                assistantMessage.put("content", response);
                                conversationHistory.put(assistantMessage);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    };
                    worker.execute();
                }
            }
        });

        loadConversations();
        frame.setVisible(true);
    }

    private void replyWithNewModel() {
        String newModel = (String) comboBox.getSelectedItem();
        if (newModel != null && !newModel.isEmpty()) {
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return apiClient.sendMessage(conversationHistory, newModel, "https://app.omnigpt.co/", "http://localhost:8080/https://odjzvygjmpogtophihqs.functions.supabase.co/connect-llm");
                }

                @Override
                protected void done() {
                    try {
                        String response = get();
                        conversationHistory.put(conversationHistory.length(), response);
                        appendText("Replying with " + newModel, response);
                        textPane.repaint();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();
        }
    }

    public void appendText(String role, String content) {
        SwingUtilities.invokeLater(() -> {
            String htmlContent = markdownToHtml("**" + role + ":** " + content);
            String currentHtml = textPane.getText();
            if (currentHtml.isEmpty()) {
                currentHtml = "<html><body></body></html>";  // Initial HTML structure
            }
            int bodyEndTagIndex = currentHtml.lastIndexOf("</body>");
            String updatedHtml = currentHtml.substring(0, bodyEndTagIndex) + htmlContent + currentHtml.substring(bodyEndTagIndex);
            textPane.setText(updatedHtml);
        });
    }

    private void refreshConversationDisplay() {
        textPane.setText("");
        for (int i = 0; i < conversationHistory.length(); i++) {
            JSONObject message = conversationHistory.getJSONObject(i);
            appendText(message.getString("role"), message.getString("content"));
        }
    }

    private void regenerateResponse() {
        conversationHistory.remove(conversationHistory.length() - 1);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return apiClient.sendMessage(conversationHistory, MODEL_STRING, "https://app.omnigpt.co/", "http://localhost:8080/https://odjzvygjmpogtophihqs.functions.supabase.co/connect-llm");
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    appendText("Assistant", response);
                    textPane.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void editLastMessage() {
        if (conversationHistory.length() > 0) {
            JSONObject lastMessage = conversationHistory.getJSONObject(conversationHistory.length() - 1);
            String newText = JOptionPane.showInputDialog(frame, "Edit Message:", lastMessage.getString("content"));
            if (newText != null) {
                lastMessage.put("content", newText);
                refreshConversationDisplay();
            }
        }
    }

    private void deleteLastMessage() {
        if (conversationHistory.length() > 0) {
            conversationHistory.remove(conversationHistory.length() - 1);
            refreshConversationDisplay();
        }
    }

    private void showSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog(frame, OmniAPI.bearerToken, SYSTEM_PROMPT, MODEL_STRING, true);
        settingsDialog.setVisible(true);
        // Update settings based on user input
        OmniAPI.bearerToken = settingsDialog.getToken();
        SYSTEM_PROMPT = settingsDialog.getSystemPrompt();
        MODEL_STRING = settingsDialog.getModelString();
        // Assume markdownEnabled is used to control markdown rendering
    }
    public String markdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        String html = renderer.render(document);
        return html;
    }


    private void loadConversations() {
        try {
            File file = CONVERSATION_DIR.resolve("conversation.json").toFile();
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                JSONArray messages = new JSONArray(content);
                for (int i = 0; i < messages.length(); i++) {
                    JSONObject message = messages.getJSONObject(i);
                    appendText(message.getString("role"), message.getString("content"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatUI::new);
    }
}
