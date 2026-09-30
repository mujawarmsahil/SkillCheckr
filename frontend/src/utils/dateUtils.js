import { EXAM_MIN_LEAD_TIME_DAYS } from "../constants/examConstants";

// Local YYYY-MM-DD, not toISOString(): that converts to UTC and shifts the
// value by a day for users east or west of Greenwich.
const toDateInputValue = (date = new Date()) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;

export const getMinimumExamDate = (leadDays = EXAM_MIN_LEAD_TIME_DAYS, referenceDate = new Date()) =>
  toDateInputValue(new Date(referenceDate.getFullYear(), referenceDate.getMonth(), referenceDate.getDate() + leadDays));

// Compares only the date portion, so the time of day never affects the
// result and a date exactly on the lead-time boundary is accepted.
export const isExamDateTooSoon = (examDate, leadDays = EXAM_MIN_LEAD_TIME_DAYS, referenceDate = new Date()) => {
  if (!examDate) return false;
  const datePart = String(examDate).trim().split(/[ T]/)[0];
  if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) return false;
  return datePart < getMinimumExamDate(leadDays, referenceDate);
};

export const parseExamSchedule = (exam, referenceDate = new Date()) => {
  if (!exam) {
    return {
      datePart: "",
      startTimeStr: "00:00",
      endTimeStr: "23:59",
      startDateTime: null,
      endDateTime: null,
      isRegistrationClosed: false,
      isExamUpcoming: false,
      isExamActive: false,
      isExamExpired: false,
    };
  }

  const rawDate = exam.date || exam.exam_date || exam.examDate || "";
  let datePart = "";
  if (rawDate) {
    const trimmed = String(rawDate).trim();
    if (trimmed.includes(" ")) {
      datePart = trimmed.split(" ")[0];
    } else if (trimmed.includes("T")) {
      datePart = trimmed.split("T")[0];
    } else {
      datePart = trimmed;
    }
  } else {
    datePart = new Date().toISOString().split("T")[0];
  }

  let year = 2026;
  let month = 1;
  let day = 1;
  if (datePart.includes("-")) {
    const parts = datePart.split("-").map((v) => parseInt(v, 10));
    year = parts[0] || 2026;
    month = parts[1] || 1;
    day = parts[2] || 1;
  }

  const startTimeStr = exam.start_time || exam.startTime || "00:00";
  const endTimeStr = exam.end_time || exam.endTime || "23:59";

  const [sh, sm] = String(startTimeStr).split(":").map((v) => parseInt(v, 10) || 0);
  const [eh, em] = String(endTimeStr).split(":").map((v) => parseInt(v, 10) || 0);

  const startDateTime = new Date(year, month - 1, day, sh, sm, 0, 0);
  const endDateTime = new Date(year, month - 1, day, eh, em, 0, 0);

  if (endDateTime < startDateTime) {
    endDateTime.setDate(endDateTime.getDate() + 1);
  }

  const now = referenceDate;
  const isRegistrationClosed = now >= startDateTime;
  const isExamUpcoming = now < startDateTime;
  const isExamActive = now >= startDateTime && now <= endDateTime;
  const isExamExpired = now > endDateTime;

  return {
    datePart,
    startTimeStr,
    endTimeStr,
    startDateTime,
    endDateTime,
    isRegistrationClosed,
    isExamUpcoming,
    isExamActive,
    isExamExpired,
  };
};

/**
 * Parses a server timestamp to an absolute instant in epoch milliseconds.
 *
 * The exam attempt endpoint sends `expiresAt` as ISO-8601 with an explicit offset, for example
 * `2026-09-17T19:00:00+05:30`. A string carrying an offset denotes one fixed instant, so the
 * browser's own timezone cannot shift the result and the countdown agrees with the server's
 * expiry check.
 *
 * A bare value with no offset is rejected rather than guessed. `new Date("2026-09-17T19:00:00")`
 * would read that wall-clock time as browser-local time, which is exactly the bug this guards
 * against, so an ambiguous value yields null and the attempt is treated as unusable instead of
 * silently counting down against the wrong instant.
 *
 * @returns epoch milliseconds, or null when the value is missing or ambiguous
 */
export const parseServerTimestamp = (value) => {
  if (value === null || value === undefined || value === "") return null;

  // Epoch milliseconds, should the representation ever change to a number.
  if (typeof value === "number") {
    return Number.isFinite(value) ? value : null;
  }

  const raw = String(value).trim();
  if (!raw) return null;

  // Require an explicit zone designator. "Z", "+05:30" and "+0530" are all valid; a bare
  // local date-time is not, because its instant depends on where it is read.
  const hasExplicitZone = /(?:Z|z|[+-]\d{2}:?\d{2})$/.test(raw);
  if (!hasExplicitZone) return null;

  const parsed = new Date(raw);
  const ms = parsed.getTime();
  return Number.isNaN(ms) ? null : ms;
};

/**
 * Seconds left until the server-stated expiry, floored at zero.
 * Returns 0 for a missing or unparseable timestamp so an attempt is never shown as live
 * on the strength of a value the client could not interpret.
 */
export const getRemainingTime = (expiresAt) => {
  const expiresAtMs = parseServerTimestamp(expiresAt);
  if (expiresAtMs === null) return 0;
  return Math.max(0, Math.ceil((expiresAtMs - Date.now()) / 1000));
};

export const formatRemainingTime = (totalSeconds) => {
  const seconds = Math.max(0, Math.floor(totalSeconds || 0));
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
};
