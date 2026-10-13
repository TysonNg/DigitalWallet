import { create } from "zustand";
import { User } from "../types/auth.types";

type AuthTab = "login" | "register";

interface AuthState {
    pendingEmail: string | null;
    registrationStep: "register" | "verify";
    currentUser: User | null;
    isAuthModalOpen: boolean;
    authModalTab: AuthTab;
    setPendingEmail: (email: string) => void;
    setRegistrationStep: (step: "register" | "verify") => void;
    setCurrentUser: (user: User | null) => void;
    resetRegistration: () => void;
    openAuthModal: (tab?: AuthTab) => void;
    closeAuthModal: () => void;
    setAuthModalTab: (tab: AuthTab) => void;
    syncAuthModalFromUrl: (tab: string | null) => void;
}

/** Native history integrates with Next's useSearchParams without a route reload. */
function updateAuthQuery(tab: AuthTab | null) {
    if (typeof window === "undefined") return;

    const url = new URL(window.location.href);
    if (tab) {
        url.searchParams.set("auth", tab);
    } else {
        url.searchParams.delete("auth");
        url.searchParams.delete("returnTo");
    }

    const href = `${url.pathname}${url.search}${url.hash}`;
    if (href === `${window.location.pathname}${window.location.search}${window.location.hash}`)
        return;

    if (tab) {
        window.history.pushState(null, "", href);
    } else {
        window.history.replaceState(null, "", href);
    }
}

// Module-level state survives locale navigation under the shared root layout.
// URL reconciliation must call syncAuthModalFromUrl, never the public URL-writing actions.
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
    openAuthModal: (tab = "login") => {
        updateAuthQuery(tab);
        set({ isAuthModalOpen: true, authModalTab: tab });
    },
    closeAuthModal: () => {
        updateAuthQuery(null);
        set({ isAuthModalOpen: false });
    },
    setAuthModalTab: (tab) => {
        updateAuthQuery(tab);
        set({ isAuthModalOpen: true, authModalTab: tab });
    },
    syncAuthModalFromUrl: (tab) => {
        if (tab === "login" || tab === "register") {
            set({ isAuthModalOpen: true, authModalTab: tab });
        } else {
            set({ isAuthModalOpen: false });
        }
    },
}));
