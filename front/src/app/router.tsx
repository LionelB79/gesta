import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ProtectedRoute } from "../shared/auth/ProtectedRoute";
import { BookingsPage } from "../features/bookings/BookingsPage";
import { LoginPage } from "../features/auth/LoginPage";

export const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/bookings"
          element={
            <ProtectedRoute>
              <BookingsPage />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/bookings" replace />} />
      </Routes>
    </BrowserRouter>
  );
};
