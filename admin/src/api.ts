const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/+$/, "");
const REQUEST_TIMEOUT_MS = 15_000;

export type User = {
  id: number;
  email: string;
  name: string;
  role: "ADMIN" | "TEACHER" | "PARENT" | "STUDENT";
};
export type Student = {
  id: number;
  studentCode: string;
  firstName: string;
  lastName: string;
  className: string;
  division: string;
  rollNumber: number;
  phone: string | null;
  address: string | null;
  status: string;
  parentName: string | null;
  parentEmail: string | null;
};
export type Note = {
  id: number;
  title: string;
  message: string;
  createdAt: string;
  read: boolean;
};
export type Homework = {
  id: number;
  title: string;
  description: string;
  className: string;
  subject: string;
  dueDate: string;
  teacherName: string;
};
export type Result = {
  id: number;
  studentId: number;
  studentName: string;
  studentCode: string;
  examName: string;
  subject: string;
  marks: number;
  maximumMarks: number;
  percentage: number;
  examDate: string | null;
};
export type Fee = {
  id: number;
  studentId: number;
  studentName: string;
  description: string;
  amount: number;
  dueDate: string | null;
  paidDate: string | null;
  status: string;
};
export type Message = {
  id: number;
  senderEmail: string;
  senderName: string;
  recipientEmail: string;
  recipientName: string;
  subject: string;
  body: string;
  sentAt: string;
  read: boolean;
};
export type Account = {
  id: number;
  email: string;
  name: string;
  phone: string | null;
  role: string;
};
export type AttendanceRecord = {
  id: number;
  studentId: number;
  studentName: string;
  studentCode: string;
  date: string;
  status: string;
};
export type SchoolEvent = {
  id: number;
  title: string;
  description: string;
  eventDate: string;
  location: string;
  createdBy: string;
};
export type BusRoute = {
  id: number;
  busNumber: string;
  routeName: string;
  driverName: string;
  driverPhone: string;
  capacity: number;
  status: string;
  latitude: number;
  longitude: number;
  speedKmvh: number;
  currentStop: string;
  stops: string;
  lastUpdated: string;
};
export type LeaveApplication = {
  id: number;
  studentId: number | null;
  studentName: string | null;
  requesterName: string;
  requesterEmail: string;
  startDate: string;
  endDate: string;
  reason: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  createdAt: string;
};

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

let token = localStorage.getItem("school_lms_token") || "";
let unauthorizedHandler: (() => void) | undefined;

export function setToken(value: string) {
  token = value;
  localStorage.setItem("school_lms_token", value);
}

export function clearToken() {
  token = "";
  localStorage.removeItem("school_lms_token");
}

export function setUnauthorizedHandler(handler?: () => void) {
  unauthorizedHandler = handler;
}

function responseMessage(status: number) {
  if (status === 401) return "Your session expired. Please sign in again.";
  if (status === 403) return "You do not have permission to do that.";
  if (status >= 500) return "The school server had a problem. Please try again later.";
  return `The request could not be completed (HTTP ${status}).`;
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  if (!API_BASE_URL) {
    throw new ApiError("School API URL is not configured. Set VITE_API_BASE_URL and rebuild the admin app.");
  }
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      signal: controller.signal,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...init.headers,
      },
    });
  } catch (error) {
    if (error instanceof Error && error.name === "AbortError") {
      throw new ApiError("The request timed out. Check your connection and try again.");
    }
    throw new ApiError(
      "Cannot reach the school server. Check your internet connection or try again later.",
    );
  } finally {
    clearTimeout(timeout);
  }

  if (!response.ok) {
    let message = responseMessage(response.status);
    try {
      const body = await response.json();
      message = body.message || body.detail || message;
    } catch {
      // Keep the friendly status-based message when the response has no JSON body.
    }

    if (response.status === 401 && path !== "/auth/login") {
      clearToken();
      unauthorizedHandler?.();
      throw new ApiError("Your session expired. Please sign in again.", 401);
    }
    if (response.status === 401) message = "Email or password is incorrect.";
    throw new ApiError(message, response.status);
  }

  if (response.status === 204) return undefined as T;
  return response.json();
}
