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

  const handleSubmit = (e: React.SyntheticEvent) => {
    e.preventDefault();
    doLogin(username, password);
  };

  const handleDemo = () => {
    doLogin("demo", "demo2026!");
  };

  const doLogin = async (username: string, password: string) => {
    setError("");
    try {
      await login(username, password);
      navigate("/bookings");
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.response.message);
      } else {
        setError("Serveur injoignable, réessayez plus tard.");
      }
    }
  };

  return (
    <>
      {error && <p>{error}</p>}
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="username">Nom :</label>
          <input
            id="username"
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="password">Mot de passe :</label>
          <input
            id="password"
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
