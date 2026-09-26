"use client";

import { useState } from "react";
import { Client, IMessage } from "@stomp/stompjs";

export default function WebSocketTestPage() {

    const [status, setStatus] =
        useState("Disconnected");

    const [messages, setMessages] =
        useState<string[]>([]);

    const [client, setClient] =
        useState<Client | null>(null);

    const [token, setToken] =
        useState("");

    const [conversationId, setConversationId] =
        useState("");

    // Message that user types
    const [messageText, setMessageText] =
        useState("");

    const connect = () => {

        if (!token) {
            alert("Enter JWT");
            return;
        }

        if (!conversationId) {
            alert("Enter conversation ID");
            return;
        }

        const stompClient = new Client({

            brokerURL:
                "ws://localhost:8080/ws",

            connectHeaders: {
                Authorization:
                    `Bearer ${token}`,
            },

            debug: (message) => {
                console.log(
                    "[STOMP]",
                    message
                );
            },

            reconnectDelay: 5000,

            onConnect: () => {

                console.log(
                    "WebSocket connected"
                );

                setStatus("Connected");

                stompClient.subscribe(
                    `/topic/conversation/${conversationId}`,
                    (message: IMessage) => {

                        console.log(
                            "Received:",
                            message.body
                        );

                        setMessages(
                            previous => [
                                ...previous,
                                message.body
                            ]
                        );
                    }
                );
            },

            onStompError: (frame) => {

                console.error(
                    "STOMP error:",
                    frame
                );

                setStatus(
                    "STOMP Error"
                );
            },

            onWebSocketError: (error) => {

                console.error(
                    "WebSocket error:",
                    error
                );

                setStatus(
                    "WebSocket Error"
                );
            },

            onDisconnect: () => {

                setStatus(
                    "Disconnected"
                );
            },
        });

        stompClient.activate();

        setClient(stompClient);
    };

    const disconnect = () => {

        if (client) {

            client.deactivate();

            setClient(null);

            setStatus(
                "Disconnected"
            );
        }
    };

    const sendMessage = () => {

        if (!client) {
            alert(
                "Connect WebSocket first"
            );
            return;
        }

        if (!messageText.trim()) {
            alert(
                "Enter a message"
            );
            return;
        }

        client.publish({

            destination:
                "/app/chat.send",

            body: JSON.stringify({

                conversationId:
                    conversationId,

                content:
                    messageText,

                messageType:
                    "TEXT",
            }),
        });

        // Clear input after sending
        setMessageText("");
    };

    return (
        <div
            style={{
                padding: "30px",
                fontFamily: "Arial",
            }}
        >

            <h1>
                LiveConnect WebSocket Test
            </h1>

            <p>
                Status:{" "}
                <strong>
                    {status}
                </strong>
            </p>

            {/* JWT */}

            <div>
                <input
                    placeholder="JWT Token"
                    value={token}
                    onChange={(e) =>
                        setToken(e.target.value)
                    }
                    style={{
                        width: "600px",
                        padding: "10px",
                        marginBottom: "10px",
                    }}
                />
            </div>

            {/* Conversation ID */}

            <div>
                <input
                    placeholder="Conversation ID"
                    value={conversationId}
                    onChange={(e) =>
                        setConversationId(
                            e.target.value
                        )
                    }
                    style={{
                        width: "600px",
                        padding: "10px",
                        marginBottom: "10px",
                    }}
                />
            </div>

            {/* Message */}

            <div>
                <input
                    placeholder="Type your message..."
                    value={messageText}
                    onChange={(e) =>
                        setMessageText(
                            e.target.value
                        )
                    }
                    onKeyDown={(e) => {

                        if (e.key === "Enter") {
                            sendMessage();
                        }

                    }}
                    style={{
                        width: "600px",
                        padding: "10px",
                        marginBottom: "10px",
                    }}
                />
            </div>

            {/* Buttons */}

            <div>

                <button
                    onClick={connect}
                    style={{
                        marginRight: "10px",
                        padding: "10px 20px",
                    }}
                >
                    Connect
                </button>

                <button
                    onClick={disconnect}
                    style={{
                        marginRight: "10px",
                        padding: "10px 20px",
                    }}
                >
                    Disconnect
                </button>

                <button
                    onClick={sendMessage}
                    style={{
                        padding: "10px 20px",
                    }}
                >
                    Send Message
                </button>

            </div>

            {/* Received Messages */}

            <h2>
                Received Messages
            </h2>

            {messages.map(
                (message, index) => (
                    <pre key={index}>
                        {message}
                    </pre>
                )
            )}

        </div>
    );
}