import { useState } from 'react'
import { uploadFile, uploadGuest, mediaSrc } from '../api/upload.js'

/**
 * Nút chọn file → S3 private+AES.
 * onUploaded({ url, viewUrl, … }) — lưu `url` (ob-s3://) vào form/DB.
 */
export default function FileUploadButton({
  purpose = 'image',
  accept = 'image/*,.pdf,.doc,.docx',
  guest = false,
  label = 'Upload file',
  onUploaded,
  className = 'btn-ghost text-xs',
}) {
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  const onChange = async (e) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setErr('')
    setBusy(true)
    try {
      const up = guest ? await uploadGuest(file, purpose) : await uploadFile(file, purpose)
      onUploaded?.(up)
    } catch (ex) {
      setErr(ex.response?.data?.error?.message || 'Upload thất bại')
    } finally {
      setBusy(false)
    }
  }

  return (
    <span className="inline-flex flex-col items-start gap-1">
      <label className={`${className} inline-flex cursor-pointer ${busy ? 'opacity-60' : ''}`}>
        <input type="file" accept={accept} className="hidden" disabled={busy} onChange={onChange} />
        {busy ? 'Đang upload…' : label}
      </label>
      {err && <span className="text-xs text-rose-600">{err}</span>}
    </span>
  )
}

export { mediaSrc }
