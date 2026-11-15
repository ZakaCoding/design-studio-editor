interface McpPersistenceCheckpoint {
    epoch: number;
    fileId: string;
}

interface McpOptions {
    /**
     * Save confirmation offered by the Penpot app (integrated remote MCP only).
     * `persistenceCheckpoint` marks the start of a task; `settlePersistence`
     * resolves once the writes made since then are saved and rejects when
     * that could not be confirmed.
     */
    persistenceCheckpoint?(): McpPersistenceCheckpoint;
    settlePersistence?(checkpoint: McpPersistenceCheckpoint): Promise<void>;
    getToken(): string;
    getServerUrl(): string;
    setMcpStatus(status: string);
    on(eventType: "disconnect" | "connect", cb: () => void);
}

declare global {
    const mcp: undefined | McpOptions;
}

export {};
