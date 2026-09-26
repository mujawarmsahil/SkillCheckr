/**
 * Date and time utilities for exams and server-authoritative timers.
 */

import { EXAM_MIN_LEAD_TIME_DAYS } from "../constants/examConstants";

/**
 * Formats a Date as a local `YYYY-MM-DD` string for `<input type="date">`.
 * toISOString() must not be used here because it converts to UTC and can shift
 * the value by a day for users east or west of Greenwich.
 */
export const toDateInputValue = (date = new Date()) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;

/**
 * Earliest exam date that satisfies the required lead time: the current day plus
 * `leadDays`. Returned in the same `YYYY-MM-DD` form used by the date input.
 */
export const getMinimumExamDate = (leadDays = EXAM_MIN_LEAD_TIME_DAYS, referenceDate = new Date()) =>
  toDateInputValue(new Date(referenceDate.getFullYear(), referenceDate.getMonth(), referenceDate.getDate() + leadDays));

/**
 * True when the exam date is earlier than the current day plus the required lead
 * time. Only the date portion is compared, so the time of day never affects the
 * result and a date exactly on the boundary is accepted.
 */
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

export const getRemainingTime = (expiresAt) => {
  if (!expiresAt) return 0;
  const expiresAtMs = new Date(expiresAt).getTime();
  if (Number.isNaN(expiresAtMs)) return 0;
  return Math.max(0, Math.ceil((expiresAtMs - Date.now()) / 1000));
};

export const formatRemainingTime = (totalSeconds) => {
  const seconds = Math.max(0, Math.floor(totalSeconds || 0));
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
};
