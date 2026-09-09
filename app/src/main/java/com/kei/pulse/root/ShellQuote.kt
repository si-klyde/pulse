package com.kei.pulse.root

/**
 * POSIX single-quote [value] so a shell treats it as one literal word. Every `'` becomes `'\''` (close quote,
 * escaped quote, reopen), so no content can terminate the quoting. Use for ANY non-numeric string that reaches
 * a PServer command: sysfs paths, governor names, values read back from `settings get`.
 */
fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
