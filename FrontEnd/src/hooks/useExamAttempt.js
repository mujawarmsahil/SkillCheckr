import { useState, useCallback } from "react";
import { getExamById, getExamQuestions, checkStudentRegistration } from "../api/examApi";
import { startExamAttempt, getAttemptAnswers } from "../api/attemptApi";
import { checkStudentExamStatus } from "../api/resultApi";
import { parseExamSchedule } from "../utils/dateUtils";
import { saveExamSession } from "../utils/storageUtils";

export function useExamAttempt({ examId, user, locationState, showError }) {
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
      const studentId = user?.roleId || localStorage.getItem("student_id") || 1;

      try {
        const checkRes = await checkStudentExamStatus(examId, studentId);
        if (checkRes?.hasSubmitted && checkRes?.result) {
          setResultData(checkRes.result);
          setAlreadySubmitted(true);
          setLoading(false);
          return;
        }
      } catch (err) {
        if (showError) {
          showError(err.message || "Unable to load your result for this exam");
        }
        setLoading(false);
        return;
      }

      let examDetails = locationState;
      if (!examDetails?.examName || !examDetails?.date) {
        try {
          examDetails = await getExamById(examId);
        } catch {
          console.warn("Using fallback exam data");
        }
      }
      const loadedExam = examDetails || { exam_name: "Exam", exam_type: "MCQ" };
      setExam(loadedExam);

      // Registration and exam window checks apply to students only
      const userRole = user?.role || localStorage.getItem("role") || "Student";
      let activeAttemptId = null;

      if (userRole === "Student") {
        let isRegistered = false;
        try {
          const regRes = await checkStudentRegistration(examId, studentId);
          isRegistered = !!regRes?.isRegistered;
        } catch {
          try {
            const localRegs = JSON.parse(
              localStorage.getItem(`student_${studentId}_registered_exams`) || "{}"
            );
            isRegistered = !!localRegs[examId];
          } catch {
            isRegistered = false;
          }
        }

        const schedule = parseExamSchedule(loadedExam);

        if (!isRegistered) {
          setAccessBlocked({
            reason: "NOT_REGISTERED",
            message: "You are not registered for this exam.",
            datePart: schedule.datePart,
            startTimeStr: schedule.startTimeStr,
            endTimeStr: schedule.endTimeStr,
            isRegistrationOpen: !schedule.isRegistrationClosed,
          });
          setLoading(false);
          return;
        }

        if (schedule.isExamUpcoming) {
          setAccessBlocked({
            reason: "NOT_STARTED",
            message: `This exam has not started yet. It opens on ${schedule.datePart} at ${schedule.startTimeStr}.`,
            datePart: schedule.datePart,
            startTimeStr: schedule.startTimeStr,
            endTimeStr: schedule.endTimeStr,
            startDateTime: schedule.startDateTime,
          });
          setLoading(false);
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
          setLoading(false);
          return;
        }

        // Resumes the open attempt when one already exists
        const attempt = await startExamAttempt(examId);
        const resolvedAttemptId = attempt?.attemptId || attempt?.attempt_id;
        if (!resolvedAttemptId) {
          throw new Error("Exam attempt could not be started.");
        }

        const currentAttempt = {
          examId: parseInt(examId, 10),
          attemptId: resolvedAttemptId,
          startedAt: attempt.startedAt || attempt.started_at || null,
          expiresAt: attempt.expiresAt || attempt.expires_at || null,
          status: attempt.status || "IN_PROGRESS",
        };
        activeAttemptId = resolvedAttemptId;
        setAttemptSession(currentAttempt);
        saveExamSession(currentAttempt.examId, currentAttempt.attemptId);
      }

      const fetchedQuestions = await getExamQuestions(examId);
      setQuestions(fetchedQuestions);

      if (activeAttemptId) {
        try {
          const savedAnswers = await getAttemptAnswers(examId, activeAttemptId);
          setInitialAnswers(savedAnswers);
        } catch (err) {
          if (showError) {
            showError(err.message || "Unable to load saved answers");
          }
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
