import express from "express";
import {CopilotClient} from "@github/copilot-sdk";
import crypto from "node:crypto";

const PORT = process.env.PORT ? Number(process.env.PORT) : 8788;

const app = express();
app.use(express.json({limit: "1mb"}));

app.use((req, res, next) => {
    // Na POC: pozwól wszystkim (albo zawęź do konkretnego origin)
    res.setHeader("Access-Control-Allow-Origin", "*");
    res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
    res.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    if (req.method === "OPTIONS") return res.sendStatus(204);
    next();
});

app.get("/healthz", (_req, res) => res.status(200).send("ok"));

/**
 * Extracts Bearer token from Authorization header.
 * Accepts: "Authorization: Bearer <token>"
 */
function getBearerToken(req) {
    const h = req.header("authorization") || req.header("Authorization");
    if (!h) return null;

    const m = h.match(/^Bearer\s+(.+)$/i);
    if (!m) return null;

    const token = (m[1] ?? "").trim();
    return token.length ? token : null;
}

function openAiError(status, message, code, param) {
    return {
        status,
        body: {
            error: {
                message,
                type: "invalid_request_error",
                code: code ?? "invalid_request",
                param: param ?? null,
            },
        },
    };
}

/**
 * Converts OpenAI-style chat messages into a single prompt string.
 * (POC-friendly; preserves roles so the model gets some structure.)
 */
function messagesToPrompt(messages) {
    if (!Array.isArray(messages) || messages.length === 0) return "";

    return messages
        .map((m) => {
            const role = (m?.role ?? "user").toString();
            const content = (m?.content ?? "").toString();
            return `${role.toUpperCase()}:\n${content}`.trim();
        })
        .join("\n\n---\n\n");
}

/**
 * OpenAI-compatible models endpoint (minimal).
 * Your backend doesn't need it, but it's handy for admin/diagnostics.
 */
app.get(["/models", "/v1/models"], async (req, res) => {
    const token = getBearerToken(req);
    if (!token) {
        const err = openAiError(401, "Missing Authorization: Bearer <token>", "missing_api_key");
        return res.status(err.status).json(err.body);
    }

    let client;
    try {
        client = new CopilotClient({githubToken: token, useLoggedInUser: false});
        await client.start();

        const models = await client.listModels();

        // Return OpenAI-like "list"
        const data = (models ?? []).map((m) => ({
            id: m?.id ?? m?.model ?? m?.name ?? String(m),
            object: "model",
            owned_by: "github-copilot",
        }));

        return res.json({object: "list", data});
    } catch (e) {
        const msg = e?.message ?? "Unknown error";
        return res.status(500).json(openAiError(500, msg, "server_error").body);
    } finally {
        await client?.stop?.().catch(() => void 0);
    }
});

/**
 * OpenAI-compatible Chat Completions endpoint.
 * This is the key endpoint for your Java LlmProxyService.
 */
app.post("/chat/completions", async (req, res) => {
    const token = getBearerToken(req);
    if (!token) {
        const err = openAiError(401, "Missing Authorization: Bearer <token>", "missing_api_key");
        return res.status(err.status).json(err.body);
    }

    const {model, messages, stream} = req.body ?? {};

    // Your Java proxy forces stream=false; reject streaming explicitly to avoid surprises.
    if (stream === true) {
        const err = openAiError(400, "Streaming is not supported by this provider.", "unsupported_parameter", "stream");
        return res.status(err.status).json(err.body);
    }

    const chosenModel = (typeof model === "string" && model.trim()) ? model.trim() : "gpt-4.1";

    // Expect messages[] like ChatDtos.Message(role, content)
    if (!Array.isArray(messages) || messages.length === 0) {
        const err = openAiError(400, "Field 'messages' must be a non-empty array.", "invalid_request", "messages");
        return res.status(err.status).json(err.body);
    }

    const prompt = messagesToPrompt(messages);
    if (!prompt.trim()) {
        const err = openAiError(400, "Messages produced an empty prompt.", "invalid_request", "messages");
        return res.status(err.status).json(err.body);
    }

    let client;
    let session;

    try {
        client = new CopilotClient({githubToken: token, useLoggedInUser: false});
        client.options
        await client.start();

        session = await client.createSession({model: chosenModel});

        // Copilot SDK examples use sendAndWait({prompt}). :contentReference[oaicite:0]{index=0}
        const resp = await session.sendAndWait({prompt});

        const content =
            resp?.data?.content ??
            resp?.data?.message ??
            resp?.content ??
            resp?.data ??
            "";

        const nowSec = Math.floor(Date.now() / 1000);
        const id = `copilot-${crypto.randomUUID()}`;

        // OpenAI-like response shape (compatible with your ChatDtos.ChatResponse mapping)
        return res.json({
            id,
            object: "chat.completion",
            created: nowSec,
            model: chosenModel,
            choices: [
                {
                    index: 0,
                    message: {role: "assistant", content: String(content ?? "")},
                    finish_reason: "stop",
                },
            ],
            // Copilot SDK usually doesn't expose token usage; keep it null/omitted-safe.
            usage: null,
        });
    } catch (e) {
        const msg = e?.message ?? "Unknown error";

        // A few common auth-ish failures map nicely to 401 for your upstream exception logs.
        const status =
            /unauthorized|forbidden|bad credentials|invalid token/i.test(msg) ? 401 : 500;

        const code =
            status === 401 ? "invalid_api_key" : "server_error";

        return res.status(status).json(openAiError(status, msg, code).body);
    } finally {
        await session?.stop?.().catch(() => void 0);
        await session?.dispose?.().catch(() => void 0);
        await client?.stop?.().catch(() => void 0);
    }
});

app.listen(PORT, () => {
    console.log(`copilot-proxy listening on :${PORT}`);
});
