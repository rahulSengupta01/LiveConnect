import { Conversation } from "@/types/conversation";

const API_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

export async function getMyConversations(
    token: string
): Promise<Conversation[]> {
    const response = await fetch(`${API_URL}/api/conversations`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
        },
        cache: "no-store",
    });

    if (!response.ok) {
        throw new Error(
            response.status === 401
                ? "Your session has expired. Please log in again."
                : "Failed to load conversations."
        );
    }

    return response.json();
}