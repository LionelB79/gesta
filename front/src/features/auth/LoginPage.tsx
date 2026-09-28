import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useAuth } from "../../shared/auth/AuthContext";

export const LoginPage = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.SyntheticEvent) => {
    e.preventDefault();
    try {
      await login(username, password);
      navigate("/bookings");
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.response.message);
      }
    }
  };

  const handleDemo = async () => {
    try {
      await login("demo", "demo2026!");
      navigate("/bookings");
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.response.message);
      }
    }
  };
  return (
    <>
      {error && <p>{error}</p>}
      <form onSubmit={handleSubmit}>
        <div>
          <label>Nom :</label>
          <input
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div>
          <label>Mot de passe :</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="button" onClick={handleDemo}>
          Connexion démo
        </button>{" "}
        <button type="submit">Se connecter</button>
      </form>
    </>
  );
};
