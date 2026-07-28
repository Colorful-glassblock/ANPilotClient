package anpilot.client.features.ai.scheduler

import anpilot.client.features.ai.agent.ANAgent
import anpilot.client.features.ai.task.AITask
import java.util.ArrayDeque

class ANTaskScheduler(private val agent: ANAgent) {
    private val stack = ArrayDeque<AITask>()

    fun push(task: AITask) {
        stack.push(task)
        task.start()
    }

    fun tick() {
        val task = stack.peek() ?: return
        task.tick()
        if (task.finished) {
            task.stop()
            if (stack.peek() == task) {
                stack.pop()
            } else {
                stack.remove(task)
            }
        }
    }

    fun stop() {
        while (stack.isNotEmpty()) {
            stack.pop().stop()
        }
    }

    fun current(): AITask? = stack.peek()
}
