export function redirectToLogin() {
  if (typeof window === 'undefined') {
    return
  }

  const basePath = (import.meta.env.BASE_URL || '/').replace(/\/$/, '')
  let currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
  if (basePath && basePath !== '/' && currentPath.startsWith(basePath)) {
    currentPath = currentPath.slice(basePath.length) || '/'
  }
  if (currentPath.includes('/login')) {
    return
  }

  const loginPath = `${basePath}/login`.replace('//login', '/login')
  const redirect = encodeURIComponent(currentPath)
  window.location.assign(`${loginPath}?redirect=${redirect}`)
}
