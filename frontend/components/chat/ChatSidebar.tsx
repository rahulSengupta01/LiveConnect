"use client";

import { useMemo, useState } from "react";
import { Conversation } from "@/types/conversation";

interface ChatSidebarProps {
    conversations: Conversation[];
    selectedConversationId: string | null;
    onSelectConversation: (conversation: Conversation) => void;
    loading: boolean;
    error: string | null;
}

export default function ChatSidebar({
    conversations,
    selectedConversationId,
    onSelectConversation,
    loading,
    error,
}: ChatSidebarProps) {

    const [search, setSearch] = useState("");

    const filteredConversations = useMemo(() => {
        return conversations.filter((conversation) =>
            (conversation.name || "")
                .toLowerCase()
                .includes(search.toLowerCase())
        );
    }, [conversations, search]);

    function formatTime(value: string | null) {
        if (!value) return "";

        const date = new Date(value);

        if (Number.isNaN(date.getTime())) return "";

        return date.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
        });
    }

    return (
        <aside className="flex h-full w-full flex-col border-r border-gray-200 bg-white">

            {/* Header */}
            <div className="border-b border-gray-200 p-5">
                <h1 className="text-2xl font-bold text-gray-900">
                    LiveConnect
                </h1>

                <p className="mt-1 text-sm text-gray-500">
                    Stay connected with everyone.
                </p>
            </div>

            {/* Search */}
            <div className="p-4">
                <input
                    type="text"
                    placeholder="Search conversations..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    className="w-full rounded-lg border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-800 outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                />
            </div>

            {/* Conversation list */}
            <div className="flex-1 overflow-y-auto">

                {loading && (
                    <p className="p-5 text-center text-sm text-gray-500">
                        Loading conversations...
                    </p>
                )}

                {!loading && error && (
                    <p className="p-5 text-center text-sm text-red-500">
                        {error}
                    </p>
                )}

                {!loading &&
                    !error &&
                    filteredConversations.length === 0 && (
                        <p className="p-5 text-center text-sm text-gray-500">
                            No conversations found.
                        </p>
                    )}

                {!loading &&
                    !error &&
                    filteredConversations.map((conversation) => {

                        const isSelected =
                            selectedConversationId ===
                            conversation.conversationId;

                        return (
                            <button
                                key={conversation.conversationId}
                                onClick={() =>
                                    onSelectConversation(conversation)
                                }
                                className={`flex w-full items-center gap-3 border-b border-gray-100 px-4 py-4 text-left transition hover:bg-gray-50 ${
                                    isSelected ? "bg-blue-50" : ""
                                }`}
                            >
                                {/* Profile picture */}
                                <div className="flex h-12 w-12 shrink-0 items-center justify-center overflow-hidden rounded-full bg-blue-100 font-semibold text-blue-700">
                                    {conversation.profilePicture ? (
                                        <img
                                            src={conversation.profilePicture}
                                            alt={conversation.name}
                                            className="h-full w-full object-cover"
                                        />
                                    ) : (
                                        (conversation.name || "?")
                                            .charAt(0)
                                            .toUpperCase()
                                    )}
                                </div>

                                {/* Conversation details */}
                                <div className="min-w-0 flex-1">

                                    <div className="flex items-center justify-between gap-2">
                                        <h3 className="truncate font-semibold text-gray-900">
                                            {conversation.name}
                                        </h3>

                                        <span className="shrink-0 text-xs text-gray-400">
                                            {formatTime(
                                                conversation.lastMessageTime
                                            )}
                                        </span>
                                    </div>

                                    <p className="mt-1 truncate text-sm text-gray-500">
                                        {conversation.lastMessage
                                            ? `${
                                                conversation.lastMessageSender
                                                    ? conversation.lastMessageSender + ": "
                                                    : ""
                                              }${conversation.lastMessage}`
                                            : "No messages yet"}
                                    </p>

                                </div>
                            </button>
                        );
                    })}

            </div>
        </aside>
    );
}