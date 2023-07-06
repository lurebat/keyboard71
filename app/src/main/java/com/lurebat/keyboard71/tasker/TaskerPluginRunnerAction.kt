@file:Suppress("USELESS_ELVIS")

package com.lurebat.keyboard71.tasker

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputObject
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.lurebat.keyboard71.TextBoxEvent
import com.lurebat.keyboard71.TextEventType
import com.lurebat.keyboard71.TextOp
import com.lurebat.keyboard71.TextOpType
import kotlinx.parcelize.Parcelize

@Parcelize
@TaskerInputRoot
data class ActionInput @JvmOverloads constructor(
    @field:TaskerInputField("opType") var opType: String? = null,
    @field:TaskerInputObject("textOperation") var textOperation: TaskerTextOperation = TaskerTextOperation(),
    @field:TaskerInputObject("textEven") var taskerTextEvent: TaskerTextEvent = TaskerTextEvent(),
) : Parcelable

@Parcelize
@TaskerInputObject("textOperation")
data class TaskerTextOperation @JvmOverloads constructor(
    @field:TaskerInputField("type") var type: String? = null,
    @field:TaskerInputField("int1") var int1: Int = 0,
    @field:TaskerInputField("int2") var int2: Int = 0,
    @field:TaskerInputField("bool1") var bool1: Boolean = false,
    @field:TaskerInputField("bool2") var bool2: Boolean = false,
    @field:TaskerInputField("str1") var str1: String? = null,
    @field:TaskerInputField("str2") var str2: String? = null,
) : Parcelable

@Parcelize
@TaskerInputObject("textEvent")
data class TaskerTextEvent @JvmOverloads constructor(
    @field:TaskerInputField("type") var type: String? = null,
    @field:TaskerInputField("first") var first: String? = null,
    @field:TaskerInputField("second") var second: String? = null,
    @field:TaskerInputField("third") var third: String? = null,
) : Parcelable

class ActionRunner : TaskerPluginRunnerActionNoOutput<ActionInput>() {
    override fun run(context: Context, input: TaskerInput<ActionInput>): TaskerPluginResult<Unit> {
        Log.d("ActionRunner", "run: ${input.regular}")
        when (input.regular.opType) {
            "TextOp" -> {
                com.lurebat.keyboard71.SoftKeyboard.doTextOp(
                    TextOp.parse(
                        TextOpType.valueOf(input.regular.textOperation.type!!),
                        input.regular.textOperation.int1,
                        input.regular.textOperation.int2,
                        input.regular.textOperation.bool1,
                        input.regular.textOperation.bool2,
                        input.regular.textOperation.str1,
                        input.regular.textOperation.str2
                    )
                )
            }

            "TextEvent" -> {
                com.lurebat.keyboard71.SoftKeyboard.doTextEvent(
                    TextBoxEvent.fromType(
                        TextEventType.valueOf(input.regular.taskerTextEvent.type!!),
                        input.regular.taskerTextEvent.first,
                        input.regular.taskerTextEvent.second,
                        input.regular.taskerTextEvent.third
                    )
                )
            }
        }

        return TaskerPluginResultSucess()
    }
}

@Parcelize
class TextOpAction(
    val type: String,
    val int1Name: String? = null,
    val int2Name: String? = null,
    val bool1Name: String? = null,
    val bool2Name: String? = null,
    val str1Name: String? = null,
    val str2Name: String? = null,
) : Parcelable


val textOpActions = listOf(
    TextOpAction(TextOpType.MU_COMMAND.name, str1Name = "Command"),
    TextOpAction(TextOpType.REQUEST_SELECTION.name),
    TextOpAction(
        TextOpType.SET_SELECTION.name,
        int1Name = "Start",
        int2Name = "End",
        bool1Name = "FromStart",
        bool2Name = "DontSignal"
    ),
    TextOpAction(TextOpType.DRAG_CURSOR_MOVE.name, int1Name = "XMove"),
    TextOpAction(TextOpType.SIMPLE_BACKSPACE.name, bool1Name = "Single Char Mode"),
    TextOpAction(
        TextOpType.BACKSPACE_REPLACEMENT.name,
        int1Name = "RawBackIndex",
        str1Name = "Old String",
        str2Name = "New String"
    ),
    TextOpAction(TextOpType.BACKSPACE_MODED.name, str1Name = "String"),
    TextOpAction(TextOpType.MARK_LIQUID.name, str1Name = "String"),
    TextOpAction(TextOpType.SOLIDIFY.name, str1Name = "String"),
)

@Parcelize
data class TextEventAction(
    val type: String,
    val firstName: String? = null,
    val secondName: String? = null,
    val thirdName: String? = null,
) : Parcelable

val textEventActions = listOf(
    TextEventAction(TextEventType.RESET.name),
    TextEventAction(
        TextEventType.SELECTION.name,
        firstName = "Current Word",
        secondName = "Pre Text",
        thirdName = "Post Text"
    ),
    TextEventAction(
        TextEventType.APP_FIELD_CHANGE.name,
        firstName = "Package Name",
        secondName = "Field Name",
        thirdName = "Type Mode"
    ),
    TextEventAction(
        TextEventType.WORD_DESTRUCTION.name,
        firstName = "Destroyed Word",
        secondName = "Destroyed String",
    ),
)

class ActionHelper(
    config: TaskerPluginConfig<ActionInput>,
    override val inputClass: Class<ActionInput> = ActionInput::class.java,
    override val runnerClass: Class<ActionRunner> = ActionRunner::class.java
) : TaskerPluginConfigHelperNoOutput<ActionInput, ActionRunner>(config)

class KeyboardActionActivity : ComponentActivity(), TaskerPluginConfig<ActionInput> {
    override val inputForTasker: TaskerInput<ActionInput> get() = TaskerInput(input)
    var input = ActionInput()

    override val context: Context get() = applicationContext
    override fun assignFromInput(input: TaskerInput<ActionInput>) {
        this.input = input.regular
    }

    private val taskerHelper by lazy { ActionHelper(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ActionInputForm(inputForTasker.regular, { taskerHelper.finishForTasker() }) {
                Log.d("KeyboardActionActivity", "setContent: $it")
                input = it
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Preview
    @Composable
    private fun ActionInputForm(
        input: ActionInput = ActionInput(),
        onFinish: (ActionInput) -> Unit = {},
        inputChanged: (ActionInput) -> Unit = {}
    ) {
        var opType by rememberSaveable {
            mutableStateOf(input.opType ?: "TextOp")
        }
        var textOp by rememberSaveable {
            mutableStateOf(input.textOperation)
        }

        var textEvent by rememberSaveable {
            mutableStateOf(input.taskerTextEvent)
        }

        val callChange = {
            inputChanged(
                ActionInput(
                    opType = opType,
                    textOperation = textOp,
                    taskerTextEvent = textEvent,
                )
            )
        }
        var expanded by rememberSaveable { mutableStateOf(false) }



        Box {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = opType,
                            onValueChange = { opType = it },
                            label = { Text("Operation") },
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = {
                                expanded = false
                            }
                        ) {
                            DropdownMenuItem(text = {
                                Text("TextOp")
                            }, onClick = { opType = "TextOp"; expanded = false; callChange() })
                            DropdownMenuItem(text = {
                                Text("TextEvent")
                            }, onClick = { opType = "TextEvent"; expanded = false; callChange() })
                        }
                    }
                }
                if (opType == "TextOp") {
                    TextOpForm(input = input, inputChanged = {
                        textOp = it
                        callChange()
                    })
                } else {
                    TextEventForm(input = input, inputChanged = {
                        textEvent = it
                        callChange()
                    })
                }

                Button(onClick = {
                    onFinish(
                        ActionInput(
                            opType = opType,
                            textOperation = textOp,
                            taskerTextEvent = textEvent,
                        )
                    )
                }) {
                    Text("Finish")
                }
            }

        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEventForm(input: ActionInput, inputChanged: (t: TaskerTextEvent) -> Unit) {
    var typeOfEvent by rememberSaveable {
        mutableStateOf(input.taskerTextEvent.type?.let { type ->
            textEventActions.find { it.type == type }
        } ?: textEventActions.first())
    }
    var first by rememberSaveable {
        mutableStateOf(input.taskerTextEvent.first ?: "")
    }
    var second by rememberSaveable {
        mutableStateOf(input.taskerTextEvent.second ?: "")
    }
    var third by rememberSaveable {
        mutableStateOf(input.taskerTextEvent.third ?: "")
    }

    val callChange = {
        inputChanged(
            TaskerTextEvent(
                typeOfEvent.type,
                first,
                second,
                third,
            )
        )
    }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = typeOfEvent.type,
                    onValueChange = { },
                    label = { Text("Type") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    textEventActions.forEach { action ->
                        DropdownMenuItem(text = {
                            Text(action.type)
                        }, onClick = { typeOfEvent = action; expanded = false; callChange() })
                    }

                }
            }
        }
        if (typeOfEvent.firstName != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = first,
                    onValueChange = { first = it; callChange() },
                    label = { Text(typeOfEvent.firstName!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (typeOfEvent.secondName != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = second,
                    onValueChange = { second = it; callChange() },
                    label = { Text(typeOfEvent.secondName!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (typeOfEvent.thirdName != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = third,
                    onValueChange = { third = it; callChange() },
                    label = { Text(typeOfEvent.thirdName!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextOpForm(input: ActionInput, inputChanged: (t: TaskerTextOperation) -> Unit) {
    var action by rememberSaveable {
        mutableStateOf(input.textOperation.type?.let { type ->
            textOpActions.find { it.type == type }
        } ?: textOpActions.first())
    }
    var int1 by rememberSaveable {
        mutableStateOf(input.textOperation.int1.toString())
    }
    var int2 by rememberSaveable {
        mutableStateOf(input.textOperation.int2.toString())
    }
    var bool1 by rememberSaveable {
        mutableStateOf(input.textOperation.bool1)
    }
    var bool2 by rememberSaveable {
        mutableStateOf(input.textOperation.bool2)
    }
    var str by rememberSaveable {
        mutableStateOf(input.textOperation.str1)
    }
    var str2 by rememberSaveable {
        mutableStateOf(input.textOperation.str2)
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

    val callChange = {
        inputChanged(
            TaskerTextOperation(
                action.type,
                int1.toIntOrNull() ?: 0,
                int2.toIntOrNull() ?: 0,
                bool1 ?: false,
                bool2 ?: false,
                str ?: "",
                str2 ?: "",

            )
        )
    }

    Column {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = action.type,
                    onValueChange = { },
                    label = { Text("Type") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = false
                        )
                    },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    textOpActions.forEach { a ->
                        DropdownMenuItem(text = {
                            Text(a.type)
                        }, onClick = { action = a; expanded = false;callChange() })
                    }
                }
            }
        }
        if (action.int1Name != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = (int1 ?: 0).toString(),
                    onValueChange = { int1 = it; callChange() },
                    label = { Text(action.int1Name!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (action.int2Name != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = int2,
                    onValueChange = { int2 = it; callChange() },
                    label = { Text(action.int2Name!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (action.bool1Name != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action.bool1Name!!)
                Checkbox(
                    checked = bool1 ?: false,
                    onCheckedChange = { bool1 = it; callChange() },

                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (action.bool2Name != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action.bool2Name!!)
                Checkbox(
                    checked = bool2 ?: false,
                    onCheckedChange = { bool2 = it; callChange() },

                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (action.str1Name != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = str ?: "",
                    onValueChange = { str = it; callChange() },
                    label = { Text(action.str1Name!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (action.str2Name != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = str2 ?: "",
                    onValueChange = { str2 = it; callChange() },
                    label = { Text(action.str2Name!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
