import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { ReactNode } from "react";


export const ProtectedRoute = ({ children }: { children: ReactNode }) => {
const { user, loading } = useAuth();

if (loading) return <div>Chargement…</div>;
if (!user) return <Navigate to="/login" replace />;
return children;
};
