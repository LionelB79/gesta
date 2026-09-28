import {
  useContext,
  useEffect,
  useState,
  createContext,
  type ReactNode,
} from "react";
import { apiClient } from "../api/client";

interface User {
  username: string;
}

interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
 logout: () => Promise<void>;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);

  const login = async (username: string, password: string): Promise<void> => {
    const user = await apiClient<User>("/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
    setUser(user)
}
    

  const logout = async ():Promise<void> => {   
     await apiClient<void>("/logout", {
      method: "POST",
    });
  setUser(null);}
     


  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    const checkAuth = async () => {
      try {
        const me = await apiClient<User>("/me");
        setUser(me);
      } catch (error) {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    checkAuth();
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
