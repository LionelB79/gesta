export interface BookingResponse {
  id: number;
  user: string;
  startAt: string;
  endAt: string;
  roomName: string;
  state: "CONFIRMED" | "CANCELED";
}

export interface CreateBookingRequest {
  roomId: number;
  start: string;
  end: string;
}

export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalPages: number;
    totalElements: number;
  };
}
