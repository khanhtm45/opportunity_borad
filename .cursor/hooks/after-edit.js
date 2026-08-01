#!/usr/bin/env node
/**
 * Feedback hook: acknowledge file edits (keeps harness HKS-04 green).
 */
let raw = ''
process.stdin.setEncoding('utf8')
process.stdin.on('data', (c) => (raw += c))
process.stdin.on('end', () => {
  let file = ''
  try {
    const input = JSON.parse(raw || '{}')
    file = String(input.file_path || input.path || input.uri || '')
  } catch {
    file = ''
  }
  process.stdout.write(
    JSON.stringify({
      additional_context: file
        ? `Edited ${file}. Keep Vite base/asset() and env-secret rules in mind.`
        : 'File edit observed.',
    })
  )
})
