import React, { useState, useEffect, useRef, useCallback } from "react";
import { Icon } from "./Icons";
import { sendChatMessage } from "../../api/chatApi";

const GENERAL_QUESTIONS = [
  "How do I register for an exam?",
  "How are exam results calculated?",
  "How does the exam timer work?",
  "How do I submit an exam?",
];

const GREETING =
  "Hi! I am the SkillCheckr AI assistant. Ask me how to register for, take, or review exams.";

const NOT_AVAILABLE =
  "I could not answer that right now. Please try again later.";

export default function Chatbot({ context = null, title = "AI Assistant" }) {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const suggestions = context?.question
    ? [
        `Explain this ${(context.questionType || "MCQ").toLowerCase()} question`,
        "Can you give me a hint?",
        "How many marks is this worth?",
      ]
    : GENERAL_QUESTIONS;

  useEffect(() => {
    if (isOpen && messages.length === 0) {
      setMessages([
        { id: "greeting", sender: "assistant", text: GREETING, answered: true },
      ]);
    }
  }, [isOpen, messages.length]);

  useEffect(() => {
    const element = messagesEndRef.current;
    if (element && typeof element.scrollIntoView === "function") {
      element.scrollIntoView({ behavior: "smooth", block: "end" });
    }
  }, [messages, isOpen]);

  const sendMessage = useCallback(
    async (rawText) => {
      const text = (rawText || "").trim();
      if (!text || isLoading) {
        return;
      }
      setIsLoading(true);
      setMessages((prev) => [
        ...prev,
        { id: `user-${Date.now()}`, sender: "user", text },
      ]);
      setInput("");
      try {
        const response = await sendChatMessage(text, {
          examId: context?.examId ?? undefined,
          questionId: context?.questionId ?? undefined,
        });
        const answer = response.answered && response.answer ? response.answer : NOT_AVAILABLE;
        setMessages((prev) => [
          ...prev,
          {
            id: `assistant-${Date.now()}`,
            sender: "assistant",
            text: answer,
            answered: response.answered,
          },
        ]);
      } catch (err) {
        const fallback = err?.message || "Sorry, I could not help right now. Please try again.";
        setMessages((prev) => [
          ...prev,
          { id: `assistant-${Date.now()}`, sender: "assistant", text: fallback, answered: false },
        ]);
      } finally {
        setIsLoading(false);
      }
    },
    [context, isLoading]
  );

  const closeAssistant = () => setIsOpen(false);

  if (!isOpen) {
    return (
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        aria-label="Open AI assistant"
        className="fixed bottom-5 right-5 z-[60] w-14 h-14 rounded-2xl bg-orange-500 hover:bg-orange-600 text-white shadow-xl transition-all flex items-center justify-center"
      >
        <Icon name="chat" className="w-7 h-7" />
      </button>
    );
  }

  return (
    <div className="fixed bottom-5 right-5 z-[60] w-[92vw] max-w-md h-[70vh] max-h-[70vh] bg-slate-900 border border-slate-700 rounded-3xl shadow-2xl flex flex-col overflow-hidden">
      <div className="p-4 bg-slate-900 border-b border-slate-700 flex items-center justify-between">
        <h3 className="text-sm font-bold text-white truncate">{context?.examName || title}</h3>
        <button
          type="button"
          onClick={closeAssistant}
          aria-label="Close AI assistant"
          className="p-1 text-slate-400 hover:text-white transition-colors"
        >
          <Icon name="x" className="w-5 h-5" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex ${message.sender === "user" ? "justify-end" : "justify-start"}`}
          >
            <div
              className={`max-w-[85%] px-4 py-3 rounded-2xl text-sm leading-relaxed ${
                message.sender === "user"
                  ? "bg-orange-500 text-white rounded-br-sm"
                  : "bg-slate-800 text-slate-100 rounded-br-sm"
              }`}
            >
              {message.text}
            </div>
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      {messages.length <= 1 && (
        <div className="px-4 pb-2 flex flex-wrap gap-2">
          {suggestions.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              onClick={() => sendMessage(suggestion)}
              className="text-xs px-3 py-1.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700 hover:border-orange-500 hover:text-white transition-all"
            >
              {suggestion}
            </button>
          ))}
        </div>
      )}

      <div className="p-3 border-t border-slate-700 bg-slate-900">
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                sendMessage(input);
              }
            }}
            placeholder="Ask a question..."
            className="flex-1 bg-slate-800 border border-slate-700 rounded-xl px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-orange-500"
          />
          <button
            type="button"
            onClick={() => sendMessage(input)}
            disabled={isLoading || !input.trim()}
            className="px-4 py-2.5 bg-orange-500 hover:bg-orange-600 disabled:opacity-40 text-white text-sm font-bold rounded-xl transition-all"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  );
}
