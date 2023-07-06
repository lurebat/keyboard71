package com.lurebat.keyboard71.tasker

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.joaomgcd.taskerpluginlibrary.condition.TaskerPluginRunnerConditionEvent
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.extensions.requestQuery
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultCondition
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionSatisfied
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultConditionUnsatisfied

const val COMPARE_METHOD_FULL = "full"
const val COMPARE_METHOD_STARTS_WITH = "startsWith"
const val COMPARE_METHOD_ENDS_WITH = "endsWith"
const val COMPARE_METHOD_REGEX = "regex"

@TaskerInputRoot
class OnWordInput @JvmOverloads constructor(
    @field:TaskerInputField("wordInput") var wordInput: String? = null,
    @field:TaskerInputField("compareMethod") var compareMethod: String? = null,
    @field:TaskerInputField("shouldOutputText") var shouldOutputText: Boolean = false
)

@TaskerInputRoot
class OnWordUpdate @JvmOverloads constructor(@field:TaskerInputField("keyboardTextOutput") var                                                        keyboardTextOutput: String = "",
                                                 @field:TaskerInputField("keyboardTextBefore") var keyboardTextBefore: String = "",
                                                 @field:TaskerInputField("keyboardTextAfter") var keyboardTextAfter: String = "") {
    fun toOutput() = OnWordOutput(keyboardTextOutput, keyboardTextBefore, keyboardTextAfter)
}

@TaskerOutputObject()
class OnWordOutput @JvmOverloads constructor(
    @get:TaskerOutputVariable("keyboardTextOutput") var keyboardTextOutput: String = "",
    @get:TaskerOutputVariable("keyboardTextBefore") var keyboardTextBefore: String = "",
    @get:TaskerOutputVariable("keyboardTextAfter") var keyboardTextAfter: String = ""
    ) {

}

class OnWordRunner : TaskerPluginRunnerConditionEvent<OnWordInput, OnWordOutput, OnWordUpdate>() {
    override fun getSatisfiedCondition(
        context: Context,
        input: TaskerInput<OnWordInput>,
        update: OnWordUpdate?
    ): TaskerPluginResultCondition<OnWordOutput> {
        val wordInput = input.regular.wordInput
        val text = (update?.keyboardTextOutput ?: "").trim()
        if (wordInput.isNullOrEmpty()) {
            return TaskerPluginResultConditionSatisfied(context, update?.toOutput() ?: OnWordOutput())
        }

        Log.e("OnWordRunner", "$wordInput, ${update?.keyboardTextOutput} ${update?.keyboardTextBefore} ${update?.keyboardTextAfter}");

        val compareFun =
            when (input.regular.compareMethod) {
                COMPARE_METHOD_STARTS_WITH -> { word: String -> word.startsWith(wordInput) }
                COMPARE_METHOD_ENDS_WITH -> { word: String -> word.endsWith(wordInput) }
                COMPARE_METHOD_REGEX -> { word: String -> word.matches(wordInput.toRegex()) }
                else -> { word: String -> word == wordInput }
            }

        if (!compareFun(text)) {
            return TaskerPluginResultConditionUnsatisfied()
        }

        return TaskerPluginResultConditionSatisfied(context, update?.toOutput() ?: OnWordOutput())
    }
}

class BasicEventHelper(
    config: TaskerPluginConfig<OnWordInput>,
    override val inputClass: Class<OnWordInput> = OnWordInput::class.java,
    override val outputClass: Class<OnWordOutput> = OnWordOutput::class.java,
    override val runnerClass: Class<OnWordRunner> = OnWordRunner::class.java
) :
    TaskerPluginConfigHelper<OnWordInput, OnWordOutput, OnWordRunner>(config)

class KeyboardOnWordActivity : ComponentActivity(), TaskerPluginConfig<OnWordInput> {
    override val context get() = applicationContext
    override var inputForTasker: TaskerInput<OnWordInput> = TaskerInput(OnWordInput())

    override fun assignFromInput(input: TaskerInput<OnWordInput>) {
        inputForTasker = input
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            InputForm(inputForTasker.regular, { BasicEventHelper(this).finishForTasker() }) {
                assignFromInput(TaskerInput(it))
            }
        }

    }


    @Preview
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun InputForm(
        input: OnWordInput = OnWordInput(),
        onFinish: () -> Unit = {},
        inputChanged: (OnWordInput) -> Unit = {}
    ) {
        Box {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var word by rememberSaveable { mutableStateOf(input.wordInput ?: "") }
                var compareMethod by rememberSaveable {
                    mutableStateOf(
                        input.compareMethod ?: COMPARE_METHOD_FULL
                    )
                }
                var shouldOutputText by rememberSaveable { mutableStateOf(input.shouldOutputText) }

                fun inputChanged() =
                    inputChanged(OnWordInput(word, compareMethod, shouldOutputText))

                TextField(
                    value = word,
                    onValueChange = { word = it; inputChanged() },
                    label = { Text("Word") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Compare Method")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        COMPARE_METHOD_FULL to "Full",
                        COMPARE_METHOD_STARTS_WITH to "Starts With",
                        COMPARE_METHOD_ENDS_WITH to "Ends With",
                        COMPARE_METHOD_REGEX to "Regex"
                    ).forEach { (method, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = compareMethod == method,
                                onClick = { compareMethod = method;inputChanged() }
                            )
                            Text(text = label)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Should output text?")
                    Checkbox(
                        checked = shouldOutputText,
                        onCheckedChange = { shouldOutputText = it; inputChanged() }
                    )
                }

                // ok button
                Button(
                    onClick = { onFinish() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send")
                }
            }
        }
    }

}

fun Context.triggerBasicTaskerEvent(string: String, before: String, after: String) {
    KeyboardOnWordActivity::class.java.requestQuery(this, OnWordUpdate(string, before, after))
}
