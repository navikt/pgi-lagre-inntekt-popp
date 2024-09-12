package no.nav.pgi.popp.lagreinntekt

import java.util.concurrent.atomic.AtomicBoolean

class ApplicationStatus {
    private var stopped : AtomicBoolean = AtomicBoolean(false)

    fun setStopped() : ApplicationStatus {
        stopped.set(true)
        return this
    }

    fun isStopped() : Boolean {
        return stopped.get()
    }

    fun isActive() : Boolean {
        return !stopped.get()
    }
}