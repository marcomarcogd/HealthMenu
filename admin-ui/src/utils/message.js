import { ElMessage } from 'element-plus'

const lastShownAt = new Map()
const DEDUPE_WINDOW_MS = 1200

function shouldShow(key) {
  const now = Date.now()
  const lastTime = lastShownAt.get(key) || 0

  if (now - lastTime < DEDUPE_WINDOW_MS) {
    return false
  }

  lastShownAt.set(key, now)

  for (const [entryKey, entryTime] of lastShownAt.entries()) {
    if (now - entryTime > DEDUPE_WINDOW_MS * 3) {
      lastShownAt.delete(entryKey)
    }
  }

  return true
}

export function showErrorMessage(message, options = {}) {
  const text = message?.trim() || '请求失败'
  const key = options.dedupeKey || `error:${text}`

  if (!options.force && !shouldShow(key)) {
    return
  }

  ElMessage.error(text)
}
