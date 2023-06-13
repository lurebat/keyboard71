package com.lurebat.keyboard71

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputInfos
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied

@TaskerInputRoot
class OnWordInput @JvmOverloads constructor(@field:TaskerInputField("wordInput") var wordInput: String? = null)
@TaskerInputRoot
class OnWordUpdate @JvmOverloads constructor(@field:TaskerInputField("wordUpdate") var wordUpdate: String? = null)

@TaskerOutputObject()
class OnWordOutput(@get:TaskerOutputVariable("wordOutput") val wordOutput: String) {

}

class OnWordRunner : TaskerPluginRunnerConditionEvent<OnWordInput, OnWordOutput, OnWordUpdate>() {
    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<OnWordInput>,
        update: OnWordUpdate?
    ): TaskerPluginResultCondition<OnWordOutput> {
        if ((input.regular.wordInput.isNullOrEmpty() || input.regular.wordInput == update?.wordUpdate) && !update?.wordUpdate.isNullOrEmpty() ) {
            return TaskerPluginResultConditionSatisfied(context, OnWordOutput(update?.wordUpdate ?: ""))
        }
        return TaskerPluginResultConditionUnsatisfied()
    }
}

class BasicEventHelper(
    config: TaskerPluginConfig<OnWordInput>,
    override val inputClass: Class<OnWordInput> = OnWordInput::class.java,
    override val outputClass: Class<OnWordOutput> = OnWordOutput::class.java,
    override val runnerClass: Class<OnWordRunner> = OnWordRunner::class.java
) :
    TaskerPluginConfigHelper<OnWordInput, OnWordOutput, OnWordRunner>(config)

class KeyboardOnWordActivity : Activity(), TaskerPluginConfig<OnWordInput> {

    override val context get() = applicationContext
    override var inputForTasker: TaskerInput<OnWordInput> = TaskerInput(OnWordInput())

    override fun assignFromInput(input: TaskerInput<OnWordInput>) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BasicEventHelper(this).apply {
        }.finishForTasker()
    }

}

fun Context.triggerBasicTaskerEvent(string: String) =
    KeyboardOnWordActivity::class.java.requestQuery(this, OnWordUpdate(string))
