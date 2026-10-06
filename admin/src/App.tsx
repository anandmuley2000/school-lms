import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Account,
  api,
  AttendanceRecord,
  BusRoute,
  clearToken,
  Fee,
  Homework,
  LeaveApplication,
  TimetableEntry,
  Message,
  Note,
  Result,
  setToken,
  setUnauthorizedHandler,
  Student,
  SchoolEvent,
  User,
} from "./api";

type Page =
  | "Overview"
  | "Students"
  | "Attendance"
  | "Homework"
  | "Results"
  | "Fees"
  | "Messages"
  | "Notifications"
  | "Profile"
  | "People"
  | "Events"
  | "Leave"
  | "Bus Tracking"
  | "Timetable";
const nav: Page[] = [
  "Overview",
  "Students",
  "Attendance",
  "Homework",
  "Results",
  "Fees",
  "Messages",
  "Notifications",
  "Profile",
  "People",
  "Events",
  "Leave",
  "Bus Tracking",
  "Timetable",
];
const pageIconPaths: Record<Page, string> = {
  Overview: "M3 10.5 12 3l9 7.5V21h-6v-7H9v7H3z",
  Students: "M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M10 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8m7-7a4 4 0 0 1 0 8m3 4a4 4 0 0 1 2 3v2",
  Attendance: "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20m-4-10 3 3 6-6",
  Homework: "M5 3h14v18l-7-4-7 4zM9 8h6m-6 4h6",
  Results: "M4 20V5m0 15h17M8 16v-4m5 4V8m5 8v-7",
  Fees: "M12 2v20m5-16H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6",
  Messages: "M21 11.5a8.4 8.4 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.4 8.4 0 0 1 3.8-.9h.5a8.5 8.5 0 0 1 8 8z",
  Notifications: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-8 13h4",
  Profile: "M20 21a8 8 0 0 0-16 0m8-10a4 4 0 1 0 0-8 4 4 0 0 0 0 8",
  People: "M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2m6-10a4 4 0 1 0 0-8 4 4 0 0 0 0 8m7-7a4 4 0 1 1 0 8m2 3a4 4 0 0 1 3 4v2",
  Events: "M4 5h16v16H4zM8 3v4m8-4v4M4 10h16",
  Leave: "M4 4h16v16H4zM4 7l8 6 8-6",
  "Bus Tracking": "M4 16V6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v10M4 11h16M7 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4m10 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4M4 16h16",
  Timetable: "M4 5h16v16H4zM8 3v4m8-4v4M4 10h16M8 14h2m4 0h2m-8 4h2m4 0h2",
};
function PageIcon({ page }: { page: Page }) {
  return <svg viewBox="0 0 24 24" aria-hidden="true" className="app-icon"><path d={pageIconPaths[page]} /></svg>;
}
const demos = [
  ["ADMIN", "admin@northstar.local", "Admin@123"],
  ["TEACHER", "teacher@northstar.local", "Teacher@123"],
  ["PARENT", "parent@northstar.local", "Parent@123"],
];
export default function App() {
  const [user, setUser] = useState<User | null>(null),
    [page, setPage] = useState<Page>("Overview"),
    [students, setStudents] = useState<Student[]>([]),
    [accounts, setAccounts] = useState<Account[]>([]),
    [notes, setNotes] = useState<Note[]>([]),
    [homework, setHomework] = useState<Homework[]>([]),
    [results, setResults] = useState<Result[]>([]),
    [fees, setFees] = useState<Fee[]>([]),
    [messages, setMessages] = useState<Message[]>([]),
    [events, setEvents] = useState<SchoolEvent[]>([]),
    [leaveRequests, setLeaveRequests] = useState<LeaveApplication[]>([]),
    [busRoutes, setBusRoutes] = useState<BusRoute[]>([]),
    [timetable, setTimetable] = useState<TimetableEntry[]>([]),
    [summary, setSummary] = useState<Record<string, number>>({}),
    [query, setQuery] = useState(""),
    [busy, setBusy] = useState(false),
    [assistantOpen, setAssistantOpen] = useState(false),
    [assistantInput, setAssistantInput] = useState(""),
    [assistantMessages, setAssistantMessages] = useState<{ id: number; from: "assistant" | "user"; text: string }[]>([
      { id: 1, from: "assistant", text: "Hi! I can help you find a section or answer simple questions about your school data." },
    ]),
    [selectedChildId, setSelectedChildId] = useState<number | null>(null),
    [error, setError] = useState(""),
    [toast, setToast] = useState(""),
    [edit, setEdit] = useState<Student | null>(null);
  const clearSessionState = useCallback((message = "") => {
    clearToken();
    setUser(null);
    setStudents([]);
    setAccounts([]);
    setNotes([]);
    setHomework([]);
    setResults([]);
    setFees([]);
    setMessages([]);
    setEvents([]);
    setLeaveRequests([]);
    setBusRoutes([]);
    setTimetable([]);
    setSummary({});
    setQuery("");
    setEdit(null);
    setToast("");
    setPage("Overview");
    setError(message);
  }, []);
  const reload = useCallback(async () => {
    if (!user) return;
    setError("");
    try {
      const tasks: Promise<unknown>[] = [
        api<Student[]>("/students").then(setStudents),
        api<Note[]>("/notifications").then(setNotes),
        api<Homework[]>("/homework").then(setHomework),
        api<Result[]>("/results").then(setResults),
        api<Fee[]>("/fees").then(setFees),
        api<Message[]>("/messages").then(setMessages),
        api<SchoolEvent[]>("/events").then(setEvents),
        api<LeaveApplication[]>("/leave-requests").then(setLeaveRequests),
        api<BusRoute[]>("/bus-routes").then(setBusRoutes),
        api<TimetableEntry[]>("/timetable").then(setTimetable),
      ];
      if (user.role === "ADMIN")
        tasks.push(
          api<Record<string, number>>("/dashboard").then(setSummary),
          api<Account[]>("/accounts").then(setAccounts),
        );
      await Promise.all(tasks);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load school data");
    }
  }, [user]);
  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearSessionState("Your session expired. Please sign in again.");
    });
    if (localStorage.getItem("school_lms_token"))
      api<User>("/auth/me")
        .then(setUser)
        .catch(() => {
          clearToken();
          setError("Please sign in with a parent, teacher, or administrator account.");
        });
    return () => setUnauthorizedHandler(undefined);
  }, [clearSessionState]);
  useEffect(() => {
    reload();
  }, [reload]);
  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(""), 3400);
    return () => window.clearTimeout(timer);
  }, [toast]);
  useEffect(() => {
    if (!user || page !== "Bus Tracking") return;
    let active = true;
    const refreshRoutes = () =>
      api<BusRoute[]>("/bus-routes")
        .then((routes) => {
          if (active) setBusRoutes(routes);
        })
        .catch((e) => {
          if (active) setError(e instanceof Error ? e.message : "Could not refresh bus locations");
        });
    void refreshRoutes();
    const timer = window.setInterval(() => void refreshRoutes(), 15_000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [user, page]);
  async function login(email: string, password: string) {
    setBusy(true);
    setError("");
    try {
      const result = await api<{ token: string; user: User }>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setToken(result.token);
      setUser(result.user);
      setPage("Overview");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Sign in failed");
    } finally {
      setBusy(false);
    }
  }
  function signOut() {
    clearSessionState();
    window.location.replace(window.location.pathname || "/");
  }
  function askAssistant(raw: string) {
    const question = raw.trim();
    if (!question) return;
    const normalized = question.toLowerCase().replace(/[^a-z0-9 ]/g, " ").trim();
    const role = user?.role;
    const routes: [RegExp, Page][] = [
      [/\b(home|dashboard)\b/, "Overview"], [/\b(attendance|absent|present)\b/, "Attendance"],
      [/\b(student|students|children|directory)\b/, "Students"], [/\b(homework|assignment)\b/, "Homework"],
      [/\b(event|events|calendar)\b/, "Events"], [/\b(timetable|schedule)\b/, "Timetable"],
      [/\b(leave|vacation)\b/, "Leave"], [/\b(bus|tracking|route)\b/, "Bus Tracking"],
      [/\b(notification|notifications|update|updates)\b/, "Notifications"], [/\b(profile|account)\b/, "Profile"],
      [/\b(result|results|grade|exam)\b/, "Results"], [/\b(fee|fees|payment)\b/, "Fees"], [/\b(message|messages|inbox)\b/, "Messages"],
    ];
    let answer = "I can help you navigate the portal and answer basic questions about loaded students, homework, leave, attendance, and bus routes. Try ‘open attendance’ or ‘how do I request leave?’";
    const found = routes.find(([pattern]) => pattern.test(normalized));
    if (found && /\b(open|show|go|take|navigate|where|view)\b/.test(normalized)) {
      setPage(found[1]);
      setAssistantOpen(false);
      answer = `Opening ${found[1] === "Overview" ? "Home" : found[1] === "Notifications" ? "Updates" : found[1]}.`;
    } else if (/\b(attendance|absent|present)\b/.test(normalized)) {
      answer = role === "ADMIN" || role === "TEACHER" ? "Open Attendance to choose a student, mark Present, Absent, Late, or Leave, then save attendance." : "Open Attendance to see attendance history for your linked student(s).";
    } else if (/\b(leave|vacation)\b/.test(normalized)) {
      answer = role === "ADMIN" ? "Open Leave to review requests and approve or reject pending requests." : "Open Leave to submit a request or check its status.";
    } else if (/\b(bus|route|tracking)\b/.test(normalized)) {
      answer = `Open Bus Tracking to view routes. There ${busRoutes.length === 1 ? "is" : "are"} ${busRoutes.length} route${busRoutes.length === 1 ? "" : "s"} available right now.`;
    } else if (/\b(homework|assignment)\b/.test(normalized)) {
      answer = `There ${homework.length === 1 ? "is" : "are"} ${homework.length} assignment${homework.length === 1 ? "" : "s"} in the loaded list. Open Homework to view them.`;
    } else if (/\b(student|students|children)\b/.test(normalized)) {
      answer = `I can see ${students.length} student${students.length === 1 ? "" : "s"} in your account. Open Students to view the list.`;
    } else if (/\b(help|what can you do|how do i)\b/.test(normalized)) {
      answer = "Ask me to open a section (for example, ‘open attendance’) or ask how to use attendance, leave, homework, or bus tracking.";
    }
    const now = Date.now();
    setAssistantMessages((current) => [...current, { id: now, from: "user", text: question }, { id: now + 1, from: "assistant", text: answer }]);
    setAssistantInput("");
  }
  if (!user) return <Login onLogin={login} busy={busy} error={error} />;
  const filtered = students.filter((s) =>
    `${s.studentCode} ${s.firstName} ${s.lastName} ${s.className}-${s.division} ${s.parentName}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  );
  const selectedChild = students.find((student) => student.id === selectedChildId) ?? null;
  const visibleStudents = user.role === "PARENT" && selectedChild ? [selectedChild] : students;
  const visibleHomework = user.role === "PARENT" && selectedChild
    ? homework.filter((item) => item.className === selectedChild.className && (!item.division || item.division === selectedChild.division))
    : homework;
  const visibleResults = user.role === "PARENT" && selectedChild ? results.filter((item) => item.studentId === selectedChild.id) : results;
  const visibleFees = user.role === "PARENT" && selectedChild ? fees.filter((item) => item.studentId === selectedChild.id) : fees;
  const visibleTimetable = user.role === "PARENT" && selectedChild
    ? timetable.filter((row) => row.className === selectedChild.className && row.division === selectedChild.division)
    : timetable;
  const visibleBusRoutes = user.role === "PARENT" && selectedChild
    ? busRoutes.filter((route) => route.id === selectedChild.busRouteId)
    : busRoutes;
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">N</span>
          <span>
            Northstar<span className="brand-sub">SCHOOL LMS</span>
          </span>
        </div>
        <div className="school-label">SCHOOL WORKSPACE</div>
        <nav>
          {nav.map((item) => (
            <button
              key={item}
              className={`nav-item ${page === item ? "active" : ""}`}
              onClick={() => setPage(item)}
            >
              <span className="nav-icon"><PageIcon page={item} /></span>
              {item === "Overview" ? "Home" : item === "Notifications" ? "Updates" : item === "Profile" ? "Profile" : user.role === "PARENT" && item === "Students" ? "My children" : item}
              {item === "Notifications" &&
                notes.filter((n) => !n.read).length > 0 && (
                  <span className="badge">
                    {notes.filter((n) => !n.read).length}
                  </span>
                )}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="profile-mini">
            <div className="avatar">{user.name.slice(0, 1)}</div>
            <div>
              <strong>{user.name}</strong>
              <small>{user.role.toLowerCase()}</small>
            </div>
            <button className="logout-button" onClick={signOut} aria-label="Log out" title="Log out">
              <span aria-hidden="true">↗</span><span className="logout-label">Log out</span>
            </button>
          </div>
        </div>
      </aside>
      <main className="main">
        <header className="topbar">
          <div className="crumb">
            School / <b>{page === "Overview" ? "Home" : page === "Notifications" ? "Updates" : page}</b>
          </div>
          <div className="top-actions">
            {user.role === "PARENT" && students.length > 1 && (
              <label className="child-switcher">Viewing
                <select aria-label="Choose child" value={selectedChildId ?? "all"} onChange={(event) => setSelectedChildId(event.target.value === "all" ? null : Number(event.target.value))}>
                  <option value="all">All children</option>
                  {students.map((student) => <option key={student.id} value={student.id}>{student.firstName} {student.lastName}</option>)}
                </select>
              </label>
            )}
            <span className="term">Academic year 2026–27</span>
            <button
              className="icon-button"
              onClick={() => setPage("Notifications")}
              aria-label="Notifications"
            >
              <svg viewBox="0 0 24 24" aria-hidden="true" className="app-icon"><path d={pageIconPaths.Notifications} /></svg>{notes.some((n) => !n.read) && <i />}
            </button>
            <div className="avatar small">{user.name.slice(0, 1)}</div>
            <button className="topbar-logout" onClick={signOut}>
              Log out
            </button>
          </div>
        </header>
        <section className="content">
          <div className="page-heading">
            <div>
              <div className="eyebrow">
                {page === "Overview"
                  ? "THURSDAY, 1 OCTOBER 2026"
                  : user.role === "PARENT" ? "FAMILY PORTAL" : `${user.role} PORTAL`}
              </div>
              <h1>
                {page === "Overview"
                  ? `Good morning, ${user.name.split(" ")[0]}`
                  : page === "Notifications" ? "Updates" : page === "Students" && user.role === "PARENT" ? "My children" : page}
              </h1>
              <p>
                {page === "Overview"
                  ? "Here’s what’s happening across your school today."
                  : "Manage your school information and daily activities."}
              </p>
            </div>
            {page === "Students" && user.role === "ADMIN" && (
              <button
                className="primary"
                onClick={() =>
                  setEdit({
                    id: 0,
                    studentCode: "",
                    firstName: "",
                    lastName: "",
                    className: "6",
                    division: "A",
                    rollNumber: 1,
                    phone: "",
                    address: "",
                    status: "ACTIVE",
                    parentName: "",
                    parentEmail: "",
                    busRouteId: null,
                  })
                }
              >
                ＋ Add student
              </button>
            )}
          </div>
          {error && (
            <div className="error-banner">
              {error}
              <button onClick={reload}>Retry</button>
            </div>
          )}
          {page === "Overview" && (
            <Overview
              user={user}
              summary={summary}
              students={visibleStudents}
              homework={visibleHomework}
              notes={notes}
              go={setPage}
            />
          )}
          {page === "Students" && (
            <>
              <div className="toolbar">
                <div className="search">
                  ⌕{" "}
                  <input
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search students, ID, class, or parent"
                  />
                </div>
                <button className="secondary" onClick={reload}>
                  ↻ Refresh
                </button>
              </div>
              <div className="table-card">
                <div className="table-head">
                  <div>
                    <h2>
                      {user.role === "PARENT"
                        ? "Your children"
                        : "Student directory"}
                    </h2>
                    <p>{filtered.length} students · sorted by class</p>
                  </div>
                  <span className="chip">2026–27</span>
                </div>
                <div className="table-scroll">
                  <table>
                    <thead>
                      <tr>
                        <th>STUDENT</th>
                        <th>CLASS</th>
                        <th>ROLL NO.</th>
                        <th>PARENT / GUARDIAN</th>
                        <th>STATUS</th>
                        <th />
                      </tr>
                    </thead>
                    <tbody>
                      {filtered.map((s) => (
                        <tr key={s.id}>
                          <td>
                            <div className="student-cell">
                              <div className="student-avatar">
                                {s.firstName[0]}
                                {s.lastName[0]}
                              </div>
                              <div>
                                <strong>
                                  {s.firstName} {s.lastName}
                                </strong>
                                <small>{s.studentCode}</small>
                              </div>
                            </div>
                          </td>
                          <td>
                            <span className="class-chip">
                              {s.className}-{s.division}
                            </span>
                          </td>
                          <td>{s.rollNumber.toString().padStart(2, "0")}</td>
                          <td>
                            <strong className="parent-name">
                              {s.parentName || "—"}
                            </strong>
                            <small className="parent-phone">
                              {s.parentEmail || ""}
                            </small>
                          </td>
                          <td>
                            <span
                              className={`status ${s.status.toLowerCase()}`}
                            >
                              {s.status}
                            </span>
                          </td>
                          <td>
                            {user.role === "ADMIN" && (
                              <button
                                className="row-action"
                                onClick={() => setEdit(s)}
                              >
                                •••
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                  {filtered.length === 0 && (
                    <div className="empty">No students match your search.</div>
                  )}
                </div>
              </div>
            </>
          )}
          {page === "Attendance" && (
            <Attendance
              user={user}
              students={visibleStudents}
              onSaved={async () => {
                await reload();
                setToast("Attendance saved and the parent was notified.");
              }}
            />
          )}
          {page === "Homework" && (
            <HomeworkPage
              user={user}
              items={visibleHomework}
              students={visibleStudents}
              onSaved={async () => {
                await reload();
                setToast(
                  "Homework published and families in the class were notified.",
                );
              }}
            />
          )}
          {page === "Results" && (
            <ResultsPage
              user={user}
              students={visibleStudents}
              results={visibleResults}
              onSaved={async () => {
                await reload();
                setToast("Result saved and family notified.");
              }}
            />
          )}
          {page === "Fees" && (
            <FeesPage
              user={user}
              students={visibleStudents}
              fees={visibleFees}
              onSaved={async () => {
                await reload();
                setToast("Fee record updated.");
              }}
            />
          )}
          {page === "Messages" && (
            <MessagesPage
              user={user}
              messages={messages}
              onSaved={async (message) => {
                await reload();
                setToast(message);
              }}
            />
          )}
          {page === "Events" && (
            <>
              <EventsCalendar items={events} />
              <EventsPage user={user} items={events} onSaved={async () => { await reload(); setToast("School events updated."); }} />
            </>
          )}
          {page === "Leave" && (
            <LeavePage
              user={user}
              items={leaveRequests}
              students={students}
              onSaved={async () => {
                await reload();
                setToast("Leave requests updated.");
              }}
            />
          )}
          {page === "Bus Tracking" && (
            <BusTrackingPage
              user={user}
              items={visibleBusRoutes}
              onSaved={async () => {
                await reload();
                setToast("Bus routes updated.");
              }}
              onLocationUpdated={(updated) =>
                setBusRoutes((routes) => routes.map((route) => route.id === updated.id ? updated : route))
              }
            />
          )}
          {page === "Timetable" && <TimetablePage user={user} items={visibleTimetable} accounts={accounts} students={students} onSaved={async () => { await reload(); setToast("Timetable updated."); }} />}
          {page === "Notifications" && (
            <div className="list-card">
              <div className="table-head">
                <div>
                  <h2>Inbox</h2>
                  <p>Attendance alerts and school updates for your account.</p>
                </div>
              </div>
              {notes.map((n) => (
                <article
                  className={`note ${n.read ? "" : "unread"}`}
                  key={n.id}
                >
                  <div className="note-icon">
                    {n.title.toLowerCase().includes("attendance") ? "✓" : "▧"}
                  </div>
                  <div>
                    <strong>{n.title}</strong>
                    <p>{n.message}</p>
                    <small>{new Date(n.createdAt).toLocaleString()}</small>
                  </div>
                  {!n.read && (
                    <button
                      className="text-action"
                      onClick={async () => {
                        await api(`/notifications/${n.id}/read`, {
                          method: "PATCH",
                        });
                        await reload();
                        setToast("Notification marked as read.");
                      }}
                    >
                      Mark read
                    </button>
                  )}
                </article>
              ))}
              {notes.length === 0 && (
                <div className="empty">You’re all caught up.</div>
              )}
            </div>
          )}
          {page === "People" &&
            (user.role === "ADMIN" ? (
              <PeoplePage
                accounts={accounts}
                onSaved={async () => {
                  await reload();
                  setToast("Account directory updated.");
                }}
              />
            ) : (
              <div className="placeholder-card">
                <div className="placeholder-icon">♙</div>
                <h2>School directory</h2>
                <p>
                  Account administration is available to school administrators.
                </p>
              </div>
            ))}
          {page === "Profile" && (
            <div className="profile-card panel">
              <div className="profile-card-avatar">{user.name.slice(0, 1).toUpperCase()}</div>
              <div className="profile-card-details">
                <h2>{user.name}</h2>
                <p>{user.email}</p>
                <span className="chip">{user.role}</span>
              </div>
              <button className="secondary" onClick={signOut}>Log out</button>
            </div>
          )}
        </section>
      </main>
      {toast && (
        <div className="toast" role="status" onClick={() => setToast("")}>
          <span className="toast-check" aria-hidden="true">OK</span>
          <span>{toast}</span>
          <button type="button" aria-label="Dismiss message" onClick={() => setToast("")}>x</button>
        </div>
      )}
      <div className="assistant-widget">
        {assistantOpen && (
          <section className="assistant-panel" aria-label="School assistant chat">
            <header><div><strong>School assistant</strong><small>Navigation and quick answers</small></div><button aria-label="Close assistant" onClick={() => setAssistantOpen(false)}>×</button></header>
            <div className="assistant-history" aria-live="polite">
              {assistantMessages.map((message) => <div key={message.id} className={`assistant-bubble ${message.from}`}>{message.text}</div>)}
            </div>
            <div className="assistant-quick-links">
              {(["Overview", "Attendance", "Leave", "Bus Tracking"] as Page[]).map((destination) => <button key={destination} onClick={() => { setPage(destination); setAssistantOpen(false); }}>{destination === "Overview" ? "Home" : destination}</button>)}
            </div>
            <form className="assistant-composer" onSubmit={(event) => { event.preventDefault(); askAssistant(assistantInput); }}>
              <input value={assistantInput} onChange={(event) => setAssistantInput(event.target.value)} placeholder="Ask or find a section..." aria-label="Ask the school assistant" />
              <button className="primary">Send</button>
            </form>
          </section>
        )}
        <button className="assistant-launcher" aria-label={assistantOpen ? "Close school assistant" : "Open school assistant"} onClick={() => setAssistantOpen((open) => !open)}>{assistantOpen ? "×" : "?"}</button>
      </div>
      {edit && (
        <StudentEditor
          student={edit}
          busRoutes={busRoutes}
          onClose={() => setEdit(null)}
          onSaved={async () => {
            setEdit(null);
            await reload();
            setToast(
              edit.id === 0
                ? "Student record created."
                : "Student record updated.",
            );
          }}
        />
      )}
    </div>
  );
}
function EventsCalendar({ items }: { items: SchoolEvent[] }) {
  const [year, setYear] = useState(new Date().getFullYear());
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().slice(0, 10));
  const selectedEvents = items.filter((event) => event.eventDate === selectedDate);
  const selectedDayIsSunday = new Date(`${selectedDate}T00:00:00`).getDay() === 0;
  return (
    <div className="calendar-panel panel">
      <div className="calendar-header">
        <div><h2>Yearly school calendar</h2><p>Sundays are weekly holidays. School holidays and events are maintained by the administration.</p></div>
        <div className="calendar-year-control"><button aria-label="Previous year" onClick={() => setYear((value) => value - 1)}>‹</button><strong>{year}</strong><button aria-label="Next year" onClick={() => setYear((value) => value + 1)}>›</button></div>
      </div>
      <div className="calendar-legend"><span><i className="calendar-holiday-dot" /> Sunday / holiday</span><span><i className="calendar-event-dot" /> School event</span></div>
      <div className="calendar-grid">
        {Array.from({ length: 12 }, (_, month) => {
          const firstWeekday = new Date(year, month, 1).getDay();
          const days = new Date(year, month + 1, 0).getDate();
          return <section className="calendar-month" key={month}>
            <h3>{new Date(year, month, 1).toLocaleDateString(undefined, { month: "long" })}</h3>
            <div className="calendar-days">
              {(["S", "M", "T", "W", "T", "F", "S"] as const).map((day, index) => <span className="calendar-weekday" key={`${day}-${index}`}>{day}</span>)}
              {Array.from({ length: firstWeekday }, (_, index) => <span className="calendar-blank" key={`blank-${index}`} />)}
              {Array.from({ length: days }, (_, index) => {
                const day = index + 1;
                const date = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
                const holiday = new Date(year, month, day).getDay() === 0 || items.some((event) => event.eventDate === date && event.holiday);
                const hasEvent = items.some((event) => event.eventDate === date);
                return <button key={date} className={`calendar-day ${holiday ? "holiday" : ""} ${date === selectedDate ? "selected" : ""}`} aria-label={`${new Date(`${date}T00:00:00`).toLocaleDateString()}${holiday ? ", holiday" : hasEvent ? ", school event" : ""}`} onClick={() => setSelectedDate(date)}>{day}{(holiday || hasEvent) && <i className={hasEvent ? "has-event" : ""} />}</button>;
              })}
            </div>
          </section>;
        })}
      </div>
      <div className="calendar-selected">
        <h3>{new Date(`${selectedDate}T00:00:00`).toLocaleDateString(undefined, { weekday: "long", year: "numeric", month: "long", day: "numeric" })}</h3>
        {selectedDayIsSunday && <div className="calendar-event"><strong>Weekly holiday</strong><span>Sunday</span></div>}
        {selectedEvents.map((event) => <article className="calendar-event" key={event.id}><div><strong>{event.title}</strong><p>{event.description || "No event details provided."}</p><small>{event.location || "School"}</small></div>{event.holiday && <span>Holiday</span>}</article>)}
        {!selectedDayIsSunday && selectedEvents.length === 0 && <p className="muted">No school events are scheduled for this date.</p>}
      </div>
    </div>
  );
}
function EventsPage({
  user,
  items,
  onSaved,
}: {
  user: User;
  items: SchoolEvent[];
  onSaved: () => Promise<void>;
}) {
  const [draft, setDraft] = useState<{
    id?: number;
    title: string;
    description: string;
    eventDate: string;
    location: string;
    holiday: boolean;
  }>({ title: "", description: "", eventDate: "", location: "", holiday: false });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const isAdmin = user.role === "ADMIN";

  async function save(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api(draft.id ? `/events/${draft.id}` : "/events", {
        method: draft.id ? "PUT" : "POST",
        body: JSON.stringify({
          title: draft.title,
          description: draft.description,
          eventDate: draft.eventDate,
          location: draft.location,
          holiday: draft.holiday,
        }),
      });
      setDraft({ title: "", description: "", eventDate: "", location: "", holiday: false });
      await onSaved();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Could not save event");
    } finally {
      setBusy(false);
    }
  }

  async function remove(item: SchoolEvent) {
    if (!window.confirm(`Delete ${item.title}?`)) return;
    try {
      await api(`/events/${item.id}`, { method: "DELETE" });
      await onSaved();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Could not delete event");
    }
  }

  return (
    <div className="records-layout">
      {isAdmin && (
        <form className="panel form-panel" onSubmit={save}>
          <h2>{draft.id ? "Edit school event" : "Add school event"}</h2>
          <p className="muted">Mark a school closure as a holiday to highlight it in the calendar and send the reminder the day before. The school controls these dates.</p>
          {error && <div className="form-error">{error}</div>}
          <div className="form-grid">
            <label>
              Event title
              <input
                required
                maxLength={120}
                value={draft.title}
                onChange={(event) => setDraft({ ...draft, title: event.target.value })}
              />
            </label>
            <label>
              Date
              <input
                required
                type="date"
                value={draft.eventDate}
                onChange={(event) => setDraft({ ...draft, eventDate: event.target.value })}
              />
            </label>
            <label>
              Location
              <input
                value={draft.location}
                onChange={(event) => setDraft({ ...draft, location: event.target.value })}
              />
            </label>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={draft.holiday}
                onChange={(event) => setDraft({ ...draft, holiday: event.target.checked })}
              />
              School holiday
            </label>
            <label className="wide">
              Description
              <textarea
                rows={3}
                value={draft.description}
                onChange={(event) => setDraft({ ...draft, description: event.target.value })}
              />
            </label>
          </div>
          <div className="modal-actions">
            <button className="primary" disabled={busy}>
              {busy ? "Saving…" : draft.id ? "Save changes" : "Add event"}
            </button>
            {draft.id && (
              <button
                type="button"
                className="secondary"
                onClick={() => setDraft({ title: "", description: "", eventDate: "", location: "", holiday: false })}
              >
                Cancel edit
              </button>
            )}
          </div>
        </form>
      )}
      <div className="record-list">
        {items.map((item) => (
          <article className="record-card" key={item.id}>
            <div>
              <span className="record-date">{item.eventDate}</span>
              <h3>{item.title}{item.holiday && <span className="holiday-badge">HOLIDAY</span>}</h3>
              <p>{item.description || "No event details provided."}</p>
              <small>{item.location || "Location to be announced"} · Added by {item.createdBy}</small>
            </div>
            {isAdmin && (
              <div className="record-actions">
                <button
                  className="secondary"
                  onClick={() => setDraft({ ...item })}
                >
                  Edit
                </button>
                <button className="danger-button" onClick={() => remove(item)}>
                  Delete
                </button>
              </div>
            )}
          </article>
        ))}
        {items.length === 0 && <div className="empty">No school events are scheduled.</div>}
      </div>
    </div>
  );
}
function LeavePage({
  user,
  items,
  students,
  onSaved,
}: {
  user: User;
  items: LeaveApplication[];
  students: Student[];
  onSaved: () => Promise<void>;
}) {
  const [draft, setDraft] = useState<{
    id?: number;
    studentId: string;
    startDate: string;
    endDate: string;
    reason: string;
  }>({ studentId: "", startDate: "", endDate: "", reason: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const isAdmin = user.role === "ADMIN";
  const canRequest = ["ADMIN", "TEACHER", "PARENT"].includes(user.role);
  const [leaveQuery, setLeaveQuery] = useState("");
  const [leaveStatus, setLeaveStatus] = useState("ALL");
  const [leavePage, setLeavePage] = useState(0);
  const matchedLeaves = items.filter((item) => {
    const matchesQuery = `${item.requesterName} ${item.requesterEmail} ${item.studentName ?? ""} ${item.reason}`.toLowerCase().includes(leaveQuery.toLowerCase());
    return matchesQuery && (leaveStatus === "ALL" || item.status === leaveStatus);
  });
  const pageSize = 10;
  const pagedLeaves = matchedLeaves.slice(leavePage * pageSize, (leavePage + 1) * pageSize);

  async function save(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await api(draft.id ? `/leave-requests/${draft.id}` : "/leave-requests", {
        method: draft.id ? "PUT" : "POST",
        body: JSON.stringify({
          studentId: draft.studentId ? Number(draft.studentId) : null,
          startDate: draft.startDate,
          endDate: draft.endDate,
          reason: draft.reason,
        }),
      });
      setDraft({ studentId: "", startDate: "", endDate: "", reason: "" });
      await onSaved();
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : "Could not save leave request");
    } finally {
      setBusy(false);
    }
  }

  async function updateStatus(item: LeaveApplication, status: "APPROVED" | "REJECTED") {
    setError("");
    try {
      const remark = window.prompt("Review note (optional)", item.reviewerRemark ?? "");
      if (remark === null) return;
      await api(`/leave-requests/${item.id}/status`, {
        method: "PATCH",
        body: JSON.stringify({ status, remark }),
      });
      await onSaved();
    } catch (statusError) {
      setError(statusError instanceof Error ? statusError.message : "Could not update request");
    }
  }

  async function remove(item: LeaveApplication) {
    if (!window.confirm("Delete this leave request?")) return;
    setError("");
    try {
      await api(`/leave-requests/${item.id}`, { method: "DELETE" });
      await onSaved();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Could not delete request");
    }
  }

  return (
    <div className="records-layout">
      {canRequest && (
        <form className="panel form-panel" onSubmit={save}>
          <h2>{draft.id ? "Edit pending leave request" : "Request leave"}</h2>
          {error && <div className="form-error">{error}</div>}
          {user.role !== "TEACHER" && (
            <label>
              Student {user.role === "PARENT" ? "(required)" : "(optional)"}
              <select
                required={user.role === "PARENT"}
                value={draft.studentId}
                onChange={(event) => setDraft({ ...draft, studentId: event.target.value })}
              >
                {user.role === "ADMIN" && <option value="">Staff or general request</option>}
                {students.map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.firstName} {student.lastName} · {student.className}-{student.division}
                  </option>
                ))}
              </select>
            </label>
          )}
          <div className="form-grid">
            <label>
              Start date
              <input
                required
                type="date"
                value={draft.startDate}
                onChange={(event) => setDraft({ ...draft, startDate: event.target.value })}
              />
            </label>
            <label>
              End date
              <input
                required
                type="date"
                value={draft.endDate}
                onChange={(event) => setDraft({ ...draft, endDate: event.target.value })}
              />
            </label>
            <label className="wide">
              Reason
              <textarea
                required
                maxLength={2000}
                rows={3}
                value={draft.reason}
                onChange={(event) => setDraft({ ...draft, reason: event.target.value })}
              />
            </label>
          </div>
          <div className="modal-actions">
            <button className="primary" disabled={busy}>
              {busy ? "Saving…" : draft.id ? "Save changes" : "Submit request"}
            </button>
            {draft.id && (
              <button
                type="button"
                className="secondary"
                onClick={() => setDraft({ studentId: "", startDate: "", endDate: "", reason: "" })}
              >
                Cancel edit
              </button>
            )}
          </div>
        </form>
      )}
      {error && !canRequest && <div className="error-banner">{error}</div>}
      <div className="form-grid"><label>Search requests<input value={leaveQuery} onChange={(event) => { setLeaveQuery(event.target.value); setLeavePage(0); }} placeholder="Name, email, reason" /></label><label>Status<select value={leaveStatus} onChange={(event) => { setLeaveStatus(event.target.value); setLeavePage(0); }}><option value="ALL">All statuses</option><option value="PENDING">Pending</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select></label></div>
      <div className="record-list">
        {pagedLeaves.map((item) => {
          const isOwner = item.requesterEmail === user.email;
          const pending = item.status === "PENDING";
          const canReview = isAdmin || (user.role === "TEACHER" && item.studentId !== null);
          return (
            <article className="record-card" key={item.id}>
              <div>
                <span className={`status ${item.status.toLowerCase()}`}>{item.status}</span>
                <h3>{item.studentId === null ? `Staff leave · ${item.requesterName}` : `Student leave · ${item.studentName}`}</h3>
                <p>{item.reason}</p>
                <small>
                  {item.startDate} – {item.endDate} · Requested by {item.requesterName}
                </small>
                {item.reviewedAt && <p><small>Reviewed by {item.reviewerName || "school staff"} · {new Date(item.reviewedAt).toLocaleString()} · {item.reviewerRemark || "No review note"}</small></p>}
              </div>
              <div className="record-actions">
                {canReview && pending && (
                  <>
                    <button className="primary" onClick={() => updateStatus(item, "APPROVED")}>
                      Approve
                    </button>
                    <button className="secondary" onClick={() => updateStatus(item, "REJECTED")}>
                      Reject
                    </button>
                  </>
                )}
                {isOwner && pending && (
                  <button
                    className="secondary"
                    onClick={() => setDraft({
                      id: item.id,
                      studentId: item.studentId ? String(item.studentId) : "",
                      startDate: item.startDate,
                      endDate: item.endDate,
                      reason: item.reason,
                    })}
                  >
                    Edit
                  </button>
                )}
                {(isAdmin || (isOwner && pending)) && (
                  <button className="danger-button" onClick={() => remove(item)}>
                    Delete
                  </button>
                )}
              </div>
            </article>
          );
        })}
        {matchedLeaves.length === 0 && <div className="empty">No leave requests match these filters.</div>}
      </div>
      {matchedLeaves.length > pageSize && <div className="modal-actions"><button className="secondary" disabled={leavePage === 0} onClick={() => setLeavePage((page) => Math.max(0, page - 1))}>Previous</button><span>Page {leavePage + 1} of {Math.ceil(matchedLeaves.length / pageSize)}</span><button className="secondary" disabled={(leavePage + 1) * pageSize >= matchedLeaves.length} onClick={() => setLeavePage((page) => page + 1)}>Next</button></div>}
    </div>
  );
}

function Login({
  onLogin,
  busy,
  error,
}: {
  onLogin: (e: string, p: string) => void;
  busy: boolean;
  error: string;
}) {
  const [email, setEmail] = useState(import.meta.env.DEV ? "admin@northstar.local" : ""),
    [password, setPassword] = useState(import.meta.env.DEV ? "Admin@123" : "");
  return (
    <main className="login-page">
      <div className="login-card">
        <div className="brand login-brand">
          <span className="brand-mark">N</span>
          <span>
            Northstar<span className="brand-sub">SCHOOL LMS</span>
          </span>
        </div>
        <div className="eyebrow">WELCOME BACK</div>
        <h1>Sign in to your school</h1>
        <p>One workspace for the people who help students thrive.</p>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            onLogin(email, password);
          }}
        >
          <label>
            Email address
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </label>
          {error && <div className="form-error">{error}</div>}
          <button className="primary full" disabled={busy}>
            {busy ? "Signing in…" : "Sign in →"}
          </button>
        </form>
        {import.meta.env.DEV && (
          <>
            <div className="demo-title">
              <span />
              DEMO ACCOUNTS
              <span />
            </div>
            <div className="demo-grid">
              {demos.map(([role, e, p]) => (
                <button
                  key={role}
                  onClick={() => {
                    setEmail(e);
                    setPassword(p);
                  }}
                >
                  <span>{role[0]}</span>
                  <b>{role}</b>
                </button>
              ))}
            </div>
          </>
        )}
        <small className="login-foot">
          Connected to the Northstar School API · MySQL
        </small>
      </div>
      <div className="login-aside">
        <div className="aside-orb orb-a" />
        <div className="aside-orb orb-b" />
        <div className="quote-mark">“</div>
        <blockquote>
          Education is the most powerful tool we can use to shape a brighter
          tomorrow.
        </blockquote>
        <div className="quote-rule" />
        <span>NORTHSTAR SCHOOL · EST. 1987</span>
      </div>
    </main>
  );
}
function Overview({
  user,
  summary,
  students,
  homework,
  notes,
  go,
}: {
  user: User;
  summary: Record<string, number>;
  students: Student[];
  homework: Homework[];
  notes: Note[];
  go: (p: Page) => void;
}) {
  const stats =
    user.role === "ADMIN"
      ? [
          [
            "Total students",
            summary.totalStudents ?? 0,
            "School roster",
            "blue",
          ],
          [
            "Teachers",
            summary.totalTeachers ?? 0,
            "Across all grades",
            "purple",
          ],
          ["Parents", summary.totalParents ?? 0, "Connected families", "green"],
          [
            "Class groups",
            summary.totalClasses ?? 0,
            "Academic year 2026–27",
            "orange",
          ],
        ]
      : [
          ["My children", students.length, "Family account", "blue"],
          ["Homework", homework.length, "Assigned work", "purple"],
          [
            "Notifications",
            notes.filter((n) => !n.read).length,
            "Unread updates",
            "orange",
          ],
          [
            "Attendance",
            students.length ? "On track" : "—",
            "Latest records",
            "green",
          ],
        ];
  const totalToday = (summary.presentToday || 0) + (summary.absentToday || 0);
  const attendancePercent = totalToday
    ? Math.round(((summary.presentToday || 0) / totalToday) * 100)
    : 0;
  return (
    <>
      <div className="stats-grid">
        {stats.map(([label, value, caption, tone]) => (
          <div className="stat-card" key={String(label)}>
            <div className="stat-top">
              <span>{label}</span>
              <i className={`stat-icon ${tone}`}>
                {String(label).includes("student") ||
                String(label).includes("children")
                  ? "♙"
                  : String(label).includes("Teacher")
                    ? "♧"
                    : String(label).includes("Parent")
                      ? "♡"
                      : "◷"}
              </i>
            </div>
            <strong>{value}</strong>
            <small>{caption}</small>
          </div>
        ))}
      </div>
      <div className="dashboard-grid">
        <div className="panel attendance-panel">
          <div className="panel-heading">
            <div>
              <h2>Today’s attendance</h2>
              <p>Class presence overview</p>
            </div>
            <button className="link-button" onClick={() => go("Attendance")}>
              View attendance →
            </button>
          </div>
          <div className="attendance-visual">
            <div
              className="donut"
              style={{
                background: `conic-gradient(#4a8de0 0 ${attendancePercent}%,#f0ad58 ${attendancePercent}% 100%)`,
              }}
            >
              <div>
                <b>{attendancePercent}%</b>
                <small>present</small>
              </div>
            </div>
            <div className="legend">
              <span>
                <i className="dot present-dot" />
                Present <b>{summary.presentToday || 0}</b>
              </span>
              <span>
                <i className="dot absent-dot" />
                Absent <b>{summary.absentToday || 0}</b>
              </span>
              <span>
                <i className="dot leave-dot" />
                Pending fees <b>{summary.pendingFees || 0}</b>
              </span>
            </div>
          </div>
        </div>
        <div className="panel activity-panel">
          <div className="panel-heading">
            <div>
              <h2>Recent activity</h2>
              <p>The latest from your school</p>
            </div>
            <span className="live-dot" />
          </div>
          <div className="activity-list">
            {notes.slice(0, 3).map((n) => (
              <div className="activity" key={n.id}>
                <span className="activity-bullet">
                  {n.title.includes("Attendance") ? "✓" : "•"}
                </span>
                <div>
                  <strong>{n.title}</strong>
                  <p>{n.message}</p>
                  <small>{new Date(n.createdAt).toLocaleDateString()}</small>
                </div>
              </div>
            ))}
            {homework.slice(0, 2).map((h) => (
              <div className="activity" key={`h${h.id}`}>
                <span className="activity-bullet purple-bullet">▧</span>
                <div>
                  <strong>{h.title}</strong>
                  <p>
                    {h.className} · {h.subject}
                  </p>
                  <small>Due {h.dueDate}</small>
                </div>
              </div>
            ))}
            {notes.length + homework.length === 0 && (
              <div className="empty small-empty">Nothing new today.</div>
            )}
          </div>
        </div>
      </div>
      <div className="lower-grid">
        <div className="panel mini-table">
          <div className="panel-heading">
            <div>
              <h2>
                {user.role === "PARENT"
                  ? "Your children"
                  : "Recently enrolled students"}
              </h2>
              <p>A quick look at the student directory</p>
            </div>
            <button className="link-button" onClick={() => go("Students")}>
              See all →
            </button>
          </div>
          {students.slice(0, 4).map((s) => (
            <div className="mini-row" key={s.id}>
              <div className="student-avatar">
                {s.firstName[0]}
                {s.lastName[0]}
              </div>
              <strong>
                {s.firstName} {s.lastName}
              </strong>
              <span>
                {s.className}-{s.division}
              </span>
              <i className="mini-status" />
            </div>
          ))}
        </div>
        <div className="panel homework-preview">
          <div className="panel-heading">
            <div>
              <h2>Upcoming homework</h2>
              <p>Due dates across your classes</p>
            </div>
            <button className="link-button" onClick={() => go("Homework")}>
              View all →
            </button>
          </div>
          {homework.slice(0, 3).map((h) => (
            <div className="homework-row" key={h.id}>
              <div className="homework-icon">▧</div>
              <div>
                <strong>{h.title}</strong>
                <small>
                  {h.subject} · Class {h.className}{h.division ? `-${h.division}` : " · all divisions"}
                </small>
              </div>
              <span>{h.dueDate}</span>
            </div>
          ))}
          {homework.length === 0 && (
            <div className="empty small-empty">No assignments due soon.</div>
          )}
        </div>
      </div>
    </>
  );
}
function Attendance({
  students,
  onSaved,
  user,
}: {
  students: Student[];
  onSaved: () => void;
  user: User;
}) {
  const [id, setId] = useState(""),
    [status, setStatus] = useState("ABSENT"),
    [saving, setSaving] = useState(false),
    [error, setError] = useState(""),
    [records, setRecords] = useState<Record<number, AttendanceRecord[]>>({});
  useEffect(() => {
    if (students.length && !id) setId(String(students[0].id));
  }, [students, id]);
  useEffect(() => {
    if (user.role === "ADMIN" || user.role === "TEACHER") return;
    Promise.all(
      students.map((s) =>
        api<AttendanceRecord[]>(`/attendance/student/${s.id}`).then(
          (rows) => [s.id, rows] as const,
        ),
      ),
    )
      .then((rows) => setRecords(Object.fromEntries(rows)))
      .catch((e) =>
        setError(e instanceof Error ? e.message : "Could not load attendance"),
      );
  }, [students, user]);
  async function save() {
    setSaving(true);
    setError("");
    try {
      await api("/attendance", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(id),
          date: new Date().toISOString().slice(0, 10),
          status,
        }),
      });
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not save attendance");
    } finally {
      setSaving(false);
    }
  }
  if (user.role !== "ADMIN" && user.role !== "TEACHER")
    return (
      <div className="panel form-panel">
        <h2>Attendance history</h2>
        <p>Daily attendance recorded by your school.</p>
        {error && <div className="form-error">{error}</div>}
        {students.map((s) => {
          const rows = records[s.id] || [];
          const present = rows.filter(
            (r) => r.status === "PRESENT" || r.status === "LATE",
          ).length;
          const percent = rows.length
            ? Math.round((present / rows.length) * 100)
            : 0;
          return (
            <div className="attendance-table" key={s.id}>
              <h3>
                {s.firstName} {s.lastName} · {percent}% present
              </h3>
              {rows.map((r) => (
                <div className="roster-row" key={r.id}>
                  <strong>
                    {new Date(`${r.date}T00:00:00`).toLocaleDateString()}
                  </strong>
                  <span className={`status ${r.status.toLowerCase()}`}>
                    {r.status}
                  </span>
                </div>
              ))}
              {rows.length === 0 && <p>No attendance has been recorded yet.</p>}
            </div>
          );
        })}
      </div>
    );
  return (
    <div className="panel form-panel">
      <h2>Record daily attendance</h2>
      <p>
        Attendance is saved to the school database. Parents receive an in-app
        notification when a student is marked absent.
      </p>
      <div className="form-grid">
        <label>
          Student
          <select value={id} onChange={(e) => setId(e.target.value)}>
            {students.map((s) => (
              <option key={s.id} value={s.id}>
                {s.firstName} {s.lastName} · {s.className}-{s.division}
              </option>
            ))}
          </select>
        </label>
        <label>
          Date
          <input
            type="date"
            defaultValue={new Date().toISOString().slice(0, 10)}
          />
        </label>
        <label>
          Attendance status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            {["PRESENT", "ABSENT", "LATE", "LEAVE"].map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </label>
      </div>
      {error && <div className="form-error">{error}</div>}
      <button
        className="primary"
        onClick={save}
        disabled={saving || !students.length}
      >
        {saving ? "Saving…" : "Save attendance"}
      </button>
      {students.length > 0 && (
        <div className="attendance-table">
          <h3>Class roster</h3>
          {students.map((s) => (
            <div className="roster-row" key={s.id}>
              <div className="student-avatar">
                {s.firstName[0]}
                {s.lastName[0]}
              </div>
              <strong>
                {s.firstName} {s.lastName}
              </strong>
              <span>
                {s.className}-{s.division}
              </span>
              <span className="class-chip">Roll {s.rollNumber}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
function TimetablePage({ user, items, accounts, students, onSaved }: { user: User; items: TimetableEntry[]; accounts: Account[]; students: Student[]; onSaved: () => void }) {
  const [editingId, setEditingId] = useState<number | null>(null), [className, setClassName] = useState("1"), [division, setDivision] = useState("A"), [dayOfWeek, setDay] = useState("MONDAY"), [subject, setSubject] = useState("Mathematics"), [teacherId, setTeacher] = useState(""), [room, setRoom] = useState("Room 1"), [startTime, setStart] = useState("09:00"), [endTime, setEnd] = useState("09:45"), [error, setError] = useState("");
  const teachers = accounts.filter((a) => a.role === "TEACHER");
  const divisions = Array.from(new Set(students.filter((s) => s.className === className).map((s) => s.division)));
  async function save(e: React.FormEvent) { e.preventDefault(); setError(""); try { const payload = { academicYear: `${new Date().getFullYear()}-${new Date().getFullYear()+1}`, className, division, dayOfWeek, subject, teacherId: Number(teacherId), room, startTime, endTime }; await api(editingId ? `/timetable/${editingId}` : "/timetable", { method: editingId ? "PUT" : "POST", body: JSON.stringify(payload) }); setEditingId(null); onSaved(); } catch (e) { setError(e instanceof Error ? e.message : "Could not save timetable"); } }
  function edit(row: TimetableEntry) { setEditingId(row.id); setClassName(row.className); setDivision(row.division); setDay(row.dayOfWeek); setSubject(row.subject); setTeacher(String(row.teacherId)); setRoom(row.room); setStart(row.startTime); setEnd(row.endTime); }
  return <div className="homework-layout">{user.role === "ADMIN" && <form className="panel form-panel" onSubmit={save}><h2>{editingId ? "Edit timetable period" : "Add timetable period"}</h2><div className="form-grid"><label>Class<input value={className} onChange={e=>setClassName(e.target.value)} required /></label><label>Division<select value={division} onChange={e=>setDivision(e.target.value)}>{(divisions.includes(division) ? divisions : [division, ...divisions]).map(x=><option key={x}>{x}</option>)}</select></label><label>Day<select value={dayOfWeek} onChange={e=>setDay(e.target.value)}>{["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"].map(x=><option key={x}>{x}</option>)}</select></label><label>Subject<input value={subject} onChange={e=>setSubject(e.target.value)} required /></label><label>Teacher<select value={teacherId} onChange={e=>setTeacher(e.target.value)} required><option value="">Select teacher</option>{teachers.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}</select></label><label>Room<input value={room} onChange={e=>setRoom(e.target.value)} /></label><label>Starts<input type="time" value={startTime} onChange={e=>setStart(e.target.value)} required /></label><label>Ends<input type="time" value={endTime} onChange={e=>setEnd(e.target.value)} required /></label></div>{error && <div className="form-error">{error}</div>}<button className="primary">{editingId ? "Save changes" : "Add period"}</button>{editingId && <button type="button" className="secondary" onClick={()=>setEditingId(null)}>Cancel edit</button>}</form>}<div className="panel assignments-panel"><h2>Class timetable</h2>{["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"].map(day=>{const rows=items.filter(i=>i.dayOfWeek===day); return rows.length ? <section key={day}><h3>{day}</h3>{rows.map(row=><article className="assignment" key={row.id}><strong>{row.startTime}–{row.endTime} · {row.subject}</strong><span>Class {row.className}-{row.division} · {row.teacherName}{row.room ? ` · ${row.room}` : ""}</span>{user.role==="ADMIN" && <><button className="text-action" onClick={()=>edit(row)}>Edit</button><button className="text-action" onClick={async()=>{await api(`/timetable/${row.id}`,{method:"DELETE"});onSaved();}}>Delete</button></>}</article>)}</section> : null;})}{items.length===0 && <div className="empty">No timetable has been published yet.</div>}</div></div>;
}

function HomeworkPage({
  user,
  items,
  students,
  onSaved,
}: {
  user: User;
  items: Homework[];
  students: Student[];
  onSaved: () => void;
}) {
  const [title, setTitle] = useState(""),
    [description, setDescription] = useState(""),
    [subject, setSubject] = useState("Mathematics"),
    [className, setClassName] = useState("6"),
    [division, setDivision] = useState("A"),
    [dueDate, setDueDate] = useState(
      new Date(Date.now() + 4 * 86400000).toISOString().slice(0, 10),
    ),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false);
  const canCreate = user.role === "ADMIN" || user.role === "TEACHER";
  async function create(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      await api("/homework", {
        method: "POST",
        body: JSON.stringify({
          title,
          description,
          className,
          division,
          subject,
          dueDate,
        }),
      });
      setTitle("");
      setDescription("");
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not publish homework");
    } finally {
      setSaving(false);
    }
  }
  const classes = useMemo(
    () => Array.from(new Set(students.map((s) => s.className))),
    [students],
  );
  const divisions = Array.from(new Set(students.filter((s) => s.className === className).map((s) => s.division)));
  return (
    <div className="homework-layout">
      {canCreate && (
        <form className="panel form-panel homework-form" onSubmit={create}>
          <h2>Publish homework</h2>
          <p>Families in the selected class and division receive a notification.</p>
          <label>
            Assignment title
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
              placeholder="e.g. Fractions practice"
            />
          </label>
          <label>
            Instructions
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              placeholder="Add a short description"
            />
          </label>
          <div className="form-grid">
            <label>
              Class
              <select
                value={className}
                onChange={(e) => setClassName(e.target.value)}
              >
                {(classes.length ? classes : ["6", "7", "8", "9", "10"]).map(
                  (c) => (
                    <option key={c}>{c}</option>
                  ),
                )}
              </select>
            </label>
            <label>Division<select value={division} onChange={(e) => setDivision(e.target.value)}>{(divisions.length ? divisions : ["A"]).map((d) => <option key={d}>{d}</option>)}</select></label>
            <label>
              Subject
              <select
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
              >
                {[
                  "Mathematics",
                  "Science",
                  "English",
                  "Marathi",
                  "Hindi",
                  "Social Science",
                  "Computer",
                ].map((c) => (
                  <option key={c}>{c}</option>
                ))}
              </select>
            </label>
          </div>
          <label>
            Due date
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              required
            />
          </label>
          {error && <div className="form-error">{error}</div>}
          <button className="primary" disabled={saving}>
            {saving ? "Publishing…" : "Publish assignment →"}
          </button>
        </form>
      )}
      <div className="panel assignments-panel">
        <div className="panel-heading">
          <div>
            <h2>Assignments</h2>
            <p>{items.length} assignments in your classes</p>
          </div>
        </div>
        {items.map((h) => (
          <article className="assignment" key={h.id}>
            <span className="assignment-icon">▧</span>
            <div>
              <strong>{h.title}</strong>
              <p>{h.description}</p>
              <small>
                {h.subject} · Class {h.className}-{h.division} · {h.teacherName}
              </small>
            </div>
            <div className="due">
              <small>DUE</small>
              <b>{h.dueDate}</b>
            </div>
          </article>
        ))}
        {items.length === 0 && (
          <div className="empty">No homework has been published yet.</div>
        )}
      </div>
    </div>
  );
}
function ResultsPage({
  user,
  students,
  results,
  onSaved,
}: {
  user: User;
  students: Student[];
  results: Result[];
  onSaved: () => void;
}) {
  const [studentId, setStudentId] = useState(""),
    [examName, setExamName] = useState("Term 1"),
    [subject, setSubject] = useState("Mathematics"),
    [marks, setMarks] = useState(""),
    [maximum, setMaximum] = useState("100"),
    [examDate, setExamDate] = useState(new Date().toISOString().slice(0, 10)),
    [error, setError] = useState("");
  useEffect(() => {
    if (students.length && !studentId) setStudentId(String(students[0].id));
  }, [students, studentId]);
  async function add(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api("/results", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(studentId),
          examName,
          subject,
          marks: Number(marks),
          maximumMarks: Number(maximum),
          examDate,
        }),
      });
      setMarks("");
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to save result");
    }
  }
  return (
    <div className="homework-layout">
      {["ADMIN", "TEACHER"].includes(user.role) && (
        <form className="panel form-panel" onSubmit={add}>
          <h2>Enter marks</h2>
          <p>Saved results are visible to the student and their parent.</p>
          <label>
            Student
            <select
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
            >
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.firstName} {s.lastName} · {s.className}-{s.division}
                </option>
              ))}
            </select>
          </label>
          <label>
            Exam
            <input
              value={examName}
              onChange={(e) => setExamName(e.target.value)}
              required
            />
          </label>
          <label>
            Subject
            <select
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
            >
              {[
                "Mathematics",
                "Science",
                "English",
                "Marathi",
                "Hindi",
                "Social Science",
                "Computer",
              ].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </label>
          <div className="form-grid">
            <label>
              Marks
              <input
                type="number"
                min="0"
                step="0.5"
                value={marks}
                onChange={(e) => setMarks(e.target.value)}
                required
              />
            </label>
            <label>
              Maximum marks
              <input
                type="number"
                min="1"
                step="0.5"
                value={maximum}
                onChange={(e) => setMaximum(e.target.value)}
                required
              />
            </label>
          </div>
          <label>
            Exam date
            <input
              type="date"
              value={examDate}
              onChange={(e) => setExamDate(e.target.value)}
            />
          </label>
          {error && <div className="form-error">{error}</div>}
          <button className="primary">Save result</button>
        </form>
      )}
      <div className="panel assignments-panel">
        <div className="panel-heading">
          <div>
            <h2>Exam results</h2>
            <p>Your latest marks and percentages</p>
          </div>
        </div>
        {results.map((r) => (
          <article className="assignment" key={r.id}>
            <span className="assignment-icon">⌁</span>
            <div>
              <strong>
                {r.studentName} · {r.subject}
              </strong>
              <p>
                {r.examName} · {r.marks} / {r.maximumMarks}
              </p>
              <small>{r.examDate || "Date not set"}</small>
            </div>
            <div className="due">
              <small>SCORE</small>
              <b>{r.percentage}%</b>
            </div>
          </article>
        ))}
        {results.length === 0 && (
          <div className="empty">No results are available.</div>
        )}
      </div>
    </div>
  );
}
function FeesPage({
  user,
  students,
  fees,
  onSaved,
}: {
  user: User;
  students: Student[];
  fees: Fee[];
  onSaved: () => void;
}) {
  const [studentId, setStudentId] = useState(""),
    [description, setDescription] = useState("Tuition fee"),
    [amount, setAmount] = useState(""),
    [dueDate, setDueDate] = useState(
      new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
    ),
    [error, setError] = useState("");
  useEffect(() => {
    if (students.length && !studentId) setStudentId(String(students[0].id));
  }, [students, studentId]);
  async function add(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api("/fees", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(studentId),
          description,
          amount: Number(amount),
          dueDate,
        }),
      });
      setAmount("");
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to add fee");
    }
  }
  async function setStatus(id: number, status: string) {
    try {
      await api(`/fees/${id}/status?status=${status}`, { method: "PATCH" });
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to update fee");
    }
  }
  return (
    <div className="homework-layout">
      {user.role === "ADMIN" && (
        <form className="panel form-panel" onSubmit={add}>
          <h2>Add fee record</h2>
          <p>Parents can view payment status for their linked children.</p>
          <label>
            Student
            <select
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
            >
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.firstName} {s.lastName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Description
            <input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              required
            />
          </label>
          <label>
            Amount (₹)
            <input
              type="number"
              min="1"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </label>
          <label>
            Due date
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
            />
          </label>
          {error && <div className="form-error">{error}</div>}
          <button className="primary">Add fee</button>
        </form>
      )}
      <div className="panel assignments-panel">
        <div className="panel-heading">
          <div>
            <h2>Fee records</h2>
            <p>Paid and pending fees</p>
          </div>
        </div>
        {fees.map((f) => (
          <article className="assignment" key={f.id}>
            <span className="assignment-icon">₹</span>
            <div>
              <strong>
                {f.studentName} · {f.description}
              </strong>
              <p>
                ₹{f.amount.toLocaleString("en-IN")} · Due {f.dueDate || "—"}
              </p>
              <span className={`status ${f.status.toLowerCase()}`}>
                {f.status}
              </span>
            </div>
            {user.role === "ADMIN" && f.status !== "PAID" && (
              <button
                className="text-action"
                onClick={() => setStatus(f.id, "PAID")}
              >
                Mark paid
              </button>
            )}
          </article>
        ))}
        {fees.length === 0 && (
          <div className="empty">No fee records are available.</div>
        )}
      </div>
    </div>
  );
}
function MessagesPage({
  user,
  messages,
  onSaved,
}: {
  user: User;
  messages: Message[];
  onSaved: (message: string) => void;
}) {
  const [recipientEmail, setRecipientEmail] = useState(
      user.role === "PARENT"
        ? "teacher@northstar.local"
        : "parent@northstar.local",
    ),
    [subject, setSubject] = useState(""),
    [body, setBody] = useState(""),
    [error, setError] = useState("");
  const [composerOpen, setComposerOpen] = useState(false);
  async function send(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api("/messages", {
        method: "POST",
        body: JSON.stringify({ recipientEmail, subject, body }),
      });
      setSubject("");
      setBody("");
      setComposerOpen(false);
      onSaved("Message sent.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to send message");
    }
  }
  async function read(m: Message) {
    if (!m.read && m.recipientEmail === user.email) {
      await api(`/messages/${m.id}/read`, { method: "PATCH" });
      onSaved("Message marked as read.");
    }
  }
  return (
    <div className="homework-layout">
      <div className="panel form-panel"><h2>School messages</h2><p>Send a private message to a permitted school contact.</p><button className="primary" onClick={() => setComposerOpen(true)}>SEND MESSAGE</button></div>
      {composerOpen && <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) setComposerOpen(false); }}><form className="modal" onSubmit={send}>
        <div className="modal-head"><div><div className="eyebrow">SCHOOL MESSAGES</div><h2>Send a message</h2></div><button type="button" className="close" onClick={() => setComposerOpen(false)}>×</button></div>
        <p>Use the school email address of an authorized contact.</p>
        <label>
          Recipient email
          <input
            type="email"
            value={recipientEmail}
            onChange={(e) => setRecipientEmail(e.target.value)}
            required
          />
        </label>
        <label>
          Subject
          <input
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            required
          />
        </label>
        <label>
          Message
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            rows={5}
            required
          />
        </label>
        {error && <div className="form-error">{error}</div>}
        <div className="modal-actions"><button className="primary">Send message</button><button type="button" className="secondary" onClick={() => setComposerOpen(false)}>Cancel</button></div>
      </form></div>}
      <div className="panel assignments-panel">
        <div className="panel-heading">
          <div>
            <h2>Inbox & sent</h2>
            <p>Private messages in your school account</p>
          </div>
        </div>
        {messages.map((m) => (
          <article className="assignment" key={m.id} onClick={() => read(m)}>
            <span className="assignment-icon">✉</span>
            <div>
              <strong>{m.subject}</strong>
              <p>{m.body}</p>
              <small>
                {m.senderEmail === user.email
                  ? `To ${m.recipientName}`
                  : `From ${m.senderName}`}{" "}
                · {new Date(m.sentAt).toLocaleString()}
              </small>
            </div>
            {!m.read && m.recipientEmail === user.email && (
              <span className="status">NEW</span>
            )}
          </article>
        ))}
        {messages.length === 0 && <div className="empty">No messages yet.</div>}
      </div>
    </div>
  );
}
function PeoplePage({
  accounts,
  onSaved,
}: {
  accounts: Account[];
  onSaved: () => void;
}) {
  const [name, setName] = useState(""),
    [email, setEmail] = useState(""),
    [phone, setPhone] = useState(""),
    [password, setPassword] = useState(""),
    [role, setRole] = useState("PARENT"),
    [error, setError] = useState("");
  async function add(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    try {
      await api("/accounts", {
        method: "POST",
        body: JSON.stringify({ name, email, phone, role, password }),
      });
      setName("");
      setEmail("");
      setPhone("");
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to create account");
    }
  }
  async function reset() {
    if (!confirm("Are you sure you want to reset demo data?")) return;
    setError("");
    try {
      await api("/admin/demo/reset", { method: "POST" });
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to reset demo data");
    }
  }
  async function remove(a: Account) {
    if (!confirm(`Delete ${a.name}'s account?`)) return;
    setError("");
    try {
      await api(`/accounts/${a.id}`, { method: "DELETE" });
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to delete account");
    }
  }
  return (
    <div className="homework-layout">
      <form className="panel form-panel" onSubmit={add}>
        <h2>Add a school account</h2>
        <p>Parents and teachers can then sign in to their portal.</p>
        <label>
          Full name
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </label>
        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          Phone
          <input value={phone} onChange={(e) => setPhone(e.target.value)} />
        </label>
        <label>
          Role
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option>PARENT</option>
            <option>TEACHER</option>
          </select>
        </label>
        <label>
          Temporary password
          <input
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        {error && <div className="form-error">{error}</div>}
        <button className="primary">Create account</button>
      </form>
      <div className="panel assignments-panel">
        <div className="panel-heading">
          <div>
            <h2>School directory</h2>
            <p>{accounts.length} accounts · credentials are never displayed</p>
          </div>
          {import.meta.env.DEV && (
            <button className="secondary" onClick={reset}>
              Reset demo data
            </button>
          )}
        </div>
        {accounts
          .filter((a) => a.role !== "ADMIN" && a.role !== "STUDENT")
          .map((a) => (
            <article className="assignment" key={a.id}>
              <span className="assignment-icon">
                {a.role === "TEACHER" ? "♧" : "♙"}
              </span>
              <div>
                <strong>{a.name}</strong>
                <p>
                  {a.email} · {a.phone || "No phone added"}
                </p>
                <small>{a.role}</small>
              </div>
              <button className="danger-quiet" onClick={() => remove(a)}>
                Delete
              </button>
            </article>
          ))}
        {accounts.length === 0 && (
          <div className="empty">No accounts found.</div>
        )}
      </div>
    </div>
  );
}
function StudentEditor({
  student,
  busRoutes,
  onClose,
  onSaved,
}: {
  student: Student;
  busRoutes: BusRoute[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState({ ...student }),
    [error, setError] = useState(""),
    [saving, setSaving] = useState(false);
  function change(k: keyof Student, v: string | number) {
    setForm((f) => ({ ...f, [k]: v }));
  }
  async function save(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const body = {
        studentCode: form.studentCode,
        firstName: form.firstName,
        lastName: form.lastName,
        className: form.className,
        division: form.division,
        rollNumber: Number(form.rollNumber),
        parentEmail: form.parentEmail,
        phone: form.phone,
        address: form.address,
        status: form.status,
        busRouteId: form.busRouteId,
      };
      await api(student.id ? `/students/${student.id}` : "/students", {
        method: student.id ? "PUT" : "POST",
        body: JSON.stringify(body),
      });
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not save student");
    } finally {
      setSaving(false);
    }
  }
  async function remove() {
    if (
      !confirm(
        `Archive ${form.firstName} ${form.lastName}? Academic history will be retained.`,
      )
    )
      return;
    try {
      await api(`/students/${student.id}`, { method: "DELETE" });
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not delete student");
    }
  }
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <form className="modal" onSubmit={save}>
        <div className="modal-head">
          <div>
            <div className="eyebrow">STUDENT RECORD</div>
            <h2>{student.id ? "Edit student" : "Add student"}</h2>
          </div>
          <button type="button" className="close" onClick={onClose}>
            ×
          </button>
        </div>
        <div className="editor-grid">
          <label>
            Student ID
            <input
              value={form.studentCode}
              onChange={(e) => change("studentCode", e.target.value)}
              required
            />
          </label>
          <label>
            First name
            <input
              value={form.firstName}
              onChange={(e) => change("firstName", e.target.value)}
              required
            />
          </label>
          <label>
            Last name
            <input
              value={form.lastName}
              onChange={(e) => change("lastName", e.target.value)}
              required
            />
          </label>
          <label>
            Class
            <select
              value={form.className}
              onChange={(e) => change("className", e.target.value)}
            >
              {["6", "7", "8", "9", "10"].map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </label>
          <label>
            Division
            <input
              value={form.division}
              onChange={(e) => change("division", e.target.value)}
              required
            />
          </label>
          <label>
            Roll number
            <input
              type="number"
              min="1"
              value={form.rollNumber}
              onChange={(e) => change("rollNumber", Number(e.target.value))}
              required
            />
          </label>
          <label>
            Parent email
            <input
              type="email"
              value={form.parentEmail || ""}
              required
              onChange={(e) => change("parentEmail", e.target.value)}
              placeholder="parent@northstar.local"
            />
          </label>
          <p className="wide family-link-note">Students use the linked parent account to access attendance, homework, results, fees, and school updates.</p>
          <label>
            Phone
            <input
              value={form.phone || ""}
              onChange={(e) => change("phone", e.target.value)}
            />
          </label>
          <label className="wide">
            Address
            <input
              value={form.address || ""}
              onChange={(e) => change("address", e.target.value)}
            />
          </label>
          <label>
            Status
            <select
              value={form.status}
              onChange={(e) => change("status", e.target.value)}
            >
              {["ACTIVE", "INACTIVE", "TRANSFERRED", "GRADUATED"].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </label>
          <label className="wide">Bus route<select value={form.busRouteId ?? ""} onChange={(e) => setForm((f) => ({ ...f, busRouteId: e.target.value ? Number(e.target.value) : null }))}><option value="">No bus assigned</option>{busRoutes.map((route) => <option key={route.id} value={route.id}>{route.busNumber} · {route.routeName}</option>)}</select></label>
        </div>
        {error && <div className="form-error">{error}</div>}
        <div className="modal-actions">
          {student.id > 0 && (
            <button className="danger-quiet" type="button" onClick={remove}>
              Delete student
            </button>
          )}
          <span />
          <button className="secondary" type="button" onClick={onClose}>
            Cancel
          </button>
          <button className="primary" disabled={saving}>
            {saving ? "Saving…" : "Save record"}
          </button>
        </div>
      </form>
    </div>
  );
}

// ─── Bus Tracking Page ──────────────────────────────────────────────────────

function openStreetMapEmbedUrl(latitude: number, longitude: number) {
  const padding = 0.01;
  const query = new URLSearchParams({
    bbox: `${longitude - padding},${latitude - padding},${longitude + padding},${latitude + padding}`,
    layer: "mapnik",
    marker: `${latitude},${longitude}`,
  });
  return `https://www.openstreetmap.org/export/embed.html?${query}`;
}

function openStreetMapLink(latitude: number, longitude: number) {
  return `https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=16/${latitude}/${longitude}`;
}

function BusTrackingPage({
  user,
  items,
  onSaved,
  onLocationUpdated,
}: {
  user: User;
  items: BusRoute[];
  onSaved: () => void;
  onLocationUpdated: (route: BusRoute) => void;
}) {
  const [editBus, setEditBus] = useState<BusRoute | null>(null);
  const [selectedBus, setSelectedBus] = useState<BusRoute | null>(items[0] ?? null);
  const [locBusy, setLocBusy] = useState(false);
  const [locError, setLocError] = useState("");
  const [sharingLocation, setSharingLocation] = useState(false);
  const gpsWatchId = useRef<number | null>(null);
  const lastLocationSentAt = useRef(0);

  useEffect(
    () => () => {
      if (gpsWatchId.current !== null && navigator.geolocation) {
        navigator.geolocation.clearWatch(gpsWatchId.current);
      }
    },
    [],
  );

  // Keep selectedBus in sync as items refresh
  const activeBus = items.find((b) => b.id === selectedBus?.id) ?? items[0] ?? null;

  const statusColor = (s: string) =>
    s === "IN_TRANSIT" ? "#22c55e" : s === "ACTIVE" ? "#3b82f6" : s === "MAINTENANCE" ? "#f59e0b" : "#94a3b8";
  const statusLabel = (s: string) =>
    s === "IN_TRANSIT" ? "In Transit" : s === "ACTIVE" ? "At Stop" : s === "MAINTENANCE" ? "Maintenance" : "Inactive";

  function stopLocationSharing() {
    if (gpsWatchId.current !== null && navigator.geolocation) {
      navigator.geolocation.clearWatch(gpsWatchId.current);
    }
    gpsWatchId.current = null;
    setSharingLocation(false);
    setLocBusy(false);
  }

  function startLocationSharing(bus: BusRoute) {
    if (!navigator.geolocation) {
      setLocError("Location sharing is not available on this device or browser.");
      return;
    }
    setLocError("");
    lastLocationSentAt.current = 0;
    gpsWatchId.current = navigator.geolocation.watchPosition(
      (position) => {
        const now = Date.now();
        if (now - lastLocationSentAt.current < 10_000) return;
        lastLocationSentAt.current = now;
        setLocBusy(true);
        void api<BusRoute>(`/bus-routes/${bus.id}/location`, {
          method: "PUT",
          body: JSON.stringify({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            speedKmvh:
              position.coords.speed === null
                ? bus.speedKmvh
                : Number((position.coords.speed * 3.6).toFixed(1)),
            currentStop: bus.currentStop,
          }),
        })
          .then(onLocationUpdated)
          .catch((e) => setLocError(e instanceof Error ? e.message : "Location update failed"))
          .finally(() => setLocBusy(false));
      },
      (positionError) => {
        setLocError(positionError.message || "Could not read this device's location.");
        stopLocationSharing();
      },
      { enableHighAccuracy: true, maximumAge: 5_000, timeout: 20_000 },
    );
    setSharingLocation(true);
  }

  const canShareLocation = user.role === "ADMIN" || user.role === "TEACHER";
  const locationAgeMs = activeBus ? Date.now() - Date.parse(activeBus.lastUpdated) : Infinity;
  const isDemoRoute = Boolean(
    activeBus &&
      (activeBus.busNumber.toUpperCase().startsWith("DEMO-") ||
        activeBus.routeName.toLowerCase().startsWith("demo")),
  );
  const lastUpdatedText = activeBus
    ? new Date(activeBus.lastUpdated).toLocaleString()
    : "Unknown";
  const hasGpsFix =
    activeBus !== null && activeBus.latitude !== null && activeBus.longitude !== null;
  const locationIsStale =
    hasGpsFix && (!Number.isFinite(locationAgeMs) || locationAgeMs > 2 * 60_000);

  const inTransit = items.filter((b) => b.status === "IN_TRANSIT").length;
  const atStop = items.filter((b) => b.status === "ACTIVE").length;
  const maintenance = items.filter((b) => b.status === "MAINTENANCE").length;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "1.5rem" }}>
      {/* Stats Row */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: "1rem" }}>
        {[
          { label: "Total Buses", value: items.length, color: "#6366f1", icon: "🚌" },
          { label: "In Transit", value: inTransit, color: "#22c55e", icon: "🟢" },
          { label: "At Stop", value: atStop, color: "#3b82f6", icon: "🔵" },
          { label: "Maintenance", value: maintenance, color: "#f59e0b", icon: "🟡" },
        ].map((stat) => (
          <div
            key={stat.label}
            style={{
              background: "var(--surface)",
              border: "1px solid var(--border)",
              borderRadius: "12px",
              padding: "1.25rem",
              display: "flex",
              alignItems: "center",
              gap: "1rem",
            }}
          >
            <span style={{ fontSize: "2rem" }}>{stat.icon}</span>
            <div>
              <div style={{ fontSize: "1.75rem", fontWeight: 700, color: stat.color, lineHeight: 1 }}>
                {stat.value}
              </div>
              <div style={{ fontSize: "0.75rem", color: "var(--text-dim)", marginTop: "0.25rem" }}>
                {stat.label}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 340px", gap: "1.5rem" }}>
        {/* Map Panel */}
        <div
          style={{
            background: "var(--surface)",
            border: "1px solid var(--border)",
            borderRadius: "12px",
            overflow: "hidden",
          }}
        >
          <div
            style={{
              padding: "1rem 1.25rem",
              borderBottom: "1px solid var(--border)",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <div>
              <h3 style={{ margin: 0, fontSize: "1rem" }}>Live Map</h3>
              <p style={{ margin: 0, fontSize: "0.75rem", color: "var(--text-dim)" }}>
                {activeBus ? activeBus.routeName : "Select a bus to track"}
              </p>
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
              <button className="secondary" onClick={onSaved} style={{ fontSize: "0.8rem" }}>
                ↻ Refresh
              </button>
              {canShareLocation && activeBus && (
                sharingLocation ? (
                  <button className="secondary" onClick={stopLocationSharing} style={{ fontSize: "0.8rem" }}>
                    Stop GPS sharing
                  </button>
                ) : (
                  <button
                    className="secondary"
                    onClick={() => startLocationSharing(activeBus)}
                    disabled={locBusy}
                    style={{ fontSize: "0.8rem" }}
                  >
                    {locBusy ? "Getting location…" : "Start GPS sharing"}
                  </button>
                )
              )}
            </div>
          </div>
          {locError && (
            <div style={{ padding: "0.5rem 1rem", color: "#ef4444", fontSize: "0.8rem" }}>
              {locError}
            </div>
          )}
          {activeBus && hasGpsFix ? (
            <iframe
              title={`Map location for ${activeBus.busNumber}`}
              src={openStreetMapEmbedUrl(activeBus.latitude!, activeBus.longitude!)}
              loading="lazy"
              referrerPolicy="no-referrer"
              style={{ width: "100%", height: 360, border: 0, display: "block" }}
            />
          ) : (
            <div className="empty" style={{ minHeight: 180, display: "grid", placeItems: "center" }}>
              {activeBus ? "No GPS location has been reported for this bus yet." : "No bus routes are configured yet."}
            </div>
          )}

          {/* Selected Bus Details */}
          {activeBus && (
            <div style={{ padding: "1rem 1.25rem", borderTop: "1px solid var(--border)" }}>
              {isDemoRoute && (
                <div className="bus-location-notice">
                  Demo record — this saved position is not live vehicle GPS.
                </div>
              )}
              {!hasGpsFix && (
                <div className="bus-location-notice stale">
                  No GPS location has been reported for this bus.
                </div>
              )}
              {locationIsStale && (
                <div className="bus-location-notice stale">
                  Location is stale. Last reported {lastUpdatedText}.
                </div>
              )}
              {canShareLocation && (
                <p style={{ margin: "0.5rem 0 1rem", color: "var(--text-dim)", fontSize: "0.8rem" }}>
                  To publish real bus GPS, start sharing on a device travelling on this bus. Sharing stops when you leave this page.
                </p>
              )}
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(135px, 1fr))", gap: "0.75rem" }}>
                {[
                  ["Bus", activeBus.busNumber],
                  ["Route", activeBus.routeName],
                  ["Status", statusLabel(activeBus.status)],
                  ["Current Stop", activeBus.currentStop || "Not reported"],
                  ["Speed", activeBus.speedKmvh === null ? "Not reported" : `${activeBus.speedKmvh} km/h`],
                  ["Driver", activeBus.driverName],
                  ["Phone", activeBus.driverPhone],
                  [
                    "GPS Coordinates",
                    hasGpsFix
                      ? `${activeBus.latitude!.toFixed(6)}, ${activeBus.longitude!.toFixed(6)}`
                      : "Not reported",
                  ],
                  ["Last Reported", lastUpdatedText],
                ].map(([label, value]) => (
                  <div key={label}>
                    <div style={{ fontSize: "0.7rem", color: "var(--text-dim)", textTransform: "uppercase", letterSpacing: "0.05em" }}>
                      {label}
                    </div>
                    <div style={{ fontSize: "0.875rem", fontWeight: 600, marginTop: "0.2rem" }}>{value}</div>
                  </div>
                ))}
              </div>
              {hasGpsFix && (
                <a
                  href={openStreetMapLink(activeBus.latitude!, activeBus.longitude!)}
                  target="_blank"
                  rel="noreferrer"
                  style={{ display: "inline-block", marginTop: "0.75rem", fontSize: "0.8rem" }}
                >
                  Open this location in OpenStreetMap
                </a>
              )}
              <div style={{ marginTop: "0.75rem" }}>
                <div style={{ fontSize: "0.7rem", color: "var(--text-dim)", textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: "0.3rem" }}>
                  Stops
                </div>
                <div style={{ display: "flex", flexWrap: "wrap", gap: "0.4rem" }}>
                  {(activeBus.stops ?? "").split(",").filter(Boolean).map((s) => (
                    <span
                      key={s}
                      style={{
                        padding: "0.2rem 0.6rem",
                        borderRadius: "999px",
                        fontSize: "0.75rem",
                        background: s.trim() === activeBus.currentStop ? "var(--accent)" : "var(--surface-alt)",
                        color: s.trim() === activeBus.currentStop ? "white" : "var(--text)",
                        border: "1px solid var(--border)",
                      }}
                    >
                      {s.trim()}
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Bus List */}
        <div
          style={{
            background: "var(--surface)",
            border: "1px solid var(--border)",
            borderRadius: "12px",
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
          <div
            style={{
              padding: "1rem 1.25rem",
              borderBottom: "1px solid var(--border)",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
            }}
          >
            <h3 style={{ margin: 0, fontSize: "1rem" }}>Fleet ({items.length})</h3>
            {user.role === "ADMIN" && (
              <button
                className="primary"
                style={{ fontSize: "0.8rem", padding: "0.35rem 0.85rem" }}
                onClick={() =>
                  setEditBus({
                    id: 0,
                    busNumber: "",
                    routeName: "",
                    driverName: "",
                    driverPhone: "",
                    capacity: 40,
                    latitude: null,
                    longitude: null,
                    speedKmvh: null,
                    currentStop: "",
                    status: "INACTIVE",
                    stops: "Stop 1, Stop 2, Main Campus",
                    lastUpdated: new Date().toISOString(),
                  })
                }
              >
                ＋ Add bus
              </button>
            )}
          </div>
          <div style={{ flex: 1, overflowY: "auto" }}>
            {items.map((bus) => {
              const isSelected = activeBus?.id === bus.id;
              return (
                <div
                  key={bus.id}
                  onClick={() => {
                    if (gpsWatchId.current !== null) stopLocationSharing();
                    setSelectedBus(bus);
                  }}
                  style={{
                    padding: "0.9rem 1.25rem",
                    borderBottom: "1px solid var(--border)",
                    cursor: "pointer",
                    background: isSelected ? "var(--surface-alt)" : "transparent",
                    transition: "background 0.15s",
                    display: "flex",
                    alignItems: "center",
                    gap: "0.875rem",
                  }}
                >
                  <div
                    style={{
                      width: 38,
                      height: 38,
                      borderRadius: "50%",
                      background: `${statusColor(bus.status)}22`,
                      border: `2px solid ${statusColor(bus.status)}`,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: "1.1rem",
                      flexShrink: 0,
                    }}
                  >
                    🚌
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontWeight: 600, fontSize: "0.875rem", overflow: "hidden", whiteSpace: "nowrap", textOverflow: "ellipsis" }}>
                      {bus.busNumber}
                    </div>
                    <div style={{ fontSize: "0.75rem", color: "var(--text-dim)", overflow: "hidden", whiteSpace: "nowrap", textOverflow: "ellipsis" }}>
                      {bus.routeName}
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.35rem", marginTop: "0.2rem" }}>
                      <span
                        style={{
                          width: 7, height: 7, borderRadius: "50%",
                          background: statusColor(bus.status), display: "inline-block", flexShrink: 0
                        }}
                      />
                      <span style={{ fontSize: "0.7rem", color: statusColor(bus.status) }}>
                        {statusLabel(bus.status)} · {bus.currentStop || "No stop reported"}
                      </span>
                    </div>
                  </div>
                  {user.role === "ADMIN" && (
                    <button
                      className="row-action"
                      onClick={(e) => {
                        e.stopPropagation();
                        setEditBus(bus);
                      }}
                    >
                      •••
                    </button>
                  )}
                </div>
              );
            })}
            {items.length === 0 && (
              <div className="empty">No buses configured. Add one above.</div>
            )}
          </div>
        </div>
      </div>

      {editBus && (
        <BusEditor
          bus={editBus}
          onClose={() => setEditBus(null)}
          onSaved={() => {
            setEditBus(null);
            onSaved();
          }}
        />
      )}
    </div>
  );
}

function BusEditor({
  bus,
  onClose,
  onSaved,
}: {
  bus: BusRoute;
  onClose: () => void;
  onSaved: () => void;
}) {
  const isNew = bus.id === 0;
  const [form, setForm] = useState({ ...bus });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  function change(k: keyof BusRoute, v: string | number) {
    setForm((f) => ({ ...f, [k]: v }));
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      if (isNew) {
        await api("/bus-routes", { method: "POST", body: JSON.stringify(form) });
      } else {
        await api(`/bus-routes/${bus.id}`, { method: "PUT", body: JSON.stringify(form) });
      }
      onSaved();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Save failed");
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!confirm("Delete this bus route permanently?")) return;
    setSaving(true);
    try {
      await api(`/bus-routes/${bus.id}`, { method: "DELETE" });
      onSaved();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Delete failed");
      setSaving(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <form
        className="modal"
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
        style={{ maxWidth: 600 }}
      >
        <div className="modal-header">
          <h2>{isNew ? "Add bus route" : "Edit bus route"}</h2>
          <button type="button" className="close" onClick={onClose}>✕</button>
        </div>
        <div className="form-grid">
          <label>
            Bus Number
            <input required value={form.busNumber} onChange={(e) => change("busNumber", e.target.value)} placeholder="MH-12-AB-1001" />
          </label>
          <label>
            Route Name
            <input required value={form.routeName} onChange={(e) => change("routeName", e.target.value)} placeholder="Route 1 - North District" />
          </label>
          <label>
            Driver Name
            <input required value={form.driverName} onChange={(e) => change("driverName", e.target.value)} />
          </label>
          <label>
            Driver Phone
            <input required value={form.driverPhone} onChange={(e) => change("driverPhone", e.target.value)} />
          </label>
          <label>
            Capacity
            <input type="number" min={1} max={100} required value={form.capacity} onChange={(e) => change("capacity", +e.target.value)} />
          </label>
          <label>
            Status
            <select value={form.status} onChange={(e) => change("status", e.target.value)}>
              {["ACTIVE", "IN_TRANSIT", "MAINTENANCE", "INACTIVE"].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </label>
          <label>
            Current Stop
            <input value={form.currentStop ?? ""} onChange={(e) => change("currentStop", e.target.value)} />
          </label>
          <label className="wide">
            Stops (comma-separated)
            <input value={form.stops ?? ""} onChange={(e) => change("stops", e.target.value)} placeholder="Stop 1, Stop 2, Main Campus" />
          </label>
        </div>
        {error && <div className="form-error">{error}</div>}
        <div className="modal-actions">
          {!isNew && (
            <button className="danger-quiet" type="button" onClick={remove}>Delete route</button>
          )}
          <span />
          <button className="secondary" type="button" onClick={onClose}>Cancel</button>
          <button className="primary" disabled={saving}>{saving ? "Saving…" : "Save route"}</button>
        </div>
      </form>
    </div>
  );
}
