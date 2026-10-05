import React, { useCallback, useEffect, useState } from "react";
import Constants from "expo-constants";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Linking,
  Modal,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";
import {
  api,
  AttendanceRecord,
  BusRoute,
  clearToken,
  Fee,
  Homework,
  Message,
  LeaveApplication,
  Note,
  Result,
  setToken,
  setUnauthorizedHandler,
  restoreToken,
  Student,
  SchoolEvent,
  TimetableEntry,
  User,
} from "./src/api";

type Tab =
  | "Home"
  | "Students"
  | "Homework"
  | "Updates"
  | "Profile"
  | "Attendance"
  | "Results"
  | "Fees"
  | "Messages"
  | "Events"
  | "Leave"
  | "Bus Tracking"
  | "Timetable";
const IS_DEVELOPMENT_BUILD =
  process.env.EXPO_PUBLIC_APP_ENV === "development" ||
  Constants.expoConfig?.extra?.environment === "development";
const dateKey = (year: number, month: number, day: number) =>
  `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
const formatCalendarDate = (date: string) =>
  new Date(`${date}T00:00:00`).toLocaleDateString(undefined, {
    weekday: "long", year: "numeric", month: "long", day: "numeric",
  });
// Common India/Maharashtra festival and public holiday dates for 2026–2027.
// Dates can vary by region and local observance; schools can add their own dates in Events.
const FESTIVAL_DATES: { date: string; title: string }[] = ([
  ["2026-01-14", "Makar Sankranti / Pongal"],
  ["2026-01-26", "Republic Day"],
  ["2026-02-15", "Maha Shivaratri"],
  ["2026-02-19", "Chhatrapati Shivaji Maharaj Jayanti"],
  ["2026-03-03", "Holi"],
  ["2026-03-19", "Gudi Padwa"],
  ["2026-03-21", "Eid al-Fitr (date may vary)"],
  ["2026-03-26", "Ram Navami"],
  ["2026-03-31", "Mahavir Jayanti"],
  ["2026-04-03", "Good Friday"],
  ["2026-04-14", "Dr. Babasaheb Ambedkar Jayanti"],
  ["2026-05-01", "Maharashtra Day / Buddha Purnima"],
  ["2026-05-27", "Eid al-Adha (date may vary)"],
  ["2026-06-26", "Muharram (date may vary)"],
  ["2026-08-15", "Independence Day / Parsi New Year"],
  ["2026-08-26", "Eid-e-Milad (date may vary)"],
  ["2026-08-04", "Janmashtami"],
  ["2026-09-05", "Gopal Kala / Dahi Handi"],
  ["2026-09-14", "Ganesh Chaturthi"],
  ["2026-09-25", "Anant Chaturdashi"],
  ["2026-10-02", "Mahatma Gandhi Jayanti"],
  ["2026-10-20", "Dussehra / Vijayadashami"],
  ["2026-11-08", "Diwali / Lakshmi Pujan"],
  ["2026-11-09", "Deepavali Holiday"],
  ["2026-11-10", "Diwali Padwa"],
  ["2026-11-11", "Bhai Dooj / Bhaubeej"],
  ["2026-11-24", "Guru Nanak Jayanti"],
  ["2026-12-25", "Christmas Day"],
  ["2027-01-26", "Republic Day"],
  ["2027-02-19", "Chhatrapati Shivaji Maharaj Jayanti"],
  ["2027-03-06", "Maha Shivaratri"],
  ["2027-03-10", "Eid al-Fitr (date may vary)"],
  ["2027-03-22", "Holi"],
  ["2027-03-26", "Good Friday"],
  ["2027-04-08", "Gudi Padwa"],
  ["2027-04-14", "Dr. Babasaheb Ambedkar Jayanti"],
  ["2027-04-15", "Ram Navami"],
  ["2027-04-19", "Mahavir Jayanti"],
  ["2027-05-01", "Maharashtra Day"],
  ["2027-05-17", "Eid al-Adha (date may vary)"],
  ["2027-05-20", "Buddha Purnima"],
  ["2027-06-15", "Muharram (date may vary)"],
  ["2027-08-15", "Independence Day / Parsi New Year / Eid-e-Milad (dates may vary)"],
  ["2027-09-04", "Ganesh Chaturthi"],
  ["2027-10-02", "Mahatma Gandhi Jayanti"],
  ["2027-10-10", "Dussehra / Vijayadashami"],
  ["2027-10-29", "Diwali"],
  ["2027-10-30", "Deepavali Holiday"],
  ["2027-11-14", "Guru Nanak Jayanti"],
  ["2027-12-25", "Christmas Day"],
] as [string, string][]).map(([date, title]) => ({ date, title }));
const festivalEventsForYear = (year: number): SchoolEvent[] =>
  FESTIVAL_DATES.filter((festival) => festival.date.startsWith(`${year}-`)).map((festival, index) => ({
    id: -1000 - index,
    title: festival.title,
    description: "Festival and public holiday calendar entry. Confirm the school's local holiday schedule with the administration.",
    eventDate: festival.date,
    location: "School calendar",
    createdBy: "School calendar",
    holiday: true,
  }));
const demos = [
  { role: "ADMIN", email: "admin@northstar.local", password: "Admin@123" },
  { role: "TEACHER", email: "teacher@northstar.local", password: "Teacher@123" },
  { role: "PARENT", email: "parent@northstar.local", password: "Parent@123" },
];
export default function App() {
  return (
    <SafeAreaProvider>
      <AppContent />
    </SafeAreaProvider>
  );
}

function AppContent() {
  const [user, setUser] = useState<User | null>(null),
    [tab, setTab] = useState<Tab>("Home"),
    [email, setEmail] = useState(IS_DEVELOPMENT_BUILD ? "admin@northstar.local" : ""),
    [password, setPassword] = useState(IS_DEVELOPMENT_BUILD ? "Admin@123" : ""),
    [students, setStudents] = useState<Student[]>([]),
    [selectedChildId, setSelectedChildId] = useState<number | null>(null),
    [busRoutes, setBusRoutes] = useState<BusRoute[]>([]),
    [timetable, setTimetable] = useState<TimetableEntry[]>([]),
    [busError, setBusError] = useState(""),
    [selectedBusId, setSelectedBusId] = useState<number | null>(null),
    [homework, setHomework] = useState<Homework[]>([]),
    [notes, setNotes] = useState<Note[]>([]),
    [results, setResults] = useState<Result[]>([]),
    [fees, setFees] = useState<Fee[]>([]),
    [messages, setMessages] = useState<Message[]>([]),
    [events, setEvents] = useState<SchoolEvent[]>([]),
    [calendarYear, setCalendarYear] = useState(new Date().getFullYear()),
    [selectedCalendarDate, setSelectedCalendarDate] = useState(
      dateKey(new Date().getFullYear(), new Date().getMonth(), new Date().getDate()),
    ),
    [leaveApplications, setLeaveApplications] = useState<LeaveApplication[]>([]),
    [leaveStudentId, setLeaveStudentId] = useState(""),
    [leaveReviewerRemark, setLeaveReviewerRemark] = useState(""),
    [leaveStart, setLeaveStart] = useState(""),
    [leaveEnd, setLeaveEnd] = useState(""),
    [leaveReason, setLeaveReason] = useState(""),
    [attendanceRecords, setAttendanceRecords] = useState<
      Record<number, AttendanceRecord[]>
    >({}),
    [recipient, setRecipient] = useState("teacher@northstar.local"),
    [subjectLine, setSubjectLine] = useState(""),
    [messageBody, setMessageBody] = useState(""),
    [messageComposerOpen, setMessageComposerOpen] = useState(false),
    [messageSending, setMessageSending] = useState(false),
    [error, setError] = useState(""),
    [toast, setToast] = useState<{ id: number; message: string; error: boolean } | null>(null),
    [busy, setBusy] = useState(false),
    [refreshing, setRefreshing] = useState(false),
    [markId, setMarkId] = useState(""),
    [status, setStatus] = useState("ABSENT"),
    [attendanceSaving, setAttendanceSaving] = useState(false),
    [attendanceMessage, setAttendanceMessage] = useState(""),
    [assistantOpen, setAssistantOpen] = useState(false),
    [assistantInput, setAssistantInput] = useState(""),
    [assistantMessages, setAssistantMessages] = useState<
      { id: number; from: "assistant" | "user"; text: string }[]
    >([
      {
        id: 1,
        from: "assistant",
        text: "Hi! I can help you find a section or answer simple questions about your school data.",
      },
    ]);
  const load = useCallback(async () => {
    if (!user) return;
    setError("");
    try {
      const [s, h, n, r, f, m, ev, lv, tt] = await Promise.all([
        api<Student[]>("/students"),
        api<Homework[]>("/homework"),
        api<Note[]>("/notifications"),
        api<Result[]>("/results"),
        api<Fee[]>("/fees"),
        api<Message[]>("/messages"),
        api<SchoolEvent[]>("/events"),
        api<LeaveApplication[]>("/leave-requests"),
        api<TimetableEntry[]>("/timetable"),
      ]);
      setStudents(s);
      setSelectedChildId((current) => current && s.some((student) => student.id === current) ? current : s[0]?.id ?? null);
      setHomework(h);
      setNotes(n);
      setResults(r);
      setFees(f);
      setMessages(m);
      setEvents(ev);
      setLeaveApplications(lv);
      setTimetable(tt);
      if (user.role === "PARENT") {
        const histories = await Promise.all(
          s.map((student) =>
            api<AttendanceRecord[]>(`/attendance/student/${student.id}`).then(
              (rows) => [student.id, rows] as const,
            ),
          ),
        );
        setAttendanceRecords(Object.fromEntries(histories));
      }
      if (s.length && !markId) setMarkId(String(s[0].id));
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not load data");
    }
  }, [user, markId]);
  const loadBusRoutes = useCallback(async () => {
    if (!user) return;
    try {
      const routes = await api<BusRoute[]>("/bus-routes");
      setBusRoutes(routes);
      setSelectedBusId((current) =>
        current != null && routes.some((route) => route.id === current)
          ? current
          : routes[0]?.id ?? null,
      );
      setBusError("");
    } catch (e) {
      setBusError(e instanceof Error ? e.message : "Could not load bus routes");
    }
  }, [user]);
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setError("Your session expired. Please sign in again.");
    });
    let mounted = true;
    void (async () => {
      try {
        const savedToken = await restoreToken();
        if (savedToken) {
          const profile = await api<User>("/auth/me");
          if (mounted) setUser(profile);
        }
      } catch (restoreError) {
        await clearToken().catch(() => undefined);
        if (mounted) {
          setError(
            restoreError instanceof Error
              ? restoreError.message
              : "Could not restore your sign-in session.",
          );
        }
      }
    })();
    return () => {
      mounted = false;
      setUnauthorizedHandler(undefined);
    };
  }, []);

  useEffect(() => {
    load();
  }, [load]);
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3200);
    return () => clearTimeout(timer);
  }, [toast]);
  useEffect(() => {
    if (!user) return;
    void loadBusRoutes();
  }, [user, loadBusRoutes]);
  useEffect(() => {
    if (!user || (user.role !== "PARENT" && user.role !== "TEACHER")) return;
    const timer = setInterval(() => {
      void api<Note[]>("/notifications").then(setNotes).catch(() => undefined);
    }, 60_000);
    return () => clearInterval(timer);
  }, [user]);
  useEffect(() => {
    if (!user || tab !== "Bus Tracking") return;
    const timer = setInterval(() => void loadBusRoutes(), 15_000);
    return () => clearInterval(timer);
  }, [user, tab, loadBusRoutes]);
  function notify(message: string, isError = false) {
    setToast({ id: Date.now(), message, error: isError });
  }
  async function login(e = email, p = password) {
    setBusy(true);
    setError("");
    try {
      const r = await api<{ token: string; user: User }>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email: e, password: p }),
      });
      await setToken(r.token);
      setUser(r.user);
      setTab("Home");
      notify(`Welcome, ${r.user.name.split(" ")[0]}.`);
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : "Sign in failed");
    } finally {
      setBusy(false);
    }
  }
  async function refresh() {
    setRefreshing(true);
    await Promise.all([load(), loadBusRoutes()]);
    setRefreshing(false);
  }
  function changeCalendarYear(amount: number) {
    const nextYear = calendarYear + amount;
    const [, monthText, dayText] = selectedCalendarDate.split("-");
    const month = Number(monthText) - 1;
    const day = Number(dayText);
    setCalendarYear(nextYear);
    setSelectedCalendarDate(dateKey(nextYear, month, Math.min(day, new Date(nextYear, month + 1, 0).getDate())));
  }
  const calendarEvents = [...events, ...festivalEventsForYear(calendarYear)];
  async function mark() {
    if (!markId) {
      setAttendanceMessage("Select a student before saving attendance.");
      return;
    }
    setAttendanceSaving(true);
    setAttendanceMessage("");
    try {
      await api("/attendance", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(markId),
          date: new Date().toISOString().slice(0, 10),
          status,
        }),
      });
      setAttendanceMessage("Attendance saved successfully.");
      notify("Attendance saved.");
      await load();
    } catch (e) {
      const message = e instanceof Error ? e.message : "Could not record attendance";
      setAttendanceMessage(message);
      notify(message, true);
    } finally {
      setAttendanceSaving(false);
    }
  }
  function navigateFromAssistant(destination: Tab) {
    const availableTabs =
      user?.role === "TEACHER"
        ? (["Home", "Attendance", "Homework", "Events", "Timetable", "Leave", "Bus Tracking", "Updates", "Profile"] as Tab[])
        : user?.role === "ADMIN"
          ? (["Home", "Students", "Attendance", "Events", "Timetable", "Leave", "Bus Tracking", "Updates", "Profile"] as Tab[])
          : (["Home", "Students", "Homework", "Events", "Timetable", "Leave", "Bus Tracking", "Updates", "Profile"] as Tab[]);
    if (!availableTabs.includes(destination)) {
      return `The ${destination} section is not available for your account.`;
    }
    setTab(destination);
    setAssistantOpen(false);
    return `Opening ${destination}.`;
  }
  function answerAssistant(rawQuestion: string) {
    const question = rawQuestion.toLowerCase().replace(/[^a-z0-9 ]/g, " ").trim();
    const navigation: [RegExp, Tab][] = [
      [/\b(home|dashboard)\b/, "Home"],
      [/\b(attendance|absent|present)\b/, "Attendance"],
      [/\b(student|students|children|directory)\b/, "Students"],
      [/\b(homework|assignment|assignments)\b/, "Homework"],
      [/\b(event|events|calendar)\b/, "Events"],
      [/\b(timetable|schedule|schedules)\b/, "Timetable"],
      [/\b(leave|vacation)\b/, "Leave"],
      [/\b(bus|track|tracking|route)\b/, "Bus Tracking"],
      [/\b(notification|notifications|update|updates)\b/, "Updates"],
      [/\b(profile|account)\b/, "Profile"],
      [/\b(result|results|grade|grades|exam|exams)\b/, "Results"],
      [/\b(fee|fees|payment|payments)\b/, "Fees"],
      [/\b(message|messages|inbox)\b/, "Messages"],
    ];
    for (const [pattern, destination] of navigation) {
      if (pattern.test(question) && /\b(open|show|go|take|navigate|where|view)\b/.test(question)) {
        return navigateFromAssistant(destination);
      }
    }
    if (/\b(attendance|absent|present)\b/.test(question)) {
      return user?.role === "ADMIN" || user?.role === "TEACHER"
        ? "Open Attendance to choose a student, select Present, Absent, Late, or Leave, then tap Save attendance."
        : "Open Attendance to see attendance history for your linked student(s).";
    }
    if (/\b(leave|vacation)\b/.test(question)) {
      return user?.role === "ADMIN"
        ? "Open Leave to review requests and approve or reject pending requests."
        : "Open Leave to submit a request or check its status.";
    }
    if (/\b(bus|route|tracking)\b/.test(question)) {
      return `Open Bus Tracking to view routes. There ${busRoutes.length === 1 ? "is" : "are"} ${busRoutes.length} route${busRoutes.length === 1 ? "" : "s"} available right now.`;
    }
    if (/\b(homework|assignment)\b/.test(question)) {
      return `There ${homework.length === 1 ? "is" : "are"} ${homework.length} assignment${homework.length === 1 ? "" : "s"} in the loaded list. Open Homework to view them.`;
    }
    if (/\b(student|students|children)\b/.test(question)) {
      return `I can see ${students.length} student${students.length === 1 ? "" : "s"} in your account. Open Students to view the list.`;
    }
    if (/\b(help|what can you do|how do i)\b/.test(question)) {
      return "Ask me to open a section (for example, ?open attendance?) or ask how to use attendance, leave, homework, or bus tracking.";
    }
    return "I can help you navigate the app and answer basic questions about loaded students, homework, leave, attendance, and bus routes. Try ?open bus tracking? or ?how do I request leave??";
  }
  function sendAssistantMessage(text = assistantInput) {
    const question = text.trim();
    if (!question) return;
    const now = Date.now();
    const answer = answerAssistant(question);
    setAssistantMessages((current) => [
      ...current,
      { id: now, from: "user", text: question },
      { id: now + 1, from: "assistant", text: answer },
    ]);
    setAssistantInput("");
  }
  async function sendMessage() {
    if (!recipient.trim() || !subjectLine.trim() || !messageBody.trim()) {
      notify("Enter a recipient, subject, and message.", true);
      return;
    }
    setMessageSending(true);
    try {
      await api("/messages", {
        method: "POST",
        body: JSON.stringify({
          recipientEmail: recipient.trim(),
          subject: subjectLine.trim(),
          body: messageBody.trim(),
        }),
      });
      setSubjectLine("");
      setMessageBody("");
      setError("");
      setMessageComposerOpen(false);
      notify("Message sent.");
      await load();
    } catch (e) {
      const message = e instanceof Error ? e.message : "Could not send message";
      setError(message);
      notify(message, true);
    } finally {
      setMessageSending(false);
    }
  }
  async function submitLeave() {
    try {
      await api("/leave-requests", { method: "POST", body: JSON.stringify({ studentId: leaveStudentId ? Number(leaveStudentId) : null, startDate: leaveStart, endDate: leaveEnd, reason: leaveReason }) });
      setLeaveStart(""); setLeaveEnd(""); setLeaveReason(""); setError(""); notify("Leave request submitted."); await load();
    } catch (e) { const message = e instanceof Error ? e.message : "Could not submit leave request"; setError(message); notify(message, true); }
  }
  async function updateLeave(id: number, status: "APPROVED" | "REJECTED") {
    try { await api(`/leave-requests/${id}/status`, { method: "PATCH", body: JSON.stringify({ status, remark: leaveReviewerRemark }) }); setLeaveReviewerRemark(""); setError(""); notify(`Leave request ${status.toLowerCase()}.`); await load(); }
    catch (e) { const message = e instanceof Error ? e.message : "Could not update leave request"; setError(message); notify(message, true); }
  }
  async function deleteLeave(id: number) {
    try { await api(`/leave-requests/${id}`, { method: "DELETE" }); setError(""); notify("Leave request deleted."); await load(); }
    catch (e) { const message = e instanceof Error ? e.message : "Could not delete leave request"; setError(message); notify(message, true); }
  }
  if (!user)
    return (
      <SafeAreaView style={styles.safe}>
        <StatusBar barStyle="dark-content" />
        <KeyboardAvoidingView
          style={styles.login}
          behavior={Platform.OS === "ios" ? "padding" : undefined}
        >
          <View style={styles.logo}>
            <Text style={styles.logoText}>N</Text>
          </View>
          <Text style={styles.kicker}>ABC SCHOOL</Text>
          <Text style={styles.title}>Welcome back</Text>
          <Text style={styles.muted}>Sign in to your school account.</Text>
          <TextInput
            style={styles.input}
            autoCapitalize="none"
            keyboardType="email-address"
            value={email}
            onChangeText={setEmail}
            placeholder="Email address"
          />
          <TextInput
            style={styles.input}
            secureTextEntry
            value={password}
            onChangeText={setPassword}
            placeholder="Password"
          />
          {error ? <Text style={styles.error}>{error}</Text> : null}
          <Pressable
            style={styles.button}
            onPress={() => login()}
            disabled={busy}
          >
            <Text style={styles.buttonText}>
              {busy ? "Signing in…" : "Sign in"}
            </Text>
          </Pressable>
          {IS_DEVELOPMENT_BUILD && (
            <>
              <Text style={styles.sectionLabel}></Text>
              <View style={styles.demoRow}>
                {demos.map((d) => (
                  <Pressable
                    key={d.role}
                    style={styles.demo}
                    onPress={() => {
                      setEmail(d.email);
                      setPassword(d.password);
                    }}
                  >
                    <Text style={styles.demoText}>{d.role}</Text>
                  </Pressable>
                ))}
              </View>
            </>
          )}
        </KeyboardAvoidingView>
      </SafeAreaView>
    );
  const tabs: Tab[] =
      user.role === "TEACHER"
      ? ["Home", "Attendance", "Homework", "Events", "Timetable", "Leave", "Bus Tracking", "Updates", "Profile"]
      : user.role === "ADMIN"
        ? ["Home", "Students", "Attendance", "Events", "Timetable", "Leave", "Bus Tracking", "Updates", "Profile"]
        : ["Home", "Students", "Attendance", "Homework", "Results", "Fees", "Messages", "Events", "Timetable", "Leave", "Bus Tracking", "Updates", "Profile"];
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
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.top}>
        <View>
          <Text style={styles.kicker}>ABC SCHOOL</Text>
          <Text style={styles.topTitle}>{user.name}</Text>
        </View>
        <Pressable style={styles.avatar} onPress={() => setTab("Profile")}>
          <Text style={styles.avatarText}>{user.name[0]}</Text>
        </Pressable>
      </View>
      <ScrollView
        style={styles.mainScroll}
        contentContainerStyle={styles.body}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={refresh} />
        }
      >
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {tab === "Home" && (
          <>
            <View style={styles.welcome}>
              <Text style={styles.welcomeTitle}>Your school, at a glance.</Text>
              <Text style={styles.welcomeText}>
                A simple place to stay up to date and connected.
              </Text>
            </View>
            <View style={styles.statRow}>
              <Stat
                label={user.role === "PARENT" ? "Children" : "Students"}
                value={students.length}
              />
              <Stat label="Homework" value={homework.length} />
              <Stat
                label="Updates"
                value={notes.filter((n) => !n.read).length}
              />
            </View>
            <Section title="School tools">
              <View style={styles.quickGrid}>
                {(["Attendance", "Results", "Fees", "Messages"] as Tab[]).map(
                  (t) => (
                    <Pressable
                      key={t}
                      style={styles.quickTile}
                      onPress={() => setTab(t)}
                    >
                      <Text style={styles.quickTitle}>{t}</Text>
                      <Text style={styles.role}>Open →</Text>
                    </Pressable>
                  ),
                )}
              </View>
            </Section>
            <Section title={user.role === "PARENT" ? "Choose a child" : "Your students"}>
              {user.role === "PARENT" && <View style={styles.optionList}>{students.map((student) => <Pressable key={student.id} style={[styles.option, selectedChildId === student.id && styles.optionSelected]} onPress={() => setSelectedChildId(student.id)}><Text style={styles.optionText}>{student.firstName} {student.lastName} · {student.className}-{student.division}</Text></Pressable>)}</View>}
              {user.role !== "PARENT" && students.slice(0, 3).map((s) => (
                <StudentCard key={s.id} s={s} />
              ))}
            </Section>
            <Section title="Upcoming homework">
              {visibleHomework.slice(0, 3).map((h) => (
                <HomeCard key={h.id} h={h} />
              ))}
            </Section>
            <Section title="Latest updates">
              {notes.slice(0, 2).map((n) => (
                <NoteCard key={n.id} n={n} />
              ))}
            </Section>
          </>
        )}
        {tab === "Students" && (
          <Section
            title={
              user.role === "PARENT" ? "Your children" : "Student directory"
            }
          >
            {visibleStudents.map((s) => (
              <StudentCard key={s.id} s={s} />
            ))}
          </Section>
        )}
        {tab === "Homework" && (
          <Section title="Assignments">
            {visibleHomework.map((h) => (
              <HomeCard key={h.id} h={h} />
            ))}
          </Section>
        )}
        {tab === "Updates" && (
          <Section title="Notifications">
            {notes.map((n) => (
              <NoteCard key={n.id} n={n} />
            ))}
          </Section>
        )}
        {tab === "Results" && (
          <Section title="Exam results">
            {visibleResults.map((r) => (
              <View key={r.id} style={styles.card}>
                <Text style={styles.cardTitle}>
                  {r.studentName} · {r.subject}
                </Text>
                <Text style={styles.cardText}>
                  {r.examName} · {r.marks} / {r.maximumMarks}
                </Text>
                <Text style={styles.role}>
                  {r.percentage}% · {r.examDate || "Date not set"}
                </Text>
              </View>
            ))}
          </Section>
        )}
        {tab === "Fees" && (
          <Section title="Fee records">
            {visibleFees.map((f) => (
              <View key={f.id} style={styles.card}>
                <Text style={styles.cardTitle}>
                  {f.studentName} · {f.description}
                </Text>
                <Text style={styles.cardText}>
                  ₹{f.amount.toLocaleString("en-IN")} · Due {f.dueDate || "—"}
                </Text>
                <Text style={styles.role}>{f.status}</Text>
              </View>
            ))}
          </Section>
        )}
        {tab === "Messages" && (
          <Section title="Inbox & sent">
            {messages.length === 0 && <Text style={styles.muted}>No messages yet.</Text>}
            {messages.map((m) => (
              <View key={m.id} style={styles.card}>
                <Text style={styles.cardTitle}>{m.subject}</Text>
                <Text style={styles.cardText}>{m.body}</Text>
                <Text style={styles.role}>
                  {m.senderEmail === user.email
                    ? `To ${m.recipientName}`
                    : `From ${m.senderName}`}
                </Text>
              </View>
            ))}
          </Section>
        )}
        {tab === "Timetable" && <Section title="Class timetable">{["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"].map(day => { const rows = visibleTimetable.filter(row => row.dayOfWeek === day); return rows.length ? <View key={day}><Text style={styles.sectionTitle}>{day}</Text>{rows.map(row => <View key={row.id} style={styles.card}><Text style={styles.cardTitle}>{row.startTime}–{row.endTime} · {row.subject}</Text><Text style={styles.cardText}>Class {row.className}-{row.division} · {row.teacherName}{row.room ? ` · ${row.room}` : ""}</Text></View>)}</View> : null; })}{visibleTimetable.length === 0 && <Text style={styles.muted}>No timetable has been published yet.</Text>}</Section>}
        {tab === "Events" && (
          <>
            <Section title="Yearly school calendar">
              <View style={styles.calendarYearRow}>
                <Pressable style={styles.calendarYearButton} onPress={() => changeCalendarYear(-1)} accessibilityLabel="Previous year"><Text style={styles.calendarYearButtonText}>{"<"}</Text></Pressable>
                <Text style={styles.calendarYear}>{calendarYear}</Text>
                <Pressable style={styles.calendarYearButton} onPress={() => changeCalendarYear(1)} accessibilityLabel="Next year"><Text style={styles.calendarYearButtonText}>{">"}</Text></Pressable>
              </View>
              <Text style={styles.calendarHint}>{calendarYear === 2026 || calendarYear === 2027 ? "Sundays are weekly holidays. Festival dates are shown as HOLIDAY; confirm locally observed dates with the school." : "Sundays are weekly holidays. Ask the school to add festival dates for this year in Events."}</Text>
              <View style={styles.calendarLegend}>
                <View style={styles.calendarLegendItem}><View style={styles.holidayLegendDot} /><Text style={styles.calendarLegendText}>Sunday / holiday</Text></View>
                <View style={styles.calendarLegendItem}><View style={styles.eventLegendDot} /><Text style={styles.calendarLegendText}>School event</Text></View>
              </View>
              <View style={styles.calendarGrid}>
                {Array.from({ length: 12 }, (_, month) => {
                  const firstWeekday = new Date(calendarYear, month, 1).getDay();
                  const daysInMonth = new Date(calendarYear, month + 1, 0).getDate();
                  const monthEvents = calendarEvents.filter((event) => event.eventDate.startsWith(`${calendarYear}-${String(month + 1).padStart(2, "0")}-`));
                  const holidayDates = new Set(monthEvents.filter((event) => event.holiday).map((event) => event.eventDate));
                  for (let day = 1; day <= daysInMonth; day++) {
                    if (new Date(calendarYear, month, day).getDay() === 0) holidayDates.add(dateKey(calendarYear, month, day));
                  }
                  const holidayCount = holidayDates.size;
                  return (
                    <View key={month} style={styles.calendarMonth}>
                      <Text style={styles.calendarMonthTitle}>{new Date(calendarYear, month, 1).toLocaleDateString(undefined, { month: "long" })}</Text>
                      <View style={styles.calendarWeekdays}>
                        {["S", "M", "T", "W", "T", "F", "S"].map((day, index) => <Text key={`${day}-${index}`} style={styles.calendarWeekday}>{day}</Text>)}
                      </View>
                      <View style={styles.calendarDays}>
                        {Array.from({ length: firstWeekday }, (_, index) => <View key={`blank-${index}`} style={styles.calendarDay} />)}
                        {Array.from({ length: daysInMonth }, (_, index) => {
                          const day = index + 1;
                          const key = dateKey(calendarYear, month, day);
                          const dayEvents = monthEvents.filter((event) => event.eventDate === key);
                          const sunday = new Date(calendarYear, month, day).getDay() === 0;
                          const holiday = sunday || dayEvents.some((event) => event.holiday);
                          return (
                            <Pressable key={key} style={[styles.calendarDay, holiday && styles.calendarDayHoliday, key === selectedCalendarDate && styles.calendarDaySelected]} onPress={() => setSelectedCalendarDate(key)} accessibilityLabel={`${formatCalendarDate(key)}${sunday ? ", Sunday weekly holiday" : holiday ? ", holiday" : dayEvents.length ? ", school event" : ""}`}>
                              <Text style={[styles.calendarDayText, holiday && styles.calendarHolidayText, key === selectedCalendarDate && styles.calendarSelectedText]}>{day}</Text>
                              {(dayEvents.length > 0 || sunday) && <View style={[styles.calendarEventDot, holiday && styles.calendarHolidayDot]} />}
                            </Pressable>
                          );
                        })}
                      </View>
                      <Text style={styles.calendarMonthSummary}>{holidayCount ? `${holidayCount} holiday${holidayCount === 1 ? "" : "s"}` : " "}</Text>
                    </View>
                  );
                })}
              </View>
            </Section>
            <Section title={formatCalendarDate(selectedCalendarDate)}>
              {new Date(`${selectedCalendarDate}T00:00:00`).getDay() === 0 && <View style={[styles.card, styles.holidayEventCard]}><View style={styles.cardTop}><Text style={styles.cardTitle}>Weekly holiday</Text><Text style={styles.holidayBadge}>SUNDAY</Text></View><Text style={styles.cardText}>Every Sunday is marked as a weekly holiday.</Text></View>}
              {calendarEvents.filter((event) => event.eventDate === selectedCalendarDate).length === 0 && new Date(`${selectedCalendarDate}T00:00:00`).getDay() !== 0 ? (
                <Text style={styles.muted}>No school events are scheduled for this date.</Text>
              ) : calendarEvents.filter((event) => event.eventDate === selectedCalendarDate).map((event) => (
                <View key={event.id} style={[styles.card, event.holiday && styles.holidayEventCard]}>
                  <View style={styles.cardTop}>
                    <Text style={styles.cardTitle}>{event.title}</Text>
                    {event.holiday && <Text style={styles.holidayBadge}>HOLIDAY</Text>}
                  </View>
                  {event.description ? <Text style={styles.cardText}>{event.description}</Text> : null}
                  <Text style={styles.role}>{event.location || "School"}</Text>
                </View>
              ))}
            </Section>
          </>
        )}
        {tab === "Bus Tracking" && (
          <BusTracking
            routes={visibleBusRoutes}
            selectedBusId={selectedBusId}
            onSelect={setSelectedBusId}
            error={busError}
            onRefresh={loadBusRoutes}
          />
        )}
        {tab === "Leave" && (
          <>
            {user.role !== "ADMIN" && <Section title="Request leave">
              {user.role !== "TEACHER" && <><Text style={styles.fieldLabel}>STUDENT</Text><View style={styles.optionList}>{students.map((student) => <Pressable key={student.id} style={[styles.option, leaveStudentId === String(student.id) && styles.optionSelected]} onPress={() => setLeaveStudentId(String(student.id))}><Text style={styles.optionText}>{student.firstName} {student.lastName} · {student.className}-{student.division}</Text></Pressable>)}</View></>}
              <TextInput style={styles.input} value={leaveStart} onChangeText={setLeaveStart} placeholder="Start date (YYYY-MM-DD)" />
              <TextInput style={styles.input} value={leaveEnd} onChangeText={setLeaveEnd} placeholder="End date (YYYY-MM-DD)" />
              <TextInput style={[styles.input, styles.messageInput]} multiline value={leaveReason} onChangeText={setLeaveReason} placeholder="Reason for leave" />
              <Pressable style={styles.button} onPress={submitLeave}><Text style={styles.buttonText}>Submit request</Text></Pressable>
            </Section>}
            <Section title={user.role === "ADMIN" ? "All leave requests & history" : user.role === "TEACHER" ? "Student requests & my leave history" : "Your leave history"}>
              {leaveApplications.length === 0 && <Text style={styles.muted}>No leave requests to show.</Text>}
              {leaveApplications.map((leave) => {
                const canReview = user.role === "ADMIN" || (user.role === "TEACHER" && leave.studentId !== null);
                return <View key={leave.id} style={styles.card}>
                  <Text style={styles.cardTitle}>{leave.studentId === null ? `Staff leave · ${leave.requesterName} · ${leave.status}` : `${leave.studentName} · ${leave.status}`}</Text>
                  {leave.studentId !== null && <Text style={styles.cardText}>Requested by {leave.requesterName} ({leave.requesterEmail})</Text>}
                  <Text style={styles.cardText}>{leave.startDate} to {leave.endDate}</Text>
                  <Text style={styles.cardText}>{leave.reason}</Text>
                  {leave.reviewedAt && <Text style={styles.role}>Reviewed {new Date(leave.reviewedAt).toLocaleDateString()} · {leave.reviewerRemark || "No review note"}</Text>}
                  {canReview && leave.status === "PENDING" && <TextInput style={styles.input} value={leaveReviewerRemark} onChangeText={setLeaveReviewerRemark} placeholder="Optional review note" />}
                  {leave.reviewedAt && <Text style={styles.role}>Reviewed {new Date(leave.reviewedAt).toLocaleDateString()} · {leave.reviewerRemark || "No review note"}</Text>}
                  {canReview && leave.status === "PENDING" && <View style={styles.demoRow}><Pressable style={styles.demo} onPress={() => updateLeave(leave.id, "APPROVED")}><Text style={styles.demoText}>APPROVE</Text></Pressable><Pressable style={styles.demo} onPress={() => updateLeave(leave.id, "REJECTED")}><Text style={styles.demoText}>REJECT</Text></Pressable></View>}
                  {(user.role === "ADMIN" || (leave.requesterEmail === user.email && leave.status === "PENDING")) && <Pressable style={styles.outlineButton} onPress={() => deleteLeave(leave.id)}><Text style={styles.outlineText}>Delete request</Text></Pressable>}
                </View>;
              })}
            </Section>
          </>
        )}
        {tab === "Attendance" &&
          (user.role === "ADMIN" || user.role === "TEACHER" ? (
            <Section title="Record attendance">
              <Text style={styles.muted}>
                Choose a student and save today’s attendance. Parents receive an
                alert for absences.
              </Text>
              <Text style={styles.fieldLabel}>STUDENT</Text>
              <View style={styles.optionList}>
                {students.map((s) => (
                  <Pressable
                    key={s.id}
                    style={[
                      styles.option,
                      markId === String(s.id) && styles.optionSelected,
                    ]}
                    onPress={() => setMarkId(String(s.id))}
                  >
                    <Text style={styles.optionText}>
                      {s.firstName} {s.lastName} · {s.className}-{s.division}
                    </Text>
                  </Pressable>
                ))}
              </View>
              <Text style={styles.fieldLabel}>STATUS</Text>
              <View style={styles.demoRow}>
                {["PRESENT", "ABSENT", "LATE", "LEAVE"].map((x) => (
                  <Pressable
                    key={x}
                    style={[styles.demo, status === x && styles.optionSelected]}
                    onPress={() => setStatus(x)}
                  >
                    <Text style={styles.demoText}>{x}</Text>
                  </Pressable>
                ))}
              </View>
              <Pressable
                style={[styles.button, attendanceSaving && styles.buttonDisabled]}
                onPress={mark}
                disabled={attendanceSaving}
              >
                <Text style={styles.buttonText}>
                  {attendanceSaving ? "Saving..." : "Save attendance"}
                </Text>
              </Pressable>
              {attendanceMessage ? (
                <Text
                  accessibilityLiveRegion="polite"
                  style={
                    attendanceMessage === "Attendance saved successfully."
                      ? styles.attendanceSuccess
                      : styles.error
                  }
                >
                  {attendanceMessage}
                </Text>
              ) : null}
            </Section>
          ) : (
            <Section title="Attendance history">
              {visibleStudents.map((s) => (
                <AttendanceCard
                  key={s.id}
                  student={s}
                  records={attendanceRecords[s.id] || []}
                />
              ))}
            </Section>
          ))}
        {tab === "Profile" && (
          <Section title="Your account">
            <View style={styles.card}>
              <Text style={styles.cardTitle}>{user.name}</Text>
              <Text style={styles.cardText}>{user.email}</Text>
              <Text style={styles.role}>{user.role}</Text>
            </View>
            <Pressable
              style={styles.outlineButton}
              onPress={() => {
                clearToken();
                setUser(null);
                setError("");
              }}
            >
              <Text style={styles.outlineText}>Sign out</Text>
            </Pressable>
          </Section>
        )}
      </ScrollView>
      {tab === "Messages" && (
        <Pressable
          style={styles.messageComposeButton}
          onPress={() => { setError(""); setMessageComposerOpen(true); }}
          accessibilityRole="button"
          accessibilityLabel="Compose a message"
        >
          <Text style={styles.messageComposeIcon}>+</Text>
          <Text style={styles.messageComposeLabel}>Compose</Text>
        </Pressable>
      )}
      <Modal
        visible={messageComposerOpen && tab === "Messages"}
        transparent
        animationType="fade"
        onRequestClose={() => setMessageComposerOpen(false)}
      >
        <KeyboardAvoidingView
          style={styles.messageModalViewport}
          behavior={Platform.OS === "ios" ? "padding" : undefined}
        >
          <View style={styles.messageModalBackdrop}>
            <View style={styles.messageComposerCard}>
              <View style={styles.messageComposerHeader}>
                <View>
                  <Text style={styles.messageComposerTitle}>New message</Text>
                  <Text style={styles.messageComposerHint}>Send a note to someone at your school.</Text>
                </View>
                <Pressable onPress={() => setMessageComposerOpen(false)} accessibilityLabel="Close message form">
                  <Text style={styles.messageComposerClose}>X</Text>
                </Pressable>
              </View>
              <TextInput
                style={styles.input}
                autoCapitalize="none"
                keyboardType="email-address"
                value={recipient}
                onChangeText={setRecipient}
                placeholder="Recipient email"
                accessibilityLabel="Recipient email"
              />
              <TextInput
                style={styles.input}
                value={subjectLine}
                onChangeText={setSubjectLine}
                placeholder="Subject"
                accessibilityLabel="Message subject"
              />
              <TextInput
                style={[styles.input, styles.messageInput]}
                multiline
                textAlignVertical="top"
                value={messageBody}
                onChangeText={setMessageBody}
                placeholder="Write a message"
                accessibilityLabel="Message"
              />
              {error ? <Text style={styles.error}>{error}</Text> : null}
              <Pressable
                style={[styles.button, (messageSending || !recipient.trim() || !subjectLine.trim() || !messageBody.trim()) && styles.buttonDisabled]}
                onPress={sendMessage}
                disabled={messageSending || !recipient.trim() || !subjectLine.trim() || !messageBody.trim()}
              >
                <Text style={styles.buttonText}>{messageSending ? "Sending..." : "Send message"}</Text>
              </Pressable>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
      {toast && (
        <View style={[styles.toast, toast.error && styles.toastError]} accessibilityLiveRegion="polite">
          <Text style={styles.toastMark}>{toast.error ? "!" : "?"}</Text>
          <Text style={styles.toastText}>{toast.message}</Text>
          <Pressable onPress={() => setToast(null)} accessibilityLabel="Dismiss message"><Text style={styles.toastClose}>X</Text></Pressable>
        </View>
      )}
      {assistantOpen && (
        <View style={styles.assistantPanel} accessibilityLabel="School assistant chat">
          <View style={styles.assistantHeader}>
            <View>
              <Text style={styles.assistantTitle}>School assistant</Text>
              <Text style={styles.assistantSubtitle}>Navigation and quick answers</Text>
            </View>
            <Pressable accessibilityLabel="Close assistant" onPress={() => setAssistantOpen(false)}>
              <Text style={styles.assistantClose}>X</Text>
            </Pressable>
          </View>
          <ScrollView style={styles.assistantHistory} contentContainerStyle={styles.assistantHistoryContent}>
            {assistantMessages.map((message) => (
              <View key={message.id} style={[styles.chatBubble, message.from === "user" ? styles.userBubble : styles.botBubble]}>
                <Text style={[styles.chatText, message.from === "user" && styles.userChatText]}>{message.text}</Text>
              </View>
            ))}
          </ScrollView>
          <View style={styles.assistantQuickLinks}>
            {(["Home", "Attendance", "Leave", "Bus Tracking"] as Tab[])
              .filter((destination) => tabs.includes(destination))
              .map((destination) => (
                <Pressable key={destination} style={styles.assistantChip} onPress={() => navigateFromAssistant(destination)}>
                  <Text style={styles.assistantChipText}>{destination}</Text>
                </Pressable>
              ))}
          </View>
          <View style={styles.assistantComposer}>
            <TextInput
              style={styles.assistantInput}
              value={assistantInput}
              onChangeText={setAssistantInput}
              placeholder="Ask or find a section..."
              returnKeyType="send"
              onSubmitEditing={() => sendAssistantMessage()}
              accessibilityLabel="Ask the school assistant"
            />
            <Pressable style={styles.assistantSend} onPress={() => sendAssistantMessage()} accessibilityLabel="Send message">
              <Text style={styles.assistantSendText}>Send</Text>
            </Pressable>
          </View>
        </View>
      )}
      <Pressable
        style={[styles.assistantLauncher, assistantOpen && styles.assistantLauncherOpen]}
        accessibilityRole="button"
        accessibilityLabel={assistantOpen ? "Close school assistant" : "Open school assistant"}
        onPress={() => setAssistantOpen((open) => !open)}
      >
        {assistantOpen ? (
          <Text style={styles.assistantLauncherText}>X</Text>
        ) : (
          <View style={styles.botIcon}>
            <View style={styles.botAntenna}>
              <View style={styles.botAntennaDot} />
            </View>
            <View style={styles.botFace}>
              <View style={styles.botEyeRow}>
                <View style={styles.botEye} />
                <View style={styles.botEye} />
              </View>
              <View style={styles.botMouth} />
            </View>
          </View>
        )}
      </Pressable>
      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.nav} contentContainerStyle={styles.navContent}>
        {tabs.map((t, i) => (
          <Pressable key={t} style={styles.navItem} onPress={() => setTab(t)}>
            <Text style={[styles.navIcon, tab === t && styles.navActive]}>
              {({ Home: "⌂", Students: "♙", Homework: "▧", Updates: "◉", Profile: "○", Attendance: "✓", Events: "☆", Leave: "✉", Results: "◈", Fees: "₹", Messages: "✉", "Bus Tracking": "🚌" } as Record<Tab, string>)[t]}
            </Text>
            <Text style={[styles.navText, tab === t && styles.navActive]}>
              {t}
            </Text>
          </Pressable>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}
function Stat({ label, value }: { label: string; value: number }) {
  return (
    <View style={styles.stat}>
      <Text style={styles.statValue}>{value}</Text>
      <Text style={styles.statLabel}>{label}</Text>
    </View>
  );
}
function BusTracking({
  routes,
  selectedBusId,
  onSelect,
  error,
  onRefresh,
}: {
  routes: BusRoute[];
  selectedBusId: number | null;
  onSelect: (id: number) => void;
  error: string;
  onRefresh: () => Promise<void>;
}) {
  const selected = routes.find((route) => route.id === selectedBusId) ?? routes[0];
  const stops = selected?.stops?.split(",").map((stop) => stop.trim()).filter(Boolean) ?? [];
  return (
    <>
      <Section title="School bus tracking">
        <Text style={styles.muted}>
          Bus information refreshes automatically every 15 seconds while this screen is open. Pull down to refresh anytime.
        </Text>
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!error && routes.length === 0 ? (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>No buses configured</Text>
            <Text style={styles.cardText}>Bus routes will appear here when the school adds them.</Text>
          </View>
        ) : null}
        {routes.map((route) => (
          <Pressable
            key={route.id}
            style={[styles.card, selected?.id === route.id && styles.busSelected]}
            onPress={() => onSelect(route.id)}
          >
            <View style={styles.cardTop}>
              <Text style={styles.cardTitle}>{route.busNumber}</Text>
              <Text style={styles.role}>{route.status.replaceAll("_", " ")}</Text>
            </View>
            <Text style={styles.cardText}>{route.routeName}</Text>
            <Text style={styles.cardText}>Current stop: {route.currentStop || "Not reported"}</Text>
          </Pressable>
        ))}
      </Section>
      {selected ? (
        <Section title={`${selected.busNumber} · ${selected.routeName}`}>
          <View style={styles.card}>
            <View style={styles.busStatusRow}>
              <View style={[styles.busDot, { backgroundColor: selected.status === "IN_TRANSIT" ? "#31a477" : "#9aa8b5" }]} />
              <Text style={styles.cardTitle}>{selected.status.replaceAll("_", " ")}</Text>
            </View>
            <Text style={styles.cardText}>Current stop: {selected.currentStop || "Not reported"}</Text>
            <Text style={styles.cardText}>Speed: {selected.speedKmvh ?? "—"} km/h</Text>
            <Text style={styles.cardText}>Driver: {selected.driverName || "Not listed"}</Text>
            {selected.driverPhone ? (
              <Pressable onPress={() => void Linking.openURL(`tel:${selected.driverPhone}`)}>
                <Text style={styles.busLink}>Call driver · {selected.driverPhone}</Text>
              </Pressable>
            ) : null}
            {selected.latitude != null && selected.longitude != null ? (
              <>
                <Text style={styles.cardText}>Location: {selected.latitude.toFixed(5)}, {selected.longitude.toFixed(5)}</Text>
                <Pressable onPress={() => void Linking.openURL(`https://www.google.com/maps/search/?api=1&query=${selected.latitude},${selected.longitude}`)}>
                  <Text style={styles.busLink}>Open location in Maps</Text>
                </Pressable>
              </>
            ) : (
              <Text style={styles.cardText}>Location has not been reported.</Text>
            )}
            <Text style={styles.role}>
              Last update: {selected.lastUpdated ? new Date(selected.lastUpdated).toLocaleString() : "Unknown"}
            </Text>
          </View>
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Route stops</Text>
            {stops.length ? stops.map((stop, index) => (
              <Text key={`${index}-${stop}`} style={[styles.cardText, stop === selected.currentStop && styles.busCurrentStop]}>
                {stop === selected.currentStop ? "●  " : "○  "}{stop}{stop === selected.currentStop ? " · Current" : ""}
              </Text>
            )) : <Text style={styles.cardText}>No stops listed.</Text>}
          </View>
          <Pressable style={styles.outlineButton} onPress={() => void onRefresh()}>
            <Text style={styles.outlineText}>Refresh bus location</Text>
          </Pressable>
        </Section>
      ) : null}
    </>
  );
}
function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {children}
    </View>
  );
}
function StudentCard({ s }: { s: Student }) {
  return (
    <View style={styles.card}>
      <View style={styles.cardTop}>
        <Text style={styles.cardTitle}>
          {s.firstName} {s.lastName}
        </Text>
        <Text style={styles.role}>
          {s.className}-{s.division}
        </Text>
      </View>
      <Text style={styles.cardText}>
        {s.studentCode} · Roll {s.rollNumber}
      </Text>
      <Text style={styles.cardText}>{s.status}</Text>
    </View>
  );
}
function HomeCard({ h }: { h: Homework }) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>{h.title}</Text>
      <Text style={styles.cardText}>{h.description}</Text>
      <Text style={styles.role}>
        {h.subject} · Class {h.className}{h.division ? `-${h.division}` : " · all divisions"} · Due {h.dueDate}
      </Text>
    </View>
  );
}
function NoteCard({ n }: { n: Note }) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>{n.title}</Text>
      <Text style={styles.cardText}>{n.message}</Text>
      <Text style={styles.role}>
        {new Date(n.createdAt).toLocaleDateString()}
      </Text>
    </View>
  );
}
function AttendanceCard({
  student,
  records,
}: {
  student: Student;
  records: AttendanceRecord[];
}) {
  const attended = records.filter(
      (r) => r.status === "PRESENT" || r.status === "LATE",
    ).length,
    percent = records.length
      ? Math.round((attended / records.length) * 100)
      : 0;
  return (
    <View style={styles.card}>
      <Text style={styles.cardTitle}>
        {student.firstName} {student.lastName} · {percent}%
      </Text>
      {records.slice(0, 6).map((r) => (
        <Text key={r.id} style={styles.cardText}>
          {r.date} · {r.status}
        </Text>
      ))}
      {records.length === 0 && (
        <Text style={styles.cardText}>
          No attendance has been recorded yet.
        </Text>
      )}
    </View>
  );
}
const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: "#f5f7fb" },
  mainScroll: { flex: 1, minHeight: 0 },
  login: { flex: 1, justifyContent: "center", paddingHorizontal: 25 },
  logo: {
    width: 47,
    height: 47,
    borderRadius: 14,
    backgroundColor: "#3577d4",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 20,
  },
  logoText: {
    fontFamily: "Georgia",
    fontSize: 25,
    color: "white",
    fontWeight: "700",
  },
  kicker: {
    fontSize: 10,
    fontWeight: "700",
    letterSpacing: 1.5,
    color: "#778b9f",
  },
  title: { fontSize: 28, fontWeight: "800", color: "#172b43", marginTop: 7 },
  muted: {
    fontSize: 13,
    color: "#8292a1",
    lineHeight: 20,
    marginTop: 7,
    marginBottom: 21,
  },
  input: {
    height: 48,
    borderRadius: 9,
    borderWidth: 1,
    borderColor: "#e0e7ed",
    backgroundColor: "white",
    paddingHorizontal: 13,
    marginBottom: 11,
    color: "#344b61",
  },
  messageInput: { height: 100, textAlignVertical: "top", paddingTop: 12 },
  button: {
    height: 47,
    backgroundColor: "#377bd2",
    borderRadius: 9,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 6,
  },
  buttonText: { fontWeight: "700", color: "white", fontSize: 14 },
  buttonDisabled: { opacity: 0.65 },
  attendanceSuccess: {
    color: "#237a48",
    backgroundColor: "#eaf7ef",
    padding: 10,
    borderRadius: 7,
    fontSize: 12,
    marginTop: 10,
  },
  sectionLabel: {
    fontSize: 9,
    color: "#91a0ad",
    fontWeight: "700",
    letterSpacing: 1,
    marginTop: 24,
    marginBottom: 10,
  },
  demoRow: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  demo: {
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: "#e1e8ef",
    backgroundColor: "white",
    borderRadius: 8,
  },
  demoText: { fontSize: 9, fontWeight: "700", color: "#647a8f" },
  error: {
    color: "#b94f46",
    backgroundColor: "#fff0ee",
    padding: 10,
    borderRadius: 7,
    fontSize: 12,
    marginBottom: 10,
  },
  top: {
    height: 66,
    paddingHorizontal: 20,
    backgroundColor: "white",
    borderBottomWidth: 1,
    borderBottomColor: "#e9edf2",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  topTitle: { fontSize: 17, fontWeight: "700", color: "#1e344a", marginTop: 2 },
  avatar: {
    width: 34,
    height: 34,
    borderRadius: 20,
    backgroundColor: "#e8f0fb",
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { color: "#3577d4", fontWeight: "700" },
  body: { padding: 18, paddingBottom: 25 },
  welcome: {
    padding: 19,
    backgroundColor: "#eaf2fd",
    borderRadius: 13,
    marginBottom: 13,
  },
  welcomeTitle: { fontSize: 18, fontWeight: "800", color: "#1c3e61" },
  welcomeText: { fontSize: 11, color: "#6e8297", marginTop: 7, lineHeight: 17 },
  statRow: { flexDirection: "row", gap: 9, marginBottom: 10 },
  stat: {
    flex: 1,
    padding: 14,
    borderRadius: 11,
    backgroundColor: "white",
    borderWidth: 1,
    borderColor: "#edf0f4",
  },
  statValue: { fontSize: 21, fontWeight: "800", color: "#234360" },
  statLabel: { fontSize: 9, color: "#8a98a5", marginTop: 3 },
  section: { marginTop: 15 },
  calendarYearRow: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 18, marginBottom: 12, padding: 8, borderRadius: 14, backgroundColor: "#f4f8ff" },
  calendarYearButton: { width: 40, height: 38, borderRadius: 11, alignItems: "center", justifyContent: "center", backgroundColor: "white", borderWidth: 1, borderColor: "#e6edf6" },
  calendarYearButtonText: { color: "#377bd2", fontSize: 20, fontWeight: "700" },
  calendarYear: { minWidth: 78, textAlign: "center", fontSize: 21, fontWeight: "800", color: "#233a50" },
  calendarHint: { color: "#697f95", fontSize: 11, lineHeight: 16, textAlign: "center", marginBottom: 10 },
  calendarLegend: { flexDirection: "row", justifyContent: "center", gap: 18, marginBottom: 14, paddingVertical: 8, borderTopWidth: 1, borderBottomWidth: 1, borderColor: "#edf1f6" },
  calendarLegendItem: { flexDirection: "row", alignItems: "center", gap: 5 },
  calendarLegendText: { fontSize: 11, color: "#718397" },
  holidayLegendDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: "#d75b5b" },
  eventLegendDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: "#377bd2" },
  calendarGrid: { flexDirection: "column", rowGap: 10 },
  calendarMonth: { width: "100%", padding: 12, borderWidth: 1, borderColor: "#e8edf3", borderRadius: 14, backgroundColor: "white" },
  calendarMonthTitle: { textAlign: "center", fontSize: 14, fontWeight: "800", color: "#354d65", marginBottom: 8 },
  calendarWeekdays: { flexDirection: "row" },
  calendarWeekday: { width: "14.28%", textAlign: "center", color: "#9aa6b1", fontSize: 10, paddingVertical: 5, fontWeight: "700" },
  calendarDays: { flexDirection: "row", flexWrap: "wrap" },
  calendarDay: { width: "14.28%", height: 37, alignItems: "center", justifyContent: "center", borderRadius: 9, marginVertical: 2 },
  calendarDayText: { fontSize: 12, color: "#4d6175" },
  calendarDayHoliday: { backgroundColor: "#fff0ef" },
  calendarHolidayText: { color: "#b33d3d", fontWeight: "800" },
  calendarDaySelected: { borderWidth: 1, borderColor: "#377bd2" },
  calendarSelectedText: { color: "#245da5", fontWeight: "800" },
  calendarEventDot: { position: "absolute", bottom: 3, width: 4, height: 4, borderRadius: 2, backgroundColor: "#377bd2" },
  calendarHolidayDot: { backgroundColor: "#d75b5b" },
  calendarMonthSummary: { minHeight: 16, textAlign: "center", color: "#b34c4c", fontSize: 10, marginTop: 5, fontWeight: "700" },
  holidayEventCard: { backgroundColor: "#fff7f6", borderColor: "#f4cccc" },
  holidayBadge: { color: "#b33d3d", backgroundColor: "#fde4e2", overflow: "hidden", borderRadius: 6, paddingHorizontal: 7, paddingVertical: 4, fontSize: 8, fontWeight: "800" },
  sectionTitle: {
    fontSize: 15,
    fontWeight: "700",
    color: "#233a50",
    marginBottom: 9,
  },
  quickGrid: { flexDirection: "row", gap: 8 },
  quickTile: {
    flex: 1,
    backgroundColor: "white",
    borderColor: "#edf0f4",
    borderWidth: 1,
    borderRadius: 10,
    padding: 12,
  },
  quickTitle: { fontSize: 11, fontWeight: "700", color: "#354d65" },
  card: {
    padding: 14,
    backgroundColor: "white",
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "#edf0f4",
    marginBottom: 8,
  },
  busSelected: { borderColor: "#77a4da", backgroundColor: "#f2f7ff" },
  busStatusRow: { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 4 },
  busDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: "#31a477" },
  busLink: { color: "#377bd2", fontSize: 12, fontWeight: "700", marginTop: 10 },
  busCurrentStop: { color: "#23815e", fontWeight: "700" },
  cardTop: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  cardTitle: { fontSize: 13, fontWeight: "700", color: "#2b435b" },
  cardText: { fontSize: 11, color: "#77899b", lineHeight: 17, marginTop: 5 },
  role: { fontSize: 9, color: "#4c80c4", marginTop: 7, fontWeight: "700" },
  toast: {
    position: "absolute",
    left: 14,
    right: 82,
    bottom: 74,
    zIndex: 30,
    flexDirection: "row",
    alignItems: "center",
    gap: 9,
    paddingVertical: 11,
    paddingHorizontal: 12,
    borderRadius: 12,
    backgroundColor: "#173b58",
    elevation: 8,
    shadowColor: "#10283f",
    shadowOpacity: 0.2,
    shadowRadius: 9,
    shadowOffset: { width: 0, height: 4 },
  },
  toastError: { backgroundColor: "#a74640" },
  toastMark: { width: 22, height: 22, textAlign: "center", textAlignVertical: "center", borderRadius: 11, overflow: "hidden", color: "#173b58", backgroundColor: "#a6e1b9", fontSize: 13, fontWeight: "800" },
  toastText: { flex: 1, color: "white", fontSize: 11, lineHeight: 16 },
  toastClose: { color: "#d9e7f2", fontSize: 12, padding: 4 },
  messageComposeButton: {
    position: "absolute",
    right: 82,
    bottom: 76,
    zIndex: 20,
    height: 50,
    paddingHorizontal: 16,
    borderRadius: 25,
    backgroundColor: "#377bd2",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    elevation: 6,
    shadowColor: "#172b43",
    shadowOpacity: 0.2,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 3 },
  },
  messageComposeIcon: { color: "white", fontSize: 23, lineHeight: 26, fontWeight: "700" },
  messageComposeLabel: { color: "white", fontSize: 12, fontWeight: "700" },
  messageModalViewport: { flex: 1 },
  messageModalBackdrop: { flex: 1, justifyContent: "center", padding: 18, backgroundColor: "rgba(14, 31, 48, 0.46)" },
  messageComposerCard: { width: "100%", maxWidth: 560, maxHeight: "92%", alignSelf: "center", padding: 18, borderRadius: 18, backgroundColor: "white", borderWidth: 1, borderColor: "#e2e9f1", elevation: 12 },
  messageComposerHeader: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 14 },
  messageComposerTitle: { color: "#233a50", fontSize: 18, fontWeight: "800" },
  messageComposerHint: { color: "#718397", fontSize: 11, marginTop: 4 },
  messageComposerClose: { color: "#60758a", fontSize: 28, paddingHorizontal: 7 },
  assistantLauncher: {
    position: "absolute",
    right: 18,
    bottom: 76,
    zIndex: 20,
    width: 54,
    height: 54,
    borderRadius: 27,
    backgroundColor: "#377bd2",
    alignItems: "center",
    justifyContent: "center",
    elevation: 6,
    shadowColor: "#172b43",
    shadowOpacity: 0.22,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 3 },
  },
  assistantLauncherOpen: { backgroundColor: "#274e78" },
  assistantLauncherText: { color: "white", fontSize: 25, fontWeight: "700" },
  botIcon: { width: 30, height: 30, alignItems: "center", justifyContent: "flex-end" },
  botAntenna: { position: "absolute", top: 1, width: 2, height: 8, backgroundColor: "white", alignItems: "center" },
  botAntennaDot: { position: "absolute", top: -2, width: 6, height: 6, borderRadius: 3, backgroundColor: "white" },
  botFace: { width: 28, height: 21, borderRadius: 7, backgroundColor: "white", alignItems: "center", justifyContent: "center", gap: 4 },
  botEyeRow: { flexDirection: "row", gap: 8 },
  botEye: { width: 3, height: 4, borderRadius: 2, backgroundColor: "#377bd2" },
  botMouth: { width: 8, height: 2, borderRadius: 1, backgroundColor: "#377bd2" },
  assistantPanel: {
    position: "absolute",
    right: 14,
    bottom: 140,
    zIndex: 19,
    width: 330,
    maxWidth: "92%",
    height: 420,
    maxHeight: "68%",
    backgroundColor: "white",
    borderRadius: 15,
    borderWidth: 1,
    borderColor: "#e2e9f1",
    overflow: "hidden",
    elevation: 10,
    shadowColor: "#172b43",
    shadowOpacity: 0.2,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 4 },
  },
  assistantHeader: {
    paddingHorizontal: 15,
    paddingVertical: 12,
    backgroundColor: "#f2f7fd",
    borderBottomWidth: 1,
    borderBottomColor: "#e6edf5",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  assistantTitle: { color: "#233a50", fontSize: 14, fontWeight: "700" },
  assistantSubtitle: { color: "#8292a1", fontSize: 10, marginTop: 3 },
  assistantClose: { color: "#60758a", fontSize: 24, paddingHorizontal: 5 },
  assistantHistory: { flex: 1 },
  assistantHistoryContent: { padding: 12, gap: 8 },
  chatBubble: { maxWidth: "90%", padding: 10, borderRadius: 12 },
  botBubble: { alignSelf: "flex-start", backgroundColor: "#f0f4f8" },
  userBubble: { alignSelf: "flex-end", backgroundColor: "#377bd2" },
  chatText: { color: "#344b61", fontSize: 12, lineHeight: 17 },
  userChatText: { color: "white" },
  assistantQuickLinks: { paddingHorizontal: 10, paddingTop: 8, flexDirection: "row", flexWrap: "wrap", gap: 6 },
  assistantChip: { paddingHorizontal: 9, paddingVertical: 6, backgroundColor: "#f2f7fd", borderRadius: 14, borderWidth: 1, borderColor: "#dce8f6" },
  assistantChipText: { color: "#377bd2", fontSize: 10, fontWeight: "600" },
  assistantComposer: { padding: 9, flexDirection: "row", alignItems: "center", gap: 7, borderTopWidth: 1, borderTopColor: "#e9edf2" },
  assistantInput: { flex: 1, height: 40, paddingHorizontal: 11, borderRadius: 20, backgroundColor: "#f5f7fb", color: "#344b61", fontSize: 12 },
  assistantSend: { paddingHorizontal: 12, paddingVertical: 10, backgroundColor: "#377bd2", borderRadius: 18 },
  assistantSendText: { color: "white", fontSize: 11, fontWeight: "700" },
  nav: {
    height: 61,
    minHeight: 61,
    flexGrow: 0,
    flexShrink: 0,
    backgroundColor: "white",
    borderTopWidth: 1,
    borderTopColor: "#e9edf2",
  },
  navContent: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
    paddingHorizontal: 4,
  },
  navItem: {
    alignItems: "center",
    justifyContent: "center",
    gap: 3,
    minWidth: 49,
    paddingHorizontal: 3,
  },
  navIcon: { fontSize: 17, color: "#94a1ad" },
  navText: { fontSize: 9, color: "#94a1ad" },
  navActive: { color: "#377bd2", fontWeight: "700" },
  fieldLabel: {
    fontSize: 9,
    letterSpacing: 1,
    color: "#8091a1",
    fontWeight: "700",
    marginTop: 16,
    marginBottom: 8,
  },
  optionList: { gap: 6 },
  option: {
    padding: 12,
    backgroundColor: "white",
    borderWidth: 1,
    borderColor: "#e6ebf0",
    borderRadius: 8,
  },
  optionSelected: { backgroundColor: "#eef5ff", borderColor: "#77a4da" },
  optionText: { fontSize: 11, color: "#425b73" },
  outlineButton: {
    height: 44,
    borderRadius: 9,
    borderWidth: 1,
    borderColor: "#dce4eb",
    alignItems: "center",
    justifyContent: "center",
    marginTop: 16,
  },
  outlineText: { color: "#ab5a56", fontSize: 12, fontWeight: "700" },
});
