import { useEffect, useState } from "react";
import type { BookingResponse, Page, RoomResponse } from "./types";
import { ApiError } from "../../shared/api/client";
import { cancelBooking, createBooking, getBookings, getRooms } from "./api";
import { useAuth } from "../../shared/auth/AuthContext";
import { useNavigate } from "react-router-dom";

export const BookingsPage = () => {
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [loading, setLoading] = useState(true);
  const [bookings, setBookings] = useState<Page<BookingResponse> | null>(null);
  const [pageNumber, setPageNumber] = useState(0);
  const [reloadKey, setReloadKey] = useState(0);
  const [rooms, setRooms] = useState<RoomResponse[]>([]);
  const [roomId, setRoomId] = useState<number | null>(null);
  const { user, logout } = useAuth();
  const [actionError, setActionError] = useState("");
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
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

  useEffect(() => {
    let ignore = false;
    const loadRooms = async () => {
      try {
        const data = await getRooms();
        if (!ignore) setRooms(data);
      } catch (e) {
        if (!ignore && e instanceof ApiError) setFormError(e.response.message);
      }
    };
    loadRooms();
    return () => {
      ignore = true;
    };
  }, []);

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
      } else console.error(e);
    }
  };

  const handleSubmit = async (e: React.SyntheticEvent) => {
    e.preventDefault();
    if (roomId === null) return;
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

  const handleCancel = async (bookingId: number) => {
    setActionError("");
    try {
      await cancelBooking(bookingId);
      setReloadKey((k) => k + 1);
    } catch (e) {
      if (e instanceof ApiError) {
        setActionError(e.response.message);
      } else console.error(e);
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
            {booking.roomName} ({booking.user}),{" "}
            {new Date(booking.startAt).toLocaleString("fr-FR")},{" "}
            {new Date(booking.endAt).toLocaleString("fr-FR")}, {booking.state}
            {booking.state === "CONFIRMED" &&
              booking.user === user?.username && (
                <button type="button" onClick={() => handleCancel(booking.id)}>
                  Annuler
                </button>
              )}
          </li>
        ))}
      </ul>
    );

  return (
    <>
      <h1>Planning des salles</h1>
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
      {actionError && <p>{actionError}</p>}
      {content}
      {formError && <p>{formError}</p>}
      <form onSubmit={handleSubmit}>
        <select
          value={roomId ?? ""}
          onChange={(e) => setRoomId(Number(e.target.value))}
          required
        >
          <option value="">Choisir une salle</option>
          {rooms.map((room) => (
            <option key={room.id} value={room.id}>
              {room.name} ({room.capacity} places)
            </option>
          ))}
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
