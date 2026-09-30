import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The test environment is jsdom because the hooks are exercised through React Testing Library.
// `npm run build` and `npm run dev` are unaffected by this block.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    include: ["src/**/*.test.{js,jsx}"],
    setupFiles: ["./src/test/setup.js"],
  },
});
