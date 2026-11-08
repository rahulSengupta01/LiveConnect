export interface Message {
    id: string;
    conversationId: string;
    senderId: string;
    senderUsername: string;
    content: string;
    messageType: string;
    createdAt: string;
    updatedAt: string;
}

export interface MessagePage {
    content: Message[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}