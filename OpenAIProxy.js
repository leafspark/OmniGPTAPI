// Listen on a specific host via the HOST environment variable
var host = process.env.HOST || '0.0.0.0';
// Listen on a specific port via the PORT environment variable
var port = process.env.PORT || 8080;

var cors_proxy = require('cors-anywhere');
cors_proxy.createServer({
    originWhitelist: [], // Allow all origins
    requireHeader: ['origin', 'x-requested-with'],
    removeHeaders: ['cookie', 'cookie2']
}).listen(port, host, function() {
    console.log('Running CORS Anywhere on ' + host + ':' + port);
});

// This is a proxy from OmniGPT's Supabase endpoint to an OpenAI compatible /v1/chat/completions, /v1/audio/transcriptions, and /v1/images/generations. Responses are returned in OpenAI format, except chat completions which are returned in OpenRouter SSE format (comments stripped).

const express = require('express');
const axios = require('axios');
const FormData = require('form-data');
const multer = require('multer');
const app = express();
const PORT = 3000;

// Middleware to handle multipart/form-data
const upload = multer();

app.post('/v1/audio/transcriptions', upload.single('audio'), (req, res) => {
    const formData = new FormData();
    formData.append('audio', req.file.buffer, req.file.originalname);

    const headers = {
        "accept": "*/*",
        "accept-language": "en-US,en;q=0.9",
        "apikey": req.headers.apikey,
        "authorization": req.headers.authorization,
        "content-type": `multipart/form-data; boundary=${formData._boundary}`,
        "cache-control": "no-cache",
        "pragma": "no-cache",
        "priority": "u=1, i",
        "sec-ch-ua": "\"Not-A.Brand\";v=\"99\", \"Chromium\";v=\"124\"",
        "sec-ch-ua-mobile": "?0",
        "sec-ch-ua-platform": "\"Windows\"",
        "sec-fetch-dest": "empty",
        "sec-fetch-mode": "cors",
        "sec-fetch-site": "same-site",
        "x-client-info": "supabase-ssr/0.3.0"
    };

    axios.post('http://localhost:8080/https://api.omnigpt.co/functions/v1/whisper-transcribe', formData, { headers })
        .then(response => {
            res.status(response.status).send(response.data);
        })
        .catch(error => {
            console.error('Error forwarding request:', error);
            res.status(500).send('Failed to process audio transcription');
        });
});

app.post('/v1/images/generations', (req, res) => {
    const proxyUrl = 'http://localhost:8080/https://api.omnigpt.co/functions/v1/image-generation';
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': req.headers.authorization, // Pass authorization header
        'apikey': req.headers.apikey // Pass API key
    };

    axios.post(proxyUrl, req.body, { headers })
        .then(response => {
            res.status(response.status).send(response.data);
        })
        .catch(error => {
            console.error('Error forwarding request:', error);
            res.status(500).send('Failed to process image generation');
        });
});

// Middleware to parse JSON bodies
app.use(express.json());

// Handle POST request
app.post('/v1/chat/completions', (req, res) => {
    // Set headers for SSE
    res.writeHead(200, {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive',
    });

    const { messages, model } = req.body;
    const authHeader = req.headers.authorization;
    const apiUrl = 'http://localhost:8080/https://odjzvygjmpogtophihqs.functions.supabase.co/connect-llm';

    // Prepare the data to send
    const dataToSend = JSON.stringify({
        messages,
        gptModel: model
    });

    // Setup Axios to stream the response
    axios({
        method: 'post',
        url: apiUrl,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': authHeader,
            'Referer': 'http://localhost:3000',
            'Origin': 'http://localhost:3000',
            'X-Requested-With': 'XMLHttpRequest'
        },
        data: dataToSend,
        responseType: 'stream'
    }).then(response => {
        response.data.on('data', (chunk) => {
            const text = chunk.toString();
            // Split the stream into individual lines
            text.split('\n').forEach(line => {
                // Check if the line starts with "data:" to ensure it's actual data
                if (line.startsWith('data:')) {
                    // Forward each data line to the client as a SSE
                    res.write(`${line}\n\n`);
                }
                // Ignore lines starting with a colon (SSE comments)
            });
        });

        response.data.on('end', () => {
            res.write('event: end\ndata: \n\n');
            res.end();
        });

    }).catch(error => {
        console.error('Error in processing request:', error.message);
        res.write(`event: error\ndata: ${JSON.stringify({ message: 'Error in processing request' })}\n\n`);
        res.end();
    });
});

app.get('/v1/models', (req, res) => {
    res.json({
        object: "list",
        data: [
            {
                id: "gpt-3.5-turbo",
                object: "model",
                created: 0,
                owned_by: "OPENAI"
            },
            {
                id: "gpt-4-turbo",
                object: "model",
                created: 0,
                owned_by: "OPENAI"
            },
            {
                id: "gpt-4",
                object: "model",
                created: 0,
                owned_by: "OPENAI"
            },
            {
                id: "llama-2-70b-chat-hf",
                object: "model",
                created: 0,
                owned_by: "FACEBOOK"
            },
            {
                id: "llama-2-13b-chat-hf",
                object: "model",
                created: 0,
                owned_by: "FACEBOOK"
            },
            {
                id: "llama-2-7b-chat-hf",
                object: "model",
                created: 0,
                owned_by: "FACEBOOK"
            },
            {
                id: "llama-3-8b-instruct",
                object: "model",
                created: 0,
                owned_by: "FACEBOOK"
            },
            {
                id: "llama-3-70b-instruct",
                object: "model",
                created: 0,
                owned_by: "FACEBOOK"
            },
            {
                id: "mistral-7b-v0.1",
                object: "model",
                created: 0,
                owned_by: "MISTRAL"
            },
            {
                id: "mistral-8x22b",
                object: "model",
                created: 0,
                owned_by: "MISTRAL"
            },
            {
                id: "mistral-8x22b-instruct",
                object: "model",
                created: 0,
                owned_by: "MISTRAL"
            },
            {
                id: "gemini-pro-1.5",
                object: "model",
                created: 0,
                owned_by: "GOOGLE"
            },
            {
                id: "claude2.0",
                object: "model",
                created: 0,
                owned_by: "ANTHROPIC"
            },
            {
                id: "claude2.1",
                object: "model",
                created: 0,
                owned_by: "ANTHROPIC"
            },
            {
                id: "claude_3_sonnet",
                object: "model",
                created: 0,
                owned_by: "ANTHROPIC"
            },
            {
                id: "claude_3_opus",
                object: "model",
                created: 0,
                owned_by: "ANTHROPIC"
            },
            {
                id: "claude_3_haiku",
                object: "model",
                created: 0,
                owned_by: "ANTHROPIC"
            },
            {
                id: "perplexity",
                object: "model",
                created: 0,
                owned_by: "PERPLEXITY"
            },
            {
                id: "perplexity-sonar-7b-online",
                object: "model",
                created: 0,
                owned_by: "PERPLEXITY"
            },
            {
                id: "perplexity-sonar-8x7b-online",
                object: "model",
                created: 0,
                owned_by: "PERPLEXITY"
            },
            {
                id: "dall-e-2",
                object: "model",
                created: 0,
                owned_by: "OPENAI"
            },
            {
                id: "dall-e-3",
                object: "model",
                created: 0,
                owned_by: "OPENAI"
            }
        ]
    });
});

app.listen(PORT, () => {
    console.log(`OpenAI Proxy Server running on http://localhost:${PORT}`);
});
