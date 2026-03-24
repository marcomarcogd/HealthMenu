export async function copyText(value) {
  const text = value?.trim()
  if (!text) {
    throw new Error('链接暂不可用')
  }

  if (navigator.clipboard?.writeText && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }

  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)

  try {
    const copied = document.execCommand('copy')
    if (!copied) {
      throw new Error('COPY_COMMAND_FAILED')
    }
  } finally {
    document.body.removeChild(textarea)
  }
}
