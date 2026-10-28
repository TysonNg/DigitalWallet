import test from "node:test";
import assert from "node:assert/strict";
import {
    streamAiChat,
    type StreamCallbacks,
} from "../src/features/ai-assistant/services/ai-stream";

function callbacks() {
    const events: unknown[] = [];
    const handlers: StreamCallbacks = {
        onToken: (token) => events.push(["token", token]),
        onDraftTransfer: (draft) => events.push(["draft", draft]),
        onError: (error) => events.push(["error", error]),
        onDone: () => events.push(["done"]),
    };

    return { events, handlers };
}

function response(source: string, fragmentSize = 1024) {
    const bytes = new TextEncoder().encode(source);
    const body = new ReadableStream<Uint8Array>({
        start(controller) {
            for (let index = 0; index < bytes.length; index += fragmentSize) {
                controller.enqueue(bytes.slice(index, index + fragmentSize));
            }
            controller.close();
        },
    });

    return new Response(body, { headers: { "Content-Type": "text/event-stream" } });
}

test("fragmented CRLF and UTF-8 deliver token, draft and done exactly once", async (context) => {
    const draft = { recipientWalletId: "recipient", amount: 50000, note: "  Tiền ăn trưa  " };
    const res = response(
        ': heartbeat\r\n\r\nevent: token\r\ndata: {"content":"Chào bạn"}\r\n\r\n' +
            `event: draft_transfer\r\ndata: ${JSON.stringify(draft)}\r\n\r\n` +
            "event: done\r\ndata: {}\r\n\r\nevent: done\r\ndata: {}\r\n\r\n",
        1
    );
    const fetchMock = context.mock.method(globalThis, "fetch", async () => res);
    const { events, handlers } = callbacks();
    const signal = new AbortController().signal;
    await streamAiChat("hello", "conversation", handlers, signal);
    assert.deepEqual(events, [["token", "Chào bạn"], ["draft", draft], ["done"]]);
    assert.equal(res.body!.locked, false);
    const [url, options] = fetchMock.mock.calls[0].arguments as unknown as [string, RequestInit];
    assert.equal(url, "/api/ai/chat/stream");
    assert.deepEqual(JSON.parse(options.body as string), {
        message: "hello",
        conversationId: "conversation",
    });
    assert.equal(options.signal, signal);
});

test("LF done without data completes once and releases the reader", async (context) => {
    const res = response('event: token\ndata: {"content":"hello"}\n\nevent: done\n\n', 3);
    context.mock.method(globalThis, "fetch", async () => res);
    const { events, handlers } = callbacks();
    await streamAiChat("hello", "conversation", handlers);
    assert.deepEqual(events, [["token", "hello"], ["done"]]);
    assert.equal(res.body!.locked, false);
});

test("a done event with data calls onDone only once", async (context) => {
    const res = response("event: done\ndata: {}\n\n");
    context.mock.method(globalThis, "fetch", async () => res);
    const { events, handlers } = callbacks();
    await streamAiChat("hello", "conversation", handlers);
    assert.deepEqual(events, [["done"]]);
    assert.equal(res.body!.locked, false);
});

test("server error rejects before tokens and after partial output", async (context) => {
    for (const prefix of ["", 'event: token\ndata: {"content":"partial"}\n\n']) {
        for (const errorData of ['{"error":"AI unavailable"}', "AI unavailable"]) {
            const res = response(
                prefix + `event: error\ndata: ${errorData}\n\nevent: done\ndata: {}\n\n`
            );
            context.mock.method(globalThis, "fetch", async () => res);
            const { events, handlers } = callbacks();
            await assert.rejects(streamAiChat("hello", "conversation", handlers), /AI unavailable/);
            assert.deepEqual(events, [
                ...(prefix ? [["token", "partial"]] : []),
                ["error", "AI unavailable"],
            ]);
            assert.equal(res.body!.locked, false);
        }
    }
});

test("EOF without a complete done event rejects instead of reporting success", async (context) => {
    for (const suffix of ["", "event: done\ndata: {}", "event: done\r\n"]) {
        const res = response('event: token\ndata: {"content":"partial"}\n\n' + suffix, 2);
        context.mock.method(globalThis, "fetch", async () => res);
        const { events, handlers } = callbacks();
        await assert.rejects(streamAiChat("hello", "conversation", handlers), /before.*done/i);
        assert.deepEqual(events, [["token", "partial"]]);
        assert.equal(res.body!.locked, false);
    }
});

test("empty EOF rejects and releases the reader", async (context) => {
    const res = response("");
    context.mock.method(globalThis, "fetch", async () => res);
    const { events, handlers } = callbacks();
    await assert.rejects(streamAiChat("hello", "conversation", handlers), /before.*done/i);
    assert.deepEqual(events, []);
    assert.equal(res.body!.locked, false);
});

test("read failures release the reader without calling onDone", async (context) => {
    const body = new ReadableStream<Uint8Array>({
        pull(controller) {
            controller.error(new Error("connection lost"));
        },
    });
    context.mock.method(globalThis, "fetch", async () => new Response(body));
    const { events, handlers } = callbacks();
    await assert.rejects(streamAiChat("hello", "conversation", handlers), /connection lost/);
    assert.deepEqual(events, []);
    assert.equal(body.locked, false);
});

test("done cancels the response body instead of leaving upstream open", async (context) => {
    let cancelled = false;
    const body = new ReadableStream<Uint8Array>({
        start(controller) {
            controller.enqueue(new TextEncoder().encode("event: done\ndata: {}\n\n"));
        },
        cancel() {
            cancelled = true;
        },
    });
    context.mock.method(globalThis, "fetch", async () => new Response(body));
    const { handlers } = callbacks();
    await streamAiChat("hello", "conversation", handlers);
    assert.equal(cancelled, true);
    assert.equal(body.locked, false);
});
