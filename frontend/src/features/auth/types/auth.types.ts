export interface User {
    id: string;
    email: string;
    fullName: string;
    phoneNumber?: string;
    dateOfBirth?: string;
    status: "ACTIVE" | "INACTIVE" | "LOCKED";
}

export interface AuthResponseData {
    accessToken: string;
    tokenType: string;
    expiresIn: number;
    user: User;
}

export interface ActionResponse<T = unknown> {
    success: boolean;
    message: string;
    data?: T;
    errors?: Record<string, string[]>;
}
