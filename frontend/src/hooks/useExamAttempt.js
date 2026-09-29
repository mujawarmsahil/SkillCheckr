import { useState, useCallback } from "react";
import { useAuth } from "../context/AuthContext";
import { getExamById, getExamQuestions, checkStudentRegistration } from "../api/examApi";
import { startExamAttempt, getAttemptAnswers } from "../api/attemptApi";
import { checkStudentExamStatus } from "../api/resultApi";
import { parseExamSchedule } from "../utils/dateUtils";

export function useExamAttempt({ examId, locationState, showError }) {
  const { user } = useAuth();
  const [exam, setExam] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [attemptSession, setAttemptSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [alreadySubmitted, setAlreadySubmitted] = useState(false);
  const [resultData, setResultData] = useState(null);
  const [accessBlocked, setAccessBlocked] = useState(null);
  const [initialAnswers, setInitialAnswers] = useState([]);

  const loadExamAndAttempt = useCallback(async () => {
    setLoading(true);
    try {
      const studentId = user?.roleId ? parseInt(user.roleId, 10) : null;
      if (!studentId) {
        throw new Error("Your student account could not be identified. Please sign in again.");
      }

      const submitted = await checkStudentExamStatus(examId, studentId);
      if (submitted?.hasSubmitted && submitted?.result) {
        setResultData(submitted.result);
        setAlreadySubmitted(true);
        return;
      }

      // Router state is only a display cache; it is never treated as authoritative.
      let loadedExam = locationState;
      if (!loadedExam?.examName && !loadedExam?.exam_name) {
        try {
          loadedExam = await getExamById(examId);
        } catch (err) {
          throw new Error(err.message || "This exam could not be loaded");
        }
      }
      if (!loadedExam?.examName && !loadedExam?.exam_name) {
        throw new Error("This exam could not be loaded");
      }
      setExam(loadedExam);

      // A failed request must never be read as "not registered": that would tell a
      // registered student they are not. The error propagates so it can be retried.
      const registration = await checkStudentRegistration(examId, studentId);
      if (!registration?.isRegistered) {
        const schedule = parseExamSchedule(loadedExam);
        setAccessBlocked({
          reason: "NOT_REGISTERED",
          message: "You are not registered for this exam.",
          datePart: schedule.datePart,
          startTimeStr: schedule.startTimeStr,
          endTimeStr: schedule.endTimeStr,
          isRegistrationOpen: !schedule.isRegistrationClosed,
        });
        return;
      }

      const schedule = parseExamSchedule(loadedExam);
      if (schedule.isExamUpcoming) {
        setAccessBlocked({
          reason: "NOT_STARTED",
          message: `This exam has not started yet. It opens on ${schedule.datePart} at ${schedule.startTimeStr}.`,
          datePart: schedule.datePart,
          startTimeStr: schedule.startTimeStr,
          endTimeStr: schedule.endTimeStr,
          startDateTime: schedule.startDateTime,
        });
        return;
      }

      if (schedule.isExamExpired) {
        setAccessBlocked({
          reason: "EXPIRED",
          message: `The exam window has closed (${schedule.datePart} ${schedule.endTimeStr}).`,
          datePart: schedule.datePart,
          startTimeStr: schedule.startTimeStr,
          endTimeStr: schedule.endTimeStr,
        });
        return;
      }

      // Resumes the open attempt when one already exists.
      const attempt = await startExamAttempt(examId);
      const attemptId = attempt?.attemptId ?? attempt?.attempt_id ?? null;
      if (!attemptId) {
        throw new Error("Exam attempt could not be started.");
      }

      setAttemptSession({
        examId: parseInt(examId, 10),
        attemptId,
        startedAt: attempt.startedAt || attempt.started_at || null,
        expiresAt: attempt.expiresAt || attempt.expires_at || null,
        status: attempt.status || "IN_PROGRESS",
      });

      const fetchedQuestions = await getExamQuestions(examId);
      setQuestions(fetchedQuestions);

      try {
        setInitialAnswers(await getAttemptAnswers(examId, attemptId));
      } catch (err) {
        if (showError) {
          showError(err.message || "Unable to load saved answers");
        }
      }
    } catch (err) {
      if (showError) {
        showError(err.message || "Failed to load exam");
      }
    } finally {
      setLoading(false);
    }
  }, [examId, locationState, user, showError]);

  return {
    exam,
    setExam,
    questions,
    attemptSession,
    loading,
    alreadySubmitted,
    resultData,
    setResultData,
    accessBlocked,
    setAccessBlocked,
    initialAnswers,
    loadExamAndAttempt,
  };
}
