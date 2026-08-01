#!/usr/bin/env node
/**
 * Gate hook: ask before destructive git/fs shell commands.
 * Reads Cursor hook JSON from stdin; writes permission JSON to stdout.
 */
let raw = ''
process.stdin.setEncoding('utf8')
process.stdin.on('data', (c) => (raw += c))
process.stdin.on('end', () => {
  let command = ''
  try {
    const input = JSON.parse(raw || '{}')
    command = String(input.command || input.tool_input?.command || '')
  } catch {
    command = ''
  }

  const risky =
    /\bgit\s+push\s+--force\b/i.test(command) ||
    /\bgit\s+reset\s+--hard\b/i.test(command) ||
    /\brm\s+-rf\s+[\/\\]/i.test(command) ||
    /\bRemove-Item\b.*-Recurse\b.*-Force\b/i.test(command)

  if (risky) {
    process.stdout.write(
      JSON.stringify({
        permission: 'ask',
        user_message: 'Potentially destructive shell command — confirm before running.',
        agent_message: 'Gate hook flagged a destructive git/fs command.',
      })
    )
    return
  }

  process.stdout.write(JSON.stringify({ permission: 'allow' }))
})
