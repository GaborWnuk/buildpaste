package dev.gaborwnuk.buildpaste.paste

import dev.gaborwnuk.buildpaste.BuildPasteLog
import java.util.ArrayDeque

/**
 * Drives running pastes forward, a bounded number of blocks per server tick.
 *
 * The Bukkit plugin used a repeating scheduler task per paste; NeoForge has no such
 * scheduler, so jobs are held here and stepped from the end-of-tick event.
 */
object PasteScheduler {

	private val jobs = ArrayDeque<PasteJob>()

	fun submit(job: PasteJob) {
		jobs.addLast(job)
	}

	/** True while any paste is still running, so `/undopaste` can refuse to interleave. */
	val isBusy: Boolean get() = jobs.isNotEmpty()

	/**
	 * Advances every queued job by one tick's worth of blocks.
	 *
	 * The budget is spent per job rather than shared across them: two players pasting at
	 * once each get a full budget, which is how the per-paste scheduler tasks behaved.
	 */
	fun tick() {
		if (jobs.isEmpty()) return

		val iterator = jobs.iterator()
		while (iterator.hasNext()) {
			val job = iterator.next()
			val healthy = try {
				job.step(PasteJob.DEFAULT_BUDGET)
			} catch (error: Exception) {
				BuildPasteLog.warn("Paste failed and was abandoned", error)
				false
			}
			if (!healthy || job.isComplete) iterator.remove()
		}
	}

	/** Drops every queued job, for when a server stops or a world unloads. */
	fun clear() = jobs.clear()
}
