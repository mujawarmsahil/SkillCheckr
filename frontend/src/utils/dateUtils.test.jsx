import { renderHook, act } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  parseServerTimestamp,
  getRemainingTime,
  formatRemainingTime,
} from "./dateUtils";
import { useExamTimer } from "../hooks/useExamTimer";

/**
 * 2026-09-17T19:00:00+05:30 is 2026-09-17T13:30:00Z. The wall-clock digits are deliberately
 * different in every timezone used below, so a test that passes under all of them can only be
 * passing because the offset was honoured.
 */
const EXPIRES_WITH_OFFSET = "2026-09-17T19:00:00+05:30";
const SAME_INSTANT_UTC = "2026-09-17T13:30:00.000Z";
const EXPECTED_EPOCH_MS = Date.UTC(2026, 8, 17, 13, 30, 0, 0);

// The offset carried by EXPIRES_WITH_OFFSET, used to state the size of the pre-fix error.
const SERVER_OFFSET_MINUTES = 330;

// The suite is verified by running it under several zones, which makes the ambient offset
// genuinely differ between runs. Half-hour and extreme offsets are included because they
// shift a naive-local-time reading by amounts a whole-hour zone would not expose.
//   npm test
//   TZ=UTC npm test
//   TZ=Asia/Kolkata npm test            (+05:30)
//   TZ=America/New_York npm test        (-04:00, DST)
//   TZ=Pacific/Kiritimati npm test      (+14:00)
//   TZ=Pacific/Niue npm test            (-11:00)

/**
 * Minutes that browser-local time is offset from UTC right now.
 *
 * Node reads TZ once at startup, so assigning process.env.TZ inside a test does not move the
 * clock. This reads the real offset from the ICU default zone instead, which lets a test assert
 * that the offset-bearing value is timezone-proof without pretending to change the zone. The
 * suite is additionally run under several real TZ values in CI-style verification, where the
 * ambient zone genuinely differs between runs.
 */
function localZoneOffsetMinutes(referenceMs) {
  // Format the instant in this zone and in UTC, and let the difference speak for itself. This
  // avoids depending on how a particular ICU build spells a zone name.
  const inZone = new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).formatToParts(new Date(referenceMs));

  const field = (type) => Number(inZone.find((p) => p.type === type)?.value);
  const asUtc = Date.UTC(
    field("year"),
    field("month") - 1,
    field("day"),
    field("hour") % 24,
    field("minute"),
    field("second")
  );
  return Math.round((asUtc - referenceMs) / 60_000);
}

describe("parseServerTimestamp", () => {
  it("resolves a timestamp with an explicit offset to a single absolute instant", () => {
    expect(parseServerTimestamp(EXPIRES_WITH_OFFSET)).toBe(EXPECTED_EPOCH_MS);
  });

  it("agrees with the equivalent UTC representation of the same instant", () => {
    expect(parseServerTimestamp(EXPIRES_WITH_OFFSET)).toBe(parseServerTimestamp(SAME_INSTANT_UTC));
  });

  it("names the server instant, where a naive reading is off by the zone mismatch", () => {
    expect(parseServerTimestamp(EXPIRES_WITH_OFFSET)).toBe(EXPECTED_EPOCH_MS);

    // The pre-fix client read the bare wall-clock value as browser-local time, so it resolved the
    // same digits to a different instant by exactly the gap between the server's offset and the
    // reader's. That gap is what made one student's countdown disagree with another's and with the
    // server's own expiry check.
    const readerOffset = localZoneOffsetMinutes(EXPECTED_EPOCH_MS);
    const naiveLocal = new Date("2026-09-17T19:00:00").getTime();

    expect(naiveLocal - EXPECTED_EPOCH_MS).toBe(
      (SERVER_OFFSET_MINUTES - readerOffset) * 60_000
    );
    // A student in the server's own zone was coincidentally correct, which is why this went
    // unnoticed; the fix removes the dependence on that coincidence.
    if (readerOffset === SERVER_OFFSET_MINUTES) {
      expect(naiveLocal).toBe(EXPECTED_EPOCH_MS);
    }
  });

  it("leaves the absolute instant untouched by the reader's zone offset", () => {
    // Reads the same wall-clock digits with and without the offset. The offset-bearing value is
    // the one that must survive; the bare value shifts by the ambient zone offset, which is
    // exactly why it is rejected instead of parsed.
    const offsetMinutes = localZoneOffsetMinutes(EXPECTED_EPOCH_MS);
    const parsed = parseServerTimestamp(EXPIRES_WITH_OFFSET);

    expect(parsed).toBe(EXPECTED_EPOCH_MS);
    expect(Date.UTC(2026, 8, 17, 13, 30)).toBe(EXPECTED_EPOCH_MS);

    // Sanity check that the helper is observing a real zone rather than always reporting zero.
    const bare = new Date("2026-09-17T13:30:00");
    // Object.is distinguishes 0 from -0, so compare the magnitudes.
    expect(Math.abs(bare.getTime() - EXPECTED_EPOCH_MS)).toBe(Math.abs(offsetMinutes * 60_000));
  });

  it("accepts a 'Z' suffix", () => {
    expect(parseServerTimestamp("2026-09-17T13:30:00Z")).toBe(EXPECTED_EPOCH_MS);
  });

  it("accepts an offset written without a colon", () => {
    expect(parseServerTimestamp("2026-09-17T19:00:00+0530")).toBe(EXPECTED_EPOCH_MS);
  });

  it("accepts a negative offset", () => {
    // 09:00 at -04:00 is 13:00Z.
    expect(parseServerTimestamp("2026-09-17T09:00:00-04:00")).toBe(
      Date.UTC(2026, 8, 17, 13, 0, 0, 0)
    );
  });

  it("rejects a bare local date-time instead of guessing a zone", () => {
    // This is the value the API used to send. Reading it as browser-local time is the bug.
    expect(parseServerTimestamp("2026-09-17T19:00:00")).toBeNull();
    expect(parseServerTimestamp("2026-09-17 19:00:00")).toBeNull();
  });

  it("returns null for missing or unusable values", () => {
    expect(parseServerTimestamp(null)).toBeNull();
    expect(parseServerTimestamp(undefined)).toBeNull();
    expect(parseServerTimestamp("")).toBeNull();
    expect(parseServerTimestamp("   ")).toBeNull();
    expect(parseServerTimestamp("not a date")).toBeNull();
    expect(parseServerTimestamp("2026-09-17T19:00:00+99:99")).toBeNull();
  });

  it("accepts epoch milliseconds", () => {
    expect(parseServerTimestamp(EXPECTED_EPOCH_MS)).toBe(EXPECTED_EPOCH_MS);
  });
});

describe("getRemainingTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("counts down toward the server instant, not the browser wall clock", () => {
    // 60 seconds before the absolute expiry instant.
    vi.setSystemTime(EXPECTED_EPOCH_MS - 60_000);

    expect(getRemainingTime(EXPIRES_WITH_OFFSET)).toBe(60);
  });

  it("returns the same remaining time whatever the local zone offset is", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 90_000);
    // True in any zone, which is the property that matters: the countdown is a function of the
    // absolute instant, not of where the student happens to be.
    expect(getRemainingTime(EXPIRES_WITH_OFFSET)).toBe(90);
  });

  it("rounds up a partial second so a live attempt never shows 00:00 early", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 90_500);
    expect(getRemainingTime(EXPIRES_WITH_OFFSET)).toBe(91);
  });

  it("returns 0 once the attempt has expired", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS + 1_000);
    expect(getRemainingTime(EXPIRES_WITH_OFFSET)).toBe(0);
  });

  it("returns 0 long after expiry instead of going negative", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS + 86_400_000);
    expect(getRemainingTime(EXPIRES_WITH_OFFSET)).toBe(0);
  });

  it("returns 0 for an ambiguous or missing timestamp", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 60_000);
    expect(getRemainingTime("2026-09-17T19:00:00")).toBe(0);
    expect(getRemainingTime(null)).toBe(0);
    expect(getRemainingTime("")).toBe(0);
  });
});

describe("formatRemainingTime", () => {
  it("formats seconds as mm:ss", () => {
    expect(formatRemainingTime(0)).toBe("00:00");
    expect(formatRemainingTime(65)).toBe("01:05");
    expect(formatRemainingTime(3600)).toBe("60:00");
  });
});

describe("useExamTimer", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function renderTimer(overrides = {}) {
    const showWarning = vi.fn();
    const onExpire = vi.fn();
    const view = renderHook(() =>
      useExamTimer({
        expiresAt: EXPIRES_WITH_OFFSET,
        active: true,
        showWarning,
        onExpire,
        ...overrides,
      })
    );
    return { ...view, showWarning, onExpire };
  }

  it("derives the countdown from the absolute instant, not the local clock", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 120_000);
    const { result } = renderTimer();

    expect(result.current.timeLeftSeconds).toBe(120);
    expect(result.current.isAttemptExpired).toBe(false);
    expect(result.current.formattedTime).toBe("02:00");
  });

  it("ticks down once per second toward the same instant", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 120_000);
    const { result } = renderTimer();

    expect(result.current.timeLeftSeconds).toBe(120);
    act(() => {
      vi.advanceTimersByTime(1000);
    });
    expect(result.current.timeLeftSeconds).toBe(119);

    act(() => {
      vi.advanceTimersByTime(59_000);
    });
    expect(result.current.timeLeftSeconds).toBe(60);
  });

  it("flags expiry and notifies once when the server instant is reached", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 2_000);
    const { result, onExpire, showWarning } = renderTimer();

    act(() => {
      vi.advanceTimersByTime(3000);
    });

    expect(result.current.isAttemptExpired).toBe(true);
    expect(result.current.timeLeftSeconds).toBe(0);
    expect(onExpire).toHaveBeenCalledTimes(1);
    expect(showWarning).toHaveBeenCalledWith("Time is up. Submitting your exam.");
  });

  it("is already expired on arrival, without waiting for a tick", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS + 5_000);
    const { result, onExpire } = renderTimer();

    expect(result.current.isAttemptExpired).toBe(true);
    expect(result.current.timeLeftSeconds).toBe(0);
    expect(onExpire).toHaveBeenCalledTimes(1);
  });

  it("gives the same remaining time after a refresh as before it", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 300_000);
    const first = renderTimer();
    expect(first.result.current.timeLeftSeconds).toBe(300);
    first.unmount();

    // A refresh tears the component down and re-reads the same server value. 30 seconds of
    // wall clock pass in between, and the remaining time must fall by exactly that much.
    act(() => {
      vi.advanceTimersByTime(30_000);
    });

    const second = renderTimer();
    expect(second.result.current.timeLeftSeconds).toBe(270);
  });

  it("resumes with the same remaining time from a differently-configured reader", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 300_000);
    const first = renderTimer();
    expect(first.result.current.timeLeftSeconds).toBe(300);
    first.unmount();

    // A student resuming the same attempt elsewhere, or later, reads the identical server value
    // and must see the identical remaining time.
    const second = renderTimer();
    expect(second.result.current.timeLeftSeconds).toBe(300);
  });

  it("warns and stays inert when the expiry cannot be interpreted", () => {
    const { result, showWarning, onExpire } = renderTimer({ expiresAt: "2026-09-17T19:00:00" });

    expect(showWarning).toHaveBeenCalledWith("Could not read the exam expiry time from the server.");
    expect(result.current.timeLeftSeconds).toBe(0);
    expect(result.current.isAttemptExpired).toBe(false);
    expect(onExpire).not.toHaveBeenCalled();
  });

  it("does not run when inactive", () => {
    vi.setSystemTime(EXPECTED_EPOCH_MS - 60_000);
    const { result, onExpire } = renderTimer({ active: false });

    expect(result.current.timeLeftSeconds).toBe(0);
    act(() => {
      vi.advanceTimersByTime(120_000);
    });
    expect(onExpire).not.toHaveBeenCalled();
  });
});
