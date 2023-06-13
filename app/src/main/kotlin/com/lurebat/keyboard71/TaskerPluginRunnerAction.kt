package com.lurebat.keyboard71

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
import com.jormy.nin.SoftKeyboard
import com.jormy.nin.TextOp
import com.jormy.nin.TextboxEvent
import com.jormy.nin.TextboxEventType
import kotlinx.parcelize.Parcelize

@Parcelize
@TaskerInputRoot
data class ActionInput @JvmOverloads constructor(
    @field:TaskerInputField("opType") var opType: String? = null,
    @field:TaskerInputObject("textOperatio") var textOperation: TextOperation = TextOperation(),
    @field:TaskerInputObject("textEven") var textEvent: TextEvent = TextEvent(),
) : Parcelable

@Parcelize
@TaskerInputObject("textOperation")
data class TextOperation @JvmOverloads constructor(
    @field:TaskerInputField("type") var type: String? = null,
    @field:TaskerInputField("int1") var int1: Int = 0,
    @field:TaskerInputField("int2") var int2: Int = 0,
    @field:TaskerInputField("bool1") var bool1: Boolean = false,
    @field:TaskerInputField("bool2") var bool2: Boolean = false,
    @field:TaskerInputField("str1") var str1: String? = null,
) : Parcelable

@Parcelize
@TaskerInputObject("textEvent")
data class TextEvent @JvmOverloads constructor(
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
                SoftKeyboard.doTextOp(
                    TextOp.parse(
                        input.regular.textOperation.type!![0],
                        input.regular.textOperation.int1,
                        input.regular.textOperation.int2,
                        input.regular.textOperation.bool1,
                        input.regular.textOperation.bool2,
                        input.regular.textOperation.str1
                    )
                )
            }

            "TextEvent" -> {
                SoftKeyboard.doTextEvent(
                    TextboxEvent(
                        TextboxEventType.valueOf(input.regular.textEvent.type!!),
                        input.regular.textEvent.first,
                        input.regular.textEvent.second,
                        input.regular.textEvent.third
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
    val name: String,
    val int1Name: String? = null,
    val int2Name: String? = null,
    val bool1Name: String? = null,
    val bool2Name: String? = null,
    val strName: String? = null,
) : Parcelable;


val textOpActions = listOf(
    TextOpAction("C", "Command", strName = "Command"),
    TextOpAction("!", "RequestSel"),
    TextOpAction(
        "e",
        "SetSel",
        int1Name = "Start",
        int2Name = "End",
        bool1Name = "FromStart",
        bool2Name = "DontSignal"
    ),
    TextOpAction("m", "DragCursorMove", int1Name = "XMove"),
    TextOpAction("<", "SimpleBackspace", bool1Name = "Single Char Mode"),
    TextOpAction(
        "r",
        "BackReplacement",
        int1Name = "RawBackIndex",
        int2Name = "Old String Length",
        strName = "New String"
    ),
    TextOpAction("b", "BackspaceModed", strName = "String"),
    TextOpAction("l", "MarkLiquid", strName = "String"),
    TextOpAction("s", "Solidify", strName = "String"),
)

@Parcelize
data class TextEventAction(
    val type: String,
    val name: String,
    val firstName: String? = null,
    val secondName: String? = null,
    val thirdName: String? = null,
) : Parcelable;

val textEventActions = listOf(
    TextEventAction("RESET", "Reset"),
    TextEventAction(
        "SELECTION",
        "Selection",
        firstName = "Current Word",
        secondName = "Pre Text",
        thirdName = "Post Text"
    ),
    TextEventAction(
        "APPFIELDCHANGE",
        "App Field Change",
        firstName = "Package Name",
        secondName = "Field Name",
        thirdName = "Type Mode"
    ),
    TextEventAction("FIELDTYPECLASSCHANGE", "Field Type Class Change", firstName = "Type Mode"),
)

class ActionHelper(
    config: TaskerPluginConfig<ActionInput>,
    override val inputClass: Class<ActionInput> = ActionInput::class.java,
    override val runnerClass: Class<ActionRunner> = ActionRunner::class.java
) : TaskerPluginConfigHelperNoOutput<ActionInput, ActionRunner>(config)

class KeyboardActionActivity() : ComponentActivity(), TaskerPluginConfig<ActionInput> {
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
                Log.d("KeyboardActionActivity", "setContent: ${it}")
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
            mutableStateOf(input.textEvent)
        }

        val callChange = {
            inputChanged(
                ActionInput(
                    opType = opType,
                    textOperation = textOp ,
                    textEvent = textEvent,
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
                            }, onClick = { opType = "TextOp"; expanded = false; callChange()})
                            DropdownMenuItem(text = {
                                Text("TextEvent")
                            }, onClick = { opType = "TextEvent"; expanded = false; callChange()})
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
                    onFinish(ActionInput(
                        opType = opType,
                        textOperation = textOp,
                        textEvent = textEvent,
                    ))
                }) {
                    Text("Finish")
                }
            }

        }
    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEventForm(input: ActionInput, inputChanged: (t: TextEvent) -> Unit) {
    var typeOfEvent by rememberSaveable {
        mutableStateOf(input.textEvent?.type?.let { type ->
            textEventActions.find { it.type == type }
        } ?: textEventActions.first())
    }
    var first by rememberSaveable {
        mutableStateOf(input.textEvent?.first ?: "")
    }
    var second by rememberSaveable {
        mutableStateOf(input.textEvent?.second ?: "")
    }
    var third by rememberSaveable {
        mutableStateOf(input.textEvent?.third ?: "")
    }

    val callChange = {
        inputChanged(
            TextEvent(
                typeOfEvent.type,
                first,
                second,
                third,
            )
        )
    };
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
                    value = typeOfEvent.name,
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
                            Text(action.name)
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
private fun TextOpForm(input: ActionInput, inputChanged: (t: TextOperation) -> Unit) {
    var action by rememberSaveable {
        mutableStateOf(input.textOperation?.type?.let { type ->
            textOpActions.find { it.type == type }
        } ?: textOpActions.first())
    }
    var int1 by rememberSaveable {
        mutableStateOf(input.textOperation?.int1.toString())
    }
    var int2 by rememberSaveable {
        mutableStateOf(input.textOperation?.int2.toString())
    }
    var bool1 by rememberSaveable {
        mutableStateOf(input.textOperation?.bool1)
    }
    var bool2 by rememberSaveable {
        mutableStateOf(input.textOperation?.bool2)
    }
    var str by rememberSaveable {
        mutableStateOf(input.textOperation?.str1)
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

    val callChange = {
        inputChanged(
            TextOperation(
                action.type,
                int1.toIntOrNull() ?: 0,
                int2.toIntOrNull() ?: 0,
                bool1 ?: false,
                bool2 ?: false,
                str ?: "",
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
                    value = action.name,
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
                            Text(a.name)
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
                    value = int2.toString(),
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

        if (action.strName != null) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = str ?: "",
                    onValueChange = { str = it; callChange() },
                    label = { Text(action.strName!!) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
