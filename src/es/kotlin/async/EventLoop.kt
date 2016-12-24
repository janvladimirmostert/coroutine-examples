package es.kotlin.async

import es.kotlin.async.coroutine.async
import es.kotlin.lang.Disposable
import java.util.concurrent.ConcurrentLinkedDeque

object EventLoop {
	data class TimerHandler(val time: Long, val handler: () -> Unit)

	val handlers = ConcurrentLinkedDeque<() -> Unit>()
	var timerHandlers = ConcurrentLinkedDeque<TimerHandler>()
	var timerHandlersBack = ConcurrentLinkedDeque<TimerHandler>()

	fun mainAsync(routine: suspend () -> Unit): Unit {
		main {
			async(routine)
		}
	}

	fun main(entry: (() -> Unit)? = null) {
		entry?.invoke()

		while (handlers.isNotEmpty() || timerHandlers.isNotEmpty() || Thread.activeCount() > 1) {
			while (handlers.isNotEmpty()) {
				val handler = handlers.removeFirst()
				handler?.invoke()
			}
			val now = System.currentTimeMillis()
			while (timerHandlers.isNotEmpty()) {
				val handler = timerHandlers.removeFirst()
				if (now >= handler.time) {
					handler.handler()
				} else {
					timerHandlersBack.add(handler)
				}
			}
			val temp = timerHandlersBack
			timerHandlersBack = timerHandlers
			timerHandlers = temp
			Thread.sleep(1L)
		}
	}

	fun setImmediate(handler: () -> Unit) {
		handlers += handler
	}

	fun setTimeout(time: Int, callback: () -> Unit): Disposable {
		val handler = TimerHandler(System.currentTimeMillis() + time, callback)
		timerHandlers.add(handler)
		return object : Disposable {
			override fun dispose(): Unit = run { timerHandlers.remove(handler) }
		}
	}
}
