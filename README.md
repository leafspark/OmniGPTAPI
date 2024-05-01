# OmniGPTAPI
An unofficial OmniGPT API wrapper

# How to use

#### OmniAPI.java
```java
String sendMessage(String userInput, String MODEL_STRING, String REFERER_URL, String API_URL)
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
## Notes:
- This does not bypass usage limits
- May get ported to C++ and JavaScript if I have time
- No Markdown support due to terminal output
