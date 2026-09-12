/**
 * Utility functions for examination scheduling, dates, and status evaluations.
 */

export const isExamDateTimePassed = (exam) => {
  if (!exam) return false;
  if (exam.status === "Completed") return true;
  const dateStr = (exam.date || exam.exam_date || "").split("T")[0];
  if (!dateStr) return false;
  const endTimeStr = exam.end_time || exam.endTime || "23:59:59";
  try {
    const [year, month, day] = dateStr.split("-").map(Number);
    const timeParts = endTimeStr.split(":").map(Number);
    const hours = timeParts[0] || 0;
    const minutes = timeParts[1] || 0;
    const seconds = timeParts[2] || 0;
    const examEndTime = new Date(year, month - 1, day, hours, minutes, seconds);
    return examEndTime < new Date();
  } catch {
    return false;
  }
};
