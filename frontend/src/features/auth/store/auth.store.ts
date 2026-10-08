import { create } from "zustand";
import { User } from "../types/auth.types";

interface AuthState {
    pendingEmail: string | null;
    registrationStep: "register" | "verify";
    currentUser: User | null;
    isAuthModalOpen: boolean;
    authModalTab: "login" | "register";
    setPendingEmail: (email: string) => void;
    setRegistrationStep: (step: "register" | "verify") => void;
    setCurrentUser: (user: User | null) => void;
    resetRegistration: () => void;
    openAuthModal: (tab?: "login" | "register") => void;
    closeAuthModal: () => void;
    setAuthModalTab: (tab: "login" | "register") => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    pendingEmail: null,
    registrationStep: "register",
    currentUser: null,
    isAuthModalOpen: false,
    authModalTab: "login",
    setPendingEmail: (email) => set({ pendingEmail: email }),
    setRegistrationStep: (step) => set({ registrationStep: step }),
    setCurrentUser: (user) => set({ currentUser: user }),
    resetRegistration: () => set({ pendingEmail: null, registrationStep: "register" }),
    openAuthModal: (tab = "login") => set({ isAuthModalOpen: true, authModalTab: tab }),
    closeAuthModal: () => set({ isAuthModalOpen: false, registrationStep: "register" }),
    setAuthModalTab: (tab) => set({ authModalTab: tab, registrationStep: "register" }),
}));
