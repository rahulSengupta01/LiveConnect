
"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";

import { Conversation } from "@/types/conversation";
import { Message } from "@/types/message";
import { getConversationMessages } from "@/services/messageService";

interface ChatWindowProps {
    conversation: Conversation | null;
    token: string;
    // onNewMessage: (message: Message) => void;
}

type ConnectionState = "connecting" | "connected" | "disconnected";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL;

function getUsernameFromToken(token: string): string | null {
    try {
        const payload = token.split(".")[1];

        if (!payload) return null;

        const base64 = payload
            .replace(/-/g, "+")
            .replace(/_/g, "/");

        const decoded = JSON.parse(atob(base64));

        return typeof decoded.sub === "string"
            ? decoded.sub
            : null;
    } catch {
        return null;
    }
}

function mergeMessages(
    current: Message[],
    incoming: Message[]
): Message[] {
    const messageMap = new Map<string, Message>();

    [...current, ...incoming].forEach((message) => {
        messageMap.set(message.id, message);
    });

    return Array.from(messageMap.values()).sort(
        (a, b) =>
            new Date(a.createdAt).getTime() -
            new Date(b.createdAt).getTime()
    );
}

export default function ChatWindow({
    conversation,
    token,
    // onNewMessage,
}: ChatWindowProps) {
    const conversationId = conversation?.conversationId ?? null;

    const [messages, setMessages] = useState<Message[]>([]);
    const [messageText, setMessageText] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [sendError, setSendError] = useState<string | null>(null);

    const [connectionState, setConnectionState] =
        useState<ConnectionState>("disconnected");

    const clientRef = useRef<Client | null>(null);
    const bottomRef = useRef<HTMLDivElement | null>(null);

    const currentUsername = getUsernameFromToken(token);

    useEffect(() => {
        if (conversationId === null || !token) {
            setMessages([]);
            setLoading(false);
            setConnectionState("disconnected");
            return;
        }

        const activeConversationId = conversationId;
        let cancelled = false;

        setMessages([]);
        setError(null);
        setSendError(null);
        setLoading(true);
        setConnectionState("connecting");

        const client = new Client({
            brokerURL: WS_URL || "ws://localhost:8080/ws",

            connectHeaders: {
                Authorization: `Bearer ${token}`,
            },

            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,

            onConnect: () => {
                if (cancelled) return;

                setConnectionState("connected");

                console.log(
                    "Connected to conversation:",
                    activeConversationId
                );

                client.subscribe(
                    `/topic/conversation/${activeConversationId}`,
                    (frame) => {
                        try {
                            const incomingMessage: Message =
                                JSON.parse(frame.body);

                            if (
                                incomingMessage.conversationId !==
                                activeConversationId
                            ) {
                                return;
                            }

                            // Update the message window.
                            setMessages((previous) =>
                                mergeMessages(previous, [
                                    incomingMessage,
                                ])
                            );

                            // Notify ChatPage to update the sidebar.
                            // onNewMessage(incomingMessage);

                        } catch (err) {
                            console.error(
                                "Failed to parse WebSocket message:",
                                err
                            );
                        }
                    }
                );
            },

            onStompError: (frame) => {
                console.error(
                    "STOMP error:",
                    frame.headers["message"],
                    frame.body
                );
            },

            onWebSocketClose: () => {
                if (!cancelled) {
                    setConnectionState("disconnected");
                }
            },

            onWebSocketError: (event) => {
                console.error("WebSocket error:", event);
            },
        });

        clientRef.current = client;
        client.activate();

        async function loadHistory() {
            try {
                const response = await getConversationMessages(
                    token,
                    activeConversationId,
                    0,
                    50
                );

                if (cancelled) return;

                setMessages((previous) =>
                    mergeMessages(previous, response.content)
                );
            } catch (err) {
                if (cancelled) return;

                setError(
                    err instanceof Error
                        ? err.message
                        : "Unable to load previous messages."
                );
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadHistory();

        return () => {
            cancelled = true;

            if (clientRef.current === client) {
                clientRef.current = null;
            }

            void client.deactivate();
        };
    }, [conversationId, token]);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({
            behavior: "smooth",
        });
    }, [messages]);

    function handleSendMessage(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();

        const content = messageText.trim();

        if (!content || !conversationId) return;

        const client = clientRef.current;

        if (!client || !client.connected) {
            setSendError(
                "Not connected to chat. Please wait for reconnection."
            );
            return;
        }

        try {
            client.publish({
                destination: "/app/chat.send",
                body: JSON.stringify({
                    conversationId,
                    content,
                    messageType: "TEXT",
                }),
            });

            setMessageText("");
            setSendError(null);
        } catch (err) {
            console.error("Message sending failed:", err);

            setSendError(
                "Unable to send message. Please try again."
            );
        }
    }

    function formatMessageTime(value: string) {
        return new Date(value).toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    if (!conversation) {
        return (
            <div className="flex h-full items-center justify-center bg-gray-50">
                <div className="text-center">
                    <h2 className="text-2xl font-semibold text-gray-700">
                        Welcome to LiveConnect
                    </h2>

                    <p className="mt-2 text-gray-500">
                        Select a conversation to start chatting.
                    </p>
                </div>
            </div>
        );
    }

    return (
        <section className="flex h-full min-w-0 flex-1 flex-col bg-gray-50">
            {/* Header */}
            <header className="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
                <div className="flex items-center gap-3">
                    <div className="flex h-11 w-11 items-center justify-center overflow-hidden rounded-full bg-blue-100 font-semibold text-blue-700">
                        {conversation.profilePicture ? (
                            <img
                                src={conversation.profilePicture}
                                alt={conversation.name}
                                className="h-full w-full object-cover"
                            />
                        ) : (
                            conversation.name?.charAt(0).toUpperCase() || "?"
                        )}
                    </div>

                    <div>
                        <h2 className="font-semibold text-gray-900">
                            {conversation.name}
                        </h2>

                        <p className="text-xs text-gray-500">
                            {conversation.type}
                        </p>
                    </div>
                </div>

                <div className="flex items-center gap-2 text-xs">
                    <span
                        className={`h-2 w-2 rounded-full ${
                            connectionState === "connected"
                                ? "bg-green-500"
                                : connectionState === "connecting"
                                  ? "bg-yellow-500"
                                  : "bg-red-500"
                        }`}
                    />

                    <span className="text-gray-500">
                        {connectionState === "connected"
                            ? "Connected"
                            : connectionState === "connecting"
                              ? "Connecting..."
                              : "Disconnected"}
                    </span>
                </div>
            </header>

            {/* Messages */}
            <div className="flex-1 space-y-4 overflow-y-auto p-6">
                {loading && (
                    <p className="text-center text-sm text-gray-500">
                        Loading previous messages...
                    </p>
                )}

                {error && (
                    <p className="text-center text-sm text-red-500">
                        {error}
                    </p>
                )}

                {!loading && !error && messages.length === 0 && (
                    <div className="flex h-full items-center justify-center">
                        <p className="text-sm text-gray-500">
                            No previous messages. Start a conversation!
                        </p>
                    </div>
                )}

                {messages.map((message) => {
                    const isMine =
                        currentUsername !== null &&
                        message.senderUsername.toLowerCase() ===
                            currentUsername.toLowerCase();

                    return (
                        <div
                            key={message.id}
                            className={`flex ${
                                isMine ? "justify-end" : "justify-start"
                            }`}
                        >
                            <div
                                className={`max-w-[75%] rounded-2xl px-4 py-3 shadow-sm ${
                                    isMine
                                        ? "rounded-br-sm bg-blue-600 text-white"
                                        : "rounded-bl-sm border border-gray-200 bg-white text-gray-900"
                                }`}
                            >
                                {!isMine && (
                                    <p className="mb-1 text-xs font-semibold text-blue-600">
                                        {message.senderUsername}
                                    </p>
                                )}

                                <p className="break-words text-sm">
                                    {message.content}
                                </p>

                                <p
                                    className={`mt-1 text-right text-[10px] ${
                                        isMine
                                            ? "text-blue-100"
                                            : "text-gray-400"
                                    }`}
                                >
                                    {formatMessageTime(message.createdAt)}
                                </p>
                            </div>
                        </div>
                    );
                })}

                <div ref={bottomRef} />
            </div>

            {/* Message composer */}
            <form
                onSubmit={handleSendMessage}
                className="border-t border-gray-200 bg-white p-4"
            >
                {sendError && (
                    <p className="mb-2 text-xs text-red-500">
                        {sendError}
                    </p>
                )}

                <div className="flex items-center gap-3">
                    <input
                        type="text"
                        value={messageText}
                        onChange={(event) =>
                            setMessageText(event.target.value)
                        }
                        placeholder="Type a message..."
                        className="flex-1 rounded-full border border-gray-200 bg-gray-50 px-5 py-3 text-sm text-gray-900 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                    />

                    <button
                        type="submit"
                        disabled={
                            !messageText.trim() ||
                            connectionState !== "connected"
                        }
                        className="rounded-full bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        Send
                    </button>
                </div>
            </form>
        </section>
    );
}
