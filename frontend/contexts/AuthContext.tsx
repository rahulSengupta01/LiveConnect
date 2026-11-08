"use client";

import {
    createContext,
    useContext,
    useEffect,
    useState,
    ReactNode,
} from "react";

import {
    loginUser,
    LoginRequest,
} from "@/services/authService";

interface AuthContextType {
    token: string | null;
    isAuthenticated: boolean;
    loading: boolean;
    login: (credentials: LoginRequest) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<
    AuthContextType | undefined
>(undefined);

export function AuthProvider({
    children,
}: {
    children: ReactNode;
}) {

    const [token, setToken] =
        useState<string | null>(null);

    const [loading, setLoading] =
        useState(true);

    useEffect(() => {

        const savedToken =
            localStorage.getItem("accessToken");

        if (savedToken) {
            setToken(savedToken);
        }

        setLoading(false);

    }, []);

    const login = async (
        credentials: LoginRequest
    ) => {

        const response =
            await loginUser(credentials);

        localStorage.setItem(
            "accessToken",
            response.accessToken
        );

        setToken(response.accessToken);
    };

    const logout = () => {

        localStorage.removeItem("accessToken");

        setToken(null);
    };

    return (
        <AuthContext.Provider
            value={{
                token,
                isAuthenticated: !!token,
                loading,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {

    const context = useContext(AuthContext);

    if (!context) {
        throw new Error(
            "useAuth must be used inside AuthProvider"
        );
    }

    return context;
}