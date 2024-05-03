const express = require('express');
const axios = require('axios');
const cors_proxy = require('cors-anywhere');
const { Transform } = require('stream');

// Start CORS Anywhere proxy
cors_proxy.createServer({
  originWhitelist: [],
  requireHeader: ['origin', 'x-requested-with'],
  removeHeaders: ['cookie', 'cookie2']
}).listen(45523, '0.0.0.0', () => {
  console.log('CORS Anywhere proxy listening on port 45523');
});

const app = express();

app.use(express.json());

app.get('/v1/models', (req, res) => {
  const models = {
    object: 'list',
    data: [
      {
        id: 'gemini-1.5-pro',
        object: 'model',
        created: 1686935002,
        owned_by: 'google'
      },
      {
        id: 'gpt-3.5-turbo',
        object: 'model',
        created: 1686935002,
        owned_by: 'openai'
      },
      {
        id: 'claude_3_sonnet',
        object: 'model',
        created: 1686935002,
        owned_by: 'anthropic'
      },
      {
        id: 'claude_3_opus',
        object: 'model',
        created: 1686935002,
        owned_by: 'anthropic'
      },
      {
        id: 'claude_3_haiku',
        object: 'model',
        created: 1686935002,
        owned_by: 'anthropic'
      },
      {
        id: 'gpt-4-turbo',
        object: 'model',
        created: 1686935002,
        owned_by: 'openai'
      }
    ]
  };

  res.json(models);
});

app.post('/v1/chat/completions', (req, res) => {
  try {
    const { model, messages } = req.body;

    const stream = new Transform({
      transform(chunk, enc, cb) {
        const data = JSON.parse(chunk);
        cb(null, JSON.stringify(data) + '\n');
      }
    });

    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Connection', 'keep-alive');
    res.setHeader('Cache-Control', 'no-cache');

    stream.pipe(res);

    axios({
      method: 'post',
      url: `http://localhost:45523/https://odjzvygjmpogtophihqs.functions.supabase.co/connect-llm`,
      data: {
        messages,
        gptModel: model
      },
      responseType: 'stream'
    }).then(apiRes => {
      apiRes.data.pipe(stream);
    });
  } catch (error) {
    console.log(error);
    res.status(500).end();
  }
});

app.listen(3000, () => {
  console.log('Express server running on port 3000');
});
