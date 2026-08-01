/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // Palette trích từ logo & loading (Phân tích ảnh)
        brand: {
          50: '#E8F6FE',
          100: '#C5EBFD',
          200: '#8FD8FB',
          300: '#57BFF8',
          400: '#2BA2F4',
          500: '#0388ED', // primary - xanh dương logo
          600: '#0270C9',
          700: '#0759A0',
          800: '#0A4379',
          900: '#0736AC', // navy - từ loading
          950: '#042057',
        },
        accent: {
          50: '#FFF4E6',
          100: '#FFE6CC',
          200: '#FFCF99',
          300: '#FFB266',
          400: '#FF9A40',
          500: '#F69022', // cam - loading accent
          600: '#E07410',
          700: '#B8570C',
          800: '#8F430E',
          900: '#66310E',
        },
        gold: {
          50: '#FFFCE6',
          100: '#FFF7C2',
          200: '#FFEF85',
          300: '#FFE44D',
          400: '#FCD215',
          500: '#FCBD0E', // vàng - điểm nhấn logo
          600: '#D99A06',
          700: '#B57305',
          800: '#925C0A',
          900: '#7A4D0F',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      boxShadow: {
        card: '0 4px 24px -8px rgba(3, 136, 237, 0.18)',
        'card-hover': '0 12px 32px -8px rgba(3, 136, 237, 0.28)',
      },
      backgroundImage: {
        'brand-gradient': 'linear-gradient(135deg, #0388ED 0%, #0736AC 55%, #F69022 100%)',
        'brand-gradient-soft': 'linear-gradient(135deg, #E8F6FE 0%, #FFF4E6 100%)',
      },
      keyframes: {
        'spin-slow': { from: { transform: 'rotate(0deg)' }, to: { transform: 'rotate(360deg)' } },
        'fade-in': { from: { opacity: '0' }, to: { opacity: '1' } },
        'pulse-ring': {
          '0%': { transform: 'scale(0.8)', opacity: '0.8' },
          '100%': { transform: 'scale(2.2)', opacity: '0' },
        },
      },
      animation: {
        'spin-slow': 'spin-slow 3s linear infinite',
        'fade-in': 'fade-in 0.4s ease-out',
        'pulse-ring': 'pulse-ring 2s ease-out infinite',
      },
    },
  },
  plugins: [],
}
