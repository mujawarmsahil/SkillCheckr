import { useState, useEffect, useRef, useMemo } from "react";
import { getRemainingTime, formatRemainingTime } from "../utils/dateUtils";

export function useExamTimer({ expiresAt, onExpire, active = true, showWarning }) {
  const [timeLeftSeconds, setTimeLeftSeconds] = useState(0);
  const [isAttemptExpired, setIsAttemptExpired] = useState(false);
  const expiryNoticeShownRef = useRef(false);
  const onExpireRef = useRef(onExpire);

  useEffect(() => {
    onExpireRef.current = onExpire;
  }, [onExpire]);

  useEffect(() => {
    if (!active || !expiresAt) return;

    const expiresAtMs = new Date(expiresAt).getTime();
    if (Number.isNaN(expiresAtMs)) {
      if (showWarning) {
        showWarning("Could not read the exam expiry time from the server.");
      }
      return;
    }

    expiryNoticeShownRef.current = false;
    setIsAttemptExpired(false);

    const updateTimer = () => {
      const remaining = getRemainingTime(expiresAt);
      setTimeLeftSeconds(remaining);

      if (remaining === 0 && !expiryNoticeShownRef.current) {
        expiryNoticeShownRef.current = true;
        setIsAttemptExpired(true);
        if (showWarning) {
          showWarning("Time is up. Submitting your exam.");
        }
        if (onExpireRef.current) {
          onExpireRef.current();
        }
      }
    };

    updateTimer();
    const interval = setInterval(updateTimer, 1000);
    return () => clearInterval(interval);
  }, [expiresAt, active, showWarning]);

  const formattedTime = useMemo(
    () => formatRemainingTime(timeLeftSeconds),
    [timeLeftSeconds]
  );

  return {
    timeLeftSeconds,
    isAttemptExpired,
    formattedTime,
  };
}
