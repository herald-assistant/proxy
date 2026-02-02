import express from "express";
import { CopilotClient } from "@github/copilot-sdk";
import crypto from "node:crypto";
import fs from "node:fs/promises";
import path from "node:path";

const PORT = process.env.PORT ? Number(process.env.PORT) : 8788;

// Debug/ops flags
const DEBUG_ERRORS = process.env.DEBUG_ERRORS === "1";          // include debug payload in HTTP errors
const ALLOW_LOGGED_IN_USER = process.env.ALLOW_LOGGED_IN_USER === "1"; // allow authless requests -> useLoggedInUser
const COPILOT_LOG_LEVEL = process.env.COPILOT_LOG_LEVEL ?? "debug";
const COPILOT_LOG_DIR = process.env.COPILOT_LOG_DIR ?? path.resolve(process.cwd(), ".copilot-logs");

// Encourage Copilot CLI logging (supported by Copilot CLI ACP server docs) :contentReference[oaicite:3]{index=3}
process.env.COPILOT_LOG_LEVEL = COPILOT_LOG_LEVEL;
process.env.COPILOT_LOG_DIR = COPILOT_LOG_DIR;

const app = express();
app.use(express.json({ limit: "1mb" }));

app.use((req, _res, next) => {
    req.reqId = req.header("x-request-id") || crypto.randomUUID();
    next();
});

app.get("/healthz", (_req, res) => res.status(200).send("ok"));

function getBearerToken(req) {
    const h = req.header("authorization") || req.header("Authorization");
    if (!h) return null;
    const m = h.match(/^Bearer\s+(.+)$/i);
    if (!m) return null;
    const token = (m[1] ?? "").trim();
    return token.length ? token : null;
}

function openAiError(status, message, code, param, debug) {
    const body = {
        error: {
            message,
            type: "invalid_request_error",
            code: code ?? "invalid_request",
            param: param ?? null,
        },
    };
    if (DEBUG_ERRORS && debug) body.debug = debug;
    return { status, body };
}

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

/** Mask secrets in logs/debug */
function redactSecrets(s) {
    if (s == null) return s;
    let out = String(s);

    // Authorization headers / Bearer tokens
    out = out.replace(/Authorization:\s*Bearer\s+[A-Za-z0-9_\-\.=]+/gi, "Authorization: Bearer [REDACTED]");
    out = out.replace(/Bearer\s+[A-Za-z0-9_\-\.=]+/gi, "Bearer [REDACTED]");

    // GitHub tokens often start with ghp_, github_pat_, ghu_, gh* etc.
    out = out.replace(/\b(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{10,}\b/g, "$1_[REDACTED]");
    out = out.replace(/\bgithub_pat_[A-Za-z0-9_]{10,}\b/g, "github_pat_[REDACTED]");

    return out;
}

/** Safe error serialization incl. nested response/data fields */
function serializeError(err) {
    const e = err ?? {};
    const base = {
        name: e.name,
        message: redactSecrets(e.message),
        stack: redactSecrets(e.stack),
        code: e.code,
        status: e.status ?? e.statusCode,
    };

    // Copy enumerable props (often where SDKs stash "response", "data", "stderr")
    const extra = {};
    for (const k of Object.keys(e)) {
        try {
            const v = e[k];
            extra[k] =
                typeof v === "string" ? redactSecrets(v) :
                    v && typeof v === "object" ? v :
                        v;
        } catch {
            // ignore
        }
    }

    // If error has response-like shape
    const responseLike =
        e.response
            ? {
                status: e.response.status,
                statusText: e.response.statusText,
                headers: e.response.headers,
                data: typeof e.response.data === "string" ? redactSecrets(e.response.data) : e.response.data,
                body: typeof e.response.body === "string" ? redactSecrets(e.response.body) : e.response.body,
            }
            : null;

    return { ...base, ...extra, response: responseLike ?? extra.response };
}

async function ensureDir(p) {
    try { await fs.mkdir(p, { recursive: true }); } catch { /* noop */ }
}

async function tailLatestCopilotLog(maxBytes = 20_000) {
    try {
        await ensureDir(COPILOT_LOG_DIR);
        const files = await fs.readdir(COPILOT_LOG_DIR);
        if (!files.length) return null;

        // pick newest by mtime
        const stats = await Promise.all(
            files.map(async (f) => {
                const full = path.join(COPILOT_LOG_DIR, f);
                const st = await fs.stat(full);
                return { full, mtimeMs: st.mtimeMs, isFile: st.isFile() };
            })
        );

        const newest = stats
            .filter(x => x.isFile)
            .sort((a, b) => b.mtimeMs - a.mtimeMs)[0];

        if (!newest) return null;

        const buf = await fs.readFile(newest.full);
        const slice = buf.length > maxBytes ? buf.subarray(buf.length - maxBytes) : buf;
        return redactSecrets(slice.toString("utf8"));
    } catch {
        return null;
    }
}

function buildCopilotClient({ token }) {
    if (token) {
        return { client: new CopilotClient({ githubToken: token, useLoggedInUser: false }), mode: "bearer-token" };
    }
    if (ALLOW_LOGGED_IN_USER) {
        return { client: new CopilotClient({ useLoggedInUser: true }), mode: "logged-in-user" };
    }
    return { client: null, mode: "none" };
}

function logJson(obj) {
    // structured logs (easy to grep in k8s)
    console.log(JSON.stringify(obj));
}

/**
 * Minimal OpenAI models endpoint
 */
app.get(["/models", "/v1/models"], async (req, res) => {
    const token = getBearerToken(req);
    const { client, mode } = buildCopilotClient({ token });

    if (!client) {
        const err = openAiError(
            401,
            "Missing Authorization: Bearer <token> (or set ALLOW_LOGGED_IN_USER=1 for server-side Copilot login).",
            "missing_api_key"
        );
        return res.status(err.status).json(err.body);
    }

    try {
        await ensureDir(COPILOT_LOG_DIR);
        await client.start();

        const models = await client.listModels();

        const data = (models ?? []).map((m) => ({
            id: m?.id ?? m?.model ?? m?.name ?? String(m),
            object: "model",
            owned_by: "github-copilot",
        }));

        return res.json({ object: "list", data });
    } catch (e) {
        const ser = serializeError(e);
        const cliLogTail = await tailLatestCopilotLog();
        logJson({ level: "error", reqId: req.reqId, route: "/models", mode, error: ser, cliLogTail });

        // 403 is very often policy/feature toggle (see issues) :contentReference[oaicite:4]{index=4}
        const status = ser.status === 403 ? 403 : 500;
        const msg =
            status === 403
                ? "Failed to list models: 403 Forbidden (policy/entitlement). Check Copilot settings/policies for this user."
                : (ser.message ?? "Unknown error");

        const err = openAiError(status, msg, status === 403 ? "forbidden" : "server_error", null, {
            reqId: req.reqId,
            mode,
            error: ser,
            copilotCliLogTail: cliLogTail,
            hint:
                "403 is commonly caused by Copilot CLI being disabled for the user (even if org is enabled). See https://github.com/settings/copilot/features (or enterprise policy settings).",
        });

        return res.status(err.status).json(err.body);
    } finally {
        await client?.stop?.().catch(() => void 0);
    }
});

/**
 * OpenAI-compatible Chat Completions endpoint.
 */
app.post("/chat/completions", async (req, res) => {
    const token = getBearerToken(req);
    const { client, mode } = buildCopilotClient({ token });

    if (!client) {
        const err = openAiError(
            401,
            "Missing Authorization: Bearer <token> (or set ALLOW_LOGGED_IN_USER=1 for server-side Copilot login).",
            "missing_api_key"
        );
        return res.status(err.status).json(err.body);
    }

    const { model, messages, stream } = req.body ?? {};

    if (stream === true) {
        const err = openAiError(400, "Streaming is not supported by this provider.", "unsupported_parameter", "stream");
        return res.status(err.status).json(err.body);
    }

    const chosenModel = (typeof model === "string" && model.trim()) ? model.trim() : "gpt-4.1";

    if (!Array.isArray(messages) || messages.length === 0) {
        const err = openAiError(400, "Field 'messages' must be a non-empty array.", "invalid_request", "messages");
        return res.status(err.status).json(err.body);
    }

    const prompt = messagesToPrompt(messages);
    if (!prompt.trim()) {
        const err = openAiError(400, "Messages produced an empty prompt.", "invalid_request", "messages");
        return res.status(err.status).json(err.body);
    }

    let session;
    try {
        await ensureDir(COPILOT_LOG_DIR);
        await client.start();

        session = await client.createSession({ model: chosenModel });

        // Don’t log prompt content (can contain sensitive data); log size + hash for correlation
        const promptHash = crypto.createHash("sha256").update(prompt).digest("hex").slice(0, 12);
        logJson({
            level: "info",
            reqId: req.reqId,
            route: "/chat/completions",
            mode,
            chosenModel,
            promptBytes: Buffer.byteLength(prompt, "utf8"),
            promptHash,
        });

        const resp = await session.sendAndWait({ prompt });

        const content =
            resp?.data?.content ??
            resp?.data?.message ??
            resp?.content ??
            resp?.data ??
            "";

        const nowSec = Math.floor(Date.now() / 1000);
        const id = `copilot-${crypto.randomUUID()}`;

        return res.json({
            id,
            object: "chat.completion",
            created: nowSec,
            model: chosenModel,
            choices: [
                {
                    index: 0,
                    message: { role: "assistant", content: String(content ?? "") },
                    finish_reason: "stop",
                },
            ],
            usage: null,
        });
    } catch (e) {
        const ser = serializeError(e);
        const cliLogTail = await tailLatestCopilotLog();

        logJson({ level: "error", reqId: req.reqId, route: "/chat/completions", mode, error: ser, cliLogTail });

        const status = ser.status === 403 ? 403 : (/unauthorized|forbidden|bad credentials|invalid token/i.test(ser.message ?? "") ? 401 : 500);
        const code = status === 401 ? "invalid_api_key" : (status === 403 ? "forbidden" : "server_error");

        const msg =
            status === 403
                ? "403 Forbidden (policy/entitlement). Copilot CLI/Chat may be disabled for this user by policy or feature toggle."
                : (ser.message ?? "Unknown error");

        const err = openAiError(status, msg, code, null, {
            reqId: req.reqId,
            mode,
            error: ser,
            copilotCliLogTail: cliLogTail,
            hint:
                "If this works only for Enterprise Managed Users, prefer logged-in mode (ALLOW_LOGGED_IN_USER=1) and authenticate Copilot CLI on the host. Also verify Copilot settings/policies for CLI/Chat.",
        });

        return res.status(err.status).json(err.body);
    } finally {
        await session?.stop?.().catch(() => void 0);
        await session?.dispose?.().catch(() => void 0);
        await client?.stop?.().catch(() => void 0);
    }
});

/**
 * Optional debug endpoint: last Copilot CLI log tail.
 * Protect in real deployments (network policy / auth).
 */
app.get("/debug/copilot/logs", async (req, res) => {
    if (!DEBUG_ERRORS) return res.status(404).send("not found");
    const tail = await tailLatestCopilotLog();
    return res.json({ reqId: req.reqId, logDir: COPILOT_LOG_DIR, tail });
});

app.listen(PORT, () => {
    console.log(`copilot-proxy listening on :${PORT}`);
    console.log(`DEBUG_ERRORS=${DEBUG_ERRORS ? "1" : "0"}, ALLOW_LOGGED_IN_USER=${ALLOW_LOGGED_IN_USER ? "1" : "0"}`);
    console.log(`COPILOT_LOG_LEVEL=${COPILOT_LOG_LEVEL}, COPILOT_LOG_DIR=${COPILOT_LOG_DIR}`);
});
