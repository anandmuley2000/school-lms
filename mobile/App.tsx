import React, { useCallback, useEffect, useState } from "react";
import Constants from "expo-constants";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
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
import { SafeAreaView } from "react-native-safe-area-context";
import {
  api,
  AttendanceRecord,
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
  | "Leave";
const IS_DEVELOPMENT_BUILD =
  process.env.EXPO_PUBLIC_APP_ENV === "development" ||
  Constants.expoConfig?.extra?.environment === "development";
const demos = [
  { role: "ADMIN", email: "admin@schooldemo.com", password: "Admin@123" },
  { role: "TEACHER", email: "teacher@schooldemo.com", password: "Teacher@123" },
  { role: "PARENT", email: "parent@schooldemo.com", password: "Parent@123" },
  { role: "STUDENT", email: "student@schooldemo.com", password: "Student@123" },
];
export default function App() {
  const [user, setUser] = useState<User | null>(null),
    [tab, setTab] = useState<Tab>("Home"),
    [email, setEmail] = useState(IS_DEVELOPMENT_BUILD ? "admin@schooldemo.com" : ""),
    [password, setPassword] = useState(IS_DEVELOPMENT_BUILD ? "Admin@123" : ""),
    [students, setStudents] = useState<Student[]>([]),
    [homework, setHomework] = useState<Homework[]>([]),
    [notes, setNotes] = useState<Note[]>([]),
    [results, setResults] = useState<Result[]>([]),
    [fees, setFees] = useState<Fee[]>([]),
    [messages, setMessages] = useState<Message[]>([]),
    [events, setEvents] = useState<SchoolEvent[]>([]),
    [leaveApplications, setLeaveApplications] = useState<LeaveApplication[]>([]),
    [leaveStudentId, setLeaveStudentId] = useState(""),
    [leaveStart, setLeaveStart] = useState(""),
    [leaveEnd, setLeaveEnd] = useState(""),
    [leaveReason, setLeaveReason] = useState(""),
    [attendanceRecords, setAttendanceRecords] = useState<
      Record<number, AttendanceRecord[]>
    >({}),
    [recipient, setRecipient] = useState("teacher@schooldemo.com"),
    [subjectLine, setSubjectLine] = useState(""),
    [messageBody, setMessageBody] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [refreshing, setRefreshing] = useState(false),
    [markId, setMarkId] = useState(""),
    [status, setStatus] = useState("ABSENT");
  const load = useCallback(async () => {
    if (!user) return;
    setError("");
    try {
      const [s, h, n, r, f, m, ev, lv] = await Promise.all([
        api<Student[]>("/students"),
        api<Homework[]>("/homework"),
        api<Note[]>("/notifications"),
        api<Result[]>("/results"),
        api<Fee[]>("/fees"),
        api<Message[]>("/messages"),
        api<SchoolEvent[]>("/events"),
        api<LeaveApplication[]>("/leave-requests"),
      ]);
      setStudents(s);
      setHomework(h);
      setNotes(n);
      setResults(r);
      setFees(f);
      setMessages(m);
      setEvents(ev);
      setLeaveApplications(lv);
      if (user.role === "PARENT" || user.role === "STUDENT") {
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
    } catch (ex) {
      setError(ex instanceof Error ? ex.message : "Sign in failed");
    } finally {
      setBusy(false);
    }
  }
  async function refresh() {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  }
  async function mark() {
    try {
      await api("/attendance", {
        method: "POST",
        body: JSON.stringify({
          studentId: Number(markId),
          date: new Date().toISOString().slice(0, 10),
          status,
        }),
      });
      setError("Attendance saved. Parent notification sent.");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not record attendance");
    }
  }
  async function sendMessage() {
    try {
      await api("/messages", {
        method: "POST",
        body: JSON.stringify({
          recipientEmail: recipient,
          subject: subjectLine,
          body: messageBody,
        }),
      });
      setSubjectLine("");
      setMessageBody("");
      setError("Message sent.");
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not send message");
    }
  }
  async function submitLeave() {
    try {
      await api("/leave-requests", { method: "POST", body: JSON.stringify({ studentId: leaveStudentId ? Number(leaveStudentId) : null, startDate: leaveStart, endDate: leaveEnd, reason: leaveReason }) });
      setLeaveStart(""); setLeaveEnd(""); setLeaveReason(""); setError("Leave request submitted."); await load();
    } catch (e) { setError(e instanceof Error ? e.message : "Could not submit leave request"); }
  }
  async function updateLeave(id: number, status: "APPROVED" | "REJECTED") {
    try { await api(`/leave-requests/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }); setError(`Leave request ${status.toLowerCase()}.`); await load(); }
    catch (e) { setError(e instanceof Error ? e.message : "Could not update leave request"); }
  }
  async function deleteLeave(id: number) {
    try { await api(`/leave-requests/${id}`, { method: "DELETE" }); setError("Leave request deleted."); await load(); }
    catch (e) { setError(e instanceof Error ? e.message : "Could not delete leave request"); }
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
          <Text style={styles.kicker}>NORTHSTAR SCHOOL</Text>
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
              <Text style={styles.sectionLabel}>DEMO ACCOUNTS · TAP TO FILL</Text>
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
      ? ["Home", "Attendance", "Homework", "Events", "Leave", "Updates", "Profile"]
      : user.role === "ADMIN"
        ? ["Home", "Students", "Attendance", "Events", "Leave", "Updates", "Profile"]
        : ["Home", "Students", "Homework", "Events", "Leave", "Updates", "Profile"];
  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.top}>
        <View>
          <Text style={styles.kicker}>NORTHSTAR SCHOOL</Text>
          <Text style={styles.topTitle}>{user.name}</Text>
        </View>
        <Pressable style={styles.avatar} onPress={() => setTab("Profile")}>
          <Text style={styles.avatarText}>{user.name[0]}</Text>
        </Pressable>
      </View>
      <ScrollView
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
            <Section title="Your students">
              {students.slice(0, 3).map((s) => (
                <StudentCard key={s.id} s={s} />
              ))}
            </Section>
            <Section title="Upcoming homework">
              {homework.slice(0, 3).map((h) => (
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
            {students.map((s) => (
              <StudentCard key={s.id} s={s} />
            ))}
          </Section>
        )}
        {tab === "Homework" && (
          <Section title="Assignments">
            {homework.map((h) => (
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
            {results.map((r) => (
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
            {fees.map((f) => (
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
          <>
            <Section title="Send a message">
              <TextInput
                style={styles.input}
                autoCapitalize="none"
                value={recipient}
                onChangeText={setRecipient}
                placeholder="Recipient email"
              />
              <TextInput
                style={styles.input}
                value={subjectLine}
                onChangeText={setSubjectLine}
                placeholder="Subject"
              />
              <TextInput
                style={[styles.input, styles.messageInput]}
                multiline
                value={messageBody}
                onChangeText={setMessageBody}
                placeholder="Write a message"
              />
              <Pressable style={styles.button} onPress={sendMessage}>
                <Text style={styles.buttonText}>Send message</Text>
              </Pressable>
            </Section>
            <Section title="Inbox & sent">
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
          </>
        )}
        {tab === "Events" && (
          <Section title="School events">
            {events.length === 0 && <Text style={styles.muted}>No events have been announced yet.</Text>}
            {events.map((event) => <View key={event.id} style={styles.card}><Text style={styles.cardTitle}>{event.title}</Text><Text style={styles.cardText}>{event.description}</Text><Text style={styles.role}>{event.eventDate} · {event.location || "School"}</Text></View>)}
          </Section>
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
            <Section title={user.role === "ADMIN" ? "All leave requests" : "Your leave requests"}>
              {leaveApplications.length === 0 && <Text style={styles.muted}>No leave requests to show.</Text>}
              {leaveApplications.map((leave) => <View key={leave.id} style={styles.card}><Text style={styles.cardTitle}>{leave.studentName || leave.requesterName} · {leave.status}</Text><Text style={styles.cardText}>{leave.startDate} to {leave.endDate}</Text><Text style={styles.cardText}>{leave.reason}</Text>{user.role === "ADMIN" && leave.status === "PENDING" && <View style={styles.demoRow}><Pressable style={styles.demo} onPress={() => updateLeave(leave.id, "APPROVED")}><Text style={styles.demoText}>APPROVE</Text></Pressable><Pressable style={styles.demo} onPress={() => updateLeave(leave.id, "REJECTED")}><Text style={styles.demoText}>REJECT</Text></Pressable></View>}{(user.role === "ADMIN" || (leave.requesterEmail === user.email && leave.status === "PENDING")) && <Pressable style={styles.outlineButton} onPress={() => deleteLeave(leave.id)}><Text style={styles.outlineText}>Delete request</Text></Pressable>}</View>)}
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
              <Pressable style={styles.button} onPress={mark}>
                <Text style={styles.buttonText}>Save attendance</Text>
              </Pressable>
            </Section>
          ) : (
            <Section title="Attendance history">
              {students.map((s) => (
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
      <View style={styles.nav}>
        {tabs.map((t, i) => (
          <Pressable key={t} style={styles.navItem} onPress={() => setTab(t)}>
            <Text style={[styles.navIcon, tab === t && styles.navActive]}>
              {({ Home: "⌂", Students: "♙", Homework: "▧", Updates: "◉", Profile: "○", Attendance: "✓", Events: "☆", Leave: "✉", Results: "◈", Fees: "₹", Messages: "✉" } as Record<Tab, string>)[t]}
            </Text>
            <Text style={[styles.navText, tab === t && styles.navActive]}>
              {t}
            </Text>
          </Pressable>
        ))}
      </View>
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
        {h.subject} · Class {h.className} · Due {h.dueDate}
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
  cardTop: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  cardTitle: { fontSize: 13, fontWeight: "700", color: "#2b435b" },
  cardText: { fontSize: 11, color: "#77899b", lineHeight: 17, marginTop: 5 },
  role: { fontSize: 9, color: "#4c80c4", marginTop: 7, fontWeight: "700" },
  nav: {
    height: 61,
    backgroundColor: "white",
    borderTopWidth: 1,
    borderTopColor: "#e9edf2",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
  },
  navItem: {
    alignItems: "center",
    justifyContent: "center",
    gap: 3,
    minWidth: 49,
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
