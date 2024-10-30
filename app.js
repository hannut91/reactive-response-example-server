const http2 = require('node:http2');
const fs = require('node:fs');

const client = http2.connect('http://localhost:9000');

const timer = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

client.on('error', (err) => {
    console.error(err);
});

const req = client.request({
    // ':path': '/hello3',
    ':path': '/response/mono/void',
    ':method': 'POST',
    'content-type': 'application/x-ndjson'
});
req.setEncoding('utf8');

let requestSamples = [
    {name: "John Doe"},
    {name: "Jane Doe"},
    {name: "John Smith"},
    {name: "Jane Smith"},
    {name: "John Johnson"},
    {name: "Jane Johnson"},
];

(async () => {
    for (let i of requestSamples) {
        req.write(JSON.stringify(i));
        console.log(`Sent: ${JSON.stringify(i)}`)
        await timer(2000);
    }
    req.end();
})();

req.on('response', (headers, flags) => {
    for (const name in headers) {
        console.log(`${name}: ${headers[name]}`);
    }
});

let data = '';
req.on('data', (chunk) => {
    console.log('chunk: ', chunk);
    data += chunk;
});

req.on('end', () => {
    console.log(`\n${data}`);
    client.close();
});
