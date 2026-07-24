/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{svelte,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: "#003ec7",
          dark: "#001452",
          container: "#0052ff",
        },
        surface: {
          DEFAULT: "#f8f9ff",
          container: "#e5eeff",
          dim: "#cbdbf5",
          lowest: "#ffffff",
        },
        on: {
          surface: "#0b1c30",
          primary: "#ffffff",
        }
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
      }
    },
  },
  plugins: [],
}