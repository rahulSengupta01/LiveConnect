import { MessagePage } from "@/types/message";

const API_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

export async function getConversationMessages(
    token: string,
    conversationId: string,
    page: number = 0,
    size: number = 50
): Promise<MessagePage> {

    const response = await fetch(
        `${API_URL}/api/messages/conversation/${conversationId}?page=${page}&size=${size}`,
        {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
            cache: "no-store",
        }
    );

    if (!response.ok) {
        throw new Error("Failed to load previous messages");
    }

    return response.json();
}