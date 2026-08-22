import React, { createContext, useContext, useState, useCallback, useRef } from "react";
import { Icon } from "../components/common/Icons";

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const activeMessagesRef = useRef(new Set());

  const removeToast = useCallback((id) => {
    setToasts((prev) => {
      const toastToRemove = prev.find((t) => t.id === id);
      if (toastToRemove) {
        activeMessagesRef.current.delete(toastToRemove.message);
      }
      return prev.filter((t) => t.id !== id);
    });
  }, []);

  const addToast = useCallback((message, type = "info", duration = 4000) => {
    if (!message) return;

    // Strict deduplication: do not display the exact same message if already showing
    if (activeMessagesRef.current.has(message)) {
      return;
    }

    activeMessagesRef.current.add(message);
    const id = Date.now() + "-" + Math.random().toString(36).substring(2, 9);

    setToasts((prev) => [...prev, { id, message, type }]);

    if (duration > 0) {
      setTimeout(() => {
        activeMessagesRef.current.delete(message);
        setToasts((prev) => prev.filter((t) => t.id !== id));
      }, duration);
    }
  }, []);

  const showSuccess = useCallback((msg, duration) => addToast(msg, "success", duration), [addToast]);
  const showError = useCallback((msg, duration) => addToast(msg, "error", duration), [addToast]);
  const showWarning = useCallback((msg, duration) => addToast(msg, "warning", duration), [addToast]);
  const showInfo = useCallback((msg, duration) => addToast(msg, "info", duration), [addToast]);

  return (
    <ToastContext.Provider value={{ addToast, removeToast, showSuccess, showError, showWarning, showInfo }}>
      {children}
      <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
        {toasts.map((toast) => {
          let bgColor = "bg-slate-900 text-white";
          let borderAccent = "border-slate-700";
          let iconName = "alert";

          if (toast.type === "success") {
            bgColor = "bg-emerald-800 text-emerald-50";
            borderAccent = "border-emerald-600";
            iconName = "check-circle";
          } else if (toast.type === "error") {
            bgColor = "bg-rose-900 text-rose-50";
            borderAccent = "border-rose-700";
            iconName = "alert";
          } else if (toast.type === "warning") {
            bgColor = "bg-amber-900 text-amber-50";
            borderAccent = "border-amber-700";
            iconName = "alert";
          }

          return (
            <div
              key={toast.id}
              className={`pointer-events-auto flex items-start gap-3 p-4 rounded-xl shadow-xl border ${borderAccent} ${bgColor} transition-all duration-300 transform translate-y-0`}
            >
              <Icon name={iconName} className="w-5 h-5 flex-shrink-0 mt-0.5" />
              <div className="flex-1 text-sm font-medium leading-5">{toast.message}</div>
              <button
                onClick={() => removeToast(toast.id)}
                className="opacity-70 hover:opacity-100 transition-opacity p-0.5"
              >
                <Icon name="x" className="w-4 h-4" />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return context;
}
