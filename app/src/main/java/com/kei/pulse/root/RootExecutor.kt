package com.kei.pulse.root

/** Something that can run one shell command as root. Production = [RootExec] over PServer; tests inject fakes. */
interface RootExecutor {
    val pServerAvailable: Boolean
    fun executeAsRoot(cmd: String): Result<String?>
}
