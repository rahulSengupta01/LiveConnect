export interface Conversation {
    conversationId: string;
    type: string;
    name: string;
    profilePicture: string | null;
    lastMessage: string | null;
    lastMessageSender: string | null;
    lastMessageTime: string | null;
    updatedAt: string;
}