import { ref, watch } from 'vue'

const theme = ref(localStorage.getItem('soulagent_theme') || 'dark')

watch(theme, (val) => {
  document.documentElement.setAttribute('data-theme', val)
  localStorage.setItem('soulagent_theme', val)
}, { immediate: true })

export function useTheme() {
  function toggle() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  return { theme, toggle }
}
