import { useEffect, useState } from "react";
import type { BookingResponse, Page } from "./types";
import { ApiError } from "../../shared/api/client";
import { createBooking, getBookings } from "./api";
import { useAuth } from "../../shared/auth/AuthContext";
import { useNavigate } from "react-router-dom";

export const BookingsPage = () => {
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [loading, setLoading] = useState(true);
  const [bookings, setBookings] = useState<Page<BookingResponse> | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [roomId, setRoomId] = useState(1);
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const { logout } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    let ignore = false;
    const getPage = async () => {
      setLoading(true);
      setError("");
      try {
        const data = await getBookings(pageNumber, 10);
        if (!ignore) setBookings(data);
      } catch (e) {
        if (!ignore && e instanceof ApiError) setError(e.response.message);
      } finally {
        if (!ignore) setLoading(false);
      }
    };

    getPage();
    return () => {
      ignore = true;
    };
  }, [pageNumber, reloadKey]);

  const before = () => {
    setPageNumber((p) => p - 1);
  };
  const next = () => {
    setPageNumber((p) => p + 1);
  };

  const handleLogout = async () => {
    try {
      await logout();
      navigate("/login");
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.response.message);
      }
      else console.error(e);
    }
  };

  const handleSubmit = async (e: React.SyntheticEvent) => {
    e.preventDefault();
    setFormError("");
    try {
      await createBooking({
        roomId,
        start: new Date(start).toISOString(),
        end: new Date(end).toISOString(),
      });
      setReloadKey((k) => k + 1);
    } catch (e) {
      if (e instanceof ApiError) setFormError(e.response.message);
      else console.error(e);
    }
  };

  let content;
  if (loading) content = <p>Chargement…</p>;
  else if (error) content = <p>{error}</p>;
  else if (!bookings || bookings.content.length === 0)
    content = <p>Aucune réservation</p>;
  else
    content = (
      <ul>
        {bookings.content.map((booking) => (
          <li key={booking.id}>
            {booking.roomName},{" "}
            {new Date(booking.startAt).toLocaleString("fr-FR")},{" "}
            {new Date(booking.endAt).toLocaleString("fr-FR")},{" "}
            {booking.state}
          </li>
        ))}
      </ul>
    );

  return (
    <>
      <h1>Mes réservations</h1>
      <button type="button" onClick={before} disabled={pageNumber === 0}>
        Précédent
      </button>
      <button
        type="button"
        onClick={next}
        disabled={!bookings || pageNumber >= bookings.page.totalPages - 1}
      >
        Suivant
      </button>
      <button type="button" onClick={handleLogout}>
        Déconnexion
      </button>
      {content}
      {formError && <p>{formError}</p>}
      <form onSubmit={handleSubmit}>
        <select
          value={roomId}
          onChange={(e) => setRoomId(Number(e.target.value))}
        >
          <option value={1}>demo</option>
          <option value={2}>mars</option>
          <option value={3}>saturne</option>
        </select>
        <input
          type="datetime-local"
          value={start}
          onChange={(e) => setStart(e.target.value)}
          required
        ></input>
        <input
          type="datetime-local"
          value={end}
          onChange={(e) => setEnd(e.target.value)}
          required
        ></input>
        <button type="submit">Enregistrer</button>
      </form>
    </>
  );
};
