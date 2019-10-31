package com.phelat.poolakey.thread

internal interface PoolakeyThread<TaskType> {

    fun execute(task: TaskType)

    fun dispose()

}
