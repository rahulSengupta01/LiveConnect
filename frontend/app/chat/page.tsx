
"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Client } from "@stomp/stompjs";

import { useAuth } from "@/contexts/AuthContext";
import { getMyConversations } from "@/services/conversationService";

import ChatSidebar from "@/components/chat/ChatSidebar";
import ChatWindow from "@/components/chat/ChatWindow";

import { Conversation } from "@/types/conversation";
import { Message } from "@/types/message";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL;

export default function ChatPage() {
    const router = useRouter();

    const {
        token,
        isAuthenticated,
        loading: authLoading,
        logout,
    } = useAuth();

    const [conversations, setConversations] = useState<Conversation[]>([]);
    const [selectedConversation, setSelectedConversation] =
        useState<Conversation | null>(null);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Redirect unauthenticated users.
    useEffect(() => {
        if (!authLoading && !isAuthenticated) {
            router.replace("/login");
        }
    }, [authLoading, isAuthenticated, router]);

    // Load conversation list.
    useEffect(() => {
        if (authLoading || !isAuthenticated || !token) return;

        const activeToken = token;
        let cancelled = false;

        async function fetchConversations() {
            try {
                setLoading(true);
                setError(null);

                const data = await getMyConversations(activeToken);

                if (cancelled) return;

                setConversations(data);

                setSelectedConversation(
                    data.length > 0 ? data[0] : null
                );
            } catch (err) {
                if (cancelled) return;

                setError(
                    err instanceof Error
                        ? err.message
                        : "Failed to load conversations."
                );
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        fetchConversations();

        return () => {
            cancelled = true;
        };
    }, [authLoading, isAuthenticated, token]);

    // Update sidebar whenever a message arrives on the personal channel.
    const handleConversationUpdate = useCallback(
        (message: Message) => {
            setConversations((previous) => {
                const updated = previous.map((conversation) => {
                    if (
                        conversation.conversationId !==
                        message.conversationId
                    ) {
                        return conversation;
                    }

                    return {
                        ...conversation,
                        lastMessage: message.content,
                        lastMessageSender: message.senderUsername,
                        lastMessageTime: message.createdAt,
                        updatedAt: message.createdAt,
                    };
                });

                return updated.sort((a, b) => {
                    const timeA = new Date(
                        a.lastMessageTime || a.updatedAt
                    ).getTime();

                    const timeB = new Date(
                        b.lastMessageTime || b.updatedAt
                    ).getTime();

                    return timeB - timeA;
                });
            });

            setSelectedConversation((current) => {
                if (
                    !current ||
                    current.conversationId !== message.conversationId
                ) {
                    return current;
                }

                return {
                    ...current,
                    lastMessage: message.content,
                    lastMessageSender: message.senderUsername,
                    lastMessageTime: message.createdAt,
                    updatedAt: message.createdAt,
                };
            });
        },
        []
    );

    // Establish one user-level WebSocket connection.
    useEffect(() => {
        if (!token || !isAuthenticated) return;

        let cancelled = false;

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

                console.log(
                    "Connected to personal conversation updates"
                );

                client.subscribe(
                    "/user/queue/conversation-updates",
                    (frame) => {
                        try {
                            const message: Message = JSON.parse(
                                frame.body
                            );

                            console.log(
                                "Personal conversation update:",
                                message
                            );

                            handleConversationUpdate(message);
                        } catch (err) {
                            console.error(
                                "Invalid conversation update:",
                                err
                            );
                        }
                    }
                );
            },

            onStompError: (frame) => {
                console.error(
                    "Personal WebSocket STOMP error:",
                    frame.headers["message"],
                    frame.body
                );
            },

            onWebSocketError: (event) => {
                console.error(
                    "Personal WebSocket error:",
                    event
                );
            },
        });

        client.activate();

        return () => {
            cancelled = true;
            void client.deactivate();
        };
    }, [token, isAuthenticated, handleConversationUpdate]);

    if (authLoading || !isAuthenticated || !token) {
        return (
            <div className="flex h-screen items-center justify-center">
                Loading LiveConnect...
            </div>
        );
    }

    return (
        <main className="relative flex h-screen overflow-hidden bg-white">
            {/* Sidebar */}
            <div className="w-full shrink-0 md:w-[360px]">
                <ChatSidebar
                    conversations={conversations}
                    selectedConversationId={
                        selectedConversation?.conversationId ?? null
                    }
                    onSelectConversation={setSelectedConversation}
                    loading={loading}
                    error={error}
                />
            </div>

            {/* Chat window */}
            <div className="hidden min-w-0 flex-1 md:block">
                <ChatWindow
                    conversation={selectedConversation}
                    token={token}
                />
            </div>

            {/* Logout */}
            <button
                onClick={() => {
                    logout();
                    router.replace("/login");
                }}
                className="absolute right-5 top-5 z-10 rounded-lg bg-red-500 px-4 py-2 text-sm font-medium text-white hover:bg-red-600"
            >
                Logout
            </button>
        </main>
    );
}
