import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { Task } from "../TaskHandler.ts";
import { ExecuteCodeTaskHandler } from "./ExecuteCodeTaskHandler.ts";

// The handler reads the `penpot` and `mcp` globals of the plugin sandbox.
const g = globalThis as any;

let events: string[];
let sent: any[];

function installGlobals(mcp: any) {
    events = [];
    sent = [];
    g.penpot = {
        flags: { naturalChildOrdering: false, throwValidationErrors: false },
        // the script marks its own run
        mark: (name: string) => events.push(name),
        ui: {
            sendMessage: (message: any) => {
                events.push("response");
                sent.push(message.response);
            },
        },
    };
    g.mcp = mcp;
}

function integratedMcp(settle: (checkpoint: unknown) => Promise<void> = async () => {}) {
    return {
        persistenceCheckpoint: () => {
            events.push("checkpoint");
            return { id: 1 };
        },
        settlePersistence: async (checkpoint: unknown) => {
            events.push("settle");
            await settle(checkpoint);
        },
    };
}

const run = (code: string) => new ExecuteCodeTaskHandler().handle(new Task("req-1", "executeCode", { code }));

beforeEach(() => installGlobals(undefined));
afterEach(() => {
    delete g.penpot;
    delete g.mcp;
});

test("a settle that rejects replaces the result: no success is sent", async () => {
    installGlobals(
        integratedMcp(async () => {
            throw new Error("DS_SAVE_BLOCKED: Autosave failed");
        })
    );
    await assert.rejects(run(`penpot.mark("script"); return 42;`), /DS_SAVE_BLOCKED/);
    assert.deepEqual(events, ["checkpoint", "script", "settle"]);
    assert.deepEqual(sent, []);
});

test("a script that throws is still settled first, then its own error is raised", async () => {
    installGlobals(integratedMcp());
    await assert.rejects(run(`penpot.mark("script"); throw new Error("boom");`), /boom/);
    assert.deepEqual(events, ["checkpoint", "script", "settle"]);
    assert.deepEqual(sent, []);
});

test("a script that throws and a settle that rejects reports the save failure", async () => {
    installGlobals(
        integratedMcp(async () => {
            throw new Error("DS_SAVE_UNCONFIRMED");
        })
    );
    await assert.rejects(run(`throw new Error("boom");`), /DS_SAVE_UNCONFIRMED/);
});

test("a read-only task settles with its checkpoint and still succeeds", async () => {
    let settledWith: unknown;
    installGlobals(
        integratedMcp(async (checkpoint) => {
            settledWith = checkpoint;
        })
    );
    await run(`return "read";`);
    assert.deepEqual(settledWith, { id: 1 });
    assert.equal(sent.length, 1);
    assert.equal(sent[0].success, true);
    assert.equal(sent[0].data.result, "read");
});

test("an integrated MCP without the extension refuses before running the script", async () => {
    installGlobals({ persistenceCheckpoint: () => ({ id: 1 }) }); // settlePersistence missing
    await assert.rejects(run(`penpot.mark("script");`), /DS_SAVE_UNAVAILABLE/);
    installGlobals({ settlePersistence: async () => {} }); // persistenceCheckpoint missing
    await assert.rejects(run(`penpot.mark("script");`), /DS_SAVE_UNAVAILABLE/);
    assert.deepEqual(events, []);
    assert.deepEqual(sent, []);
});

test("without an integrated MCP the task runs as before and never settles", async () => {
    await run(`penpot.mark("script"); return 1;`);
    assert.deepEqual(events, ["script", "response"]);
});

test("call order: checkpoint, script, settle, then the response", async () => {
    installGlobals(
        integratedMcp(async () => {
            // the response must wait for the save: nothing was sent while settling
            await new Promise((resolve) => setTimeout(resolve, 5));
            events.push("settled");
        })
    );
    await run(`penpot.mark("script"); return 1;`);
    assert.deepEqual(events, ["checkpoint", "script", "settle", "settled", "response"]);
    assert.equal(sent[0].success, true);
});
