package no.nav.pgi.popp.lagreinntekt

import java.util.concurrent.atomic.AtomicBoolean

class ApplicationStatus {
    private var started : AtomicBoolean = AtomicBoolean(false)
    private var stopped : AtomicBoolean = AtomicBoolean(false)

    fun setStarted() : ApplicationStatus {
        started.set(true)
        return this
    }

    fun setStopped() : ApplicationStatus {
        stopped.set(true)
        return this
    }

    fun isStopped() : Boolean {
        return stopped.get()
    }
}