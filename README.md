# OmniGPTAPI
An unofficial OmniGPT API wrapper and Java app

# How to use

#### OmniAPI.java
```java
String sendMessage(String userInput, String MODEL_STRING, String REFERER_URL, String API_URL, String (Regex) FUNCTION_CALL, boolean ENABLE_REGEX)
```
```java
OmniAPI(String bearerToken)
```
Requirements:
```
npm install cors-anywhere
```
Run cors-anywhere server:
```
node app.js
```
#### ChatApp.java

Get Bearer token from OmniGPT website > DevTools, check connect-llm request (saves to your C:\ drive desktop). Get the API key and Search Engine ID from: [programmablesearchengine.google.com](programmablesearchengine.google.com) 

**Features:**
- Web Search
- Edit messages
- Delete messages
- Custom model
- CommonMark Markdown rendering
- Lightweight (Swing; no external libaries except CommonMark and JSON)
- Model Prefill (Starts model response with "Sure thing!" or non refusal; model continues response)
- Plugins; add your own in callFunctions()

## Notes:
- This does not bypass usage limits
- May get ported to C++ and JavaScript if I have time
- ~~No Markdown support due to terminal output~~Use ChatUI.java
- No error handling or debug output
- ~~Close/edit buttons do not appear~~Right click (ChatApp.java)
