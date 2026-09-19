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

      // 1. Check if student already submitted this exam
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
          showError(err.message || "Unable to load the backend result for this exam");
        }
        setLoading(false);
        return;
      }

      // 2. Fetch exam details
      let examDetails = locationState;
      if (!examDetails?.examName || !examDetails?.date) {
        try {
          examDetails = await getExamById(examId);
        } catch {
          console.warn("Using fallback exam data");
        }
      }
      const loadedExam = examDetails || { exam_name: "Examination", exam_type: "MCQ" };
      setExam(loadedExam);

      // 3. Registration & Schedule Window Verification
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
            message: "You are not registered for this examination. Registered candidate access only.",
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
            message: `This examination has not started yet. The examination window opens on ${schedule.datePart} at ${schedule.startTimeStr}.`,
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
            message: `The scheduled testing window for this examination has ended (${schedule.datePart} ${schedule.endTimeStr}).`,
            datePart: schedule.datePart,
            startTimeStr: schedule.startTimeStr,
            endTimeStr: schedule.endTimeStr,
          });
          setLoading(false);
          return;
        }

        // 4. Start or Resume Attempt
        const attempt = await startExamAttempt(examId);
        const resolvedAttemptId = attempt?.attemptId || attempt?.attempt_id;
        if (!resolvedAttemptId) {
          throw new Error("The exam attempt could not be started.");
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

      // 5. Fetch Questions
      const fetchedQuestions = await getExamQuestions(examId);
      setQuestions(fetchedQuestions);

      // 6. Restore saved answers from server
      if (activeAttemptId) {
        try {
          const savedAnswers = await getAttemptAnswers(examId, activeAttemptId);
          setInitialAnswers(savedAnswers);
        } catch (err) {
          if (showError) {
            showError(err.message || "Unable to restore saved exam answers");
          }
        }
      }
    } catch (err) {
      if (showError) {
        showError(err.message || "Failed to load examination data");
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
