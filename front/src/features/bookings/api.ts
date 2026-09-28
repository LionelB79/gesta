import { apiClient } from "../../shared/api/client";
import type { BookingResponse, CreateBookingRequest, Page } from "./types";

export function getBookings(page:number, size:number): Promise<Page<BookingResponse>> {
    return apiClient<Page<BookingResponse>>(`/bookings?page=${page}&size=${size}`);
}
export function createBooking(request: CreateBookingRequest): Promise<BookingResponse> {
    return apiClient<BookingResponse>("/bookings", { method: "POST", body: JSON.stringify(request) });
}
