/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        primary: '#35627A',
        'primary-dark': '#2B4F63',
        secondary: '#B46258',
        'secondary-dark': '#9B534A',
        accent: '#E5AEA9',
        muted: '#A6A9D0',
        background: '#F5F5F5',
        surface: '#FFFFFF',
        'text-muted': '#8E9A98',
        'text-primary': '#1E2C33',
      },
      fontFamily: {
        heading: ['Montserrat', 'sans-serif'],
        body: ['Roboto Flex', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
