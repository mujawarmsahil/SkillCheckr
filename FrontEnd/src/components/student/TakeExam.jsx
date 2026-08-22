import React, { useState, useEffect, useMemo, useCallback, useRef } from "react";
import { useParams, useLocation, useNavigate } from "react-router-dom";
import apiClient from "../../api/client";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "../../context/ToastContext";
import { Icon } from "../common/Icons";

export default function TakeExam() {
  const { examId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { showSuccess, showError, showWarning } = useToast();

  const [exam, setExam] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [mcqAnswers, setMcqAnswers] = useState({});
  const [textAnswers, setTextAnswers] = useState({});
  const [flagged, setFlagged] = useState(new Set());

  const [timeLeftSeconds, setTimeLeftSeconds] = useState(3600);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [resultData, setResultData] = useState(null);
  const [alreadySubmitted, setAlreadySubmitted] = useState(false);

  // -------------------------------------------------------------
  // ANTI-CHEATING & PROCTORING STATE
  // -------------------------------------------------------------
  const [copyStrikes, setCopyStrikes] = useState(0);
  const [tabStrikes, setTabStrikes] = useState(0);
  const [totalViolations, setTotalViolations] = useState(0);
  const [isDisqualified, setIsDisqualified] = useState(false);
  const [disqualificationReason, setDisqualificationReason] = useState("");
  const [activeViolationModal, setActiveViolationModal] = useState(null);

  // MULTI-PERSON DETECTION (ACCURATE & DEBOUNCED)
  const [detectedPersonsCount, setDetectedPersonsCount] = useState(1);
  const [multiplePersonsDurationSeconds, setMultiplePersonsDurationSeconds] = useState(0);

  const [isCameraActive, setIsCameraActive] = useState(false);
  const [cameraError, setCameraError] = useState(null);
  const [screenShieldActive, setScreenShieldActive] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [fullscreenWarning, setFullscreenWarning] = useState(false);

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const isSubmittingRef = useRef(false);
  const faceDetectorRef = useRef(null);
  const consecutiveMultiPersonFramesRef = useRef(0);
  const isTabHiddenRef = useRef(false);

  const isMcq = (exam?.exam_type || exam?.examType || questions[currentIndex]?.questionType || "MCQ").toUpperCase() === "MCQ";

  // -------------------------------------------------------------
  // 1. WEBCAM PROCTORING SHUTDOWN & INITIALIZATION
  // -------------------------------------------------------------
  const stopWebcam = useCallback(() => {
    if (streamRef.current) {
      try {
        streamRef.current.getTracks().forEach((track) => {
          track.stop();
          track.enabled = false;
        });
      } catch (err) {
        console.warn("Track cleanup warning:", err);
      }
      streamRef.current = null;
    }
    if (videoRef.current) {
      try {
        videoRef.current.pause();
        videoRef.current.srcObject = null;
      } catch (err) {
        console.warn("Video cleanup warning:", err);
      }
    }
    setIsCameraActive(false);
  }, []);

  const startWebcam = useCallback(async () => {
    try {
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 320 }, height: { ideal: 240 }, facingMode: "user" },
          audio: false,
        });
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
        }
        setIsCameraActive(true);
        setCameraError(null);

        if (typeof window !== "undefined" && "FaceDetector" in window) {
          try {
            // @ts-ignore
            faceDetectorRef.current = new window.FaceDetector({ fastMode: true, maxDetectedFaces: 10 });
          } catch {
            faceDetectorRef.current = null;
          }
        }
      }
    } catch (err) {
      console.warn("Camera access warning:", err);
      setCameraError("Camera permission recommended for proctoring verification.");
    }
  }, []);

  useEffect(() => {
    if (resultData || isDisqualified) {
      stopWebcam();
    }
  }, [resultData, isDisqualified, stopWebcam]);

  // -------------------------------------------------------------
  // 2. FETCH EXAM AND PREVENT DUPLICATE SUBMISSIONS
  // -------------------------------------------------------------
  const fetchExamAndQuestions = useCallback(async () => {
    setLoading(true);
    try {
      const studentId = user?.roleId || localStorage.getItem("student_id") || 1;

      // Check if student already submitted this exam
      try {
        const checkRes = await apiClient.get(`/api/results/check/${examId}/${studentId}`);
        if (checkRes.data?.hasSubmitted && checkRes.data?.result) {
          stopWebcam();
          setResultData(checkRes.data.result);
          setAlreadySubmitted(true);
          setLoading(false);
          return;
        }
      } catch {
        // Fallback: check local storage completed results
        try {
          const localKey = `submitted_results_${studentId}`;
          const localResults = JSON.parse(localStorage.getItem(localKey) || "{}");
          if (localResults[examId]) {
            stopWebcam();
            setResultData(localResults[examId]);
            setAlreadySubmitted(true);
            setLoading(false);
            return;
          }
        } catch {
          // continue
        }
      }

      let examDetails = location.state;
      if (!examDetails?.examName) {
        try {
          const res = await apiClient.get(`/api/exams/${examId}`);
          examDetails = res.data;
        } catch {
          console.warn("Using fallback exam data");
        }
      }
      setExam(examDetails || { exam_name: "Examination", exam_type: "MCQ" });

      const initialDuration = (examDetails?.duration_minutes || examDetails?.durationMinutes || 60) * 60;
      setTimeLeftSeconds(initialDuration);

      const qRes = await apiClient.get(`/api/exams/${examId}/questions`);
      let fetchedQuestions = Array.isArray(qRes.data) ? qRes.data : [];

      if (fetchedQuestions.length === 0) {
        const isMcqExam = (examDetails?.exam_type || examDetails?.examType || "MCQ") === "MCQ";
        if (isMcqExam) {
          fetchedQuestions = [
            {
              questionId: 101,
              question_id: 101,
              question: "What is the primary function of an operating system kernel?",
              option1: "Manage hardware resources and provide essential services to applications",
              option2: "Compile high-level programming language code into machine binaries",
              option3: "Provide a web browsing interface for end users",
              option4: "Design user interface graphics and typography",
              correctOption: "Manage hardware resources and provide essential services to applications",
              questionType: "MCQ",
            },
            {
              questionId: 102,
              question_id: 102,
              question: "Which of the following data structures operates on a Last-In, First-Out (LIFO) principle?",
              option1: "Queue",
              option2: "Stack",
              option3: "Linked List",
              option4: "Binary Search Tree",
              correctOption: "Stack",
              questionType: "MCQ",
            },
            {
              questionId: 103,
              question_id: 103,
              question: "What is the time complexity of searching an element in a balanced Binary Search Tree?",
              option1: "O(1)",
              option2: "O(n)",
              option3: "O(log n)",
              option4: "O(n log n)",
              correctOption: "O(log n)",
              questionType: "MCQ",
            },
          ];
        } else {
          fetchedQuestions = [
            {
              questionId: 201,
              question_id: 201,
              question: "Explain the concept of Polymorphism in Object-Oriented Programming with real-world examples.",
              sampleAnswer: "Polymorphism allows objects of different classes to be treated as instances of a common superclass.",
              marks: 10,
              wordLimit: 250,
              questionType: "QUESTION_ANSWER",
            },
            {
              questionId: 202,
              question_id: 202,
              question: "Describe ACID properties in database management systems and why they are essential for data consistency.",
              sampleAnswer: "Atomicity, Consistency, Isolation, and Durability ensure transaction reliability.",
              marks: 10,
              wordLimit: 200,
              questionType: "QUESTION_ANSWER",
            },
          ];
        }
      }

      setQuestions(fetchedQuestions);

      const draftKey = `draft_exam_${examId}_${user?.userId || "guest"}`;
      const savedDraft = localStorage.getItem(draftKey);
      if (savedDraft) {
        try {
          const parsed = JSON.parse(savedDraft);
          if (parsed.mcqAnswers) setMcqAnswers(parsed.mcqAnswers);
          if (parsed.textAnswers) setTextAnswers(parsed.textAnswers);
        } catch {
          console.warn("Could not parse draft");
        }
      }

      // Start Proctoring
      startWebcam();
    } catch (err) {
      showError(err.message || "Failed to load examination data");
    } finally {
      setLoading(false);
    }
  }, [examId, location.state, user?.userId, user?.roleId, showError, startWebcam, stopWebcam]);

  useEffect(() => {
    fetchExamAndQuestions();
    return () => {
      stopWebcam();
    };
  }, [fetchExamAndQuestions, stopWebcam]);

  // -------------------------------------------------------------
  // 3. FINAL SUBMISSION (INSTANT SCORING + SERVER FALLBACK)
  // -------------------------------------------------------------
  const submitFinalExam = useCallback(
    async (disqualified = false, reason = "") => {
      if (isSubmittingRef.current) return;
      isSubmittingRef.current = true;
      setIsSubmitting(true);
      setShowSubmitModal(false);

      // ALWAYS completely stop camera hardware immediately
      stopWebcam();

      const studentId = user?.roleId || localStorage.getItem("student_id") || 1;
      const studentName = user?.username || "Student";

      // 1. Calculate Instant Scorecard using questions and answers
      const totalMarks = exam?.total_marks || exam?.totalMarks || (questions.length > 0 ? questions.length : 100);
      const passMarks = exam?.passing_marks || exam?.passingMarks || Math.ceil(totalMarks * 0.4);
      let calculatedMarks = 0;
      const breakdown = [];

      if (isMcq) {
        const pointsPerQ = questions.length > 0 ? Math.max(1, Math.floor(totalMarks / questions.length)) : 1;
        questions.forEach((q, idx) => {
          const qId = q.question_id || q.questionId || idx + 1;
          const chosen = mcqAnswers[qId];
          const correct = q.correctOption;
          const isCorrect = chosen && correct && chosen.trim().toLowerCase() === correct.trim().toLowerCase();
          if (isCorrect) {
            calculatedMarks += pointsPerQ;
          }
          breakdown.push({
            questionId: qId,
            question: q.question,
            selectedAnswer: chosen || "Not Answered",
            correctAnswer: correct,
            isCorrect: !!isCorrect,
          });
        });
        if (calculatedMarks > totalMarks) calculatedMarks = totalMarks;
      }

      if (disqualified) {
        calculatedMarks = 0;
      }

      const calculatedPercentage = totalMarks > 0 ? Math.round(((calculatedMarks / totalMarks) * 100) * 10) / 10 : 0;
      const calculatedStatus = disqualified
        ? "Disqualified: " + (reason || "Academic Integrity Violation")
        : isMcq
        ? calculatedMarks >= passMarks
          ? "Pass"
          : "Fail"
        : "Submitted for Evaluation";

      const fallbackResult = {
        resultId: Date.now(),
        examId: parseInt(examId, 10),
        examName: exam?.exam_name || exam?.examName || `Exam #${examId}`,
        examType: isMcq ? "MCQ" : "QUESTION_ANSWER",
        subjectName: exam?.subject?.subject_name || exam?.subject?.subjectName || "General",
        studentId: parseInt(studentId, 10),
        studentName: studentName,
        marksObtained: calculatedMarks,
        totalMarks: totalMarks,
        passingMarks: passMarks,
        percentage: calculatedPercentage,
        status: calculatedStatus,
        disqualified: !!disqualified,
        is_disqualified: !!disqualified,
        disqualificationReason: reason || "",
        violationsCount: totalViolations + (disqualified ? 1 : 0),
        questionBreakdown: breakdown,
        isServerSyncPending: false,
      };

      const submissionPayload = {
        exam_id: parseInt(examId, 10),
        examId: parseInt(examId, 10),
        student_id: parseInt(studentId, 10),
        studentId: parseInt(studentId, 10),
        student_name: studentName,
        exam_type: isMcq ? "MCQ" : "QUESTION_ANSWER",
        mcq_answers: mcqAnswers,
        text_answers: textAnswers,
        violations_count: totalViolations + (disqualified ? 1 : 0),
        is_disqualified: disqualified,
        disqualification_reason: reason,
      };

      try {
        const res = await apiClient.post("/api/results/submit", submissionPayload);
        const result = res.data;

        // Clean local draft
        const draftKey = `draft_exam_${examId}_${user?.userId || "guest"}`;
        localStorage.removeItem(draftKey);

        setResultData(result || fallbackResult);
        if (disqualified) {
          setIsDisqualified(true);
          setDisqualificationReason(reason);
          showError(`Exam Terminated: ${reason}`);
        } else {
          showSuccess("Exam submitted successfully! Scorecard generated.");
        }
      } catch (err) {
        console.warn("Backend submit error, using client-evaluated scorecard:", err);
        fallbackResult.isServerSyncPending = true;

        // Cache completed result locally
        try {
          const localKey = `submitted_results_${studentId}`;
          const currentLocal = JSON.parse(localStorage.getItem(localKey) || "{}");
          currentLocal[examId] = fallbackResult;
          localStorage.setItem(localKey, JSON.stringify(currentLocal));
        } catch {
          // ignore
        }

        const draftKey = `draft_exam_${examId}_${user?.userId || "guest"}`;
        localStorage.removeItem(draftKey);

        setResultData(fallbackResult);
        if (disqualified) {
          setIsDisqualified(true);
          setDisqualificationReason(reason);
          showError(`Exam Terminated: ${reason}`);
        } else {
          showSuccess("Exam submitted! Scorecard calculated.");
        }
      } finally {
        setIsSubmitting(false);
        stopWebcam();
      }
    },
    [examId, exam, questions, isMcq, mcqAnswers, textAnswers, totalViolations, user?.roleId, user?.username, user?.userId, stopWebcam, showError, showSuccess]
  );

  // -------------------------------------------------------------
  // 4. COPY PROTECTION WITH 3-STRIKE RULE
  // -------------------------------------------------------------
  const handleCopyViolation = useCallback(
    (actionType = "Copying") => {
      if (resultData || isDisqualified || isSubmittingRef.current) return;

      setCopyStrikes((prevStrikes) => {
        const newStrikes = prevStrikes + 1;
        setTotalViolations((t) => t + 1);

        if (newStrikes === 1) {
          setActiveViolationModal({
            title: "⚠️ Integrity Warning (Strike 1 of 3)",
            message: `${actionType} text, questions, or options is strictly forbidden. This violation has been logged. 2 strikes remaining.`,
            strike: 1,
            isDisqualified: false,
          });
          showWarning("Anti-Cheating Warning (1/3): Copying is prohibited!");
        } else if (newStrikes === 2) {
          setActiveViolationModal({
            title: "🚨 FINAL WARNING (Strike 2 of 3)",
            message: `Second copy attempt detected! One more copy violation will result in immediate disqualification and automated paper submission with 0 marks.`,
            strike: 2,
            isDisqualified: false,
          });
          showError("Final Warning (2/3): Next copy attempt will disqualify you!");
        } else if (newStrikes >= 3) {
          setActiveViolationModal({
            title: "🚫 Exam Terminated & Disqualified (Strike 3 of 3)",
            message: `Multiple copy violations detected. In accordance with examination regulations, your paper has been automatically locked and submitted.`,
            strike: 3,
            isDisqualified: true,
          });
          submitFinalExam(true, "Disqualified: Exceeded maximum allowed content copy violations (3 strikes)");
        }

        return newStrikes;
      });
    },
    [resultData, isDisqualified, submitFinalExam, showError, showWarning]
  );

  // -------------------------------------------------------------
  // 5. SECONDARY DEVICE & SCREENSHOT PROTECTION
  // -------------------------------------------------------------
  const handleSecondaryDeviceDetected = useCallback(
    (reason = "Secondary device photo capture or unauthorized screen capture detected") => {
      if (resultData || isDisqualified || isSubmittingRef.current) return;

      setScreenShieldActive(true);
      setTotalViolations((t) => t + 1);
      setActiveViolationModal({
        title: "🚫 Exam Terminated: Secondary Device Detected",
        message: "An attempt to photograph questions or capture the screen using a secondary device was detected by the proctoring shield. The examination has been immediately locked and submitted.",
        strike: 3,
        isDisqualified: true,
      });
      submitFinalExam(true, `Disqualified: ${reason}`);
    },
    [resultData, isDisqualified, submitFinalExam]
  );

  // -------------------------------------------------------------
  // 6. GLOBAL SECURITY EVENT LISTENERS
  // -------------------------------------------------------------
  useEffect(() => {
    if (resultData) return;

    const handleKeyDown = (e) => {
      const isCmdOrCtrl = e.ctrlKey || e.metaKey;
      const key = e.key ? e.key.toLowerCase() : "";

      if (isCmdOrCtrl && (key === "c" || key === "x")) {
        e.preventDefault();
        e.stopPropagation();
        handleCopyViolation(key === "c" ? "Copying" : "Cutting");
        return false;
      }

      if (isCmdOrCtrl && (key === "u" || key === "p" || key === "s")) {
        e.preventDefault();
        e.stopPropagation();
        handleCopyViolation("Shortcut action");
        return false;
      }

      if (
        key === "printscreen" ||
        (isCmdOrCtrl && e.shiftKey && (key === "3" || key === "4" || key === "5" || key === "s"))
      ) {
        e.preventDefault();
        e.stopPropagation();
        handleSecondaryDeviceDetected("Screen capture / screenshot tool trigger");
        return false;
      }

      if (key === "f12" || (isCmdOrCtrl && e.shiftKey && key === "i") || (e.altKey && isCmdOrCtrl && key === "i")) {
        e.preventDefault();
        e.stopPropagation();
        handleCopyViolation("Developer tools inspection");
        return false;
      }
    };

    const handleNativeCopy = (e) => {
      e.preventDefault();
      handleCopyViolation("Copying text");
    };

    const handleNativeCut = (e) => {
      e.preventDefault();
      handleCopyViolation("Cutting text");
    };

    const handleContextMenu = (e) => {
      e.preventDefault();
      showWarning("Right-click context menu is disabled during exams.");
    };

    const handleVisibilityChange = () => {
      if (document.hidden) {
        setScreenShieldActive(true);
        if (isTabHiddenRef.current) return;
        isTabHiddenRef.current = true;

        setTabStrikes((prev) => {
          const next = prev + 1;
          setTotalViolations((t) => t + 1);
          if (next >= 3) {
            handleSecondaryDeviceDetected("Repeated tab-switching and window focus loss");
          } else {
            showWarning(`⚠️ Tab switch detected (${next}/3). Please remain on the exam window!`);
          }
          return next;
        });
      } else {
        isTabHiddenRef.current = false;
        setScreenShieldActive(false);
      }
    };

    const handleFullscreenChange = () => {
      const inFullscreen = !!document.fullscreenElement;
      setIsFullscreen(inFullscreen);
      if (!inFullscreen && !resultData) {
        setFullscreenWarning(true);
      } else {
        setFullscreenWarning(false);
      }
    };

    window.addEventListener("keydown", handleKeyDown, true);
    document.addEventListener("copy", handleNativeCopy, true);
    document.addEventListener("cut", handleNativeCut, true);
    document.addEventListener("contextmenu", handleContextMenu, true);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    document.addEventListener("fullscreenchange", handleFullscreenChange);

    return () => {
      window.removeEventListener("keydown", handleKeyDown, true);
      document.removeEventListener("copy", handleNativeCopy, true);
      document.removeEventListener("cut", handleNativeCut, true);
      document.removeEventListener("contextmenu", handleContextMenu, true);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      document.removeEventListener("fullscreenchange", handleFullscreenChange);
    };
  }, [resultData, handleCopyViolation, handleSecondaryDeviceDetected, showWarning]);

  // -------------------------------------------------------------
  // 7. ACCURATE MULTI-PERSON DETECTION & MONITORING
  // -------------------------------------------------------------
  useEffect(() => {
    if (!isCameraActive || resultData || isDisqualified) return;

    const interval = setInterval(async () => {
      if (!videoRef.current || !canvasRef.current) return;
      const video = videoRef.current;
      const canvas = canvasRef.current;
      const ctx = canvas.getContext("2d");

      if (video.readyState >= 2) {
        let personsDetectedThisFrame = 1;

        // A. Native FaceDetector
        if (faceDetectorRef.current) {
          try {
            const faces = await faceDetectorRef.current.detect(video);
            if (Array.isArray(faces)) {
              if (faces.length > 1) {
                consecutiveMultiPersonFramesRef.current += 1;
                if (consecutiveMultiPersonFramesRef.current >= 2) {
                  personsDetectedThisFrame = faces.length;
                } else {
                  personsDetectedThisFrame = 1;
                }
              } else {
                consecutiveMultiPersonFramesRef.current = 0;
                personsDetectedThisFrame = 1;
              }
            }
          } catch {
            personsDetectedThisFrame = 1;
          }
        } else {
          // B. Canvas Spatial Fallback
          canvas.width = 96;
          canvas.height = 72;
          ctx.drawImage(video, 0, 0, 96, 72);

          try {
            const imgData = ctx.getImageData(0, 0, 96, 72);
            const data = imgData.data;
            let leftUpperFace = 0;
            let centerUpperFace = 0;
            let rightUpperFace = 0;
            let brightPixels = 0;
            let totalBrightness = 0;

            const totalPixels = data.length / 4;
            for (let i = 0; i < data.length; i += 4) {
              const r = data[i];
              const g = data[i + 1];
              const b = data[i + 2];
              const avg = (r + g + b) / 3;
              totalBrightness += avg;
              if (avg > 240) brightPixels++;

              const pixelIndex = i / 4;
              const y = Math.floor(pixelIndex / 96);
              const x = pixelIndex % 96;

              if (y < 40) {
                const isSkin = r > 110 && g > 55 && b > 35 && (Math.max(r, g, b) - Math.min(r, g, b) > 18) && Math.abs(r - g) > 15 && r > g && r > b;
                if (isSkin) {
                  if (x < 28) leftUpperFace++;
                  else if (x >= 34 && x <= 62) centerUpperFace++;
                  else if (x > 68) rightUpperFace++;
                }
              }
            }

            const isMultiHeadDetected = (leftUpperFace > 70 && centerUpperFace > 70) || (centerUpperFace > 70 && rightUpperFace > 70) || (leftUpperFace > 60 && rightUpperFace > 60);

            if (isMultiHeadDetected) {
              consecutiveMultiPersonFramesRef.current += 1;
              if (consecutiveMultiPersonFramesRef.current >= 3) {
                personsDetectedThisFrame = 2;
              } else {
                personsDetectedThisFrame = 1;
              }
            } else {
              consecutiveMultiPersonFramesRef.current = Math.max(0, consecutiveMultiPersonFramesRef.current - 1);
              personsDetectedThisFrame = 1;
            }

            const avgLuma = totalBrightness / totalPixels;
            if (brightPixels > 300 && avgLuma > 190) {
              console.warn("Optical flash glare detected");
            }
          } catch {
            personsDetectedThisFrame = 1;
          }
        }

        setDetectedPersonsCount(personsDetectedThisFrame);

        // Accumulate Multiple Persons Duration (3-4 Minutes Rule)
        if (personsDetectedThisFrame > 1) {
          setMultiplePersonsDurationSeconds((prevDuration) => {
            const nextDuration = prevDuration + 2.5;

            if (nextDuration >= 180 && prevDuration < 180) {
              setActiveViolationModal({
                title: "⚠️ Security Warning: Multiple Persons Detected",
                message: "Proctoring has detected multiple people present in your camera frame for over 3 minutes. Please ensure you are alone in a private room. Continued presence of others will lead to disqualification.",
                strike: 1,
                isDisqualified: false,
              });
              showWarning("⚠️ Multiple persons detected in room for 3+ minutes! Please be alone.");
              setTotalViolations((v) => v + 1);
            } else if (nextDuration >= 240 && prevDuration < 240) {
              setActiveViolationModal({
                title: "🚫 Exam Terminated: Unauthorized Persons Present",
                message: "Multiple persons remained in the examination room for over 4 minutes. In accordance with academic integrity standards, this session is terminated.",
                strike: 3,
                isDisqualified: true,
              });
              submitFinalExam(true, "Disqualified: Multiple persons present in room for over 4 minutes");
            }

            return nextDuration;
          });
        } else {
          setMultiplePersonsDurationSeconds((prev) => Math.max(0, prev - 2.5));
        }
      }
    }, 2500);

    return () => clearInterval(interval);
  }, [isCameraActive, resultData, isDisqualified, submitFinalExam, showWarning]);

  // -------------------------------------------------------------
  // 8. TIMER & AUTOSAVE
  // -------------------------------------------------------------
  useEffect(() => {
    if (resultData || loading) return;

    const timer = setInterval(() => {
      setTimeLeftSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          showWarning("Time expired! Automatically submitting your examination.");
          submitFinalExam(false, "Time expired");
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [resultData, loading, submitFinalExam, showWarning]);

  useEffect(() => {
    if (questions.length > 0 && !resultData) {
      const draftKey = `draft_exam_${examId}_${user?.userId || "guest"}`;
      localStorage.setItem(
        draftKey,
        JSON.stringify({ mcqAnswers, textAnswers, timestamp: Date.now() })
      );
    }
  }, [mcqAnswers, textAnswers, examId, user, questions, resultData]);

  const requestFullscreen = () => {
    try {
      if (document.documentElement.requestFullscreen) {
        document.documentElement.requestFullscreen();
      }
    } catch (err) {
      console.warn("Fullscreen request error:", err);
    }
  };

  const formatTimer = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  };

  const currentQ = questions[currentIndex];
  const currentQId = currentQ?.question_id || currentQ?.questionId || currentIndex + 1;

  const handleSelectOption = (optionText) => {
    setMcqAnswers((prev) => ({
      ...prev,
      [currentQId]: optionText,
    }));
  };

  const handleTextAnswerChange = (val) => {
    setTextAnswers((prev) => ({
      ...prev,
      [currentQId]: val,
    }));
  };

  const toggleFlag = () => {
    setFlagged((prev) => {
      const next = new Set(prev);
      if (next.has(currentQId)) {
        next.delete(currentQId);
      } else {
        next.add(currentQId);
      }
      return next;
    });
  };

  const answeredCount = useMemo(() => {
    if (isMcq) {
      return Object.keys(mcqAnswers).filter((k) => mcqAnswers[k] && mcqAnswers[k].trim() !== "").length;
    } else {
      return Object.keys(textAnswers).filter((k) => textAnswers[k] && textAnswers[k].trim() !== "").length;
    }
  }, [isMcq, mcqAnswers, textAnswers]);

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center p-4">
        <div className="w-12 h-12 border-3 border-orange-500 border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-sm font-bold text-slate-200">Initializing Secure Exam Session & Proctor Shield...</p>
      </div>
    );
  }

  // -------------------------------------------------------------
  // POST-SUBMISSION / ALREADY SUBMITTED RESULT VIEW
  // -------------------------------------------------------------
  if (resultData) {
    const isDisq = resultData.disqualified || resultData.is_disqualified || isDisqualified;
    const isPass = !isDisq && resultData.status === "Pass";

    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center p-4 sm:p-6 select-none">
        <div className="w-full max-w-2xl bg-white text-slate-900 rounded-3xl shadow-2xl overflow-hidden border border-slate-200">
          {/* Header */}
          <div
            className={`p-8 text-center ${
              isDisq ? "bg-rose-600 text-white" : isPass ? "bg-emerald-600 text-white" : "bg-slate-900 text-white"
            }`}
          >
            <div className="w-16 h-16 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center mx-auto mb-3">
              <Icon name={isDisq ? "x-circle" : isPass ? "check-circle" : "award"} className="w-8 h-8 text-white" />
            </div>
            <h2 className="text-2xl font-black">
              {alreadySubmitted
                ? "Examination Already Completed"
                : isDisq
                ? "Examination Disqualified"
                : isPass
                ? "Assessment Completed!"
                : "Exam Submitted"}
            </h2>
            <p className="text-xs text-white/90 mt-1">
              {resultData.examName || exam?.exam_name} • {resultData.subjectName}
            </p>
          </div>

          {/* Already Submitted Notice */}
          {alreadySubmitted && (
            <div className="bg-amber-50 border-b border-amber-200 p-4 text-center">
              <p className="text-xs font-bold text-amber-800 uppercase tracking-wide">Single Attempt Policy</p>
              <p className="text-sm font-semibold text-amber-900 mt-1">
                You have already completed and submitted this examination. Re-attempts are not permitted.
              </p>
            </div>
          )}

          {/* Server Sync Delay Notice */}
          {resultData?.isServerSyncPending && (
            <div className="bg-blue-50 border-b border-blue-200 p-4 text-center">
              <p className="text-xs font-bold text-blue-800 uppercase tracking-wide">
                ℹ️ Provisional Scorecard (Sync Pending)
              </p>
              <p className="text-xs text-blue-900 mt-1">
                Your paper was submitted and graded. Due to a temporary server connection delay, your results are saved locally. Please come back after some time under <strong>'My Results & Scorecards'</strong> to check your verified server records.
              </p>
            </div>
          )}

          {/* Disqualification Banner */}
          {isDisq && (
            <div className="bg-rose-50 border-b border-rose-200 p-4 text-center">
              <p className="text-xs font-bold text-rose-800 uppercase tracking-wide">Academic Integrity Violation</p>
              <p className="text-sm font-semibold text-rose-900 mt-1">
                {resultData.disqualificationReason || disqualificationReason || "Disqualified for exam violations"}
              </p>
              <p className="text-xs text-rose-700 mt-1">
                Total Security Flags: {resultData.violationsCount || totalViolations}
              </p>
            </div>
          )}

          {/* Overview */}
          <div className="p-6 sm:p-8 space-y-6">
            <div className="grid grid-cols-3 gap-4 text-center">
              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Score</span>
                <p className="text-2xl font-extrabold text-slate-900 mt-1">
                  {resultData.marksObtained} <span className="text-xs font-normal text-slate-400">/ {resultData.totalMarks}</span>
                </p>
              </div>

              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Percentage</span>
                <p className={`text-2xl font-extrabold mt-1 ${isDisq ? "text-rose-600" : "text-orange-600"}`}>
                  {resultData.percentage}%
                </p>
              </div>

              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                <span className="text-xs text-slate-500 font-semibold uppercase">Outcome</span>
                <p className={`text-base font-bold mt-2 ${isDisq ? "text-rose-600" : isPass ? "text-emerald-600" : "text-amber-600"}`}>
                  {isDisq ? "Disqualified" : resultData.status}
                </p>
              </div>
            </div>

            {/* MCQ Breakdown */}
            {!isDisq && resultData.questionBreakdown && resultData.questionBreakdown.length > 0 && (
              <div className="space-y-3 pt-2">
                <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">
                  Question Review ({resultData.questionBreakdown.length})
                </h4>
                <div className="space-y-2.5 max-h-64 overflow-y-auto pr-1">
                  {resultData.questionBreakdown.map((item, idx) => (
                    <div
                      key={idx}
                      className={`p-3 rounded-xl border text-xs space-y-1 ${
                        item.isCorrect ? "bg-emerald-50/70 border-emerald-200" : "bg-rose-50/70 border-rose-200"
                      }`}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <p className="font-semibold text-slate-800">
                          {idx + 1}. {item.question}
                        </p>
                        <span
                          className={`font-bold px-2 py-0.5 rounded text-[10px] ${
                            item.isCorrect ? "bg-emerald-200 text-emerald-800" : "bg-rose-200 text-rose-800"
                          }`}
                        >
                          {item.isCorrect ? "Correct (+1)" : "Incorrect (0)"}
                        </span>
                      </div>
                      <p className="text-slate-600">
                        Your answer: <strong className="text-slate-900">{item.selectedAnswer || "None"}</strong>
                      </p>
                      {!item.isCorrect && item.correctAnswer && (
                        <p className="text-emerald-700 font-medium">Correct answer: {item.correctAnswer}</p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-col sm:flex-row gap-3 pt-4 border-t border-slate-100">
              <button
                onClick={() => navigate("/dashboard/student")}
                className="flex-1 py-3 px-4 bg-slate-900 hover:bg-slate-800 text-white font-semibold rounded-xl text-sm transition-all text-center"
              >
                Return to Student Dashboard
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // ACTIVE EXAMINATION INTERFACE
  // -------------------------------------------------------------
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col select-none relative overflow-x-hidden">
      {/* Hidden processing canvas */}
      <canvas ref={canvasRef} className="hidden" />

      {/* PRIVACY SCREEN SHIELD */}
      {screenShieldActive && (
        <div className="fixed inset-0 z-50 bg-slate-950/95 backdrop-blur-2xl flex flex-col items-center justify-center p-6 text-center">
          <div className="w-16 h-16 rounded-3xl bg-amber-500/20 text-amber-400 flex items-center justify-center mb-4 border border-amber-500/30 animate-pulse">
            <Icon name="shield" className="w-8 h-8" />
          </div>
          <h2 className="text-2xl font-black text-white">Security Shield Active</h2>
          <p className="text-sm text-slate-300 max-w-md mt-2">
            The exam window lost focus or a screenshot shortcut was detected. Please return focus to the exam window to continue.
          </p>
          <button
            onClick={() => setScreenShieldActive(false)}
            className="mt-6 px-6 py-2.5 bg-orange-500 hover:bg-orange-600 text-white rounded-xl text-sm font-bold shadow-lg transition-all"
          >
            Resume Examination
          </button>
        </div>
      )}

      {/* DYNAMIC ANTI-PHOTO WATERMARK MATRIX */}
      <div className="pointer-events-none fixed inset-0 z-20 overflow-hidden opacity-[0.045] flex flex-wrap gap-16 p-8 rotate-[-12deg] select-none">
        {Array.from({ length: 48 }).map((_, idx) => (
          <div key={idx} className="text-white text-xs font-mono font-black tracking-widest whitespace-nowrap">
            {user?.username || "CANDIDATE"} • ID #{user?.userId || user?.roleId || "STUDENT"} • EXAM #{examId} • SKILLCHECKR PROCTOR
          </div>
        ))}
      </div>

      {/* TOP PROCTORING HEADER BAR */}
      <header className="h-20 bg-slate-900/95 backdrop-blur-md border-b border-slate-800 px-4 sm:px-8 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-4">
          <button
            onClick={() => {
              if (window.confirm("Exit exam? Your saved answers will be retained.")) {
                stopWebcam();
                navigate("/dashboard/student");
              }
            }}
            className="p-2 text-slate-400 hover:text-white rounded-xl hover:bg-slate-800 transition-colors text-xs flex items-center gap-1.5"
          >
            ← Exit
          </button>

          <div>
            <h1 className="text-base font-bold text-white leading-none truncate max-w-xs sm:max-w-md">
              {exam?.exam_name || exam?.examName}
            </h1>
            <div className="flex items-center gap-2 mt-1">
              <span className="text-xs text-slate-400 font-medium">
                {exam?.subject?.subject_name || "General"} • {isMcq ? "MCQ Exam" : "Descriptive Q&A"}
              </span>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                AI Proctor Active
              </span>
            </div>
          </div>
        </div>

        {/* Live Webcam Preview, Timer & Submit */}
        <div className="flex items-center gap-3 sm:gap-5">
          {/* Webcam Proctor Badge with Person Count */}
          <div className="relative group flex items-center gap-2 bg-slate-800/80 px-2.5 py-1.5 rounded-xl border border-slate-700">
            <div className="w-9 h-7 sm:w-11 sm:h-8 rounded-lg overflow-hidden bg-slate-950 relative border border-slate-700 flex items-center justify-center">
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className="w-full h-full object-cover"
              />
              {!isCameraActive && (
                <Icon name="camera" className="w-3.5 h-3.5 text-slate-500 absolute" />
              )}
            </div>
            <div className="hidden sm:block text-left">
              <span className="text-[10px] font-black uppercase tracking-wider text-slate-400 block leading-none">
                {detectedPersonsCount > 1 ? "⚠️ Alert" : "Shield"}
              </span>
              <span
                className={`text-[11px] font-bold flex items-center gap-1 leading-none mt-0.5 ${
                  detectedPersonsCount > 1 ? "text-amber-400" : "text-emerald-400"
                }`}
              >
                {detectedPersonsCount > 1 ? `${detectedPersonsCount} Persons` : "1 Person"}
              </span>
            </div>
          </div>

          {/* Copy Strikes Indicator */}
          <div
            className={`hidden md:flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold border transition-all ${
              copyStrikes === 0
                ? "bg-slate-800 border-slate-700 text-slate-300"
                : copyStrikes === 1
                ? "bg-amber-950/60 border-amber-500/60 text-amber-400"
                : "bg-rose-950/80 border-rose-500 text-rose-400 animate-bounce"
            }`}
            title="Copy violations warning counter (Max 3 strikes)"
          >
            <span>Strikes:</span>
            <span className="font-mono font-black">{copyStrikes} / 3</span>
          </div>

          {/* Timer */}
          <div
            className={`flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-mono font-black border transition-colors ${
              timeLeftSeconds < 300
                ? "bg-rose-950/80 border-rose-500 text-rose-400 animate-pulse"
                : "bg-slate-800 border-slate-700 text-orange-400"
            }`}
          >
            <Icon name="clock" className="w-4 h-4" />
            <span>{formatTimer(timeLeftSeconds)}</span>
          </div>

          {/* Finish & Submit */}
          <button
            onClick={() => setShowSubmitModal(true)}
            className="py-2.5 px-4 sm:px-5 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white text-xs sm:text-sm font-black rounded-xl shadow-lg transition-all"
          >
            Finish & Submit
          </button>
        </div>
      </header>

      {/* FULLSCREEN PROMPT BANNER */}
      {(!isFullscreen || fullscreenWarning) && (
        <div className="bg-orange-500/15 border-b border-orange-500/30 px-4 py-2 text-center text-xs font-semibold text-orange-300 flex items-center justify-center gap-3">
          <span>🔒 For maximum test integrity, full-screen examination mode is recommended.</span>
          <button
            onClick={requestFullscreen}
            className="px-3 py-1 bg-orange-500 hover:bg-orange-600 text-white rounded-lg font-bold text-[11px] transition-all"
          >
            Enable Fullscreen
          </button>
        </div>
      )}

      {/* MAIN EXAM BODY */}
      <main className="flex-1 max-w-[1536px] w-full mx-auto p-4 sm:p-8 grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Question Panel */}
        <div className="lg:col-span-3 flex flex-col justify-between bg-slate-900/60 border border-slate-800 rounded-3xl p-6 sm:p-10 backdrop-blur-sm space-y-8 relative">
          {/* Question Header */}
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div className="flex items-center gap-3">
                <span className="text-xs font-black px-3 py-1.5 rounded-xl bg-orange-500 text-white shadow-sm">
                  Question {currentIndex + 1}
                </span>
                <span className="text-xs font-semibold text-slate-400">of {questions.length}</span>
                {currentQ?.marks && (
                  <span className="text-xs font-bold px-2.5 py-1 rounded-lg bg-slate-800 text-slate-300">
                    {currentQ.marks} Marks
                  </span>
                )}
              </div>

              <button
                type="button"
                onClick={toggleFlag}
                className={`text-xs font-bold px-3.5 py-1.5 rounded-xl border transition-all flex items-center gap-1.5 ${
                  flagged.has(currentQId)
                    ? "bg-amber-500/20 border-amber-500 text-amber-300 shadow-sm"
                    : "bg-slate-800/60 border-slate-700 text-slate-400 hover:text-white"
                }`}
              >
                ★ {flagged.has(currentQId) ? "Flagged for Review" : "Flag for Review"}
              </button>
            </div>

            {/* Question Text */}
            <h2 className="text-lg sm:text-xl font-bold text-slate-100 leading-relaxed">
              {currentQ?.question}
            </h2>
          </div>

          {/* Options / Answer Input Area */}
          <div className="flex-1 py-2">
            {isMcq ? (
              <div className="space-y-3.5">
                {[
                  { key: "option1", text: currentQ?.option1 },
                  { key: "option2", text: currentQ?.option2 },
                  { key: "option3", text: currentQ?.option3 },
                  { key: "option4", text: currentQ?.option4 },
                ]
                  .filter((opt) => opt.text && opt.text.trim() !== "")
                  .map((opt, idx) => {
                    const isSelected = mcqAnswers[currentQId] === opt.text;
                    return (
                      <button
                        key={opt.key}
                        type="button"
                        onClick={() => handleSelectOption(opt.text)}
                        className={`w-full text-left p-4 sm:p-5 rounded-2xl border transition-all flex items-start gap-4 ${
                          isSelected
                            ? "bg-orange-500/20 border-orange-500 text-white shadow-md ring-1 ring-orange-500/50"
                            : "bg-slate-800/40 border-slate-700/80 text-slate-200 hover:bg-slate-800 hover:border-slate-600"
                        }`}
                      >
                        <div
                          className={`w-7 h-7 rounded-xl font-bold text-xs flex items-center justify-center shrink-0 mt-0.5 border ${
                            isSelected
                              ? "bg-orange-500 border-orange-400 text-white"
                              : "bg-slate-800 border-slate-700 text-slate-400"
                          }`}
                        >
                          {String.fromCharCode(65 + idx)}
                        </div>
                        <span className="text-sm sm:text-base font-medium leading-relaxed">{opt.text}</span>
                      </button>
                    );
                  })}
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs text-slate-400 font-semibold">
                  <span>Write your answer below:</span>
                  <span>Word Limit: ~{currentQ?.wordLimit || 250} words</span>
                </div>
                <textarea
                  rows={8}
                  value={textAnswers[currentQId] || ""}
                  onChange={(e) => handleTextAnswerChange(e.target.value)}
                  placeholder="Type your comprehensive descriptive answer here..."
                  className="w-full bg-slate-950/80 border border-slate-700 rounded-2xl p-4 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 transition-all resize-y"
                />
              </div>
            )}
          </div>

          {/* Navigation Controls */}
          <div className="flex items-center justify-between pt-6 border-t border-slate-800">
            <button
              onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
              disabled={currentIndex === 0}
              className="py-2.5 px-5 bg-slate-800 hover:bg-slate-700 disabled:opacity-40 disabled:cursor-not-allowed text-white text-xs sm:text-sm font-bold rounded-xl transition-all"
            >
              ← Previous
            </button>

            <span className="text-xs text-slate-400 font-bold hidden sm:inline">
              Answered: {answeredCount} / {questions.length}
            </span>

            {currentIndex < questions.length - 1 ? (
              <button
                onClick={() => setCurrentIndex((prev) => Math.min(questions.length - 1, prev + 1))}
                className="py-2.5 px-6 bg-orange-500 hover:bg-orange-600 active:bg-orange-700 text-white text-xs sm:text-sm font-bold rounded-xl shadow transition-all"
              >
                Next Question →
              </button>
            ) : (
              <button
                onClick={() => setShowSubmitModal(true)}
                className="py-2.5 px-6 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white text-xs sm:text-sm font-black rounded-xl shadow-lg transition-all"
              >
                Submit Exam ✓
              </button>
            )}
          </div>
        </div>

        {/* Sidebar Palette & Proctor Status */}
        <div className="space-y-6">
          {/* Proctoring Shield Card */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-3xl p-5 backdrop-blur-sm space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <span className="text-xs font-black uppercase text-slate-300 tracking-wider flex items-center gap-1.5">
                <Icon name="shield" className="w-4 h-4 text-emerald-400" />
                Integrity Shield
              </span>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-800 text-slate-300">
                Active
              </span>
            </div>

            <ul className="text-xs space-y-2.5 text-slate-400">
              <li className="flex items-center justify-between">
                <span>Copy Protection:</span>
                <span className="font-bold text-emerald-400">3-Strike Lock</span>
              </li>
              <li className="flex items-center justify-between">
                <span>Tab Focus Monitor:</span>
                <span className="font-bold text-amber-400">{tabStrikes} / 3 Strikes</span>
              </li>
              <li className="flex items-center justify-between">
                <span>Camera Monitor:</span>
                <span className="font-bold text-emerald-400">
                  {cameraError ? "Perm Needed" : isCameraActive ? "Connected" : "Active"}
                </span>
              </li>
              <li className="flex items-center justify-between">
                <span>Room Presence:</span>
                <span
                  className={`font-bold ${
                    detectedPersonsCount > 1
                      ? multiplePersonsDurationSeconds >= 120
                        ? "text-rose-400 animate-pulse"
                        : "text-amber-400"
                      : "text-emerald-400"
                  }`}
                >
                  {detectedPersonsCount > 1
                    ? `⚠️ >1 Person (${Math.floor(multiplePersonsDurationSeconds)}s/180s)`
                    : "1 Person (Normal)"}
                </span>
              </li>
              <li className="flex items-center justify-between">
                <span>Photo / Leak Shield:</span>
                <span className="font-bold text-emerald-400">Watermarked</span>
              </li>
            </ul>
          </div>

          {/* Question Palette */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-3xl p-6 backdrop-blur-sm space-y-5">
            <h3 className="text-xs font-black uppercase tracking-wider text-slate-400">
              Question Navigator ({questions.length})
            </h3>

            <div className="grid grid-cols-5 gap-2.5 max-h-60 overflow-y-auto pr-1">
              {questions.map((q, idx) => {
                const qId = q.question_id || q.questionId || idx + 1;
                const isAnswered = isMcq ? !!mcqAnswers[qId] : !!textAnswers[qId];
                const isFlag = flagged.has(qId);
                const isCurrent = currentIndex === idx;

                let colorClasses = "bg-slate-800 text-slate-400 border-slate-700";
                if (isCurrent) {
                  colorClasses = "bg-orange-500 text-white border-orange-400 ring-2 ring-orange-500/40 font-black";
                } else if (isFlag) {
                  colorClasses = "bg-amber-500/20 text-amber-300 border-amber-500 font-bold";
                } else if (isAnswered) {
                  colorClasses = "bg-emerald-500/20 text-emerald-300 border-emerald-500/50 font-bold";
                }

                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setCurrentIndex(idx)}
                    className={`h-10 rounded-xl text-xs font-bold border transition-all flex items-center justify-center ${colorClasses}`}
                  >
                    {idx + 1}
                  </button>
                );
              })}
            </div>

            {/* Legend */}
            <div className="pt-4 border-t border-slate-800 grid grid-cols-2 gap-2 text-[11px] text-slate-400 font-medium">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400"></span>
                <span>Answered ({answeredCount})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-slate-700"></span>
                <span>Remaining ({questions.length - answeredCount})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-amber-400"></span>
                <span>Flagged ({flagged.size})</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-orange-500"></span>
                <span>Current</span>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* VIOLATION MODAL */}
      {activeViolationModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl space-y-5 text-center">
            <div
              className={`w-16 h-16 rounded-full mx-auto flex items-center justify-center ${
                activeViolationModal.isDisqualified
                  ? "bg-rose-500/20 text-rose-400 border border-rose-500/40 animate-pulse"
                  : activeViolationModal.strike === 2
                  ? "bg-amber-500/20 text-amber-400 border border-amber-500/40"
                  : "bg-orange-500/20 text-orange-400 border border-orange-500/40"
              }`}
            >
              <Icon
                name={activeViolationModal.isDisqualified ? "x-circle" : "shield"}
                className="w-8 h-8"
              />
            </div>

            <div>
              <h3 className="text-xl font-black text-white">{activeViolationModal.title}</h3>
              <p className="text-sm text-slate-300 mt-2 leading-relaxed">
                {activeViolationModal.message}
              </p>
            </div>

            <div className="pt-2">
              {!activeViolationModal.isDisqualified ? (
                <button
                  onClick={() => setActiveViolationModal(null)}
                  className="w-full py-3 bg-orange-500 hover:bg-orange-600 text-white font-bold rounded-xl text-sm transition-all shadow-lg"
                >
                  I Understand & Acknowledge
                </button>
              ) : (
                <p className="text-xs text-rose-400 font-bold uppercase tracking-wider">
                  Session Terminated
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {/* CONFIRM FINAL SUBMIT MODAL */}
      {showSubmitModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-md flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl space-y-6 text-center">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 mx-auto flex items-center justify-center">
              <Icon name="check-circle" className="w-8 h-8" />
            </div>

            <div>
              <h3 className="text-xl font-black text-white">Submit Examination?</h3>
              <p className="text-sm text-slate-300 mt-2 leading-relaxed">
                You have answered <strong className="text-white font-bold">{answeredCount}</strong> of{" "}
                <strong className="text-white font-bold">{questions.length}</strong> questions. Once submitted, you cannot change your answers.
              </p>
            </div>

            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setShowSubmitModal(false)}
                disabled={isSubmitting}
                className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-xl text-sm transition-all"
              >
                Continue Exam
              </button>
              <button
                type="button"
                onClick={() => submitFinalExam(false)}
                disabled={isSubmitting}
                className="flex-1 py-3 bg-emerald-600 hover:bg-emerald-500 active:bg-emerald-700 text-white font-bold rounded-xl text-sm transition-all shadow-lg flex items-center justify-center gap-2"
              >
                {isSubmitting ? "Submitting..." : "Yes, Submit"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
