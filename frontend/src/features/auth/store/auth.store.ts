import { create } from "zustand";
import { User } from "../types/auth.types";

interface AuthState {
    pendingEmail: string | null;
    registrationStep: "register" | "verify";
    currentUser: User | null;
    setPendingEmail: (email: string) => void;
    setRegistrationStep: (step: "register" | "verify") => void;
    setCurrentUser: (user: User | null) => void;
    resetRegistration: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    pendingEmail: null,
    registrationStep: "register",
    currentUser: null,
    setPendingEmail: (email) => set({ pendingEmail: email }),
    setRegistrationStep: (step) => set({ registrationStep: step }),
    setCurrentUser: (user) => set({ currentUser: user }),
    resetRegistration: () => set({ pendingEmail: null, registrationStep: "register" }),
}));
