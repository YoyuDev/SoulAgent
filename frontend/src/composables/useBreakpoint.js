import { ref } from 'vue'

// 断点：< 768px 手机 / 768–1024px 平板 / > 1024px PC
const mobileQuery = window.matchMedia('(max-width: 767px)')
const tabletQuery = window.matchMedia('(min-width: 768px) and (max-width: 1024px)')

const isMobile = ref(mobileQuery.matches)
const isTablet = ref(tabletQuery.matches)

function sync() {
  isMobile.value = mobileQuery.matches
  isTablet.value = tabletQuery.matches
}

mobileQuery.addEventListener('change', sync)
tabletQuery.addEventListener('change', sync)

export function useBreakpoint() {
  return { isMobile, isTablet }
}